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
 * If there's only one target, deal all damage directly.
 * If there are multiple targets, create a DistributeDecision for the player
 * to allocate damage (minimum 1 per target for damage effects).
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

        // Multiple targets - create a distribution decision
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
