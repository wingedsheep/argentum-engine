package com.wingedsheep.engine.handlers.effects.permanent.stats

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.SetBasePowerEffect
import kotlin.reflect.KClass

/**
 * Executor for SetBasePowerEffect.
 * "Change this creature's base power to target creature's power."
 *
 * Evaluates the dynamic power amount and creates a floating effect at
 * Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES that only sets the power,
 * leaving toughness unchanged.
 */
class SetBasePowerExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<SetBasePowerEffect> {

    override val effectType: KClass<SetBasePowerEffect> = SetBasePowerEffect::class

    override fun execute(
        state: GameState,
        effect: SetBasePowerEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return ExecutionResult.success(state)

        // Verify target is on the battlefield
        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val powerValue = amountEvaluator.evaluate(state, effect.power, context)

        val newState = state.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            modification = SerializableModification.SetPower(powerValue),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context,
            sublayer = Sublayer.SET_VALUES
        )

        return ExecutionResult.success(newState)
    }
}
