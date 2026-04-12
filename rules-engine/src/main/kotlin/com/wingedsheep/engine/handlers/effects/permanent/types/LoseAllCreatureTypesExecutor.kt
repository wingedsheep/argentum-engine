package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.LoseAllCreatureTypesEffect
import kotlin.reflect.KClass

/**
 * Executor for LoseAllCreatureTypesEffect.
 * "Target creature loses all creature types until end of turn."
 *
 * Uses SetCreatureSubtypes(emptySet()) modification to remove all creature subtypes
 * without adding any new ones.
 */
class LoseAllCreatureTypesExecutor : EffectExecutor<LoseAllCreatureTypesEffect> {

    override val effectType: KClass<LoseAllCreatureTypesEffect> = LoseAllCreatureTypesEffect::class

    override fun execute(
        state: GameState,
        effect: LoseAllCreatureTypesEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.SetCreatureSubtypes(emptySet()),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
