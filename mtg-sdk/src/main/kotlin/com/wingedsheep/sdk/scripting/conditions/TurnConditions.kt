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
/**
 * Condition: "if you gained life this turn"
 * Checks whether the controller has gained life at any point during the current turn.
 * Used for Lunar Convocation.
 */
@SerialName("YouGainedLifeThisTurn")
@Serializable
data object YouGainedLifeThisTurn : Condition {
    override val description: String = "if you gained life this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "as long as you've lost life this turn"
 * Checks whether the controller has lost life at any point during the current turn.
 * Used for Essence Channeler.
 */
@SerialName("YouLostLifeThisTurn")
@Serializable
data object YouLostLifeThisTurn : Condition {
    override val description: String = "as long as you've lost life this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if you gained and lost life this turn"
 * Checks whether the controller has BOTH gained AND lost life during the current turn.
 * Used for Lunar Convocation's second ability.
 */
@SerialName("YouGainedAndLostLifeThisTurn")
@Serializable
data object YouGainedAndLostLifeThisTurn : Condition {
    override val description: String = "if you gained and lost life this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if you gained or lost life this turn"
 * Checks whether the controller has gained OR lost life at any point during the current turn.
 * Used for Star Charter and similar Bloomburrow cards.
 */
@SerialName("YouGainedOrLostLifeThisTurn")
@Serializable
data object YouGainedOrLostLifeThisTurn : Condition {
    override val description: String = "if you gained or lost life this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

@SerialName("OpponentLostLifeThisTurn")
@Serializable
data object OpponentLostLifeThisTurn : Condition {
    override val description: String = "if an opponent lost life this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "if this is the first spell of a given type category cast by you this turn"
 * Checks the per-player spell type count tracker. The spell category uses the same keys
 * as GameState.spellTypesCastThisTurn (e.g., "INSTANT", "SORCERY", "SUBTYPE_OTTER").
 * The condition is true if the count for that category is exactly 1 (just this spell).
 * Used for Alania, Divergent Storm.
 */
@SerialName("IsFirstSpellOfTypeCastThisTurn")
@Serializable
data class IsFirstSpellOfTypeCastThisTurn(
    val spellCategory: String
) : Condition {
    override val description: String = "if it's the first ${spellCategory.lowercase().replace('_', ' ')} spell you've cast this turn"
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
// Graveyard/Sacrifice Tracking Conditions
// =============================================================================

/**
 * Condition: "If N or more cards left your graveyard this turn"
 * Used for Bonecache Overseer.
 */
@SerialName("CardsLeftGraveyardThisTurn")
@Serializable
data class CardsLeftGraveyardThisTurn(val count: Int) : Condition {
    override val description: String = "if $count or more cards left your graveyard this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

/**
 * Condition: "If you've sacrificed a Food this turn"
 * Used for Bonecache Overseer.
 */
@SerialName("SacrificedFoodThisTurn")
@Serializable
data object SacrificedFoodThisTurn : Condition {
    override val description: String = "if you've sacrificed a Food this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Ability Resolution Conditions
// =============================================================================

/**
 * Condition: "if this is the Nth time this ability has resolved this turn"
 * Checks the AbilityResolutionCountThisTurnComponent on the source entity.
 * Used for cards like Harvestrite Host.
 */
@SerialName("SourceAbilityResolvedNTimesThisTurn")
@Serializable
data class SourceAbilityResolvedNTimesThisTurn(val count: Int) : Condition {
    override val description: String = "if this is the ${ordinal(count)} time this ability has resolved this turn"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this

    private fun ordinal(n: Int): String = when (n) {
        1 -> "first"
        2 -> "second"
        3 -> "third"
        else -> "${n}th"
    }
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

