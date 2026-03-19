package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.scripting.text.TextReplacer
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If it's not your turn"
 */
@SerialName("IsNotYourTurn")
@Serializable
data object IsNotYourTurn : Condition {
    override val description: String = "if it's not your turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If you were dealt combat damage this turn"
 */
@SerialName("YouWereDealtCombatDamageThisTurn")
@Serializable
data object YouWereDealtCombatDamageThisTurn : Condition {
    override val description: String = "if you were dealt combat damage this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If you attacked this turn"
 */
@SerialName("YouAttackedThisTurn")
@Serializable
data object YouAttackedThisTurn : Condition {
    override val description: String = "if you attacked this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If an opponent lost life this turn"
 * Checks whether any opponent has lost life (from any source: damage, life loss, payment)
 * at any point during the current turn. Per MTG rules, this cares about whether life was
 * lost, not whether the net life total changed.
 * Used for cards like Hired Claw.
 */
@SerialName("OpponentLostLifeThisTurn")
@Serializable
data object OpponentLostLifeThisTurn : Condition {
    override val description: String = "if an opponent lost life this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Land Conditions
// =============================================================================

/**
 * Condition: "If you've played a land this turn"
 * Checks if the player has used any land drops this turn.
 * Used for cards like Rock Jockey.
 */
@SerialName("PlayedLandThisTurn")
@Serializable
data object PlayedLandThisTurn : Condition {
    override val description: String = "if you've played a land this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

