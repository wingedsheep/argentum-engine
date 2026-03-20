package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.continuations.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.stack.StackResolver
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
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver = StackResolver(),
    private val triggerProcessor: com.wingedsheep.engine.event.TriggerProcessor? = null,
    private val triggerDetector: com.wingedsheep.engine.event.TriggerDetector? = null,
    private val combatManager: CombatManager? = null,
    private val targetFinder: TargetFinder = TargetFinder()
) {

    private val effectRunner = EffectContinuationRunner(effectExecutorRegistry)

    private val ctx = ContinuationContext(
        effectExecutorRegistry = effectExecutorRegistry,
        stackResolver = stackResolver,
        triggerProcessor = triggerProcessor,
        triggerDetector = triggerDetector,
        combatManager = combatManager,
        targetFinder = targetFinder
    )

    private val registry = ContinuationResumerRegistry().apply {
        // Core engine resumers
        registerModule(EffectAndTriggerContinuationResumer(ctx, effectRunner))
        registerModule(MiscContinuationResumer(ctx, effectRunner))

        // Core engine auto-resumers
        registerAutoResumerModule(CoreAutoResumerModule(ctx, effectRunner))

        // Specialized resumer modules
        registerModule(CombatContinuationResumer(ctx))
        registerModule(ColorChoiceContinuationResumer(ctx))
        registerModule(ChainSpellContinuationResumer(ctx))
        registerModule(CreatureTypeChoiceContinuationResumer(ctx))
        registerModule(DrawReplacementContinuationResumer(ctx))
        registerModule(CardSpecificContinuationResumer(ctx))
        registerModule(DiscardAndDrawContinuationResumer(ctx))
        registerModule(StateBasedContinuationResumer(ctx))
        registerModule(SacrificeAndPayContinuationResumer(ctx))
        registerModule(ManaPaymentContinuationResumer(ctx))
        registerModule(LibraryAndZoneContinuationResumer(ctx))
        registerModule(ModalAndCloneContinuationResumer(ctx))
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
