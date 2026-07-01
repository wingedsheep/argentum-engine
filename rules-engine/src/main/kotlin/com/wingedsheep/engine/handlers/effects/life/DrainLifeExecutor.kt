package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DrainLifeEffect
import kotlin.reflect.KClass

/**
 * Executor for DrainLifeEffect.
 * "Each opponent loses X life. You gain life equal to the life lost this way."
 *
 * Each loss goes through [DamageUtils.loseLife] with life-loss modification applied, and the
 * gain is the sum of the amounts *actually* lost (read back from the emitted events), so a
 * `ModifyLifeLoss` replacement on one player is reflected in the total. The gain lands as a
 * single life-gain event after all losses (CR: "the life lost this way" is one amount).
 */
class DrainLifeExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DrainLifeEffect> {

    override val effectType: KClass<DrainLifeEffect> = DrainLifeEffect::class

    override fun execute(
        state: GameState,
        effect: DrainLifeEffect,
        context: EffectContext
    ): EffectResult {
        val fromPlayers = context.resolvePlayerTargets(effect.from, state)
        if (fromPlayers.isEmpty()) {
            return EffectResult.error(state, "No valid players for life drain")
        }

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<EngineGameEvent>()
        var totalLost = 0

        for (playerId in fromPlayers) {
            val (updatedState, event) = DamageUtils.loseLife(
                newState, playerId, amount,
                reason = LifeChangeReason.LIFE_LOSS,
                applyLifeLossModification = true,
            )
            newState = updatedState
            if (event != null) {
                totalLost += event.oldLife - event.newLife
                events.add(event)
            }
        }

        val gainerId = context.resolvePlayerTarget(effect.to, newState)
        if (gainerId != null && totalLost > 0) {
            val (updatedState, gainEvent) = DamageUtils.gainLife(newState, gainerId, totalLost)
            newState = updatedState
            if (gainEvent != null) events.add(gainEvent)
        }

        return EffectResult.success(newState, events)
    }
}
