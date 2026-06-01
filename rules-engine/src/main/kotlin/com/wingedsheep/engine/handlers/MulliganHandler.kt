package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID

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
class MulliganHandler(
    /**
     * Card registry used to look up [com.wingedsheep.sdk.model.CardDefinition]s during the
     * leyline phase. Mulligan and bottoming themselves don't need the registry — it's only
     * consulted when scanning a player's opening hand for leyline-marked cards once all
     * players have kept and bottomed.
     */
    private val cardRegistry: CardRegistry? = null
) {

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
    ): ExecutionResult {
        val playerId = action.playerId
        val mullState = getMulliganState(state, playerId)

        // Validate player can mulligan
        if (mullState.hasKept) {
            return ExecutionResult.error(state, "Player has already kept their hand")
        }

        if (!mullState.canMulligan) {
            return ExecutionResult.error(state, "Cannot mulligan - hand size would be 0")
        }

        val events = mutableListOf<GameEvent>()
        var newState = state

        // 1. Put current hand on bottom of library
        val handKey = ZoneKey(playerId, Zone.HAND)
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)
        val hand = newState.getZone(handKey)

        for (cardId in hand) {
            newState = newState.removeFromZone(handKey, cardId)
            // Add to bottom of library (end of list, since first() = top)
            val library = newState.getZone(libraryKey)
            newState = newState.copy(zones = newState.zones + (libraryKey to library + listOf(cardId)))
        }

        // 2. Shuffle library (clearing any per-card reveals first)
        newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .clearLibraryReveals(newState, playerId)
        val (shuffledLibrary, shuffledState) = newState.nextRandom { shuffle(newState.getZone(libraryKey)) }
        newState = shuffledState.copy(zones = shuffledState.zones + (libraryKey to shuffledLibrary))
        events.add(LibraryShuffledEvent(playerId))

        // 3. Update mulligan count
        val newMullState = mullState.takeMulligan()
        newState = newState.updateEntity(playerId) { container ->
            container.with(newMullState)
        }

        // 4. Draw new hand of 7 cards (London Mulligan always draws 7)
        val drawCount = MulliganStateComponent.STARTING_HAND_SIZE
        val drawnCardIds = mutableListOf<EntityId>()

        repeat(drawCount) {
            val library = newState.getZone(libraryKey)
            if (library.isNotEmpty()) {
                val cardId = library.first()
                drawnCardIds.add(cardId)
                newState = newState.removeFromZone(libraryKey, cardId)
                newState = newState.addToZone(handKey, cardId)

                events.add(ZoneChangeEvent(
                    entityId = cardId,
                    entityName = newState.getEntity(cardId)
                        ?.get<CardComponent>()?.name ?: "Unknown",
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.HAND,
                    ownerId = playerId
                ))
            }
        }

        if (drawnCardIds.isNotEmpty()) {
            val cardNames = drawnCardIds.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(CardsDrawnEvent(playerId, drawnCardIds.size, drawnCardIds, cardNames))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Handle a KeepHand action.
     */
    fun handleKeepHand(
        state: GameState,
        action: KeepHand
    ): ExecutionResult {
        val playerId = action.playerId
        val mullState = getMulliganState(state, playerId)

        // Validate player hasn't already kept
        if (mullState.hasKept) {
            return ExecutionResult.error(state, "Player has already kept their hand")
        }

        // Mark player as having kept
        val newMullState = mullState.keep()
        val newState = state.updateEntity(playerId) { container ->
            container.with(newMullState)
        }

        return ExecutionResult.success(newState, emptyList())
    }

    /**
     * Handle a BottomCards action.
     */
    fun handleBottomCards(
        state: GameState,
        action: BottomCards
    ): ExecutionResult {
        val playerId = action.playerId
        val mullState = getMulliganState(state, playerId)

        // Validate player has kept
        if (!mullState.hasKept) {
            return ExecutionResult.error(state, "Player has not kept their hand yet")
        }

        // Validate correct number of cards
        if (action.cardIds.size != mullState.cardsToBottom) {
            return ExecutionResult.error(
                state,
                "Must put exactly ${mullState.cardsToBottom} cards on bottom, got ${action.cardIds.size}"
            )
        }

        // Validate cards are in player's hand
        val hand = state.getHand(playerId).toSet()
        val invalidCards = action.cardIds.filter { it !in hand }
        if (invalidCards.isNotEmpty()) {
            return ExecutionResult.error(state, "Cards not in hand: $invalidCards")
        }

        val events = mutableListOf<GameEvent>()
        var newState = state
        val handKey = ZoneKey(playerId, Zone.HAND)
        val libraryKey = ZoneKey(playerId, Zone.LIBRARY)

        // Move cards to bottom of library (in order specified)
        for (cardId in action.cardIds) {
            newState = newState.removeFromZone(handKey, cardId)
            // Add to bottom of library (end of list, since first() = top)
            val library = newState.getZone(libraryKey)
            newState = newState.copy(zones = newState.zones + (libraryKey to library + listOf(cardId)))

            events.add(ZoneChangeEvent(
                entityId = cardId,
                entityName = newState.getEntity(cardId)
                    ?.get<CardComponent>()?.name ?: "Unknown",
                fromZone = Zone.HAND,
                toZone = Zone.LIBRARY,
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

        return ExecutionResult.success(newState, events)
    }

    /**
     * Create a pending decision for mulligan.
     */
    fun createMulliganDecision(state: GameState, playerId: EntityId): SelectCardsDecision {
        val mullState = getMulliganState(state, playerId)
        val hand = state.getHand(playerId)
        val bottomCount = mullState.mulligansTaken + 1

        return SelectCardsDecision(
            id = "mulligan-${playerId.value}",
            playerId = playerId,
            prompt = if (mullState.mulligansTaken == 0) {
                "Keep hand or mulligan?"
            } else {
                "Keep hand (bottom $bottomCount) or mulligan?"
            },
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

    // =========================================================================
    // Leyline phase
    // =========================================================================

    /**
     * Whether any player still has pending leyline yes/no decisions, OR the leyline phase
     * hasn't been initiated yet for at least one player (but mulligans are complete). This
     * blocks the transition to turn 1 — see [com.wingedsheep.engine.handlers.actions.mulligan.KeepHandHandler]
     * and `BottomCardsHandler.checkMulliganCompletion`.
     */
    fun isInLeylinePhase(state: GameState): Boolean {
        return state.turnOrder.any { playerId ->
            val mullState = getMulliganState(state, playerId)
            mullState.hasPendingLeylineChoices
        }
    }

    /**
     * Populate each player's [MulliganStateComponent.pendingLeylineCardIds] by scanning
     * their opening hand for cards whose [com.wingedsheep.sdk.model.CardScript.mayStartOnBattlefield]
     * is true. Idempotent — sets `leylinePhaseStarted = true` so re-entry into the mulligan
     * completion path doesn't re-scan.
     *
     * Returns the updated state. Callers should then call [tryStartNextLeylineDecision] to
     * see whether a yes/no decision needs to be paused on.
     */
    fun initiateLeylinePhase(state: GameState): GameState {
        val registry = cardRegistry ?: return state
        var newState = state
        for (playerId in newState.turnOrder) {
            val mullState = getMulliganState(newState, playerId)
            if (mullState.leylinePhaseStarted) continue

            val hand = newState.getHand(playerId)
            val leylineCardIds = hand.filter { cardId ->
                val cardComponent = newState.getEntity(cardId)?.get<CardComponent>() ?: return@filter false
                val cardDef = registry.getCard(cardComponent.cardDefinitionId) ?: return@filter false
                cardDef.script.mayStartOnBattlefield
            }

            val updatedMullState = mullState.copy(
                leylinePhaseStarted = true,
                pendingLeylineCardIds = leylineCardIds
            )
            newState = newState.updateEntity(playerId) { container ->
                container.with(updatedMullState)
            }
        }
        return newState
    }

    /**
     * Find the next leyline yes/no decision that needs to be asked, in turn order starting
     * with the active player (CR 103.6: once mulligans are done, the starting player takes
     * their opening-hand actions first, then each other player in turn order). Returns null
     * if no decisions remain.
     *
     * The returned pair is `(playerId, cardId)` — the player making the choice and the
     * specific leyline card in their hand.
     */
    fun getNextLeylineChoice(state: GameState): Pair<EntityId, EntityId>? {
        val activePlayerId = state.activePlayerId
        val ordered = if (activePlayerId != null && state.turnOrder.contains(activePlayerId)) {
            val idx = state.turnOrder.indexOf(activePlayerId)
            state.turnOrder.subList(idx, state.turnOrder.size) + state.turnOrder.subList(0, idx)
        } else {
            state.turnOrder
        }
        for (playerId in ordered) {
            val mullState = getMulliganState(state, playerId)
            val firstLeyline = mullState.pendingLeylineCardIds.firstOrNull() ?: continue
            return playerId to firstLeyline
        }
        return null
    }

    /**
     * Try to advance the game past the mulligan/leyline setup.
     *
     * Called from [com.wingedsheep.engine.handlers.actions.mulligan.KeepHandHandler] and
     * [com.wingedsheep.engine.handlers.actions.mulligan.BottomCardsHandler] once their own
     * work completes. Three outcomes:
     *  - Mulligans/bottoming still pending → return the carried-through events; the next
     *    KeepHand / BottomCards action will resume the flow.
     *  - A leyline yes/no is still needed → pause with that decision.
     *  - Setup is done → call [TurnManager.advanceStep] to start turn 1.
     *
     * Centralising the logic here keeps the two handlers from drifting and gives the leyline
     * phase a single point of truth.
     */
    fun tryAdvancePastMulliganPhase(
        state: GameState,
        events: List<GameEvent>,
        turnManager: TurnManager
    ): ExecutionResult {
        if (isInMulliganPhase(state) || needsBottomCards(state)) {
            return ExecutionResult.success(state, events)
        }

        val stateWithLeylineScan = initiateLeylinePhase(state)
        val nextLeyline = getNextLeylineChoice(stateWithLeylineScan)
        if (nextLeyline != null) {
            val (playerId, cardId) = nextLeyline
            val (decision, continuation) = createLeylineDecision(stateWithLeylineScan, playerId, cardId)
                ?: return ExecutionResult.success(stateWithLeylineScan, events)
            val pausedState = stateWithLeylineScan
                .pushContinuation(continuation)
                .withPendingDecision(decision)
            return ExecutionResult.paused(
                pausedState,
                decision,
                events + DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        }

        val advanceResult = turnManager.advanceStep(stateWithLeylineScan)
        return ExecutionResult.success(
            advanceResult.newState,
            events + advanceResult.events
        )
    }

    /**
     * Build the [YesNoDecision] + [LeylineDecisionContinuation] pair for a specific leyline
     * card in a player's opening hand. The caller is responsible for pushing the continuation
     * and setting the pending decision on state.
     *
     * Returns null when the card no longer has a [CardComponent] (defensive — shouldn't happen).
     */
    fun createLeylineDecision(state: GameState, playerId: EntityId, leylineCardId: EntityId): Pair<YesNoDecision, LeylineDecisionContinuation>? {
        val cardName = state.getEntity(leylineCardId)?.get<CardComponent>()?.name ?: return null
        val decisionId = "leyline-${leylineCardId.value}-${UUID.randomUUID()}"
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Begin the game with $cardName on the battlefield?",
            context = DecisionContext(
                sourceId = leylineCardId,
                sourceName = cardName,
                phase = DecisionPhase.CASTING
            ),
            yesText = "Yes",
            noText = "No",
            hint = "Leyline — If this card is in your opening hand, you may begin the game with it on the battlefield."
        )
        val continuation = LeylineDecisionContinuation(
            decisionId = decisionId,
            playerId = playerId,
            leylineCardId = leylineCardId,
            cardName = cardName
        )
        return decision to continuation
    }
}
