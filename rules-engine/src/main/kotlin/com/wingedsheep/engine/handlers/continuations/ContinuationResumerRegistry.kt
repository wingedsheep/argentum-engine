package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.state.GameState
import kotlin.reflect.KClass

/**
 * Registry that maps continuation frame types to their resumers.
 *
 * This implements the Strategy pattern, allowing each continuation frame type
 * to have its own dedicated resumer while providing a unified dispatch mechanism.
 *
 * The registry uses a map-based dispatch system with modular sub-registries
 * for each category of continuations, reducing merge conflicts and enabling
 * dynamic resumer registration.
 */
class ContinuationResumerRegistry {
    private val resumers = mutableMapOf<KClass<out ContinuationFrame>, ContinuationResumer<*>>()
    private val autoResumers = mutableMapOf<KClass<out ContinuationFrame>, AutoResumer<*>>()

    /**
     * Register all resumers from a module.
     */
    fun registerModule(module: ContinuationResumerModule) {
        module.resumers().forEach { resumer ->
            resumers[resumer.frameType] = resumer
        }
    }

    /**
     * Register all auto-resumers from a module.
     */
    fun registerAutoResumerModule(module: AutoResumerModule) {
        module.autoResumers().forEach { autoResumer ->
            autoResumers[autoResumer.frameType] = autoResumer
        }
    }

    /**
     * Register a single resumer.
     */
    fun <T : ContinuationFrame> register(resumer: ContinuationResumer<T>) {
        resumers[resumer.frameType] = resumer
    }

    /**
     * Register a single auto-resumer.
     */
    fun <T : ContinuationFrame> registerAutoResumer(autoResumer: AutoResumer<T>) {
        autoResumers[autoResumer.frameType] = autoResumer
    }

    /**
     * Resume a continuation using the appropriate resumer.
     *
     * @param state The game state after popping the continuation
     * @param continuation The continuation frame to resume
     * @param response The player's decision response
     * @param checkForMore Callback to check for more continuations on the stack
     * @return The execution result with new state and events
     */
    @Suppress("UNCHECKED_CAST")
    fun resume(
        state: GameState,
        continuation: ContinuationFrame,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val resumer = resumers[continuation::class] as? ContinuationResumer<ContinuationFrame>
            ?: return ExecutionResult.error(state, "No resumer registered for continuation type: ${continuation::class.simpleName}")
        val references = continuation.objectReferences()?.copy(captured = true)
        val propagateThenContinue: CheckForMore = { nextState, events ->
            val updated = references?.authorize(events)
            checkForMore(if (updated == null) nextState else propagateObjectReferences(nextState, updated), events)
        }
        val result = resumer.resume(state, if (references == null) continuation else continuation.withObjectReferences(references), response, propagateThenContinue)
        val updated = references?.authorize(result.events)
        return if (updated == null) result else result.copy(state = propagateObjectReferences(result.newState, updated))
    }

    /**
     * Try to auto-resume the top continuation on the stack.
     *
     * Peeks at the stack top, finds a matching auto-resumer, checks [AutoResumer.canAutoResume],
     * pops the frame, and dispatches. Returns null if no matching auto-resumer or if
     * [canAutoResume] returns false.
     *
     * @param state The current game state (continuation still on stack)
     * @param events Accumulated events from prior processing
     * @param checkForMore Callback to recursively check for more continuations
     * @return The execution result, or null if no auto-resumer matched
     */
    @Suppress("UNCHECKED_CAST")
    fun tryAutoResume(
        state: GameState,
        events: List<GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult? {
        val top = state.peekContinuation() ?: return null
        val resumer = autoResumers[top::class] as? AutoResumer<ContinuationFrame> ?: return null
        if (!resumer.canAutoResume(top)) return null

        val (_, stateAfterPop) = state.popContinuation()
        val references = top.objectReferences()?.copy(captured = true)
        val propagateThenContinue: CheckForMore = { nextState, nextEvents ->
            val updated = references?.authorize(nextEvents.drop(events.size))
            checkForMore(if (updated == null) nextState else propagateObjectReferences(nextState, updated), nextEvents)
        }
        return resumer.autoResume(stateAfterPop, if (references == null) top else top.withObjectReferences(references), events, propagateThenContinue)
    }

    /**
     * Returns the number of registered resumers.
     */
    fun resumerCount(): Int = resumers.size
}
