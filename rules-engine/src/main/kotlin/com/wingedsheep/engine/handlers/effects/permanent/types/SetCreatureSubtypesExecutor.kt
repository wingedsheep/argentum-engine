package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import kotlin.reflect.KClass

/**
 * Executor for SetCreatureSubtypesEffect.
 * "It becomes a Bird Giant." — sets creature subtypes on a single target.
 */
class SetCreatureSubtypesExecutor : EffectExecutor<SetCreatureSubtypesEffect> {

    override val effectType: KClass<SetCreatureSubtypesEffect> = SetCreatureSubtypesEffect::class

    override fun execute(
        state: GameState,
        effect: SetCreatureSubtypesEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val projected = state.projectedState
        if (!projected.isCreature(targetId)) {
            return EffectResult.success(state)
        }

        // Resolve subtypes: from chosen value in context, or from hardcoded field
        val subtypes = if (effect.fromChosenValueKey != null) {
            val chosen = context.pipeline.chosenValues[effect.fromChosenValueKey]
                ?: return EffectResult.success(state)
            setOf(chosen)
        } else {
            effect.subtypes
        }

        val newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.SetCreatureSubtypes(subtypes),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
