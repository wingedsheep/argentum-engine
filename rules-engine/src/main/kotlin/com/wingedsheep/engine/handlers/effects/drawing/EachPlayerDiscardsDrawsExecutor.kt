package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EachPlayerSelectsThenDrawsContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.scripting.EachPlayerDiscardsDrawsEffect

/**
 * Executor for EachPlayerDiscardsDrawsEffect.
 *
 * Handles effects like Flux and Windfall where each player discards cards
 * and then draws based on how many they discarded.
 *
 * The executor creates a decision for the first player (in APNAP order),
 * then pushes a continuation to handle subsequent players and the final draws.
 */
class EachPlayerDiscardsDrawsExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<EachPlayerDiscardsDrawsEffect> {

    override fun execute(
        state: GameState,
        effect: EachPlayerDiscardsDrawsEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get players in APNAP order (active player first)
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        // Start with the first player
        return askPlayerToSelect(
            state = state,
            effect = effect,
            context = context,
            playerOrder = playerOrder,
            currentPlayerIndex = 0,
            drawAmounts = emptyMap()
        )
    }

    /**
     * Create a decision for a player to select cards to discard.
     */
    private fun askPlayerToSelect(
        state: GameState,
        effect: EachPlayerDiscardsDrawsEffect,
        context: EffectContext,
        playerOrder: List<com.wingedsheep.sdk.model.EntityId>,
        currentPlayerIndex: Int,
        drawAmounts: Map<com.wingedsheep.sdk.model.EntityId, Int>
    ): ExecutionResult {
        val playerId = playerOrder[currentPlayerIndex]
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val hand = state.getZone(handZone)

        // Determine selection bounds
        val minSelection: Int
        val maxSelection: Int

        if (effect.discardEntireHand) {
            // Must discard entire hand
            minSelection = hand.size
            maxSelection = hand.size
        } else {
            minSelection = effect.minDiscard
            maxSelection = effect.maxDiscard ?: hand.size
        }

        // If hand is empty or player must discard 0, skip to next player or finish
        if (hand.isEmpty() || (minSelection == 0 && maxSelection == 0)) {
            val newDrawAmounts = drawAmounts + (playerId to 0)
            return proceedToNextPlayerOrFinish(
                state = state,
                effect = effect,
                context = context,
                playerOrder = playerOrder,
                currentPlayerIndex = currentPlayerIndex,
                drawAmounts = newDrawAmounts
            )
        }

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val prompt = when {
            effect.discardEntireHand -> "Discard your hand"
            minSelection == 0 -> "Choose any number of cards to discard"
            minSelection == maxSelection -> "Choose $minSelection card${if (minSelection != 1) "s" else ""} to discard"
            else -> "Choose $minSelection to $maxSelection cards to discard"
        }

        // Create selection decision
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = hand,
            minSelections = minSelection,
            maxSelections = maxSelection.coerceAtMost(hand.size),
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Build continuation for remaining players
        val remainingPlayers = playerOrder.drop(currentPlayerIndex + 1)

        val continuation = EachPlayerSelectsThenDrawsContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            currentPlayerId = playerId,
            remainingPlayers = remainingPlayers,
            drawAmounts = drawAmounts,
            controllerBonusDraw = effect.controllerBonusDraw,
            minSelection = effect.minDiscard,
            maxSelection = effect.maxDiscard,
            selectionPrompt = prompt
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Move to the next player or finish the effect if all players have selected.
     */
    private fun proceedToNextPlayerOrFinish(
        state: GameState,
        effect: EachPlayerDiscardsDrawsEffect,
        context: EffectContext,
        playerOrder: List<com.wingedsheep.sdk.model.EntityId>,
        currentPlayerIndex: Int,
        drawAmounts: Map<com.wingedsheep.sdk.model.EntityId, Int>
    ): ExecutionResult {
        val nextIndex = currentPlayerIndex + 1

        return if (nextIndex < playerOrder.size) {
            // Ask next player
            askPlayerToSelect(
                state = state,
                effect = effect,
                context = context,
                playerOrder = playerOrder,
                currentPlayerIndex = nextIndex,
                drawAmounts = drawAmounts
            )
        } else {
            // All players have selected, execute draws
            executeDraws(state, drawAmounts, effect.controllerBonusDraw, context.controllerId)
        }
    }

    companion object {
        /**
         * Execute draws for all players based on their discard counts.
         * Called after all players have made their selections.
         */
        fun executeDraws(
            state: GameState,
            drawAmounts: Map<com.wingedsheep.sdk.model.EntityId, Int>,
            controllerBonusDraw: Int,
            controllerId: com.wingedsheep.sdk.model.EntityId
        ): ExecutionResult {
            var currentState = state
            val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

            // Draw cards for each player
            for ((playerId, count) in drawAmounts) {
                if (count > 0) {
                    val drawResult = drawCards(currentState, playerId, count)
                    currentState = drawResult.state
                    allEvents.addAll(drawResult.events)

                    // Check for draw failure (empty library)
                    if (!drawResult.isSuccess) {
                        return ExecutionResult(currentState, allEvents, drawResult.error)
                    }
                }
            }

            // Controller draws bonus cards
            if (controllerBonusDraw > 0) {
                val bonusResult = drawCards(currentState, controllerId, controllerBonusDraw)
                currentState = bonusResult.state
                allEvents.addAll(bonusResult.events)

                if (!bonusResult.isSuccess) {
                    return ExecutionResult(currentState, allEvents, bonusResult.error)
                }
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        /**
         * Draw cards for a player.
         */
        private fun drawCards(
            state: GameState,
            playerId: com.wingedsheep.sdk.model.EntityId,
            count: Int
        ): ExecutionResult {
            var newState = state
            val drawnCards = mutableListOf<com.wingedsheep.sdk.model.EntityId>()

            val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
            val handZone = ZoneKey(playerId, ZoneType.HAND)

            repeat(count) {
                val library = newState.getZone(libraryZone)
                if (library.isEmpty()) {
                    // Failed to draw - loss condition
                    return ExecutionResult.success(
                        newState,
                        listOf(com.wingedsheep.engine.core.DrawFailedEvent(playerId, "Empty library"))
                    )
                }

                val cardId = library.first()
                drawnCards.add(cardId)

                newState = newState.removeFromZone(libraryZone, cardId)
                newState = newState.addToZone(handZone, cardId)
            }

            return ExecutionResult.success(
                newState,
                listOf(com.wingedsheep.engine.core.CardsDrawnEvent(playerId, drawnCards.size, drawnCards))
            )
        }
    }
}
