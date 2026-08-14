package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CollectEvidenceContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.state.GameState

/**
 * Finishes a resolution-time collect evidence (CR 701.59) once the player has picked which
 * graveyard cards to exile.
 *
 * The selection was already validated against the threshold by
 * `DecisionValidators.validateSelectCards` (via `SelectCardsDecision.minTotalManaValue`), and is
 * validated again here by [CollectEvidenceResolver.collect] — a `GameAction` is client-supplied, so
 * the payment path never trusts the response on its own.
 */
class CollectEvidenceContinuationResumer : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CollectEvidenceContinuation::class, ::resumeCollectEvidence)
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
            is CollectEvidenceResolver.Result.Success -> checkForMore(result.state, result.events)
            // Unreachable via the interactive path (the decision was only raised because a legal
            // collection existed, and the validator rejects an under-total submission), but a
            // graveyard can in principle change between the prompt and the response.
            is CollectEvidenceResolver.Result.Failure ->
                ExecutionResult.error(state, "Collect evidence failed: ${result.reason}")
        }
    }
}
