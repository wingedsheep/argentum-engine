package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeCountersContinuation
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DistributeCountersFromSelfEffect
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
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "No source for distribute counters effect")

        val counterType = effect.counterType

        // Check how many counters are on the source
        val sourceEntity = state.getEntity(sourceId)
            ?: return EffectResult.success(state, emptyList())
        val countersComponent = sourceEntity.get<CountersComponent>() ?: CountersComponent()
        val totalCounters = countersComponent.getCount(counterType)

        if (totalCounters <= 0) {
            return EffectResult.success(state, emptyList())
        }

        // Find all other creatures on the battlefield, by their current types: a manifested
        // noncreature card is a creature, an animated land is one too (CR 708.2, 613.1d).
        val projected = state.projectedState
        val otherCreatures = state.getBattlefield()
            .filter { it != sourceId && projected.isCreature(it) }

        if (otherCreatures.isEmpty()) {
            return EffectResult.success(state, emptyList())
        }

        val sourceName = sourceEntity.get<CardComponent>()?.name ?: "Creature"

        val decision = { decisionId: String -> DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Distribute up to $totalCounters ${effect.counterType.printed} counter${if (totalCounters != 1) "s" else ""} from $sourceName onto other creatures",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = totalCounters,
            targets = otherCreatures,
            minPerTarget = 0,
            allowPartial = true
        ) }

        val continuation = DistributeCountersContinuation(
            sourceId = sourceId,
            controllerId = context.controllerId,
            counterType = effect.counterType
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "DISTRIBUTE"))
    }
}
