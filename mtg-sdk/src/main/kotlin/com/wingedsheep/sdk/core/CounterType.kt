package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * Types of counters that can be placed on permanents.
 */
@Serializable
enum class CounterType {
    PLUS_ONE_PLUS_ONE,
    MINUS_ONE_MINUS_ONE,
    LOYALTY,
    CHARGE,
    GEM,
    POISON,
    SILVER,
    GOLD,
    PLAGUE,
    TRAP,
    DEPLETION,
    EGG,
    LORE,
    AIM,
    STUN,
    FINALITY,
    SUPPLY,
    FLYING,
    INDESTRUCTIBLE,
    STASH,
    BLIGHT,
    COIN
}

/**
 * String constants for counter types used in card definitions and effects.
 * Use these instead of raw string literals for consistency and refactorability.
 */
object Counters {
    const val PLUS_ONE_PLUS_ONE = "+1/+1"
    const val MINUS_ONE_MINUS_ONE = "-1/-1"
    const val LOYALTY = "loyalty"
    const val CHARGE = "charge"
    const val GEM = "gem"
    const val POISON = "poison"
    const val SILVER = "silver"
    const val GOLD = "gold"
    const val PLAGUE = "plague"
    const val TRAP = "trap"
    const val DEPLETION = "depletion"
    const val EGG = "egg"
    const val LORE = "lore"
    const val AIM = "aim"
    const val STUN = "stun"
    const val FINALITY = "finality"
    const val SUPPLY = "supply"
    const val FLYING = "flying"
    const val INDESTRUCTIBLE = "indestructible"
    const val STASH = "stash"
    const val BLIGHT = "blight"
    const val COIN = "coin"
}
