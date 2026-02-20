package com.wingedsheep.sdk.scripting.values

import kotlinx.serialization.Serializable

/**
 * A numeric property of a card that can be aggregated over.
 * Used with [DynamicAmount.AggregateBattlefield].
 */
@Serializable
enum class CardNumericProperty(val description: String) {
    MANA_VALUE("mana value"),
    POWER("power"),
    TOUGHNESS("toughness")
}

/**
 * An aggregation function applied to a collection of battlefield entities.
 * Used with [DynamicAmount.AggregateBattlefield].
 */
@Serializable
enum class Aggregation {
    COUNT, MAX, MIN, SUM
}
