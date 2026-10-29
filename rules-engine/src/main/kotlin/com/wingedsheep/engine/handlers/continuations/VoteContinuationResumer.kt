package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PipelineOutputs
import com.wingedsheep.engine.handlers.effects.player.VoteExecutor
import com.wingedsheep.engine.state.GameState

/**
 * Resumes a [com.wingedsheep.sdk.scripting.effects.VoteEffect] one vote at a time: records and
 * announces the vote, asks the next player in turn order, and after the last vote publishes every
 * tally to the frame below so the rest of the spell or ability reads it.
 */
class VoteContinuationResumer : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(VoteContinuation::class, ::resumeVote)
    )

    fun resumeVote(
        state: GameState,
        continuation: VoteContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for a vote")
        }
        val choice = continuation.choices.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid vote index: ${response.optionIndex}")

        val tallies = continuation.tallies + (choice to (continuation.tallies[choice] ?: 0) + 1)
        val voted = VoteCastEvent(continuation.currentPlayerId, choice, continuation.sourceId, continuation.sourceName)

        // Skip a voter who left the game while an earlier vote was pending.
        val next = continuation.remainingVoters.dropWhile { it !in state.activePlayers }
        if (next.isNotEmpty()) {
            return VoteExecutor.askVote(
                state,
                continuation.copy(currentPlayerId = next.first(), remainingVoters = next.drop(1), tallies = tallies),
                events = listOf(voted),
            )
        }

        val numbers = continuation.voteCounts.mapValues { (_, tallied) -> tallies[tallied] ?: 0 }
        val newState = exposeOutputsToNextFrame(state, PipelineOutputs(numbers = numbers))
        return checkForMore(newState, listOf(voted))
    }
}
