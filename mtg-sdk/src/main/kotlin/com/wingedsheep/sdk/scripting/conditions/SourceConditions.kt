package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Source Conditions
// =============================================================================

/**
 * Condition: "If this creature is attacking"
 */
@Serializable
data object SourceIsAttacking : Condition {
    override val description: String = "if this creature is attacking"
}

/**
 * Condition: "If this creature is blocking"
 */
@Serializable
data object SourceIsBlocking : Condition {
    override val description: String = "if this creature is blocking"
}

/**
 * Condition: "If this creature is tapped"
 */
@Serializable
data object SourceIsTapped : Condition {
    override val description: String = "if this creature is tapped"
}

/**
 * Condition: "If this creature is untapped"
 */
@Serializable
data object SourceIsUntapped : Condition {
    override val description: String = "if this creature is untapped"
}

/**
 * Condition: "If this creature has dealt damage"
 * Used for cards like Karakyk Guardian: "has hexproof if it hasn't dealt damage yet"
 *
 * The engine tracks damage dealt history per-object since entering the battlefield.
 */
@Serializable
data object SourceHasDealtDamage : Condition {
    override val description: String = "this creature has dealt damage"
}

/**
 * Condition: "If this creature has dealt combat damage to a player"
 * Used for Saboteur abilities and similar effects.
 */
@Serializable
data object SourceHasDealtCombatDamageToPlayer : Condition {
    override val description: String = "this creature has dealt combat damage to a player"
}

/**
 * Condition: "If this creature entered the battlefield this turn"
 * Used for summoning sickness checks and ETB-sensitive abilities.
 */
@Serializable
data object SourceEnteredThisTurn : Condition {
    override val description: String = "this creature entered the battlefield this turn"
}
