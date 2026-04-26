package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ProliferateContinuation
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ProliferateEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [ProliferateEffect].
 *
 * "Choose any number of permanents and/or players, then give each another counter
 * of each kind already there."
 *
 * Resolution:
 * 1. Build the eligible set: every permanent on the battlefield and every player
 *    that has at least one counter of any kind on it.
 * 2. If empty, the effect is a no-op.
 * 3. Otherwise pause with a [SelectCardsDecision] (min=0, max=eligibleEntities.size,
 *    `useTargetingUI=true`) so the controller picks directly on the board.
 * 4. The continuation handler ([ProliferateContinuation]) reads the chosen entities
 *    and adds one counter of each existing kind via the resumer.
 */
class ProliferateExecutor : EffectExecutor<ProliferateEffect> {

    override val effectType: KClass<ProliferateEffect> = ProliferateEffect::class

    override fun execute(
        state: GameState,
        effect: ProliferateEffect,
        context: EffectContext
    ): EffectResult {
        val eligible = findEntitiesWithCounters(state)

        if (eligible.isEmpty()) {
            return EffectResult.success(state, emptyList())
        }

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Proliferate"

        val decisionId = UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Proliferate — choose any number of permanents and/or players that have a counter",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = eligible,
            minSelections = 0,
            maxSelections = eligible.size,
            useTargetingUI = true
        )

        val continuation = ProliferateContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            eligibleEntities = eligible
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = context.controllerId,
                decisionType = "PROLIFERATE",
                prompt = decision.prompt
            )
        )

        return EffectResult.paused(newState, decision, events)
    }

    companion object {
        /**
         * All battlefield permanents + all players that currently have at least one
         * counter of any kind.
         */
        fun findEntitiesWithCounters(state: GameState): List<com.wingedsheep.sdk.model.EntityId> {
            val permanents = state.getBattlefield().filter { entityId ->
                val counters = state.getEntity(entityId)?.get<CountersComponent>()
                counters != null && counters.counters.any { it.value > 0 }
            }
            val players = state.turnOrder.filter { playerId ->
                val counters = state.getEntity(playerId)?.get<CountersComponent>()
                counters != null && counters.counters.any { it.value > 0 }
            }
            return permanents + players
        }
    }
}
