package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AddCreatureTypeEffect
import kotlin.reflect.KClass

/**
 * Executor for AddCreatureTypeEffect.
 * "It becomes a Zombie in addition to its other types." — adds a creature subtype to a single target.
 */
class AddCreatureTypeExecutor : EffectExecutor<AddCreatureTypeEffect> {

    override val effectType: KClass<AddCreatureTypeEffect> = AddCreatureTypeEffect::class

    override fun execute(
        state: GameState,
        effect: AddCreatureTypeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val projected = state.projectedState
        if (!projected.isCreature(targetId)) {
            return ExecutionResult.success(state)
        }

        val subtype = if (effect.fromChosenValueKey != null) {
            context.pipeline.chosenValues[effect.fromChosenValueKey]
                ?: return ExecutionResult.success(state)
        } else {
            effect.subtype
        }

        val newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddSubtype(subtype),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
