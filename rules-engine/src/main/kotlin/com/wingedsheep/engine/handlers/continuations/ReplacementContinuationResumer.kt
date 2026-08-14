package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.*
import com.wingedsheep.engine.state.GameState

/**
 * Continuation resumers for the replacement effect system.
 *
 * Handles:
 * - [ReplacementChoiceContinuation] — player chose between competing replacements
 *   (decision-driven resumer)
 * - [ReplacementResolveContinuation] — after a replacement chain completes,
 *   resume the original context (auto-resumer)
 */
class ReplacementContinuationResumer(
    private val processor: ReplacementEffectProcessor,
    private val services: EngineServices
) : ContinuationResumerModule, AutoResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ReplacementChoiceContinuation::class, ::resumeReplacementChoice)
    )

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(ReplacementResolveContinuation::class) { state, continuation, events, checkForMore ->
            resumeReplacementResolve(state, continuation, events, checkForMore)
        }
    )

    /**
     * Resume after the player chose one of multiple competing replacement
     * effects (CR 616.1e).
     *
     * Delegates outcome computation to [ReplacementEffectProcessor.applySingle],
     * then manages lifecycle (NextUse shield consumption) before resuming
     * the original context.
     */
    private fun resumeReplacementChoice(
        state: GameState,
        continuation: ReplacementChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for replacement")
        }

        val chosenIndex = response.optionIndex
        if (chosenIndex < 0 || chosenIndex >= continuation.options.size) {
            return ExecutionResult.error(state, "Invalid replacement choice index: $chosenIndex")
        }

        val chosen = continuation.options[chosenIndex]

        // The processor's applySingle() builds the execution context from floating-shield
        // data when applicable, returning it in ProcessorResult.Resolved.executionContext.
        // Pass continuation.context for condition evaluation during recursive processing.
        val context = continuation.context

        // Push domain-specific remainder continuation (e.g. remaining draws
        // in the draw loop) before the replacement resolves, so it sits below
        // any ReplacementResolveContinuation in the stack and can resume after
        // the replacement effect completes.
        val stateWithRemaining = continuation.pendingEvent.remainderContinuation(state)
            ?.let { state.pushContinuation(it) }
            ?: state

        // Compute the outcome.
        val result = processor.applySingle(
            state = stateWithRemaining,
            gathered = chosen,
            event = continuation.pendingEvent,
            alreadyApplied = continuation.alreadyApplied
        )

        return when (result) {
            is ProcessorResult.Resolved -> {
                // Consume NextUse floating-effect shield if applicable (caller's lifecycle responsibility).
                val stateAfterLifecycle = if (result.identity is ReplacementEffectIdentity.FloatingIdentity) {
                    processor.consumeFloatingEffect(result.state, result.identity.floatingId)
                } else {
                    result.state
                }
                when (val outcome = result.outcome) {
                    is ReplacementOutcome.Replaced -> {
                        val execCtx = result.executionContext ?: context
                        handleReplacedOutcome(stateAfterLifecycle, outcome, execCtx, checkForMore)
                    }
                    is ReplacementOutcome.Consumed -> checkForMore(stateAfterLifecycle, emptyList())
                    is ReplacementOutcome.Modified -> {
                        // Unlike Replaced/Consumed — where the replacement *is* what happens —
                        // a Modified outcome leaves the (modified) event still to be performed.
                        // The call site that would have performed it returned when this paused,
                        // so the event supplies a frame that performs it on resume. Without
                        // this the whole instruction is silently dropped.
                        val performFrame = outcome.modifiedEvent.performContinuation(stateAfterLifecycle)
                        // CR 614.5 is per-event: this event is done being replaced, so the
                        // chain must not leak into the events performing it carries.
                        val cleared = stateAfterLifecycle.copy(activeReplacementChain = null)
                        val stateToResume = performFrame?.let { cleared.pushContinuation(it) } ?: cleared
                        checkForMore(stateToResume, emptyList())
                    }
                }
            }
            is ProcessorResult.Paused -> {
                ExecutionResult.paused(result.state, result.decision)
            }
            is ProcessorResult.Pass -> {
                // Shouldn't happen — the chosen effect was matched
                error("resumeReplacementChoice returned a Pass result")
            }
        }
    }

    /**
     * Auto-resume after a replacement chain has fully resolved. Pops the
     * [ReplacementResolveContinuation] and calls checkForMore so the original
     * execution context resumes.
     */
    private fun resumeReplacementResolve(
        state: GameState,
        continuation: ReplacementResolveContinuation,
        events: List<GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        // The new effect has completed executing. Resume the original context
        // by calling checkForMore.
        return checkForMore(state, events)
    }

    /**
     * Execute the replacement effect for a [ReplacementOutcome.Replaced],
     * then push a [ReplacementResolveContinuation] so the original context
     * resumes after the new effect completes.
     */
    private fun handleReplacedOutcome(
        state: GameState,
        outcome: ReplacementOutcome.Replaced,
        context: EffectContext?,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val resumeContinuation = ReplacementResolveContinuation(
            decisionId = "pending"
        )

        val stateWithResumeFrame = state.pushContinuation(resumeContinuation)

        // Execute the new effect
        if (context != null) {
            // The processor stamped activeReplacementChain onto stateWithResumeFrame
            // with all effects applied in this chain, so nested effect execution
            // won't re-trigger them. Clear the chain after execution so the
            // ReplacementResolveContinuation and any remaining draws resume fresh.
            val effectResult = services.effectExecutorRegistry.execute(stateWithResumeFrame, outcome.newEffect, context)
            if (effectResult.isPaused) {
                // Clear chain on pause so subsequent execution is unaffected.
                val clearedState = effectResult.state.copy(activeReplacementChain = null)
                return ExecutionResult(clearedState, effectResult.events, effectResult.error, effectResult.pendingDecision, effectResult.triggersAlreadyProcessed)
            }
            val clearedState = effectResult.state.copy(activeReplacementChain = null)
            return checkForMore(clearedState, effectResult.events)
        }

        return checkForMore(stateWithResumeFrame, emptyList())
    }
}
