package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import kotlin.reflect.KClass

/**
 * Executor for SetLifeTotalEffect.
 * Sets a player's life total to a specific amount.
 *
 * Per MTG Rule 118.5, setting a life total causes the player to gain or lose
 * the necessary amount of life. This executor emits the appropriate LifeChangedEvent.
 */
class SetLifeTotalExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<SetLifeTotalEffect> {

    override val effectType: KClass<SetLifeTotalEffect> = SetLifeTotalEffect::class

    override fun execute(
        state: GameState,
        effect: SetLifeTotalEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "Could not resolve target for SetLifeTotal")
        }

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (playerId in playerIds) {
            val currentLife = newState.getEntity(playerId)?.get<LifeTotalComponent>()?.life
                ?: continue

            val newLife = amountEvaluator.evaluate(newState, effect.amount, context)

            // Per Sunspine Lynx ruling: if an effect would set a player's life total
            // higher than their current life total while life gain is prevented, their
            // life total doesn't change.
            if (newLife > currentLife && DamageUtils.isLifeGainPrevented(newState, playerId)) {
                continue
            }

            if (newLife != currentLife) {
                newState = newState.updateEntity(playerId) { container ->
                    container.with(LifeTotalComponent(newLife))
                }

                val reason = if (newLife > currentLife) LifeChangeReason.LIFE_GAIN else LifeChangeReason.LIFE_LOSS
                events.add(LifeChangedEvent(playerId, currentLife, newLife, reason))
                if (newLife > currentLife) {
                    newState = DamageUtils.markLifeGainedThisTurn(newState, playerId)
                }
                if (newLife < currentLife) {
                    newState = DamageUtils.markLifeLostThisTurn(newState, playerId)
                }
            }
        }

        return EffectResult.success(newState, events)
    }
}
