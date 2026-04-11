package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.ClassLevelChangedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import kotlin.reflect.KClass

/**
 * Executor for the LevelUpClassEffect.
 * Advances a Class enchantment to the specified target level.
 */
class LevelUpClassExecutor(
    private val staticAbilityHandler: StaticAbilityHandler? = null
) : EffectExecutor<LevelUpClassEffect> {
    override val effectType: KClass<LevelUpClassEffect> = LevelUpClassEffect::class

    override fun execute(
        state: GameState,
        effect: LevelUpClassEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId ?: return ExecutionResult.success(state)
        val container = state.getEntity(sourceId) ?: return ExecutionResult.success(state)
        val classComponent = container.get<ClassLevelComponent>() ?: return ExecutionResult.success(state)

        // Only level up if the target level is exactly one above current
        if (effect.targetLevel != classComponent.currentLevel + 1) {
            return ExecutionResult.success(state)
        }

        var newState = state.updateEntity(sourceId) { c ->
            c.with(classComponent.withLevelUp())
        }

        // Re-add continuous effects and replacement effects to include any from the new class level
        if (staticAbilityHandler != null) {
            newState = newState.updateEntity(sourceId) { c ->
                var updated = staticAbilityHandler.addContinuousEffectComponent(c)
                updated = staticAbilityHandler.addReplacementEffectComponent(updated)
                updated
            }
        }

        val controllerId = container.get<ControllerComponent>()?.playerId ?: return ExecutionResult.success(newState)

        return ExecutionResult.success(
            newState,
            listOf(ClassLevelChangedEvent(sourceId, effect.targetLevel, controllerId))
        )
    }
}
