package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.BecomeChosenManaColorEffect
import kotlin.reflect.KClass

/**
 * Executor for [BecomeChosenManaColorEffect].
 *
 * Reads [EffectContext.manaColorChoice] (set when activating an "Add one mana of any color"
 * mana ability) and creates a Layer-5 floating effect that replaces the target's colors
 * with the chosen color for the configured duration.
 *
 * If no mana color was chosen (e.g., the effect is composed outside of a mana ability
 * activation), the effect is a no-op rather than an error so it stays composable.
 */
class BecomeChosenManaColorExecutor : EffectExecutor<BecomeChosenManaColorEffect> {

    override val effectType: KClass<BecomeChosenManaColorEffect> = BecomeChosenManaColorEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeChosenManaColorEffect,
        context: EffectContext
    ): EffectResult {
        val chosenColor = context.manaColorChoice ?: return EffectResult.success(state)
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (!state.getBattlefield().contains(targetId)) return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.COLOR,
            modification = SerializableModification.ChangeColor(setOf(chosenColor.name)),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
