package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState

class MultiPlayerContinuationResumer(
    private val ctx: ContinuationContext
) {

    /**
     * Resume after a player selected creature cards from their hand to reveal.
     *
     * Records the reveal count, then asks the next player (if any).
     * After all players have selected, creates tokens for each player.
     */
    fun resumeEachPlayerMayRevealCreatures(
        state: GameState,
        continuation: EachPlayerMayRevealCreaturesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for each-player-may-reveal-creatures")
        }

        val playerId = continuation.currentPlayerId
        val revealCount = response.selectedCards.size

        // Update reveal counts
        val updatedRevealCounts = if (revealCount > 0) {
            continuation.revealCounts + (playerId to revealCount)
        } else {
            continuation.revealCounts
        }

        // Ask the next player
        val remainingPlayers = continuation.remainingPlayers
        if (remainingPlayers.isNotEmpty()) {
            val nextResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerMayRevealCreaturesExecutor.askNextPlayer(
                state = state,
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                players = remainingPlayers,
                currentIndex = 0,
                revealCounts = updatedRevealCounts,
                tokenPower = continuation.tokenPower,
                tokenToughness = continuation.tokenToughness,
                tokenColors = continuation.tokenColors,
                tokenCreatureTypes = continuation.tokenCreatureTypes,
                tokenImageUri = continuation.tokenImageUri
            )
            return ExecutionResult(
                state = nextResult.newState,
                events = nextResult.events,
                pendingDecision = nextResult.pendingDecision,
                error = nextResult.error
            )
        }

        // All players have made their selection - create tokens
        val tokenResult = com.wingedsheep.engine.handlers.effects.library.EachPlayerMayRevealCreaturesExecutor.createTokensForAllPlayers(
            state = state,
            revealCounts = updatedRevealCounts,
            tokenPower = continuation.tokenPower,
            tokenToughness = continuation.tokenToughness,
            tokenColors = continuation.tokenColors,
            tokenCreatureTypes = continuation.tokenCreatureTypes,
            tokenImageUri = continuation.tokenImageUri
        )

        return checkForMore(tokenResult.newState, tokenResult.events)
    }

}
