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
    FIRST_STRIKE,
    LIFELINK,
    INDESTRUCTIBLE,
    DEATHTOUCH,
    TRAMPLE,
    HEXPROOF,
    REACH,
    STASH,
    BLIGHT,
    COIN,
    FLOOD,
    CHORUS,
    DREAM,
    QUEST,
    GROWTH,
    TIME,
    FEATHER,
    HOURGLASS,
    DECAYED
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
    const val FIRST_STRIKE = "first strike"
    const val LIFELINK = "lifelink"
    const val INDESTRUCTIBLE = "indestructible"
    const val DEATHTOUCH = "deathtouch"
    const val TRAMPLE = "trample"
    const val HEXPROOF = "hexproof"
    const val REACH = "reach"
    const val STASH = "stash"
    const val BLIGHT = "blight"
    const val COIN = "coin"
    const val FLOOD = "flood"
    const val CHORUS = "chorus"
    const val DREAM = "dream"
    const val QUEST = "quest"
    const val GROWTH = "growth"
    const val TIME = "time"
    const val FEATHER = "feather"
    const val HOURGLASS = "hourglass"

    /**
     * Decayed counter (Tarkir: Dragonstorm). A keyword-ability counter (CR 702.147a): a creature
     * with one or more decayed counters has Decayed — "This creature can't block" and "When this
     * creature attacks, sacrifice it at end of combat." Granted to *any* creature, independent of
     * its printed abilities (e.g. Rot-Curse Rakshasa's Renew). The behavior is realized by the
     * engine off the counter, mirroring how the printed [com.wingedsheep.sdk.dsl.card]`.decayed()`
     * helper composes the same static + triggered ability.
     */
    const val DECAYED = "decayed"

    /**
     * Wildcard sentinel for triggers/events that fire on counters of *any* type, e.g.
     * "whenever one or more counters are put on a creature you control" (Stalwart Successor).
     * A [com.wingedsheep.sdk.scripting.EventPattern.CountersPlacedEvent] with this `counterType`
     * matches every counter-placement event regardless of the counter kind.
     */
    const val ANY = "any"
}
