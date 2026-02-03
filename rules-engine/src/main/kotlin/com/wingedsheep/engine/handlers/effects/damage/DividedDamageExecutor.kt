package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributeDamageContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.toEntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.DividedDamageEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DividedDamageEffect.
 *
 * "Deal X damage divided as you choose among N targets"
 *
 * Per MTG rules, damage distribution must be chosen as part of targeting (at cast time),
 * not when the spell resolves. This executor uses the pre-supplied distribution from
 * the EffectContext.
 *
 * If there's only one target, deal all damage directly (no distribution needed).
 * If there are multiple targets, use the pre-supplied damageDistribution from context.
 */
class DividedDamageExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<DividedDamageEffect> {

    override val effectType: KClass<DividedDamageEffect> = DividedDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DividedDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get the targets from context
        val targets = context.targets.map { it.toEntityId() }

        if (targets.isEmpty()) {
            return ExecutionResult.error(state, "No targets for divided damage")
        }

        // If there's only one target, deal all damage directly
        if (targets.size == 1) {
            return dealDamageToTarget(state, targets.first(), effect.totalDamage, context.sourceId)
        }

        // Multiple targets - use pre-supplied distribution from context
        val distribution = context.damageDistribution
        if (distribution == null) {
            // Fallback: This shouldn't happen with proper flow, but handle gracefully
            // by creating a decision (legacy behavior)
            return createDistributionDecision(state, effect, context, targets)
        }

        // Deal damage to each target per the distribution
        var currentState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for ((targetId, amount) in distribution) {
            if (amount > 0) {
                val result = dealDamageToTarget(currentState, targetId, amount, context.sourceId)
                if (!result.isSuccess) {
                    return result
                }
                currentState = result.newState
                events.addAll(result.events)
            }
        }

        return ExecutionResult.success(currentState, events)
    }

    /**
     * Legacy behavior: Create a DistributeDecision for backwards compatibility.
     * This should only be used if damageDistribution is not provided in context.
     */
    private fun createDistributionDecision(
        state: GameState,
        effect: DividedDamageEffect,
        context: EffectContext,
        targets: List<com.wingedsheep.sdk.model.EntityId>
    ): ExecutionResult {
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: "Effect"

        val decisionId = UUID.randomUUID().toString()
        val decision = DistributeDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Divide ${effect.totalDamage} damage among ${targets.size} targets",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            totalAmount = effect.totalDamage,
            targets = targets,
            minPerTarget = 1 // Per MTG rules, must assign at least 1 damage to each target
        )

        // Push continuation so we know how to resume
        val continuation = DistributeDamageContinuation(
            decisionId = decisionId,
            sourceId = context.sourceId,
            controllerId = context.controllerId,
            targets = targets
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
