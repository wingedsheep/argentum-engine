package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AddCardTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for AddCardTypeEffect.
 * "That creature becomes an artifact in addition to its other types."
 *
 * Creates a floating effect on Layer.TYPE with AddType modification.
 */
class AddCardTypeExecutor : EffectExecutor<AddCardTypeEffect> {

    override val effectType: KClass<AddCardTypeEffect> = AddCardTypeEffect::class

    override fun execute(
        state: GameState,
        effect: AddCardTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.success(state)

        // Verify the target is still on the battlefield
        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddType(effect.cardType.uppercase()),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
