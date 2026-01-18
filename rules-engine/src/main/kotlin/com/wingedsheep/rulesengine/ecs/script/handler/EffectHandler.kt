package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for executing a specific type of effect.
 *
 * Each effect type has a dedicated handler that knows how to execute it.
 * This follows the Open-Closed Principle: new effects can be added by
 * creating new handlers without modifying existing code.
 *
 * @param T The specific Effect subtype this handler executes
 */
interface EffectHandler<T : Effect> {
    /**
     * The effect class this handler supports.
     */
    val effectClass: KClass<T>

    /**
     * Execute the effect and return the new state with events.
     *
     * @param state The current game state
     * @param effect The effect to execute
     * @param context Execution context (controller, source, targets)
     * @return Result containing new state, events, and any temporary modifiers
     */
    fun execute(
        state: EcsGameState,
        effect: T,
        context: ExecutionContext
    ): ExecutionResult
}

/**
 * Base class for effect handlers that provides common utility methods.
 */
abstract class BaseEffectHandler<T : Effect> : EffectHandler<T> {

    /**
     * Create a no-op result (state unchanged, no events).
     */
    protected fun noOp(state: EcsGameState): ExecutionResult =
        ExecutionResult(state)

    /**
     * Create a result with the given state and events.
     */
    protected fun result(
        state: EcsGameState,
        vararg events: EcsEvent
    ): ExecutionResult = ExecutionResult(state, events.toList())

    /**
     * Create a result with the given state and event list.
     */
    protected fun result(
        state: EcsGameState,
        events: List<EcsEvent>
    ): ExecutionResult = ExecutionResult(state, events)
}
