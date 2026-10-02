package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ExileCounteredSpellInstead
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * The owed rest of one application of a replacement effect whose printed text goes on to *do*
 * something — [com.wingedsheep.sdk.scripting.PreventDamage.onPrevented] ("You gain life equal to
 * the damage prevented this way") and [ExileCounteredSpellInstead.then] ("and you may play that
 * card without paying its mana cost").
 *
 * @property effect the rest of the replacement
 * @property hostId the permanent whose ability applied — the effect's source
 * @property controllerId that permanent's controller when it applied — "you"
 * @property subjectId what the replacement acted on: the permanent or player the damage would have
 *   been dealt to ("that creature"), or the card exiled instead of countered ("that card")
 * @property amount how much damage this application prevented; 0 where nothing is counted
 */
@Serializable
data class PendingReplacementRider(
    val effect: Effect,
    val hostId: EntityId,
    val controllerId: EntityId,
    val subjectId: EntityId,
    val amount: Int = 0,
    val replacementChain: Set<ReplacementEffectIdentity>? = null
)

/**
 * Runs the replacement results queued on [GameState.pendingReplacementRiders].
 *
 * The rest of a replacement effect is part of that same replacement: it doesn't use the stack, and
 * nobody gets priority between the replaced event and the rest. The engine's damage arithmetic and
 * its counter routine can't run an effect, so they queue one here instead. It runs at the end of
 * the effect that caused the event ([com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry.execute])
 * — before the next instruction of the resolving spell or ability — or at the settle boundary
 * ([com.wingedsheep.engine.core.Settler]) for combat damage, which isn't an effect. Both come before
 * state-based actions and trigger detection, so a 2/2 that Vigor keeps from 3 damage survives with
 * its counters, and a Hostility token's entry is seen by enter triggers.
 */
object ReplacementRiders {

    /**
     * Execute and clear every queued rider, oldest first. A rider that asks a question leaves the
     * rest queued for the next drain and returns the paused result.
     */
    fun drain(
        state: GameState,
        execute: (GameState, Effect, EffectContext) -> EffectResult
    ): EffectResult {
        var current = state
        val events = mutableListOf<GameEvent>()
        while (current.pendingReplacementRiders.isNotEmpty()) {
            val rider = current.pendingReplacementRiders.first()
            val remaining = current.pendingReplacementRiders.drop(1)
            current = current.copy(pendingReplacementRiders = emptyList())
            if (remaining.isNotEmpty()) current = current.pushContinuation(
                com.wingedsheep.engine.core.ReplacementRidersContinuation(remaining))
            val previousChain = current.activeReplacementChain
            val result = execute(current.copy(activeReplacementChain = rider.replacementChain ?: previousChain),
                rider.effect, contextFor(current, rider))
            events += result.events
            current = result.state
            if (result.outcome is Outcome.Paused) return EffectResult.propagatePause(current, events)
            current = current.copy(activeReplacementChain = previousChain)
            if (remaining.isNotEmpty()) {
                check(current.peekContinuation() is com.wingedsheep.engine.core.ReplacementRidersContinuation)
                current = current.popContinuation().second.copy(
                    pendingReplacementRiders = current.pendingReplacementRiders + remaining)
            }
            if (result.outcome !is Outcome.Done) return result.copy(state = current, events = events)
        }
        return EffectResult.success(current, events)
    }

    private fun contextFor(state: GameState, rider: PendingReplacementRider): EffectContext {
        val subjectIsPlayer = rider.subjectId in state.turnOrder
        return EffectContext(
            sourceId = rider.hostId,
            controllerId = rider.controllerId,
            triggeringEntityId = rider.subjectId.takeUnless { subjectIsPlayer },
            triggeringPlayerId = rider.subjectId.takeIf { subjectIsPlayer },
            triggerContext = TriggerContext(
                triggeringEntityId = rider.subjectId,
                damageAmount = rider.amount
            ),
            pipeline = PipelineState(
                storedCollections = mapOf(ExileCounteredSpellInstead.EXILED_CARD to listOf(rider.subjectId))
            )
        )
    }
}
