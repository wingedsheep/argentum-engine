package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.ChangeColorEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChangeColorEffect].
 *
 * Resolves the target and creates a Layer-5 floating effect that replaces its colors with
 * the configured set for the given duration. An empty color set turns the target colorless.
 */
class ChangeColorExecutor : EffectExecutor<ChangeColorEffect> {

    override val effectType: KClass<ChangeColorEffect> = ChangeColorEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeColorEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (!state.getBattlefield().contains(targetId)) return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.COLOR,
            modification = SerializableModification.ChangeColor(effect.colors),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
