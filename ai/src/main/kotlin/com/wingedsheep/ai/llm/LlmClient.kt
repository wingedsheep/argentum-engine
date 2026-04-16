package com.wingedsheep.ai.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val logger = LoggerFactory.getLogger(LlmClient::class.java)

class LlmClient(
    private val properties: AiConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.timeoutMs))
        .build()

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
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("${properties.baseUrl}/chat/completions"))
                    .header("Authorization", "Bearer ${properties.effectiveApiKey}")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(properties.timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val body = response.body()

                if (response.statusCode() >= 400) {
                    logger.warn("LLM API error (attempt {}, status {}): {}", attempt, response.statusCode(), body)
                } else if (body != null) {
                    val parsed = json.decodeFromString<ChatCompletionResponse>(body)
                    val content = parsed.choices.firstOrNull()?.message?.content
                    if (content != null) {
                        logger.info("LLM response (attempt {}):\n{}", attempt, content)
                        return content
                    }
                }
                logger.warn("Empty response from LLM on attempt {}", attempt)
            } catch (e: Exception) {
                lastException = e
                logger.warn("LLM request failed (attempt {}): {}", attempt, e.message)
                if (attempt < properties.maxRetries) {
                    val delayMs = 1000L * (1 shl attempt)
                    Thread.sleep(delayMs)
                }
            }
        }

        logger.error("LLM request failed after {} attempts", properties.maxRetries + 1, lastException)
        return null
    }

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

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    @kotlinx.serialization.Transient
    val cacheControl: CacheControl? = null
)

@Serializable
data class CacheControl(
    val type: String = "ephemeral"
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
