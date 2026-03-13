package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTargets
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for DealDamageEffect.
 * Handles both fixed and dynamic damage amounts, and both single-target and
 * multi-player targets (e.g., PlayerRef(Player.Each), PlayerRef(Player.EachOpponent)).
 */
class DealDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DealDamageEffect> {

    override val effectType: KClass<DealDamageEffect> = DealDamageEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return ExecutionResult.success(state)
        }

        // Use damageSource override if specified (e.g., EnchantedCreature for Lavamancer's Skill)
        val damageSourceTarget = effect.damageSource
        val sourceId = if (damageSourceTarget != null) {
            EffectExecutorUtils.resolveTarget(damageSourceTarget, context, state)
        } else {
            context.sourceId
        }

        // For PlayerRef targets, resolve to potentially multiple players
        if (effect.target is EffectTarget.PlayerRef) {
            val playerIds = resolvePlayerTargets(effect.target, state, context)
            if (playerIds.isEmpty()) {
                return ExecutionResult.error(state, "No valid target for damage")
            }

            var newState = state
            val events = mutableListOf<EngineGameEvent>()
            for (playerId in playerIds) {
                val result = dealDamageToTarget(newState, playerId, amount, sourceId, effect.cantBePrevented)
                newState = result.newState
                events.addAll(result.events)
            }
            return ExecutionResult.success(newState, events)
        }

        // Single target resolution
        val targetId = EffectExecutorUtils.resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "No valid target for damage")

        return dealDamageToTarget(state, targetId, amount, sourceId, effect.cantBePrevented)
    }
}
