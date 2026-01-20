package com.wingedsheep.rulesengine.ecs.replacement

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent
import kotlinx.serialization.Serializable

/**
 * Represents a replacement effect per MTG Rule 614.
 *
 * Replacement effects modify or replace events as they happen, BEFORE
 * they are finalized. Unlike triggered abilities that respond to events
 * after they occur, replacement effects intercept and transform events.
 *
 * Key MTG rules:
 * - Rule 614.1: Replacement effects use "instead" or "skip"
 * - Rule 614.5: Replacement effects can only apply once per event
 * - Rule 614.6: If multiple replacement effects apply, affected player/controller chooses
 * - Rule 614.7: Prevention effects are a type of replacement effect
 *
 * Common replacement effects:
 * - "If you would draw a card, instead..."
 * - "If a creature would die, exile it instead"
 * - "If one or more counters would be put on a creature, put twice that many instead"
 * - "Damage that would be dealt is prevented"
 *
 * Usage:
 * ```kotlin
 * val effect = ReplacementEffect.DrawReplacement(
 *     sourceId = EntityId.of("notion_thief"),
 *     description = "If an opponent would draw a card, instead that player skips that draw and you draw a card",
 *     appliesToPlayer = { playerId, state -> playerId != controllerId },
 *     replacement = { event, state -> /* create replacement events */ }
 * )
 * ```
 */
@Serializable
sealed interface ReplacementEffect {
    /** The source permanent/spell that creates this effect */
    val sourceId: EntityId

    /** Human-readable description of the effect */
    val description: String

    /** Check if this replacement applies to a given event */
    fun appliesTo(event: GameActionEvent, state: GameState): Boolean

    /** Apply the replacement, returning the modified event(s) */
    fun apply(event: GameActionEvent, state: GameState): ReplacementResult
}

/**
 * The result of applying a replacement effect.
 */
@Serializable
sealed interface ReplacementResult {
    /**
     * The original event is replaced with new events.
     * An empty list means the event is completely prevented/skipped.
     */
    @Serializable
    data class Replaced(val newEvents: List<GameActionEvent>) : ReplacementResult

    /**
     * The event is modified in place (e.g., damage amount changed).
     */
    @Serializable
    data class Modified(val modifiedEvent: GameActionEvent) : ReplacementResult

    /**
     * The replacement doesn't apply - continue with original event.
     */
    @Serializable
    data object NotApplicable : ReplacementResult
}

/**
 * Replacement effect for damage events.
 *
 * Examples:
 * - Hardened Scales: "If one or more +1/+1 counters would be put on a creature you control,
 *   that many plus one +1/+1 counters are put on it instead."
 * - Fog: "Prevent all combat damage that would be dealt this turn."
 * - Dictate of the Twin Gods: "If a source would deal damage to a permanent or player,
 *   it deals double that damage instead."
 */
@Serializable
data class DamageReplacementEffect(
    override val sourceId: EntityId,
    override val description: String,
    val controllerId: EntityId,
    /** Filter: which damage events this applies to */
    val filter: DamageFilter,
    /** How to modify the damage amount (return 0 to prevent) */
    val modifyAmount: DamageModifier
) : ReplacementEffect {

    override fun appliesTo(event: GameActionEvent, state: GameState): Boolean {
        return when (event) {
            is GameActionEvent.DamageDealtToPlayer -> filter.matchesPlayerDamage(event, state, controllerId)
            is GameActionEvent.DamageDealtToCreature -> filter.matchesCreatureDamage(event, state, controllerId)
            else -> false
        }
    }

    override fun apply(event: GameActionEvent, state: GameState): ReplacementResult {
        return when (event) {
            is GameActionEvent.DamageDealtToPlayer -> {
                val newAmount = modifyAmount.modify(event.amount, event, state)
                if (newAmount == 0) {
                    ReplacementResult.Replaced(emptyList()) // Prevented
                } else if (newAmount != event.amount) {
                    ReplacementResult.Modified(event.copy(amount = newAmount))
                } else {
                    ReplacementResult.NotApplicable
                }
            }
            is GameActionEvent.DamageDealtToCreature -> {
                val newAmount = modifyAmount.modify(event.amount, event, state)
                if (newAmount == 0) {
                    ReplacementResult.Replaced(emptyList())
                } else if (newAmount != event.amount) {
                    ReplacementResult.Modified(event.copy(amount = newAmount))
                } else {
                    ReplacementResult.NotApplicable
                }
            }
            else -> ReplacementResult.NotApplicable
        }
    }
}

/**
 * Filter criteria for damage replacement effects.
 */
@Serializable
sealed interface DamageFilter {
    fun matchesPlayerDamage(event: GameActionEvent.DamageDealtToPlayer, state: GameState, controllerId: EntityId): Boolean
    fun matchesCreatureDamage(event: GameActionEvent.DamageDealtToCreature, state: GameState, controllerId: EntityId): Boolean

    /** All damage dealt */
    @Serializable
    data object AllDamage : DamageFilter {
        override fun matchesPlayerDamage(event: GameActionEvent.DamageDealtToPlayer, state: GameState, controllerId: EntityId) = true
        override fun matchesCreatureDamage(event: GameActionEvent.DamageDealtToCreature, state: GameState, controllerId: EntityId) = true
    }

    /** Damage dealt to creatures you control */
    @Serializable
    data object ToCreaturesYouControl : DamageFilter {
        override fun matchesPlayerDamage(event: GameActionEvent.DamageDealtToPlayer, state: GameState, controllerId: EntityId) = false
        override fun matchesCreatureDamage(event: GameActionEvent.DamageDealtToCreature, state: GameState, controllerId: EntityId): Boolean {
            val targetController = state.getComponent<com.wingedsheep.rulesengine.ecs.components.ControllerComponent>(event.targetId)
            return targetController?.controllerId == controllerId
        }
    }

    /** Damage dealt to you (the controller) */
    @Serializable
    data object ToYou : DamageFilter {
        override fun matchesPlayerDamage(event: GameActionEvent.DamageDealtToPlayer, state: GameState, controllerId: EntityId): Boolean {
            return event.targetId == controllerId
        }
        override fun matchesCreatureDamage(event: GameActionEvent.DamageDealtToCreature, state: GameState, controllerId: EntityId) = false
    }

    /** Damage from a specific source */
    @Serializable
    data class FromSource(val sourceEntityId: EntityId) : DamageFilter {
        override fun matchesPlayerDamage(event: GameActionEvent.DamageDealtToPlayer, state: GameState, controllerId: EntityId): Boolean {
            return event.sourceId == sourceEntityId
        }
        override fun matchesCreatureDamage(event: GameActionEvent.DamageDealtToCreature, state: GameState, controllerId: EntityId): Boolean {
            return event.sourceId == sourceEntityId
        }
    }
}

/**
 * How to modify damage amount.
 */
@Serializable
sealed interface DamageModifier {
    fun modify(originalAmount: Int, event: GameActionEvent, state: GameState): Int

    /** Prevent all damage (return 0) */
    @Serializable
    data object PreventAll : DamageModifier {
        override fun modify(originalAmount: Int, event: GameActionEvent, state: GameState) = 0
    }

    /** Double the damage */
    @Serializable
    data object Double : DamageModifier {
        override fun modify(originalAmount: Int, event: GameActionEvent, state: GameState) = originalAmount * 2
    }

    /** Halve the damage (rounded down) */
    @Serializable
    data object Halve : DamageModifier {
        override fun modify(originalAmount: Int, event: GameActionEvent, state: GameState) = originalAmount / 2
    }

    /** Reduce by a fixed amount */
    @Serializable
    data class ReduceBy(val amount: Int) : DamageModifier {
        override fun modify(originalAmount: Int, event: GameActionEvent, state: GameState) = maxOf(0, originalAmount - amount)
    }

    /** Add a fixed amount */
    @Serializable
    data class AddAmount(val amount: Int) : DamageModifier {
        override fun modify(originalAmount: Int, event: GameActionEvent, state: GameState) = originalAmount + amount
    }
}

/**
 * Replacement effect for card draw events.
 *
 * Examples:
 * - Notion Thief: "If an opponent would draw a card except the first one they draw
 *   in each of their draw steps, instead that player skips that draw and you draw a card."
 * - Spirit of the Labyrinth: "Each player can't draw more than one card each turn."
 */
@Serializable
data class DrawReplacementEffect(
    override val sourceId: EntityId,
    override val description: String,
    val controllerId: EntityId,
    /** Which players' draws this affects */
    val affectsPlayers: DrawPlayerFilter,
    /** What happens instead of the draw */
    val replacement: DrawReplacement
) : ReplacementEffect {

    override fun appliesTo(event: GameActionEvent, state: GameState): Boolean {
        return event is GameActionEvent.CardDrawn &&
               affectsPlayers.matches(event.playerId, controllerId, state)
    }

    override fun apply(event: GameActionEvent, state: GameState): ReplacementResult {
        if (event !is GameActionEvent.CardDrawn) return ReplacementResult.NotApplicable
        return replacement.apply(event, controllerId, state)
    }
}

@Serializable
sealed interface DrawPlayerFilter {
    fun matches(drawingPlayerId: EntityId, controllerId: EntityId, state: GameState): Boolean

    @Serializable
    data object Opponents : DrawPlayerFilter {
        override fun matches(drawingPlayerId: EntityId, controllerId: EntityId, state: GameState) =
            drawingPlayerId != controllerId
    }

    @Serializable
    data object You : DrawPlayerFilter {
        override fun matches(drawingPlayerId: EntityId, controllerId: EntityId, state: GameState) =
            drawingPlayerId == controllerId
    }

    @Serializable
    data object AllPlayers : DrawPlayerFilter {
        override fun matches(drawingPlayerId: EntityId, controllerId: EntityId, state: GameState) = true
    }
}

@Serializable
sealed interface DrawReplacement {
    fun apply(event: GameActionEvent.CardDrawn, controllerId: EntityId, state: GameState): ReplacementResult

    /** Skip the draw entirely */
    @Serializable
    data object Skip : DrawReplacement {
        override fun apply(event: GameActionEvent.CardDrawn, controllerId: EntityId, state: GameState) =
            ReplacementResult.Replaced(emptyList())
    }

    /** Redirect the draw to a different player */
    @Serializable
    data class RedirectTo(val playerId: EntityId) : DrawReplacement {
        override fun apply(event: GameActionEvent.CardDrawn, controllerId: EntityId, state: GameState) =
            ReplacementResult.Modified(event.copy(playerId = playerId))
    }
}

/**
 * Replacement effect for counter-adding events.
 *
 * Examples:
 * - Hardened Scales: "If one or more +1/+1 counters would be put on a creature you control,
 *   that many plus one +1/+1 counters are put on it instead."
 * - Doubling Season: "If an effect would create one or more tokens under your control,
 *   it creates twice that many instead. If an effect would put counters on a permanent you control,
 *   it puts twice that many instead."
 */
@Serializable
data class CounterReplacementEffect(
    override val sourceId: EntityId,
    override val description: String,
    val controllerId: EntityId,
    /** Which counter additions this affects */
    val filter: CounterFilter,
    /** How to modify the counter count */
    val modifier: CounterModifier
) : ReplacementEffect {

    override fun appliesTo(event: GameActionEvent, state: GameState): Boolean {
        return event is GameActionEvent.CounterAdded &&
               filter.matches(event, controllerId, state)
    }

    override fun apply(event: GameActionEvent, state: GameState): ReplacementResult {
        if (event !is GameActionEvent.CounterAdded) return ReplacementResult.NotApplicable

        val newCount = modifier.modify(event.count)
        return if (newCount != event.count) {
            ReplacementResult.Modified(event.copy(count = newCount))
        } else {
            ReplacementResult.NotApplicable
        }
    }
}

@Serializable
sealed interface CounterFilter {
    fun matches(event: GameActionEvent.CounterAdded, controllerId: EntityId, state: GameState): Boolean

    /** +1/+1 counters on creatures you control */
    @Serializable
    data object PlusOnePlusOneOnYourCreatures : CounterFilter {
        override fun matches(event: GameActionEvent.CounterAdded, controllerId: EntityId, state: GameState): Boolean {
            if (event.counterType != "PLUS_ONE_PLUS_ONE") return false
            val targetController = state.getComponent<com.wingedsheep.rulesengine.ecs.components.ControllerComponent>(event.entityId)
            return targetController?.controllerId == controllerId
        }
    }

    /** All counters on permanents you control */
    @Serializable
    data object AllCountersOnYourPermanents : CounterFilter {
        override fun matches(event: GameActionEvent.CounterAdded, controllerId: EntityId, state: GameState): Boolean {
            val targetController = state.getComponent<com.wingedsheep.rulesengine.ecs.components.ControllerComponent>(event.entityId)
            return targetController?.controllerId == controllerId
        }
    }
}

@Serializable
sealed interface CounterModifier {
    fun modify(originalCount: Int): Int

    /** Add N more counters */
    @Serializable
    data class AddN(val n: Int) : CounterModifier {
        override fun modify(originalCount: Int) = originalCount + n
    }

    /** Double the counters */
    @Serializable
    data object Double : CounterModifier {
        override fun modify(originalCount: Int) = originalCount * 2
    }
}
