package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Turn/Phase Conditions
// =============================================================================

/**
 * Condition: "If it's your turn"
 */
@SerialName("IsYourTurn")
@Serializable
data object IsYourTurn : Condition {
    override val description: String = "if it's your turn"
}

/**
 * Condition: "If it's not your turn"
 */
@SerialName("IsNotYourTurn")
@Serializable
data object IsNotYourTurn : Condition {
    override val description: String = "if it's not your turn"
}

// =============================================================================
// Combat Conditions
// =============================================================================

/**
 * Condition: "If you've been attacked this step"
 * Used for cards like Defiant Stand and Harsh Justice that can only be cast
 * during the declare attackers step if you've been attacked.
 */
@SerialName("YouWereAttackedThisStep")
@Serializable
data object YouWereAttackedThisStep : Condition {
    override val description: String = "if you've been attacked this step"
}

/**
 * Condition: "If you were dealt combat damage this turn"
 */
@SerialName("YouWereDealtCombatDamageThisTurn")
@Serializable
data object YouWereDealtCombatDamageThisTurn : Condition {
    override val description: String = "if you were dealt combat damage this turn"
}

/**
 * Condition: "If you attacked this turn"
 */
@SerialName("YouAttackedThisTurn")
@Serializable
data object YouAttackedThisTurn : Condition {
    override val description: String = "if you attacked this turn"
}

// =============================================================================
// Stack Conditions
// =============================================================================

/**
 * Condition: "If an opponent has cast a spell (it's on the stack)"
 * Used for Portal counterspells like Mystic Denial that can only be cast
 * in response to an opponent's spell.
 */
@SerialName("OpponentSpellOnStack")
@Serializable
data object OpponentSpellOnStack : Condition {
    override val description: String = "if an opponent has cast a spell"
}
