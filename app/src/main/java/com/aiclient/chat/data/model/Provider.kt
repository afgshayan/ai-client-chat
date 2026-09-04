package com.aiclient.chat.data.model

/** How a provider's HTTP API is shaped, so the client knows which request/response format to speak. */
enum class ProviderKind {
    /** Anthropic's native Messages API shape (used directly, and by Anthropic-compatible gateways). */
    ANTHROPIC,

    /** Anthropic models served through Google Cloud Vertex AI — same Messages shape, minor body differences. */
    VERTEX_ANTHROPIC,

    /** The OpenAI Chat Completions shape, used by OpenAI itself and most third-party/self-hosted models. */
    OPENAI_COMPATIBLE,
}

enum class AuthScheme { BEARER, RAW }

/** A configured connection to an AI backend: base URL, auth, and the model to talk to. */
data class AiProvider(
    val id: String,
    val name: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val authHeaderName: String,
    val authScheme: AuthScheme,
    val model: String,
)

/** A starting point offered when adding a provider — the user still supplies their own key and can edit everything. */
data class ProviderPreset(
    val presetId: String,
    val displayName: String,
    val description: String,
    val kind: ProviderKind,
    val baseUrlTemplate: String,
    val baseUrlHint: String,
    val authHeaderName: String,
    val authScheme: AuthScheme,
    val keyLabel: String,
    val keyHint: String,
    val defaultModel: String,
    val modelSuggestions: List<String>,
)

object ProviderPresets {

    val ANTHROPIC_DIRECT = ProviderPreset(
        presetId = "anthropic",
        displayName = "Anthropic",
        description = "Claude models, direct from Anthropic's API",
        kind = ProviderKind.ANTHROPIC,
        baseUrlTemplate = "https://api.anthropic.com/v1/messages",
        baseUrlHint = "",
        authHeaderName = "x-api-key",
        authScheme = AuthScheme.RAW,
        keyLabel = "API key",
        keyHint = "You can get a key from console.anthropic.com.",
        defaultModel = "claude-sonnet-5",
        modelSuggestions = listOf("claude-opus-5", "claude-sonnet-5", "claude-fable-5-1", "claude-haiku-4-5-20251001"),
    )

    val GOOGLE_VERTEX = ProviderPreset(
        presetId = "vertex",
        displayName = "Google Vertex AI",
        description = "Claude models through your Google Cloud project",
        kind = ProviderKind.VERTEX_ANTHROPIC,
        baseUrlTemplate = "https://{REGION}-aiplatform.googleapis.com/v1/projects/{PROJECT_ID}/locations/{REGION}/publishers/anthropic/models/{MODEL}:streamRawPredict",
        baseUrlHint = "Replace {REGION} (e.g. us-east5), {PROJECT_ID}, and {MODEL} (e.g. claude-sonnet-5) with your Vertex AI values.",
        authHeaderName = "Authorization",
        authScheme = AuthScheme.BEARER,
        keyLabel = "Access token",
        keyHint = "Vertex needs a short-lived OAuth access token, e.g. from `gcloud auth print-access-token` — you'll need to refresh it here roughly every hour.",
        defaultModel = "claude-sonnet-5",
        modelSuggestions = listOf("claude-opus-5", "claude-sonnet-5"),
    )

    val AZURE_FOUNDRY = ProviderPreset(
        presetId = "azure_foundry",
        displayName = "Azure AI Foundry",
        description = "Claude models deployed in your Azure AI Foundry resource",
        kind = ProviderKind.ANTHROPIC,
        baseUrlTemplate = "https://{RESOURCE_NAME}.services.ai.azure.com/anthropic/v1/messages",
        baseUrlHint = "Replace {RESOURCE_NAME} with your Azure AI Foundry resource name.",
        authHeaderName = "api-key",
        authScheme = AuthScheme.RAW,
        keyLabel = "API key",
        keyHint = "Found on your deployment's page in the Azure AI Foundry portal.",
        defaultModel = "claude-sonnet-5",
        modelSuggestions = listOf("claude-opus-5", "claude-sonnet-5"),
    )

    val DEEPSEEK = ProviderPreset(
        presetId = "deepseek",
        displayName = "DeepSeek",
        description = "DeepSeek's own API",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        baseUrlTemplate = "https://api.deepseek.com",
        baseUrlHint = "",
        authHeaderName = "Authorization",
        authScheme = AuthScheme.BEARER,
        keyLabel = "API key",
        keyHint = "You can get a key from platform.deepseek.com.",
        defaultModel = "deepseek-chat",
        modelSuggestions = listOf("deepseek-chat", "deepseek-reasoner"),
    )

    val OPENAI = ProviderPreset(
        presetId = "openai",
        displayName = "OpenAI",
        description = "GPT models, direct from OpenAI",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        baseUrlTemplate = "https://api.openai.com/v1",
        baseUrlHint = "",
        authHeaderName = "Authorization",
        authScheme = AuthScheme.BEARER,
        keyLabel = "API key",
        keyHint = "You can get a key from platform.openai.com.",
        defaultModel = "gpt-4o-mini",
        modelSuggestions = listOf("gpt-4o", "gpt-4o-mini", "o3-mini"),
    )

    val CUSTOM_ANTHROPIC = ProviderPreset(
        presetId = "custom_anthropic",
        displayName = "Custom (Anthropic-compatible)",
        description = "Any endpoint that speaks the Anthropic Messages API — self-hosted gateways, proxies, etc.",
        kind = ProviderKind.ANTHROPIC,
        baseUrlTemplate = "",
        baseUrlHint = "The full URL of the /v1/messages-style endpoint.",
        authHeaderName = "x-api-key",
        authScheme = AuthScheme.RAW,
        keyLabel = "API key",
        keyHint = "",
        defaultModel = "",
        modelSuggestions = emptyList(),
    )

    val CUSTOM_OPENAI = ProviderPreset(
        presetId = "custom_openai",
        displayName = "Custom (OpenAI-compatible)",
        description = "Any endpoint that speaks the OpenAI Chat Completions API — local servers, other model hosts, etc.",
        kind = ProviderKind.OPENAI_COMPATIBLE,
        baseUrlTemplate = "",
        baseUrlHint = "The API root, without /chat/completions (e.g. http://localhost:11434/v1).",
        authHeaderName = "Authorization",
        authScheme = AuthScheme.BEARER,
        keyLabel = "API key",
        keyHint = "Leave blank if your endpoint doesn't require one.",
        defaultModel = "",
        modelSuggestions = emptyList(),
    )

    val ALL = listOf(ANTHROPIC_DIRECT, GOOGLE_VERTEX, AZURE_FOUNDRY, DEEPSEEK, OPENAI, CUSTOM_ANTHROPIC, CUSTOM_OPENAI)
}
