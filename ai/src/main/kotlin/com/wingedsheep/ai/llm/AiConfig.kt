package com.wingedsheep.ai.llm

data class AiConfig(
    val enabled: Boolean = false,
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
    val effectiveDeckbuildingModel: String get() = deckbuildingModel.ifBlank { model }
    val effectiveApiKey: String get() = apiKey.ifBlank { openRouterApiKey }
    val isEngineMode: Boolean get() = mode.equals("engine", ignoreCase = true)
    val isLlmMode: Boolean get() = !isEngineMode
}
