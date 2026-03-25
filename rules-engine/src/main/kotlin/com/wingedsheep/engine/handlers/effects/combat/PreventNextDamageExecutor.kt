package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventNextDamageEffect.
 * "Prevent the next X damage that would be dealt to target creature this turn."
 *
 * Creates a floating effect with a PreventNextDamage shield that is consumed
 * as damage is dealt to the target. The shield expires at end of turn.
 */
class PreventNextDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<PreventNextDamageEffect> {

    override val effectType: KClass<PreventNextDamageEffect> = PreventNextDamageEffect::class

    override fun execute(
        state: GameState,
        effect: PreventNextDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = TargetResolutionUtils.resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "Could not resolve target for PreventNextDamageEffect")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) return ExecutionResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventNextDamage(amount),
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
