package com.wingedsheep.rulesengine.player

import kotlinx.serialization.Serializable
import java.util.UUID

@JvmInline
@Serializable
value class PlayerId(val value: String) {
    companion object {
        fun generate(): PlayerId = PlayerId(UUID.randomUUID().toString())
        fun of(value: String): PlayerId = PlayerId(value)
    }

    override fun toString(): String = value
}
