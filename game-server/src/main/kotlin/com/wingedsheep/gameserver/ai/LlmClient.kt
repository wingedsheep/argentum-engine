package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.config.AiProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

private val logger = LoggerFactory.getLogger(LlmClient::class.java)

/**
 * HTTP client for OpenAI-compatible chat completions APIs.
 *
 * Works with any provider that implements the OpenAI chat completions endpoint:
 * OpenRouter, Ollama, LiteLLM, vLLM, etc. Configure via [AiProperties.baseUrl] and [AiProperties.apiKey].
 */
class LlmClient(
    private val properties: AiProperties
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.timeoutMs))
        .build()

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .defaultHeader("Authorization", "Bearer ${properties.effectiveApiKey}")
        .defaultHeader("Content-Type", "application/json")
        .requestFactory(JdkClientHttpRequestFactory(httpClient).also {
            it.setReadTimeout(Duration.ofMillis(properties.timeoutMs))
        })
        .build()

    /**
     * Send a chat completion request and return the assistant's response text.
     * Retries with exponential backoff on failure.
     *
     * @param cacheControl When set, adds a top-level `cache_control` field to the request.
     *   This enables automatic prompt caching for Anthropic models via OpenRouter — the provider
     *   automatically caches all content up to the last cacheable block and advances the cache
     *   boundary as the conversation grows. Individual messages can also carry their own
     *   [ChatMessage.cacheControl] for explicit per-block cache breakpoints (works across
     *   Anthropic, Bedrock, Vertex, and Gemini providers).
     */
    fun chatCompletion(messages: List<ChatMessage>, modelOverride: String? = null, cacheControl: CacheControl? = null): String? {
        val effectiveModel = modelOverride ?: properties.model
        val reasoningEffort = properties.reasoningEffort.ifBlank { null }

        val requestBody = buildRequestJson(effectiveModel, messages, reasoningEffort, cacheControl)

        val lastUserMsg = messages.lastOrNull { it.role == "user" }?.content
        logger.info("LLM request: url={}, model={}, messages={}", properties.baseUrl, effectiveModel, messages.size)
        if (lastUserMsg != null) {
            logger.debug("LLM prompt:\n{}", lastUserMsg)
        }

        var lastException: Exception? = null
        for (attempt in 0..properties.maxRetries) {
            try {
                val responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .exchange { _, response ->
                        val body = response.body.bufferedReader().readText()
                        if (response.statusCode.isError) {
                            logger.warn("LLM API error (attempt {}, status {}): {}", attempt, response.statusCode.value(), body)
                            null
                        } else {
                            body
                        }
                    }

                if (responseBody != null) {
                    val response = json.decodeFromString<ChatCompletionResponse>(responseBody)
                    val content = response.choices.firstOrNull()?.message?.content
                    if (content != null) {
                        logger.info("LLM response (attempt {}):\n{}", attempt, content)
                        return content
                    }
                }
                logger.warn("Empty response from LLM on attempt {}", attempt)
            } catch (e: Exception) {
                lastException = e
                logger.warn("LLM request failed (attempt $attempt): ${e.message}")
                if (attempt < properties.maxRetries) {
                    val delayMs = 1000L * (1 shl attempt) // exponential backoff: 1s, 2s, 4s
                    Thread.sleep(delayMs)
                }
            }
        }

        logger.error("LLM request failed after ${properties.maxRetries + 1} attempts", lastException)
        return null
    }

    /**
     * Build the JSON request body, handling per-message cache_control breakpoints.
     *
     * When a [ChatMessage] has [ChatMessage.cacheControl] set, its `content` is serialized as an
     * array of content blocks (required by Anthropic/Gemini for explicit cache breakpoints).
     * Otherwise, `content` is a plain string. The top-level [cacheControl] enables Anthropic's
     * automatic caching mode.
     */
    private fun buildRequestJson(
        model: String,
        messages: List<ChatMessage>,
        reasoningEffort: String?,
        cacheControl: CacheControl?
    ): String {
        val jsonObj = buildJsonObject {
            put("model", model)
            put("temperature", 0.8)
            reasoningEffort?.let { put("reasoning_effort", it) }
            cacheControl?.let {
                putJsonObject("cache_control") {
                    put("type", it.type)
                }
            }
            putJsonArray("messages") {
                for (msg in messages) {
                    addJsonObject {
                        put("role", msg.role)
                        if (msg.cacheControl != null) {
                            // Explicit cache breakpoint: serialize content as array of content blocks
                            putJsonArray("content") {
                                addJsonObject {
                                    put("type", "text")
                                    put("text", msg.content)
                                    putJsonObject("cache_control") {
                                        put("type", msg.cacheControl.type)
                                    }
                                }
                            }
                        } else {
                            put("content", msg.content)
                        }
                    }
                }
            }
        }
        return jsonObj.toString()
    }
}

// Keep backward-compatible type alias
@Deprecated("Use LlmClient instead", ReplaceWith("LlmClient"))
typealias OpenRouterClient = LlmClient

// =============================================================================
// Request/Response DTOs
// =============================================================================

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    /** When set, this message gets an explicit cache breakpoint (content serialized as blocks). */
    @kotlinx.serialization.Transient
    val cacheControl: CacheControl? = null
)

@Serializable
data class CacheControl(
    val type: String = "ephemeral"
)

// Note: ChatCompletionRequest is no longer used — request JSON is built manually
// in LlmClient.buildRequestJson() to support per-message cache_control breakpoints
// (which require content to be serialized as an array of content blocks).

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: ChatMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)
