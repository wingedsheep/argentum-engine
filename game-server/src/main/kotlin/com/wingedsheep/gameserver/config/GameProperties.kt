package com.wingedsheep.gameserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game")
data class GameProperties(
    val handSmoother: HandSmootherProperties = HandSmootherProperties()
)

data class HandSmootherProperties(
    val enabled: Boolean = true,
    val candidates: Int = 3
)