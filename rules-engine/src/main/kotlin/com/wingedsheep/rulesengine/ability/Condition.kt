package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Conditions that can be checked against the game state.
 * Used for conditional effects like "If you control...", "If your life total is...".
 */
@Serializable
sealed interface Condition {
    /** Human-readable description of this condition */
    val description: String

    /**
     * Check if this condition is met.
     *
     * @param state The current game state
     * @param controllerId The player who controls the source of this condition
     * @param sourceId The card that is the source of this condition (optional)
     * @return true if the condition is met
     */
    fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId? = null): Boolean
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).life <= threshold
    }
}

/**
 * Condition: "If your life total is X or more"
 */
@Serializable
data class LifeTotalAtLeast(val threshold: Int) : Condition {
    override val description: String = "if your life total is $threshold or more"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).life >= threshold
    }
}

/**
 * Condition: "If you have more life than an opponent"
 */
@Serializable
data object MoreLifeThanOpponent : Condition {
    override val description: String = "if you have more life than an opponent"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        val myLife = state.getPlayer(controllerId).life
        return state.players.values.any { it.id != controllerId && myLife > it.life }
    }
}

/**
 * Condition: "If you have less life than an opponent"
 */
@Serializable
data object LessLifeThanOpponent : Condition {
    override val description: String = "if you have less life than an opponent"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        val myLife = state.getPlayer(controllerId).life
        return state.players.values.any { it.id != controllerId && myLife < it.life }
    }
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.any {
            it.controllerId == controllerId.value && it.isCreature
        }
    }
}

/**
 * Condition: "If you control X or more creatures"
 */
@Serializable
data class ControlCreaturesAtLeast(val count: Int) : Condition {
    override val description: String = "if you control $count or more creatures"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.count {
            it.controllerId == controllerId.value && it.isCreature
        } >= count
    }
}

/**
 * Condition: "If you control a creature with keyword X"
 */
@Serializable
data class ControlCreatureWithKeyword(val keyword: Keyword) : Condition {
    override val description: String = "if you control a creature with ${keyword.displayName.lowercase()}"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.any {
            it.controllerId == controllerId.value && it.isCreature && it.hasKeyword(keyword)
        }
    }
}

/**
 * Condition: "If you control a [subtype] creature" (e.g., "If you control a Dragon")
 */
@Serializable
data class ControlCreatureOfType(val subtype: Subtype) : Condition {
    override val description: String = "if you control a ${subtype.value}"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.any {
            it.controllerId == controllerId.value &&
            it.isCreature &&
            subtype in it.definition.typeLine.subtypes
        }
    }
}

/**
 * Condition: "If you control an enchantment"
 */
@Serializable
data object ControlEnchantment : Condition {
    override val description: String = "if you control an enchantment"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.any {
            it.controllerId == controllerId.value && it.isEnchantment
        }
    }
}

/**
 * Condition: "If you control an artifact"
 */
@Serializable
data object ControlArtifact : Condition {
    override val description: String = "if you control an artifact"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.any {
            it.controllerId == controllerId.value && it.isArtifact
        }
    }
}

/**
 * Condition: "If an opponent controls a creature"
 */
@Serializable
data object OpponentControlsCreature : Condition {
    override val description: String = "if an opponent controls a creature"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.battlefield.cards.any {
            it.controllerId != controllerId.value && it.isCreature
        }
    }
}

/**
 * Condition: "If an opponent controls more creatures than you"
 */
@Serializable
data object OpponentControlsMoreCreatures : Condition {
    override val description: String = "if an opponent controls more creatures than you"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        val myCreatures = state.battlefield.cards.count {
            it.controllerId == controllerId.value && it.isCreature
        }
        return state.players.values.any { opponent ->
            opponent.id != controllerId &&
            state.battlefield.cards.count {
                it.controllerId == opponent.id.value && it.isCreature
            } > myCreatures
        }
    }
}

/**
 * Condition: "If an opponent controls more lands than you"
 * Used by Gift of Estates and similar cards.
 */
@Serializable
data object OpponentControlsMoreLands : Condition {
    override val description: String = "if an opponent controls more lands than you"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        val myLands = state.battlefield.cards.count {
            it.controllerId == controllerId.value && it.isLand
        }
        return state.players.values.any { opponent ->
            opponent.id != controllerId &&
            state.battlefield.cards.count {
                it.controllerId == opponent.id.value && it.isLand
            } > myLands
        }
    }
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).hand.isEmpty
    }
}

/**
 * Condition: "If you have X or more cards in hand"
 */
@Serializable
data class CardsInHandAtLeast(val count: Int) : Condition {
    override val description: String = "if you have $count or more cards in hand"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).hand.size >= count
    }
}

/**
 * Condition: "If you have X or fewer cards in hand"
 */
@Serializable
data class CardsInHandAtMost(val count: Int) : Condition {
    override val description: String = "if you have $count or fewer cards in hand"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).hand.size <= count
    }
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).graveyard.cards.count { it.isCreature } >= count
    }
}

/**
 * Condition: "If there are X or more cards in your graveyard"
 */
@Serializable
data class CardsInGraveyardAtLeast(val count: Int) : Condition {
    override val description: String = "if there are $count or more cards in your graveyard"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return state.getPlayer(controllerId).graveyard.size >= count
    }
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        if (sourceId == null) return false
        @Suppress("DEPRECATION")
        return state.combat?.isAttacking(sourceId) == true
    }
}

/**
 * Condition: "If this creature is blocking"
 */
@Serializable
data object SourceIsBlocking : Condition {
    override val description: String = "if this creature is blocking"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        if (sourceId == null) return false
        @Suppress("DEPRECATION")
        return state.combat?.isBlocking(sourceId) == true
    }
}

/**
 * Condition: "If this creature is tapped"
 */
@Serializable
data object SourceIsTapped : Condition {
    override val description: String = "if this creature is tapped"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        if (sourceId == null) return false
        val card = state.battlefield.getCard(sourceId) ?: return false
        return card.isTapped
    }
}

/**
 * Condition: "If this creature is untapped"
 */
@Serializable
data object SourceIsUntapped : Condition {
    override val description: String = "if this creature is untapped"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        if (sourceId == null) return false
        val card = state.battlefield.getCard(sourceId) ?: return false
        return !card.isTapped
    }
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        @Suppress("DEPRECATION")
        return state.turnState.isActivePlayer(controllerId)
    }
}

/**
 * Condition: "If it's not your turn"
 */
@Serializable
data object IsNotYourTurn : Condition {
    override val description: String = "if it's not your turn"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        @Suppress("DEPRECATION")
        return !state.turnState.isActivePlayer(controllerId)
    }
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

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return conditions.all { it.isMet(state, controllerId, sourceId) }
    }
}

/**
 * Condition: Any of the sub-conditions must be met (OR)
 */
@Serializable
data class AnyCondition(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" or ") { it.description }

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return conditions.any { it.isMet(state, controllerId, sourceId) }
    }
}

/**
 * Condition: The sub-condition must NOT be met
 */
@Serializable
data class NotCondition(val condition: Condition) : Condition {
    override val description: String = "if not (${condition.description})"

    override fun isMet(state: GameState, controllerId: PlayerId, sourceId: CardId?): Boolean {
        return !condition.isMet(state, controllerId, sourceId)
    }
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
