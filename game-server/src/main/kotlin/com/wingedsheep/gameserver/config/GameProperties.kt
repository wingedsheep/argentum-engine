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
    val dominariaEnabled: Boolean = false
)

data class AdminProperties(
    val password: String = ""
)

data class AiProperties(
    val enabled: Boolean = false,
    val openRouterApiKey: String = "",
    val model: String = "google/gemini-3.1-flash-lite-preview",
    val maxRetries: Int = 2,
    val timeoutMs: Long = 10000,
    val thinkingDelayMs: Long = 500
)