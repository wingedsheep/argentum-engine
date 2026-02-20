package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Interface for effect executors.
 *
 * Each concrete effect type has a corresponding executor that handles its execution logic.
 * This follows the Strategy pattern, allowing effect execution to be modular and testable.
 *
 * @param T The specific effect type this executor handles
 */
interface EffectExecutor<T : Effect> {
    /**
     * The effect type this executor handles.
     * Used for automatic registration in the executor registry.
     */
    val effectType: KClass<T>

    /**
     * Execute the effect against the game state.
     *
     * @param state The current game state
     * @param effect The effect to execute
     * @param context The execution context (source, controller, targets, etc.)
     * @return The execution result with new state and events
     */
    fun execute(state: GameState, effect: T, context: EffectContext): ExecutionResult
}
