package com.aiclient.chat.data.remote

import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data object Started : StreamEvent
    data object Done : StreamEvent
    data class Failed(val message: String, val isAuthError: Boolean = false) : StreamEvent
}

class AnthropicApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // streaming: no read timeout
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Streams a chat completion from the Anthropic Messages API using
     * server-sent events. Cancelling the collecting coroutine aborts the
     * underlying HTTP call.
     */
    fun streamMessage(
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        maxTokens: Int = 4096,
    ): Flow<StreamEvent> = callbackFlow {
        val body = buildRequestBody(model, systemPrompt, history, maxTokens)
        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .addHeader("content-type", "application/json")
            .addHeader("accept", "text/event-stream")
            .post(Json.encodeToString(JsonObject.serializer(), body).toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(request)

        try {
            val response = call.execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val errText = resp.body?.string().orEmpty()
                    val isAuth = resp.code == 401 || resp.code == 403
                    trySend(StreamEvent.Failed(extractErrorMessage(errText, resp.code), isAuth))
                    close()
                    return@use
                }
                trySend(StreamEvent.Started)
                val source = resp.body?.source()
                if (source == null) {
                    trySend(StreamEvent.Failed("پاسخ خالی از سرور دریافت شد."))
                    close()
                    return@use
                }
                readEventStream(source, isActive = { isActive }) { event, data ->
                    when (event) {
                        "content_block_delta" -> {
                            val delta = runCatching { Json.parseToJsonElement(data).jsonObject["delta"]?.jsonObject }.getOrNull()
                            val text = delta?.get("text")?.jsonPrimitive?.content
                            if (text != null) trySend(StreamEvent.TextDelta(text))
                        }
                        "message_stop" -> {
                            trySend(StreamEvent.Done)
                        }
                        "error" -> {
                            val message = runCatching {
                                Json.parseToJsonElement(data).jsonObject["error"]?.jsonObject
                                    ?.get("message")?.jsonPrimitive?.content
                            }.getOrNull() ?: "خطای ناشناخته از سرور"
                            trySend(StreamEvent.Failed(message))
                        }
                    }
                }
                trySend(StreamEvent.Done)
                close()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (io: IOException) {
            trySend(StreamEvent.Failed("خطا در اتصال به سرور: ${io.message ?: ""}"))
            close()
        } catch (e: Exception) {
            trySend(StreamEvent.Failed(e.message ?: "خطای ناشناخته"))
            close()
        }

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    /** Reads an SSE stream, invoking [onEvent] for every `event: / data:` pair. Stops early if [isActive] turns false (e.g. the caller cancelled generation). */
    private fun readEventStream(source: BufferedSource, isActive: () -> Boolean, onEvent: (event: String, data: String) -> Unit) {
        var currentEvent = "message"
        val dataBuilder = StringBuilder()

        fun flush() {
            if (dataBuilder.isNotEmpty()) {
                onEvent(currentEvent, dataBuilder.toString())
            }
            dataBuilder.setLength(0)
            currentEvent = "message"
        }

        while (isActive() && !source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            when {
                line.isEmpty() -> flush()
                line.startsWith("event:") -> currentEvent = line.removePrefix("event:").trim()
                line.startsWith("data:") -> {
                    if (dataBuilder.isNotEmpty()) dataBuilder.append('\n')
                    dataBuilder.append(line.removePrefix("data:").trim())
                }
                else -> Unit
            }
        }
        flush()
    }

    private fun extractErrorMessage(body: String, code: Int): String {
        val fromServer = runCatching {
            Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject
                ?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return when {
            fromServer != null -> fromServer
            code == 401 -> "کلید API نامعتبر است."
            code == 429 -> "تعداد درخواست‌ها بیش از حد مجاز است. کمی صبر کنید."
            else -> "خطای سرور ($code)"
        }
    }

    private fun buildRequestBody(
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        maxTokens: Int,
    ): JsonObject = buildJsonObject {
        put("model", model)
        put("max_tokens", maxTokens)
        put("stream", true)
        if (systemPrompt.isNotBlank()) put("system", systemPrompt)
        putJsonArray("messages") {
            for (message in history) {
                if (message.role == Role.SYSTEM) continue
                pushJsonObject {
                    put("role", if (message.role == Role.USER) "user" else "assistant")
                    putJsonArray("content") {
                        for (image in message.images) {
                            val (mediaType, data) = parseDataUri(image) ?: continue
                            pushJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", mediaType)
                                    put("data", data)
                                }
                            }
                        }
                        if (message.text.isNotBlank()) {
                            pushJsonObject {
                                put("type", "text")
                                put("text", message.text)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseDataUri(dataUri: String): Pair<String, String>? {
        // data:image/png;base64,AAAA...
        if (!dataUri.startsWith("data:")) return null
        val comma = dataUri.indexOf(',')
        if (comma == -1) return null
        val header = dataUri.substring(5, comma) // image/png;base64
        val mediaType = header.substringBefore(';')
        val data = dataUri.substring(comma + 1)
        return mediaType to data
    }

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
}

private inline fun kotlinx.serialization.json.JsonArrayBuilder.pushJsonObject(
    builderAction: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
) {
    add(buildJsonObject(builderAction))
}
