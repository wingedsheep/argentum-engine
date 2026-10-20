package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeCountersContinuation
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongFilteredEffect
import kotlin.reflect.KClass

/**
 * Executor for [DistributeCountersAmongFilteredEffect].
 *
 * "Distribute N counters among permanents matching a filter, chosen at resolution." Unlike
 * [DistributeCountersFromSelfExecutor] this creates *new* counters on the chosen recipients (nothing
 * is removed from a source — the continuation runs with `removeFromSource = false`), and unlike the
 * targets-based [DistributeCountersAmongTargetsExecutor] the eligible recipients are resolved here
 * from the battlefield via the filter (using projected state). No-op when nothing matches.
 *
 * Crashing Wave: "distribute three stun counters among any number of tapped creatures your opponents
 * control" — the recipients are the tapped opponent creatures at resolution (including any just
 * tapped by the same spell), and `minPerTarget = 0` lets the controller pile all three on one.
 * The pool is evaluated once, at resolution (Feast of the Victorious Dead's "that many").
 */
class DistributeCountersAmongFilteredExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<DistributeCountersAmongFilteredEffect> {

    override val effectType: KClass<DistributeCountersAmongFilteredEffect> =
        DistributeCountersAmongFilteredEffect::class

    override fun execute(
        state: GameState,
        effect: DistributeCountersAmongFilteredEffect,
        context: EffectContext
    ): EffectResult {
        val totalCounters = amountEvaluator.evaluate(state, effect.totalCounters, context)
        if (totalCounters <= 0) {
            return EffectResult.success(state, emptyList())
        }

        val eligible = BattlefieldFilterUtils.findMatchingOnBattlefield(state, effect.filter, context, predicateEvaluator = amountEvaluator.predicates)
        if (eligible.isEmpty()) {
            return EffectResult.success(state, emptyList())
        }

        // sourceId is only carried for the decision context; with removeFromSource = false nothing is
        // taken off it. Fall back to the controller if the resolving source is unavailable.
        val sourceId = context.sourceId ?: context.controllerId
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Spell"

        val decision = { decisionId: String -> DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Distribute ${totalCounters} ${effect.counterType.printed} " +
                "counter${if (totalCounters != 1) "s" else ""} among ${effect.filter.description}s",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = totalCounters,
            targets = eligible,
            minPerTarget = effect.minPerTarget,
            // All N must be placed (distribute "three counters", not "up to three").
            allowPartial = false
        ) }

        val continuation = DistributeCountersContinuation(
            sourceId = sourceId,
            controllerId = context.controllerId,
            counterType = effect.counterType,
            removeFromSource = false
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "DISTRIBUTE"))
    }
}
