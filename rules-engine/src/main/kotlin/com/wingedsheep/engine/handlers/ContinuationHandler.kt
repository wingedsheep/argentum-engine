package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.continuations.*
import com.wingedsheep.engine.state.GameState

/**
 * Handles resumption of execution after a player decision.
 *
 * When the engine pauses for player input, it pushes a ContinuationFrame
 * onto the state's continuation stack. When the player submits their decision,
 * this handler pops the frame and resumes execution based on the frame type.
 *
 * Delegates to specialized resumer modules via the ContinuationResumerRegistry.
 */
class ContinuationHandler(
    private val services: EngineServices
) {

    private val effectRunner = EffectContinuationRunner(services.effectExecutorRegistry)

    private val registry = ContinuationResumerRegistry().apply {
        // Core engine resumers
        registerModule(EffectAndTriggerContinuationResumer(services, effectRunner))
        registerModule(MiscContinuationResumer(services, effectRunner))

        // Core engine auto-resumers
        registerAutoResumerModule(CoreAutoResumerModule(services, effectRunner))

        // Specialized resumer modules
        registerModule(CombatContinuationResumer(services))
        registerModule(ColorChoiceContinuationResumer(services))
        val chainResumer = ChainSpellContinuationResumer(services)
        registerModule(chainResumer)
        registerAutoResumerModule(chainResumer)
        registerModule(CreatureTypeChoiceContinuationResumer(services))
        registerModule(DrawReplacementContinuationResumer(services))
        registerModule(CardSpecificContinuationResumer(services))
        registerModule(DiscardAndDrawContinuationResumer(services))
        registerModule(StateBasedContinuationResumer(services))
        registerModule(SacrificeAndPayContinuationResumer(services))
        registerModule(ManaPaymentContinuationResumer(services))
        registerModule(LibraryAndZoneContinuationResumer(services))
        registerModule(ModalAndCloneContinuationResumer(services))
        registerModule(CastModalContinuationResumer(services))
        registerModule(TokenContinuationResumer(services))
    }

    /**
     * Resume execution after a decision is submitted.
     *
     * @param state The game state with the pending decision already cleared
     * @param response The player's decision response
     * @return The result of resuming execution
     */
    fun resume(state: GameState, response: DecisionResponse): ExecutionResult {
        val (continuation, stateAfterPop) = state.popContinuation()

        if (continuation == null) {
            return ExecutionResult.success(state)
        }

        if (continuation.decisionId != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${continuation.decisionId}, got ${response.decisionId}"
            )
        }

        return registry.resume(stateAfterPop, continuation, response, ::checkForMoreContinuations)
    }

    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        return registry.tryAutoResume(state, events, ::checkForMoreContinuations)
            ?: ExecutionResult.success(state, events)
    }
}
