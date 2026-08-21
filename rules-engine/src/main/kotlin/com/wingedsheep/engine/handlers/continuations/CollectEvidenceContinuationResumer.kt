package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseEvidenceAmountContinuation
import com.wingedsheep.engine.core.CollectEvidenceContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Finishes a resolution-time collect evidence (CR 701.59) once the player has picked which
 * graveyard cards to exile.
 *
 * The selection was already validated against the threshold by
 * `DecisionValidators.validateSelectCards` (via `SelectCardsDecision.minTotalManaValue`), and is
 * validated again here by [CollectEvidenceResolver.collect] — a `GameAction` is client-supplied, so
 * the payment path never trusts the response on its own.
 *
 * Also owns the first hop of the chosen-X shape ([ChooseEvidenceAmountContinuation]): pick X, then
 * fall into the very same card-selection path with X as the floor.
 */
class CollectEvidenceContinuationResumer : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CollectEvidenceContinuation::class, ::resumeCollectEvidence),
        resumer(ChooseEvidenceAmountContinuation::class, ::resumeChooseEvidenceAmount),
    )

    fun resumeCollectEvidence(
        state: GameState,
        continuation: CollectEvidenceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for collect evidence")
        }

        val result = CollectEvidenceResolver.collect(
            state = state,
            playerId = continuation.playerId,
            amount = continuation.amount,
            chosenCards = response.selectedCards,
            sourceName = continuation.sourceName,
        )

        return when (result) {
            is CollectEvidenceResolver.Result.Success ->
                checkForMore(
                    publishAmount(result.state, continuation.storeAmountAs, continuation.amount),
                    result.events,
                )
            // Unreachable via the interactive path (the decision was only raised because a legal
            // collection existed, and the validator rejects an under-total submission), but a
            // graveyard can in principle change between the prompt and the response.
            is CollectEvidenceResolver.Result.Failure ->
                ExecutionResult.error(state, "Collect evidence failed: ${result.reason}")
        }
    }

    /**
     * Hop one of "collect evidence **X**": the player has named X, now they pick the cards.
     *
     * X was bounded at the graveyard's total mana value when the prompt was raised, so it is
     * reachable by construction. Two cases skip the second prompt because no choice remains —
     * X of 0 (exile nothing) and X exactly equal to the whole graveyard (exile all of it) — which
     * mirrors the same shortcut in `CollectEvidenceExecutor`.
     */
    fun resumeChooseEvidenceAmount(
        state: GameState,
        continuation: ChooseEvidenceAmountContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is NumberChosenResponse) {
            return ExecutionResult.error(state, "Expected number response for collect evidence X")
        }

        val candidates = CollectEvidenceResolver.candidates(state, continuation.playerId)
        val chosen = response.number.coerceIn(0, candidates.totalManaValue)

        if (chosen == 0 || candidates.totalManaValue == chosen) {
            val cards = if (chosen == 0) emptyList() else candidates.cards
            return when (
                val result = CollectEvidenceResolver.collect(
                    state = state,
                    playerId = continuation.playerId,
                    amount = chosen,
                    chosenCards = cards,
                    sourceName = continuation.sourceName,
                )
            ) {
                is CollectEvidenceResolver.Result.Success ->
                    checkForMore(
                        publishAmount(result.state, continuation.storeAmountAs, chosen),
                        result.events,
                    )
                is CollectEvidenceResolver.Result.Failure ->
                    ExecutionResult.error(state, "Collect evidence failed: ${result.reason}")
            }
        }

        val decisionResult = DecisionHandler().createCardSelectionDecision(
            state = state,
            playerId = continuation.playerId,
            sourceId = null,
            sourceName = continuation.sourceName,
            prompt = "Collect evidence $chosen: exile cards with total mana value " +
                "$chosen or greater from your graveyard",
            options = candidates.cards,
            minSelections = 1,
            maxSelections = candidates.cards.size,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            minTotalManaValue = chosen,
        )

        return ExecutionResult.paused(
            decisionResult.state.pushContinuation(
                CollectEvidenceContinuation(
                    decisionId = decisionResult.pendingDecision!!.id,
                    playerId = continuation.playerId,
                    amount = chosen,
                    sourceName = continuation.sourceName,
                    storeAmountAs = continuation.storeAmountAs,
                )
            ),
            decisionResult.pendingDecision,
            decisionResult.events,
        )
    }

    /**
     * Republish the collected threshold so a composed follow-up — or the "when you do" half of a
     * reflexive trigger, which resolves from a fresh context on the far side of a stack round-trip
     * — can read it as `DynamicAmount.VariableReference(name)`. Routed through
     * [exposeCollectionsToNextFrame] rather than `DrawUpToExecutor.injectStoredNumber` because the
     * consumer here is usually a `ReflexiveTriggerTargetContinuation`, which that helper doesn't
     * recognize.
     */
    private fun publishAmount(state: GameState, name: String?, amount: Int): GameState =
        if (name == null) state else exposeCollectionsToNextFrame(
            state,
            collections = emptyMap<String, List<EntityId>>(),
            numbers = mapOf(name to amount),
        )
}
