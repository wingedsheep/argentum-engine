package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import kotlinx.serialization.Serializable

/**
 * Conditions that can be checked against the game state.
 * Used for conditional effects like "If you control...", "If your life total is...".
 *
 * Conditions are data objects - evaluation is handled by ConditionEvaluator
 * which checks these conditions against GameState.
 */
@Serializable
sealed interface Condition {
    /** Human-readable description of this condition */
    val description: String
}

// =============================================================================
// Life Total Conditions
// =============================================================================

/**
 * Condition: "If your life total is X or less"
 */
@Serializable
data class LifeTotalAtMost(val threshold: Int) : Condition {
    override val description: String = "if your life total is $threshold or less"
}

/**
 * Condition: "If your life total is X or more"
 */
@Serializable
data class LifeTotalAtLeast(val threshold: Int) : Condition {
    override val description: String = "if your life total is $threshold or more"
}

/**
 * Condition: "If you have more life than an opponent"
 */
@Serializable
data object MoreLifeThanOpponent : Condition {
    override val description: String = "if you have more life than an opponent"
}

/**
 * Condition: "If you have less life than an opponent"
 */
@Serializable
data object LessLifeThanOpponent : Condition {
    override val description: String = "if you have less life than an opponent"
}

// =============================================================================
// Battlefield Conditions
// =============================================================================

/**
 * Condition: "If you control a creature"
 */
@Serializable
data object ControlCreature : Condition {
    override val description: String = "if you control a creature"
}

/**
 * Condition: "If you control X or more creatures"
 */
@Serializable
data class ControlCreaturesAtLeast(val count: Int) : Condition {
    override val description: String = "if you control $count or more creatures"
}

/**
 * Condition: "If you control a creature with keyword X"
 */
@Serializable
data class ControlCreatureWithKeyword(val keyword: Keyword) : Condition {
    override val description: String = "if you control a creature with ${keyword.displayName.lowercase()}"
}

/**
 * Condition: "If you control a [subtype] creature" (e.g., "If you control a Dragon")
 */
@Serializable
data class ControlCreatureOfType(val subtype: Subtype) : Condition {
    override val description: String = "if you control a ${subtype.value}"
}

/**
 * Condition: "If you control an enchantment"
 */
@Serializable
data object ControlEnchantment : Condition {
    override val description: String = "if you control an enchantment"
}

/**
 * Condition: "If you control an artifact"
 */
@Serializable
data object ControlArtifact : Condition {
    override val description: String = "if you control an artifact"
}

/**
 * Condition: "If an opponent controls a creature"
 */
@Serializable
data object OpponentControlsCreature : Condition {
    override val description: String = "if an opponent controls a creature"
}

/**
 * Condition: "If an opponent controls more creatures than you"
 */
@Serializable
data object OpponentControlsMoreCreatures : Condition {
    override val description: String = "if an opponent controls more creatures than you"
}

/**
 * Condition: "If an opponent controls more lands than you"
 * Used by Gift of Estates and similar cards.
 */
@Serializable
data object OpponentControlsMoreLands : Condition {
    override val description: String = "if an opponent controls more lands than you"
}

// =============================================================================
// Hand/Library Conditions
// =============================================================================

/**
 * Condition: "If you have no cards in hand"
 */
@Serializable
data object EmptyHand : Condition {
    override val description: String = "if you have no cards in hand"
}

/**
 * Condition: "If you have X or more cards in hand"
 */
@Serializable
data class CardsInHandAtLeast(val count: Int) : Condition {
    override val description: String = "if you have $count or more cards in hand"
}

/**
 * Condition: "If you have X or fewer cards in hand"
 */
@Serializable
data class CardsInHandAtMost(val count: Int) : Condition {
    override val description: String = "if you have $count or fewer cards in hand"
}

// =============================================================================
// Graveyard Conditions
// =============================================================================

/**
 * Condition: "If there are X or more creature cards in your graveyard"
 */
@Serializable
data class CreatureCardsInGraveyardAtLeast(val count: Int) : Condition {
    override val description: String = "if there are $count or more creature cards in your graveyard"
}

/**
 * Condition: "If there are X or more cards in your graveyard"
 */
@Serializable
data class CardsInGraveyardAtLeast(val count: Int) : Condition {
    override val description: String = "if there are $count or more cards in your graveyard"
}

/**
 * Condition: "If there is a [subtype] card in your graveyard"
 * Used for tribal synergies like Dawnhand Eulogist.
 */
@Serializable
data class GraveyardContainsSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if there is a ${subtype.value} card in your graveyard"
}

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

// =============================================================================
// Turn/Phase Conditions
// =============================================================================

/**
 * Condition: "If it's your turn"
 */
@Serializable
data object IsYourTurn : Condition {
    override val description: String = "if it's your turn"
}

/**
 * Condition: "If it's not your turn"
 */
@Serializable
data object IsNotYourTurn : Condition {
    override val description: String = "if it's not your turn"
}

// =============================================================================
// Composite Conditions
// =============================================================================

/**
 * Condition: All of the sub-conditions must be met (AND)
 */
@Serializable
data class AllConditions(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" and ") { it.description }
}

/**
 * Condition: Any of the sub-conditions must be met (OR)
 */
@Serializable
data class AnyCondition(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" or ") { it.description }
}

/**
 * Condition: The sub-condition must NOT be met
 */
@Serializable
data class NotCondition(val condition: Condition) : Condition {
    override val description: String = "if not (${condition.description})"
}

// =============================================================================
// Conditional Effect
// =============================================================================

/**
 * An effect that only happens if a condition is met.
 */
@Serializable
data class ConditionalEffect(
    val condition: Condition,
    val effect: Effect,
    val elseEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append(condition.description.replaceFirstChar { it.uppercase() })
        append(", ")
        append(effect.description.replaceFirstChar { it.lowercase() })
        if (elseEffect != null) {
            append(". Otherwise, ")
            append(elseEffect.description.replaceFirstChar { it.lowercase() })
        }
    }
}
