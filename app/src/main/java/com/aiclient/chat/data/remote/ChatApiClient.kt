package com.aiclient.chat.data.remote

import com.aiclient.chat.data.model.AiProvider
import com.aiclient.chat.data.model.AuthScheme
import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.ProviderKind
import com.aiclient.chat.data.model.Role
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
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

/**
 * Speaks either the Anthropic Messages API shape (used directly, by Vertex AI,
 * and by Azure AI Foundry's Claude deployments) or the OpenAI Chat Completions
 * shape (OpenAI, DeepSeek, and most other/self-hosted model servers),
 * depending on the [AiProvider]'s [ProviderKind].
 */
class ChatApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // streaming: no read timeout
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Streams a chat completion from [provider]'s API using server-sent
     * events. Cancelling the collecting coroutine aborts the underlying HTTP
     * call.
     */
    fun streamMessage(
        provider: AiProvider,
        apiKey: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        maxTokens: Int = 4096,
    ): Flow<StreamEvent> = callbackFlow {
        val request = buildRequest(provider, apiKey, systemPrompt, history, maxTokens)
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
                    trySend(StreamEvent.Failed("Received an empty response from the server."))
                    close()
                    return@use
                }
                readEventStream(source, isActive = { isActive }) { event, data ->
                    when (provider.kind) {
                        ProviderKind.ANTHROPIC, ProviderKind.VERTEX_ANTHROPIC -> handleAnthropicEvent(event, data) { trySend(it) }
                        ProviderKind.OPENAI_COMPATIBLE -> handleOpenAiEvent(data) { trySend(it) }
                    }
                }
                trySend(StreamEvent.Done)
                close()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (io: IOException) {
            trySend(StreamEvent.Failed("Connection error: ${io.message ?: ""}"))
            close()
        } catch (e: Exception) {
            trySend(StreamEvent.Failed(e.message ?: "Unknown error"))
            close()
        }

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun handleAnthropicEvent(event: String, data: String, send: (StreamEvent) -> Unit) {
        when (event) {
            "content_block_delta" -> {
                val delta = runCatching { Json.parseToJsonElement(data).jsonObject["delta"]?.jsonObject }.getOrNull()
                val text = delta?.get("text")?.jsonPrimitive?.contentOrNull
                if (text != null) send(StreamEvent.TextDelta(text))
            }
            "message_stop" -> send(StreamEvent.Done)
            "error" -> {
                val message = runCatching {
                    Json.parseToJsonElement(data).jsonObject["error"]?.jsonObject
                        ?.get("message")?.jsonPrimitive?.contentOrNull
                }.getOrNull() ?: "Unknown server error"
                send(StreamEvent.Failed(message))
            }
        }
    }

    private fun handleOpenAiEvent(data: String, send: (StreamEvent) -> Unit) {
        val trimmed = data.trim()
        if (trimmed == "[DONE]") {
            send(StreamEvent.Done)
            return
        }
        val delta = runCatching {
            Json.parseToJsonElement(trimmed).jsonObject["choices"]
                ?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("delta")?.jsonObject
                ?.get("content")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        if (!delta.isNullOrEmpty()) send(StreamEvent.TextDelta(delta))
    }

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
                ?.get("message")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        return when {
            fromServer != null -> fromServer
            code == 401 || code == 403 -> "Invalid API key or credentials."
            code == 429 -> "Too many requests. Please wait a moment and try again."
            else -> "Server error ($code)"
        }
    }

    private fun buildRequest(
        provider: AiProvider,
        apiKey: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        maxTokens: Int,
    ): Request {
        val bodyJson = when (provider.kind) {
            ProviderKind.ANTHROPIC -> buildAnthropicBody(provider.model, systemPrompt, history, maxTokens, includeModelField = true)
            ProviderKind.VERTEX_ANTHROPIC -> buildAnthropicBody(
                provider.model, systemPrompt, history, maxTokens,
                includeModelField = false,
                bodyAnthropicVersion = "vertex-2023-10-16",
            )
            ProviderKind.OPENAI_COMPATIBLE -> buildOpenAiBody(provider.model, systemPrompt, history, maxTokens)
        }

        val url = when (provider.kind) {
            ProviderKind.ANTHROPIC, ProviderKind.VERTEX_ANTHROPIC -> provider.baseUrl
            ProviderKind.OPENAI_COMPATIBLE -> provider.baseUrl.trimEnd('/') + "/chat/completions"
        }

        val builder = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
            .addHeader("accept", "text/event-stream")
            .post(Json.encodeToString(JsonObject.serializer(), bodyJson).toRequestBody(jsonMediaType))

        if (provider.kind == ProviderKind.ANTHROPIC) {
            builder.addHeader("anthropic-version", ANTHROPIC_VERSION)
        }
        if (apiKey.isNotBlank()) {
            val headerValue = if (provider.authScheme == AuthScheme.BEARER) "Bearer $apiKey" else apiKey
            builder.addHeader(provider.authHeaderName, headerValue)
        }

        return builder.build()
    }

    private fun buildAnthropicBody(
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        maxTokens: Int,
        includeModelField: Boolean,
        bodyAnthropicVersion: String? = null,
    ): JsonObject = buildJsonObject {
        if (includeModelField) put("model", model)
        if (bodyAnthropicVersion != null) put("anthropic_version", bodyAnthropicVersion)
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

    private fun buildOpenAiBody(
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>,
        maxTokens: Int,
    ): JsonObject = buildJsonObject {
        put("model", model)
        put("stream", true)
        put("max_tokens", maxTokens)
        putJsonArray("messages") {
            if (systemPrompt.isNotBlank()) {
                pushJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
            }
            for (message in history) {
                if (message.role == Role.SYSTEM) continue
                pushJsonObject {
                    put("role", if (message.role == Role.USER) "user" else "assistant")
                    if (message.images.isEmpty()) {
                        put("content", message.text)
                    } else {
                        putJsonArray("content") {
                            if (message.text.isNotBlank()) {
                                pushJsonObject {
                                    put("type", "text")
                                    put("text", message.text)
                                }
                            }
                            for (image in message.images) {
                                pushJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") { put("url", image) }
                                }
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
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
}

private inline fun kotlinx.serialization.json.JsonArrayBuilder.pushJsonObject(
    builderAction: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
) {
    add(buildJsonObject(builderAction))
}
