package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.config.AiProperties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
     */
    fun chatCompletion(messages: List<ChatMessage>, modelOverride: String? = null): String? {
        val effectiveModel = modelOverride ?: properties.model
        val request = ChatCompletionRequest(
            model = effectiveModel,
            messages = messages,
            temperature = 0.3
        )

        val requestBody = json.encodeToString(request)

        logger.info("LLM request: url={}, model={}, messages={}, lastUserMsg={}chars",
            properties.baseUrl, effectiveModel, messages.size,
            messages.lastOrNull { it.role == "user" }?.content?.length ?: 0)

        var lastException: Exception? = null
        for (attempt in 0..properties.maxRetries) {
            try {
                val responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String::class.java)

                if (responseBody != null) {
                    val response = json.decodeFromString<ChatCompletionResponse>(responseBody)
                    val content = response.choices.firstOrNull()?.message?.content
                    if (content != null) {
                        logger.info("LLM response (attempt {}): {}", attempt, content.take(500))
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
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3
)

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
