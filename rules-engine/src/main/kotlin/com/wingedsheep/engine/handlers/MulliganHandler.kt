package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles mulligan-related actions during game setup.
 *
 * The mulligan process follows the London Mulligan rule:
 * 1. Players draw 7 cards
 * 2. Each player (in turn order) decides to keep or mulligan
 * 3. If mulligan: shuffle hand into library, draw 7 again
 * 4. After all players keep, each player who mulliganed puts N cards
 *    on the bottom of their library (where N = mulligans taken)
 *
 * ## Usage
 * The handler processes three action types:
 * - [TakeMulligan]: Player shuffles hand back and redraws
 * - [KeepHand]: Player accepts current hand
 * - [BottomCards]: Player puts cards on bottom after keeping
 */
class MulliganHandler {

    /**
     * Check if the game is still in mulligan phase.
     */
    fun isInMulliganPhase(state: GameState): Boolean {
        return state.turnOrder.any { playerId ->
            val mullState = getMulliganState(state, playerId)
            !mullState.hasKept
        }
    }

    /**
     * Check if any players need to bottom cards after keeping.
     */
    fun needsBottomCards(state: GameState): Boolean {
        return state.turnOrder.any { playerId ->
            val mullState = getMulliganState(state, playerId)
            mullState.hasKept && mullState.cardsToBottom > 0
        }
    }

    /**
     * Get the next player who needs to make a mulligan decision.
     */
    fun getNextMulliganPlayer(state: GameState): EntityId? {
        return state.turnOrder.firstOrNull { playerId ->
            val mullState = getMulliganState(state, playerId)
            !mullState.hasKept
        }
    }

    /**
     * Get the next player who needs to bottom cards.
     */
    fun getNextBottomCardsPlayer(state: GameState): EntityId? {
        return state.turnOrder.firstOrNull { playerId ->
            val mullState = getMulliganState(state, playerId)
            mullState.hasKept && mullState.cardsToBottom > 0
        }
    }

