package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TurnedFaceDownEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.TurnFaceDownEffect
import kotlin.reflect.KClass

/**
 * Executor for TurnFaceDownEffect.
 * "Turn target creature face down."
 *
 * Adds FaceDownComponent to the target creature. The creature becomes
 * a 2/2 colorless creature with no name, types, or abilities (Rule 707.2).
 * MorphDataComponent should already be present on the creature.
 */
class TurnFaceDownExecutor : EffectExecutor<TurnFaceDownEffect> {

    override val effectType: KClass<TurnFaceDownEffect> = TurnFaceDownEffect::class

    override fun execute(
        state: GameState,
        effect: TurnFaceDownEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for turn face down")

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target entity not found")

        // Already face-down — nothing to do
        if (container.has<FaceDownComponent>()) {
            return ExecutionResult.success(state)
        }

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId

        val newState = state.updateEntity(targetId) { c ->
            c.with(FaceDownComponent)
        }

        return ExecutionResult.success(
            newState,
            listOf(TurnedFaceDownEvent(targetId, controllerId))
        )
    }
}
