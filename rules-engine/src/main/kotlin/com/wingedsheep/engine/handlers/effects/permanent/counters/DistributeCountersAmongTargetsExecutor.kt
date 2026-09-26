package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DistributeCountersContinuation
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect
import kotlin.reflect.KClass

/**
 * Executor for DistributeCountersAmongTargetsEffect.
 * "Distribute N counters among one or more target creatures."
 *
 * The division is the controller's choice, announced as the spell or ability is put on the stack
 * (CR 601.2d; CR 603.3d for triggered abilities), with each target getting at least one. A
 * triggered ability announces it before it reaches the stack (see
 * `EffectAndTriggerContinuationResumer`), and it arrives here as `context.damageDistribution` — the
 * stack object's announced division, shared with divided damage. It is honored verbatim: a target
 * that became illegal loses its share (CR 608.2b) and the survivors keep exactly what they were
 * assigned.
 *
 * With no announced division, a single target takes the whole pool and two or more targets are
 * asked for the division now, through the same [DistributeCountersContinuation] the resolution-time
 * distribute effects use.
 */
class DistributeCountersAmongTargetsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<DistributeCountersAmongTargetsEffect> {

    override val effectType: KClass<DistributeCountersAmongTargetsEffect> = DistributeCountersAmongTargetsEffect::class

    override fun execute(
        state: GameState,
        effect: DistributeCountersAmongTargetsEffect,
        context: EffectContext
    ): EffectResult {
        val targetIds = context.targets
            .map { it.toEntityId() }
            .filter { state.getEntity(it) != null }

        if (targetIds.isEmpty()) {
            return EffectResult.success(state)
        }

        val announced = context.damageDistribution
        if (announced != null) {
            val stillLegal = targetIds.toSet()
            return placeCounters(state, effect, context, announced.filterKeys { it in stillLegal })
        }

        // The pool is evaluated once, at resolution — an X-scaled pool (Grove's Bounty) reads the
        // X that was paid when the spell went on the stack.
        val totalCounters = amountEvaluator.evaluate(state, effect.totalCounters, context)
        if (totalCounters <= 0) {
            return EffectResult.success(state)
        }

        if (targetIds.size == 1) {
            return placeCounters(state, effect, context, mapOf(targetIds.single() to totalCounters))
        }

        val sourceId = context.sourceId ?: context.controllerId
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Effect"
        val decision = { decisionId: String -> DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Distribute $totalCounters ${effect.counterType.printed} " +
                "counter${if (totalCounters != 1) "s" else ""} among ${targetIds.size} targets",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = totalCounters,
            targets = targetIds,
            minPerTarget = effect.minPerTarget,
            allowPartial = false
        ) }
        val continuation = DistributeCountersContinuation(
            sourceId = sourceId,
            controllerId = context.controllerId,
            counterType = effect.counterType,
            removeFromSource = false,
            objectReferences = context.objectReferences
        )
        return EffectResult.from(state.suspendForDecision(decision, continuation, eventType = "DISTRIBUTE"))
    }

    private fun placeCounters(
        state: GameState,
        effect: DistributeCountersAmongTargetsEffect,
        context: EffectContext,
        distribution: Map<EntityId, Int>
    ): EffectResult {
        val counterType = effect.counterType
        var currentState = state
        val events = mutableListOf<GameEvent>()

        for ((targetId, countersForTarget) in distribution) {
            if (countersForTarget <= 0) continue

            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                currentState, targetId, counterType, countersForTarget, placerId = context.controllerId,
                predicateEvaluator = amountEvaluator.predicates
            )

            val current = currentState.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            val firstThisTurn = DamageUtils.isFirstCounterThisTurn(currentState, targetId)
            currentState = currentState.updateEntity(targetId) { container ->
                container.with(current.withAdded(counterType, modifiedCount))
            }
            currentState = DamageUtils.markCounterPlacedOnCreature(currentState, context.controllerId, targetId, counterType)

            val entityName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""
            events.add(CountersAddedEvent(targetId, counterType, modifiedCount, entityName, firstThisTurn, placedBy = context.controllerId))
        }

        return EffectResult.success(currentState, events)
    }
}
