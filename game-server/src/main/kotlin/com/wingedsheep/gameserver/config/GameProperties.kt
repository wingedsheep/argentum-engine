package com.wingedsheep.gameserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game")
data class GameProperties(
    val handSmoother: HandSmootherProperties = HandSmootherProperties(),
    val sets: SetsProperties = SetsProperties(),
    val admin: AdminProperties = AdminProperties(),
    val ai: AiProperties = AiProperties()
)

data class HandSmootherProperties(
    val enabled: Boolean = true,
    val candidates: Int = 3
)

data class SetsProperties(
    val onslaughtEnabled: Boolean = true,
    val scourgeEnabled: Boolean = true,
    val legionsEnabled: Boolean = true,
    val khansEnabled: Boolean = true,
    val dominariaEnabled: Boolean = false,
    val bloomburrowEnabled: Boolean = false
)

data class AdminProperties(
    val password: String = ""
)

data class AiProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val openRouterApiKey: String = "",
    val model: String = "qwen/qwen3.5-flash-02-23",
    val deckbuildingModel: String = "",
    val maxRetries: Int = 2,
    val timeoutMs: Long = 10000,
    val thinkingDelayMs: Long = 500
) {
    /** Returns the model to use for deckbuilding — falls back to the gameplay model if not set. */
    val effectiveDeckbuildingModel: String get() = deckbuildingModel.ifBlank { model }

    /** Returns the effective API key — prefers [apiKey], falls back to [openRouterApiKey] for backward compatibility. */
    val effectiveApiKey: String get() = apiKey.ifBlank { openRouterApiKey }
}
