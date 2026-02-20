package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.conditions.Condition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Source Conditions
// =============================================================================

/**
 * Condition: "If this creature is attacking"
 */
@SerialName("SourceIsAttacking")
@Serializable
data object SourceIsAttacking : Condition {
    override val description: String = "if this creature is attacking"
}

/**
 * Condition: "If this creature is blocking"
 */
@SerialName("SourceIsBlocking")
@Serializable
data object SourceIsBlocking : Condition {
    override val description: String = "if this creature is blocking"
}

/**
 * Condition: "If this creature is tapped"
 */
@SerialName("SourceIsTapped")
@Serializable
data object SourceIsTapped : Condition {
    override val description: String = "if this creature is tapped"
}

/**
 * Condition: "If this creature is untapped"
 */
@SerialName("SourceIsUntapped")
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
@SerialName("SourceHasDealtDamage")
@Serializable
data object SourceHasDealtDamage : Condition {
    override val description: String = "this creature has dealt damage"
}

/**
 * Condition: "If this creature has dealt combat damage to a player"
 * Used for Saboteur abilities and similar effects.
 */
@SerialName("SourceHasDealtCombatDamageToPlayer")
@Serializable
data object SourceHasDealtCombatDamageToPlayer : Condition {
    override val description: String = "this creature has dealt combat damage to a player"
}

/**
 * Condition: "If this creature entered the battlefield this turn"
 * Used for summoning sickness checks and ETB-sensitive abilities.
 */
@SerialName("SourceEnteredThisTurn")
@Serializable
data object SourceEnteredThisTurn : Condition {
    override val description: String = "this creature entered the battlefield this turn"
}

/**
 * Condition: "As long as this creature is a [subtype]"
 * Used for cards like Mistform Wall: "This creature has defender as long as it's a Wall."
 *
 * Evaluated during state projection against projected subtypes, so type-changing
 * effects in Layer 4 are properly accounted for when checking conditions in Layer 6.
 */
@SerialName("SourceHasSubtype")
@Serializable
data class SourceHasSubtype(val subtype: Subtype) : Condition {
    override val description: String = "as long as this creature is a ${subtype.value}"
}
