package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.RemoveCountersFromAmongContinuation
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveCountersFromAmongEffect
import kotlin.reflect.KClass

/**
 * Executor for [RemoveCountersFromAmongEffect] — "you may remove any number of +1/+1 counters from
 * among other creatures you control" (Galloping Lizrog).
 *
 * Resolves the filtered group on the battlefield (projected state), keeps the permanents carrying at
 * least one counter of the kind, and asks the controller one [DistributeDecision] over them: each
 * permanent's share is capped at its own count (`maxPerTarget`) and any total, including none, is
 * legal (`allowPartial`). [RemoveCountersFromAmongContinuation] removes the chosen counters and stores
 * the total under the effect's `storeAmountAs`. With nothing eligible it stores 0 without asking.
 */
class RemoveCountersFromAmongExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<RemoveCountersFromAmongEffect> {

    override val effectType: KClass<RemoveCountersFromAmongEffect> = RemoveCountersFromAmongEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveCountersFromAmongEffect,
        context: EffectContext
    ): EffectResult {
        val counts = BattlefieldFilterUtils
            .findMatchingOnBattlefield(state, effect.filter, context, predicateEvaluator = predicateEvaluator)
            .associateWith { state.getEntity(it)?.get<CountersComponent>()?.getCount(effect.counterType) ?: 0 }
            .filterValues { it > 0 }

        if (counts.isEmpty()) {
            return EffectResult(state, emptyList(), updatedStoredNumbers = mapOf(effect.storeAmountAs to 0))
        }

        val sourceId = context.sourceId ?: context.controllerId
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Ability"
        val total = counts.values.sum()

        val decision = { decisionId: String -> DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Remove any number of ${effect.counterType.printed} counters from among " +
                "${effect.filter.description}s",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = total,
            targets = counts.keys.toList(),
            minPerTarget = 0,
            maxPerTarget = counts,
            allowPartial = true
        ) }

        val continuation = RemoveCountersFromAmongContinuation(
            controllerId = context.controllerId,
            counterType = effect.counterType,
            storeAmountAs = effect.storeAmountAs,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "DISTRIBUTE"))
    }
}
