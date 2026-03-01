package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DistributeCountersContinuation
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.DistributeCountersFromSelfEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DistributeCountersFromSelfEffect.
 *
 * "Move any number of +1/+1 counters from this creature onto other creatures."
 *
 * Per Forgotten Ancient's rulings, this does not target — creatures are chosen at resolution.
 * The player distributes up to the total number of counters on the source among other creatures.
 */
class DistributeCountersFromSelfExecutor : EffectExecutor<DistributeCountersFromSelfEffect> {

    override val effectType: KClass<DistributeCountersFromSelfEffect> = DistributeCountersFromSelfEffect::class

    override fun execute(
        state: GameState,
        effect: DistributeCountersFromSelfEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for distribute counters effect")

        val counterType = try {
            CounterType.valueOf(
                effect.counterType.uppercase()
                    .replace(' ', '_')
                    .replace('+', 'P')
                    .replace('-', 'M')
                    .replace("/", "_")
            )
        } catch (e: IllegalArgumentException) {
            CounterType.PLUS_ONE_PLUS_ONE
        }

        // Check how many counters are on the source
        val sourceEntity = state.getEntity(sourceId)
            ?: return ExecutionResult.success(state, emptyList())
        val countersComponent = sourceEntity.get<CountersComponent>() ?: CountersComponent()
        val totalCounters = countersComponent.getCount(counterType)

        if (totalCounters <= 0) {
            return ExecutionResult.success(state, emptyList())
        }

        // Find all other creatures on the battlefield
        val otherCreatures = state.getBattlefield()
            .filter { it != sourceId }
            .filter { entityId ->
                val entity = state.getEntity(entityId) ?: return@filter false
                val card = entity.get<CardComponent>() ?: return@filter false
                card.isCreature
            }

        if (otherCreatures.isEmpty()) {
            return ExecutionResult.success(state, emptyList())
        }

        val sourceName = sourceEntity.get<CardComponent>()?.name ?: "Creature"

        val decisionId = UUID.randomUUID().toString()
        val decision = DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Distribute up to $totalCounters ${effect.counterType} counter${if (totalCounters != 1) "s" else ""} from $sourceName onto other creatures",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = totalCounters,
            targets = otherCreatures,
            minPerTarget = 0,
            allowPartial = true
        )

        val continuation = DistributeCountersContinuation(
            decisionId = decisionId,
            sourceId = sourceId,
            controllerId = context.controllerId,
            counterType = effect.counterType
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = context.controllerId,
                decisionType = "DISTRIBUTE",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(newState, decision, events)
    }
}
