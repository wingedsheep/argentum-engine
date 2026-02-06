package com.wingedsheep.gameserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game")
data class GameProperties(
    val handSmoother: HandSmootherProperties = HandSmootherProperties(),
    val sets: SetsProperties = SetsProperties()
)

data class HandSmootherProperties(
    val enabled: Boolean = true,
    val candidates: Int = 3
)

data class SetsProperties(
    val onslaughtEnabled: Boolean = true
)