    /**
     * Handle a TakeMulligan action.
     */
    fun handleTakeMulligan(
        state: GameState,
        action: TakeMulligan
    ): EngineResult {
        val playerId = action.playerId
        val mullState = getMulliganState(state, playerId)

        // Validate player can mulligan
        if (mullState.hasKept) {
            return EngineResult.Failure(
                state,
                FailureReason.RULE,
                "Player has already kept their hand"
            )
        }

        if (!mullState.canMulligan) {
            return EngineResult.Failure(
                state,
                FailureReason.RULE,
                "Cannot mulligan - hand size would be 0"
            )
        }

        val events = mutableListOf<GameEvent>()
        var newState = state

        // 1. Put current hand on bottom of library
        val handKey = ZoneKey(playerId, ZoneType.HAND)
        val libraryKey = ZoneKey(playerId, ZoneType.LIBRARY)
        val hand = newState.getZone(handKey)

        for (cardId in hand) {
            newState = newState.removeFromZone(handKey, cardId)
            // Add to bottom of library (beginning of list)
            val library = newState.getZone(libraryKey)
            newState = newState.copy(zones = newState.zones + (libraryKey to listOf(cardId) + library))
        }

        // 2. Shuffle library
        val shuffledLibrary = newState.getZone(libraryKey).shuffled()
        newState = newState.copy(zones = newState.zones + (libraryKey to shuffledLibrary))
        events.add(LibraryShuffledEvent(playerId))

        // 3. Update mulligan count
        val newMullState = mullState.takeMulligan()
        newState = newState.updateEntity(playerId) { container ->
            container.with(newMullState)
        }

        // 4. Draw new hand (7 - mulligans taken)
        val drawCount = newMullState.handSize
        val drawnCardIds = mutableListOf<EntityId>()

        repeat(drawCount) {
            val library = newState.getZone(libraryKey)
            if (library.isNotEmpty()) {
                val cardId = library.last()
                drawnCardIds.add(cardId)
                newState = newState.removeFromZone(libraryKey, cardId)
                newState = newState.addToZone(handKey, cardId)

                events.add(ZoneChangeEvent(
                    entityId = cardId,
                    entityName = newState.getEntity(cardId)
                        ?.get<CardComponent>()?.name ?: "Unknown",
                    fromZone = ZoneType.LIBRARY,
                    toZone = ZoneType.HAND,
                    ownerId = playerId
                ))
            }
        }

        if (drawnCardIds.isNotEmpty()) {
            events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds))
        }

        return EngineResult.Success(newState, events)
    }

    /**
     * Handle a KeepHand action.
     */
    fun handleKeepHand(
        state: GameState,
        action: KeepHand
    ): EngineResult {
        val playerId = action.playerId
        val mullState = getMulliganState(state, playerId)

        // Validate player hasn't already kept
        if (mullState.hasKept) {
            return EngineResult.Failure(
                state,
                FailureReason.RULE,
                "Player has already kept their hand"
            )
        }

        // Mark player as having kept
        val newMullState = mullState.keep()
        val newState = state.updateEntity(playerId) { container ->
            container.with(newMullState)
        }

        return EngineResult.Success(newState, emptyList())
    }

    /**
     * Handle a BottomCards action.
     */
    fun handleBottomCards(
        state: GameState,
        action: BottomCards
    ): EngineResult {
        val playerId = action.playerId
        val mullState = getMulliganState(state, playerId)

        // Validate player has kept
        if (!mullState.hasKept) {
            return EngineResult.Failure(
                state,
                FailureReason.RULE,
                "Player has not kept their hand yet"
            )
        }

        // Validate correct number of cards
        if (action.cardIds.size != mullState.cardsToBottom) {
            return EngineResult.Failure(
                state,
                FailureReason.RULE,
                "Must put exactly ${mullState.cardsToBottom} cards on bottom, got ${action.cardIds.size}"
            )
        }

        // Validate cards are in player's hand
        val hand = state.getHand(playerId).toSet()
        val invalidCards = action.cardIds.filter { it !in hand }
        if (invalidCards.isNotEmpty()) {
            return EngineResult.Failure(
                state,
                FailureReason.RULE,
                "Cards not in hand: $invalidCards"
            )
        }

        val events = mutableListOf<GameEvent>()
        var newState = state
        val handKey = ZoneKey(playerId, ZoneType.HAND)
        val libraryKey = ZoneKey(playerId, ZoneType.LIBRARY)

        // Move cards to bottom of library (in order specified)
        for (cardId in action.cardIds) {
            newState = newState.removeFromZone(handKey, cardId)
            // Add to bottom of library (beginning of list)
            val library = newState.getZone(libraryKey)
            newState = newState.copy(zones = newState.zones + (libraryKey to listOf(cardId) + library))

            events.add(ZoneChangeEvent(
                entityId = cardId,
                entityName = newState.getEntity(cardId)
                    ?.get<CardComponent>()?.name ?: "Unknown",
                fromZone = ZoneType.HAND,
                toZone = ZoneType.LIBRARY,
                ownerId = playerId
            ))
        }

        // Clear the mulligan count so we know cards have been bottomed
        val clearedMullState = MulliganStateComponent(
            mulligansTaken = 0,  // Reset after bottoming
            hasKept = true
        )
        newState = newState.updateEntity(playerId) { container ->
            container.with(clearedMullState)
        }

        return EngineResult.Success(newState, events)
    }

    /**
     * Create a pending decision for mulligan.
     */
    fun createMulliganDecision(state: GameState, playerId: EntityId): SelectCardsDecision {
        val mullState = getMulliganState(state, playerId)
        val hand = state.getHand(playerId)

        return SelectCardsDecision(
            id = "mulligan-${playerId.value}",
            playerId = playerId,
            prompt = "Mulligan (draw ${mullState.handSize - 1} cards) or keep?",
            context = DecisionContext(phase = DecisionPhase.CASTING),
            options = hand,
            minSelections = 0,  // 0 = keep, hand.size = mulligan
            maxSelections = hand.size
        )
    }

    /**
     * Create a pending decision for bottoming cards.
     */
    fun createBottomCardsDecision(state: GameState, playerId: EntityId): SelectCardsDecision {
        val mullState = getMulliganState(state, playerId)
        val hand = state.getHand(playerId)

        return SelectCardsDecision(
            id = "bottom-${playerId.value}",
            playerId = playerId,
            prompt = "Put ${mullState.cardsToBottom} card(s) on the bottom of your library",
            context = DecisionContext(phase = DecisionPhase.CASTING),
            options = hand,
            minSelections = mullState.cardsToBottom,
            maxSelections = mullState.cardsToBottom,
            ordered = true  // Order matters for bottom of library
        )
    }

    private fun getMulliganState(state: GameState, playerId: EntityId): MulliganStateComponent {
        return state.getEntity(playerId)
            ?.get<MulliganStateComponent>()
            ?: MulliganStateComponent()
    }
}
