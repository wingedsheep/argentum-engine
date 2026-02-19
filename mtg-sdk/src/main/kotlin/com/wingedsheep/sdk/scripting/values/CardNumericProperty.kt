package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A numeric property of a card that can be aggregated over (max, min, sum).
 * Used with generic battlefield aggregation primitives like DynamicAmount.MaxBattlefield.
 */
@Serializable
sealed interface CardNumericProperty {
    val description: String

    @SerialName("ManaValue")
    @Serializable
    data object ManaValue : CardNumericProperty {
        override val description: String = "mana value"
    }

    @SerialName("Power")
    @Serializable
    data object Power : CardNumericProperty {
        override val description: String = "power"
    }

    @SerialName("Toughness")
    @Serializable
    data object Toughness : CardNumericProperty {
        override val description: String = "toughness"
    }
}
