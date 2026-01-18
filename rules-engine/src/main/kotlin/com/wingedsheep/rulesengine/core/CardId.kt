package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable
import java.util.UUID

@JvmInline
@Serializable
value class CardId(val value: String) {
    companion object {
        fun generate(): CardId = CardId(UUID.randomUUID().toString())
    }
}
