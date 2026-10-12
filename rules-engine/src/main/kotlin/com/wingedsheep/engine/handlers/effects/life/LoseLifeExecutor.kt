package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for LoseLifeEffect.
 * "You lose X life" or "Target player loses X life"
 */
class LoseLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<LoseLifeEffect> {

    override val effectType: KClass<LoseLifeEffect> = LoseLifeEffect::class

    override fun execute(
        state: GameState,
        effect: LoseLifeEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid target for life loss")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (playerId in playerIds) {
            val (updatedState, event) = DamageUtils.loseLife(
                newState, playerId, amount,
                reason = if (effect.origin == com.wingedsheep.sdk.scripting.effects.LifeLossOrigin.RADIATION)
                    LifeChangeReason.RADIATION else LifeChangeReason.LIFE_LOSS,
                applyLifeLossModification = true,
                predicateEvaluator = amountEvaluator.predicates
            )
            newState = updatedState
            if (event != null) events.add(event)
        }

        return EffectResult.success(newState, events)
    }
}
