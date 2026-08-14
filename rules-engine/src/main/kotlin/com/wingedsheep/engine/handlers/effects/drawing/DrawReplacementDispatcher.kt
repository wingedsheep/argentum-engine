package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.DrawReplacementRemainingDrawsContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ProcessorResult
import com.wingedsheep.engine.replacement.ReplacementEffectIdentity
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.replacement.ReplacementOutcome
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyDrawAmount
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Runs the draw-replacement checks that fire before each individual card
 * is drawn.
 *
 * Delegates entirely to [ReplacementEffectProcessor], which handles both
 * mandatory replacements (PreventDraw, ModifyDrawAmount, non-optional
 * ReplaceDrawWithEffect) and optional replacements (Parallel Thoughts-style
 * yes/no prompts).
 *
 * The dispatcher is called by [DrawLoop] during each iteration of a multi-draw
 * sequence, shared by both [DrawCardsExecutor] and [DrawPhaseManager].
 */
class DrawReplacementDispatcher(
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)?,
    /**
     * The game's [ReplacementEffectProcessor]. Deliberately has no default: the processor is
     * meant to be a single central instance per game (see
     * [com.wingedsheep.engine.core.EngineServices.replacementEffectProcessor]), and a default
     * here is how a second one quietly appears. It is stateless today; requiring the caller to
     * name its source is what keeps it safe for it to stop being so.
     */
    private val processor: ReplacementEffectProcessor
) {
    /**
     * The outcome of [checkBeforeDraw].
     */
    sealed interface DispatchResult {
        /** No replacement fires — caller should perform a primitive draw. */
        data object None : DispatchResult

        /**
         * A replacement completed synchronously. The caller should **not**
         * perform a primitive draw this iteration; it should fold [state] and
         * [events] into its running state and proceed to the next iteration.
         */
        data class Replaced(val state: GameState, val events: List<GameEvent>) : DispatchResult

        /**
         * A replacement emitted a decision and the draw is paused. The caller
         * must return this result (possibly with its own events prepended).
         */
        data class Paused(val result: EffectResult) : DispatchResult

        /**
         * A [ModifyDrawAmount] replacement adjusted the draw count. The caller
         * should add [delta] to the remaining draws and re-check without drawing
         * a card this iteration.
         */
        data class Modified(val state: GameState, val delta: Int) : DispatchResult
    }

    /**
     * Run replacement checks for a draw, delegating entirely to
     * [ReplacementEffectProcessor].
     *
     * @param drawsLeftIncludingThis the number of draws remaining including the
     *     current one (i.e., `count - i` in an outer `for (i in 0 until count)`
     *     loop).
     * @param drawnCardsSoFar cards already drawn before this iteration, used
     *     for continuation state and partial-draw event flushing.
     * @param isDrawStep whether this is the active player's draw-step draw
     *     (vs a spell/ability draw).
     */
    fun checkBeforeDraw(
        state: GameState,
        playerId: EntityId,
        drawsLeftIncludingThis: Int,
        drawnCardsSoFar: List<EntityId>,
        isDrawStep: Boolean,
        context: EffectContext? = null
    ): DispatchResult {
        val remainingDraws = drawsLeftIncludingThis - 1
        val event = PendingGameEvent.DrawPending(
            playerId = playerId,
            count = 1,
            remainingDraws = remainingDraws,
            isDrawStep = isDrawStep,
            drawnCardsSoFar = drawnCardsSoFar
        )
        when (val processorResult = processor.process(state, event, context)) {
            is ProcessorResult.Paused -> {
                // Player must choose between competing replacements (CR 616.1)
                // or answer a yes/no prompt for an optional replacement.
                return DispatchResult.Paused(
                    EffectResult.paused(processorResult.state, processorResult.decision)
                )
            }
            is ProcessorResult.Resolved -> {
                when (val outcome = processorResult.outcome) {
                    is ReplacementOutcome.Consumed -> {
                        // Draw prevented, consume NextUse shield if applicable.
                        val state = consumeIfFloating(processorResult.state, processorResult.identity)
                        return DispatchResult.Replaced(state, emptyList())
                    }
                    is ReplacementOutcome.Replaced -> {
                        // A replacement effect should execute instead of drawing.
                        val ctx = processorResult.executionContext
                        if (ctx != null) {
                            return executeReplacement(
                                processorResult.state, playerId,
                                outcome.newEffect, ctx,
                                remainingDraws, isDrawStep,
                                processorResult.identity
                            )
                        }
                        return DispatchResult.Replaced(processorResult.state, emptyList())
                    }
                    is ReplacementOutcome.Modified -> {
                        // Unreachable by construction: the only replacement that produces a
                        // Modified outcome is ModifyDrawAmount, whose `appliesTo` is typed as
                        // EventPattern.DrawCardsEvent and so can never match DrawPending. Kept
                        // as an error rather than a fallthrough because a per-card count
                        // modification does not terminate — the loop would re-check an
                        // unchanged game state and re-apply the same effect forever.
                        error(
                            "Per-card draw replacement produced a Modified outcome " +
                                "(${outcome.modifiedEvent::class.simpleName}); draw-count " +
                                "modification belongs at the announcement (CR 121.2a)"
                        )
                    }
                }
            }
            is ProcessorResult.Pass -> {
                /* No replacement matched. */
            }
        }

        return DispatchResult.None
    }

    /**
     * Check replacement effects at the draw instruction announcement site (CR 121.2a).
     *
     * Called **before** the per-card [DrawLoop] fires, this sends a single
     * [PendingGameEvent.DrawAmountPending] with the **total** draw count so that
     * `ModifyDrawAmount` effects like Quantum Riddler ("draw that many cards
     * plus one instead") can modify the instruction's total before any
     * individual card is drawn.
     *
     * @param state Current game state
     * @param playerId The player who would draw the cards
     * @param totalCount The total number of cards the instruction says to draw
     * @param isDrawStep Whether this is the draw-step draw
     * @param context Optional execution context
     * @return A [DispatchResult] guiding the caller, or `null` when no
     *         total-count replacement fires (proceed to per-card loop).
     */
    fun checkDrawAmount(
        state: GameState,
        playerId: EntityId,
        totalCount: Int,
        isDrawStep: Boolean = false,
        context: EffectContext? = null
    ): DispatchResult? {
        if (totalCount <= 0) return null

        val event = PendingGameEvent.DrawAmountPending(
            playerId = playerId,
            totalCount = totalCount,
            isDrawStep = isDrawStep
        )
        when (val processorResult = processor.process(state, event, context)) {
            is ProcessorResult.Paused -> {
                return DispatchResult.Paused(
                    EffectResult.paused(processorResult.state, processorResult.decision)
                )
            }
            is ProcessorResult.Resolved -> {
                when (val outcome = processorResult.outcome) {
                    is ReplacementOutcome.Modified -> {
                        val modifiedEvent = outcome.modifiedEvent as? PendingGameEvent.DrawAmountPending
                        val newTotal = modifiedEvent?.totalCount ?: totalCount
                        val delta = newTotal - totalCount
                        if (delta != 0) {
                            val state = consumeIfFloating(processorResult.state, processorResult.identity)
                            return DispatchResult.Modified(state, delta)
                        }
                    }
                    is ReplacementOutcome.Replaced -> {
                        val ctx = processorResult.executionContext
                        if (ctx != null) {
                            return executeReplacement(
                                processorResult.state, playerId,
                                outcome.newEffect, ctx,
                                totalCount - 1, isDrawStep,
                                processorResult.identity
                            )
                        }
                        return DispatchResult.Replaced(processorResult.state, emptyList())
                    }
                    is ReplacementOutcome.Consumed -> {
                        val state = consumeIfFloating(processorResult.state, processorResult.identity)
                        return DispatchResult.Replaced(state, emptyList())
                    }
                }
            }
            is ProcessorResult.Pass -> {
                // No replacement matched.
            }
        }
        return null
    }

    /**
     * Consume a NextUse floating-effect shield from state if [identity] is a
     * [ReplacementEffectIdentity.FloatingIdentity]. This is a no-op for all
     * other identity types (battlefield, granted, self-redirect).
     */
    private fun consumeIfFloating(
        state: GameState,
        identity: ReplacementEffectIdentity?
    ): GameState {
        if (identity is ReplacementEffectIdentity.FloatingIdentity) {
            return processor.consumeFloatingEffect(state, identity.floatingId)
        }
        return state
    }

    /**
     * Execute the effect a [ReplacementOutcome.Replaced] put in the draw's place, using the
     * execution context built by the [ReplacementEffectProcessor] — from floating-shield data
     * for a Words-cycle shield, from the affected player otherwise. Pushes a
     * [DrawReplacementRemainingDrawsContinuation] if more draws remain.
     *
     * Returns the appropriate [DispatchResult].
     */
    private fun executeReplacement(
        processorState: GameState,
        playerId: EntityId,
        replacementEffect: Effect,
        context: EffectContext,
        remainingDraws: Int,
        isDrawStep: Boolean,
        identity: ReplacementEffectIdentity? = null
    ): DispatchResult {
        val executor = effectExecutor ?: return DispatchResult.Replaced(processorState, emptyList())

        // Confirm execution before consuming the shield (point of no return).
        // NextUse shields must be consumed here (not in the processor) so that
        // a null effectExecutor doesn't silently burn the shield.
        var state = consumeIfFloating(processorState, identity)
        if (remainingDraws > 0) {
            state = state.pushContinuation(
                DrawReplacementRemainingDrawsContinuation(
                    drawingPlayerId = playerId,
                    remainingDraws = remainingDraws,
                    isDrawStep = isDrawStep,
                    // Tail of an instruction already announced by executeDraws (CR 121.2a).
                    announcementApplied = true
                )
            )
        }

        // Execute the stored replacement effect.
        // The processor has already stamped the activeReplacementChain onto state
        // (containing all effects applied in this chain), so nested effect execution
        // won't re-trigger them. Clear the chain after execution.
        val pipelineResult = executor(state, replacementEffect, context)
        if (pipelineResult.isPaused) {
            // Clear chain on pause so subsequent draw iterations are unaffected.
            val clearedState = pipelineResult.state.copy(activeReplacementChain = null)
            return DispatchResult.Paused(
                EffectResult.paused(clearedState, pipelineResult.pendingDecision!!, pipelineResult.events)
            )
        }

        // Pipeline completed synchronously — pop remaining-draws continuation
        var resultState = pipelineResult.state
        if (remainingDraws > 0) {
            val (popped, stateAfterPop) = resultState.popContinuation()
            if (popped is DrawReplacementRemainingDrawsContinuation) {
                resultState = stateAfterPop
            }
        }

        // Clear the active replacement chain so subsequent draw iterations
        // (and any continuations) start with a clean slate.
        resultState = resultState.copy(activeReplacementChain = null)

        return DispatchResult.Replaced(resultState, pipelineResult.events)
    }

}
