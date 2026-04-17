package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveAllAbilitiesEffect.
 * "Target permanent loses all abilities until [duration]." Works for any permanent
 * type — Azure Beastbinder, for example, can strip abilities from artifacts and
 * planeswalkers, not just creatures. The card's target requirement enforces what
 * is a legal target; the executor only applies the floating effect.
 */
class RemoveAllAbilitiesExecutor : EffectExecutor<RemoveAllAbilitiesEffect> {

    override val effectType: KClass<RemoveAllAbilitiesEffect> = RemoveAllAbilitiesEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveAllAbilitiesEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RemoveAllAbilities,
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState, emptyList())
    }
}
