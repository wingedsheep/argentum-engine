package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTargets
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect
import kotlin.reflect.KClass

/**
 * Executor for DealDamageToPlayersEffect.
 * Deals damage to players based on the target specification.
 *
 * Often composed with DealDamageToGroupEffect for cards like:
 * - Earthquake: DealDamageToGroupEffect(...).then(DealDamageToPlayersEffect(...))
 * - Fire Tempest: DealDamageToGroupEffect(6).then(DealDamageToPlayersEffect(6))
 */
class DealDamageToPlayersExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<DealDamageToPlayersEffect> {

    override val effectType: KClass<DealDamageToPlayersEffect> = DealDamageToPlayersEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageToPlayersEffect,
        context: EffectContext
    ): ExecutionResult {
        val damageAmount = amountEvaluator.evaluate(state, effect.amount, context)

        // If damage is 0, no damage is dealt
        if (damageAmount == 0) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        val playersToDamage = resolvePlayerTargets(effect.target, state, context)

        for (playerId in playersToDamage) {
            val result = dealDamageToTarget(newState, playerId, damageAmount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
