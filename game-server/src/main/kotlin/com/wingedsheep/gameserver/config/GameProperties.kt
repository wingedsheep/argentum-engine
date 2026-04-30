package com.wingedsheep.gameserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game")
data class GameProperties(
    val handSmoother: HandSmootherProperties = HandSmootherProperties(),
    val sets: SetsProperties = SetsProperties(),
    val admin: AdminProperties = AdminProperties(),
    val ai: AiProperties = AiProperties(),
    val debugMode: Boolean = false
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
    val phyrexiaAllWillBeOneEnabled: Boolean = true,
    val dominariaEnabled: Boolean = false,
    val dominariaUnitedEnabled: Boolean = true,
    val bloomburrowEnabled: Boolean = true,
    val brothersWarEnabled: Boolean = true,
    val edgeOfEternitiesEnabled: Boolean = false,
    val lorwynEclipsedEnabled: Boolean = true,
    val lostCavernsOfIxalanEnabled: Boolean = true,
    val murdersAtKarlovManorEnabled: Boolean = true,
    val foundationsEnabled: Boolean = true,
    val duskmournEnabled: Boolean = true,
    val spiderManEnabled: Boolean = true,
    val wildsOfEldrainEnabled: Boolean = true
)

data class AdminProperties(
    val password: String = ""
)

data class AiProperties(
    val enabled: Boolean = false,
    /** AI mode: "engine" (built-in rules engine AI, default) or "llm" (LLM-based AI via API). */
    val mode: String = "engine",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val openRouterApiKey: String = "",
    val model: String = "qwen/qwen3.6-plus:free",
    val deckbuildingModel: String = "",
    val reasoningEffort: String = "low",
    val maxRetries: Int = 2,
    val timeoutMs: Long = 300000,
    val thinkingDelayMs: Long = 500
) {
    /** Returns the model to use for deckbuilding — falls back to the gameplay model if not set. */
    val effectiveDeckbuildingModel: String get() = deckbuildingModel.ifBlank { model }

    /** Returns the effective API key — prefers [apiKey], falls back to [openRouterApiKey] for backward compatibility. */
    val effectiveApiKey: String get() = apiKey.ifBlank { openRouterApiKey }

    /** Whether we're using the built-in engine AI (no API key required). */
    val isEngineMode: Boolean get() = mode.equals("engine", ignoreCase = true)

    /** Whether we're using the LLM-based AI. */
    val isLlmMode: Boolean get() = !isEngineMode
}
