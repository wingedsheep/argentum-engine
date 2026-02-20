package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReadTheRunesEffect
import kotlin.reflect.KClass

/**
 * Executor for ReadTheRunesEffect.
 *
 * Handles "Draw X cards. For each card drawn this way, discard a card unless you sacrifice a permanent."
 *
 * Flow:
 * 1. Draw X cards, tracking actual cards drawn
 * 2. For each card drawn, iterate:
 *    a. If player has permanents and cards in hand: ask to sacrifice a permanent (0-1 selection)
 *       - If they select 1: sacrifice it
 *       - If they select 0: proceed to discard choice
 *    b. If player has permanents but no cards in hand: must sacrifice (1 selection)
 *    c. If player has cards but no permanents: must discard (1 selection)
 *    d. If neither: skip
 */
class ReadTheRunesExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ReadTheRunesEffect> {

    override val effectType: KClass<ReadTheRunesEffect> = ReadTheRunesEffect::class

    override fun execute(
        state: GameState,
        effect: ReadTheRunesEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val xValue = context.xValue ?: 0

        if (xValue <= 0) {
            return ExecutionResult.success(state)
        }

        // Draw X cards
        val drawResult = DrawCardsExecutor().executeDraws(state, playerId, xValue)
        if (drawResult.error != null) {
            return drawResult
        }

        // Count actual cards drawn from the events
        val cardsDrawnEvent = drawResult.events.filterIsInstance<CardsDrawnEvent>()
            .firstOrNull { it.playerId == playerId }
        val actualDrawn = cardsDrawnEvent?.count ?: 0

        if (actualDrawn <= 0) {
            return drawResult
        }

        // Start the iterative choice loop
        return startChoiceLoop(
            state = drawResult.state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            remainingChoices = actualDrawn,
            priorEvents = drawResult.events
        )
    }

    companion object {
        private val decisionHandlerStatic = DecisionHandler()

        /**
         * Start or continue the discard-or-sacrifice choice loop.
         */
        fun startChoiceLoop(
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            remainingChoices: Int,
            priorEvents: List<GameEvent> = emptyList()
        ): ExecutionResult {
            if (remainingChoices <= 0) {
                return ExecutionResult.success(state, priorEvents)
            }

            val handZone = ZoneKey(playerId, Zone.HAND)
            val hand = state.getZone(handZone)
            val permanents = findControlledPermanents(state, playerId)

            val hasCards = hand.isNotEmpty()
            val hasPermanents = permanents.isNotEmpty()

            return when {
                // Both options available: ask player to sacrifice (0 = discard instead)
                hasPermanents && hasCards -> askSacrificeChoice(
                    state, playerId, sourceId, sourceName, permanents, remainingChoices, priorEvents
                )
                // Only permanents: must sacrifice
                hasPermanents -> askForcedSacrifice(
                    state, playerId, sourceId, sourceName, permanents, remainingChoices, priorEvents
                )
                // Only cards: must discard
                hasCards -> askDiscardChoice(
                    state, playerId, sourceId, sourceName, hand, remainingChoices, priorEvents
                )
                // Neither: skip this iteration
                else -> startChoiceLoop(
                    state, playerId, sourceId, sourceName, remainingChoices - 1, priorEvents
                )
            }
        }

        /**
         * Ask player to optionally sacrifice a permanent (0-1 selection).
         * If they select 0, they'll discard instead.
         */
        private fun askSacrificeChoice(
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            permanents: List<EntityId>,
            remainingChoices: Int,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            val decisionResult = decisionHandlerStatic.createCardSelectionDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Sacrifice a permanent or select none to discard a card ($remainingChoices remaining)",
                options = permanents,
                minSelections = 0,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION,
                useTargetingUI = true
            )

            val continuation = ReadTheRunesContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingChoices = remainingChoices,
                phase = ReadTheRunesPhase.SACRIFICE_CHOICE
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                priorEvents + decisionResult.events
            )
        }

        /**
         * Force player to sacrifice a permanent (no cards in hand).
         */
        private fun askForcedSacrifice(
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            permanents: List<EntityId>,
            remainingChoices: Int,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            // If only 1 permanent, auto-sacrifice
            if (permanents.size == 1) {
                val permanentId = permanents.first()
                val result = sacrificePermanent(state, playerId, permanentId)
                return startChoiceLoop(
                    result.state, playerId, sourceId, sourceName,
                    remainingChoices - 1, priorEvents + result.events
                )
            }

            val decisionResult = decisionHandlerStatic.createCardSelectionDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Sacrifice a permanent ($remainingChoices remaining)",
                options = permanents,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION,
                useTargetingUI = true
            )

            val continuation = ReadTheRunesContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingChoices = remainingChoices,
                phase = ReadTheRunesPhase.SACRIFICE_CHOICE
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                priorEvents + decisionResult.events
            )
        }

        /**
         * Ask player to discard a card.
         */
        private fun askDiscardChoice(
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            hand: List<EntityId>,
            remainingChoices: Int,
            priorEvents: List<GameEvent>
        ): ExecutionResult {
            // If only 1 card, auto-discard
            if (hand.size == 1) {
                val cardId = hand.first()
                val result = discardCard(state, playerId, cardId)
                return startChoiceLoop(
                    result.state, playerId, sourceId, sourceName,
                    remainingChoices - 1, priorEvents + result.events
                )
            }

            val decisionResult = decisionHandlerStatic.createCardSelectionDecision(
                state = state,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Choose a card to discard ($remainingChoices remaining)",
                options = hand,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = ReadTheRunesContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = playerId,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingChoices = remainingChoices,
                phase = ReadTheRunesPhase.DISCARD_CHOICE
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                priorEvents + decisionResult.events
            )
        }

        /**
         * Find all permanents controlled by a player.
         */
        fun findControlledPermanents(state: GameState, playerId: EntityId): List<EntityId> {
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            return state.getZone(battlefieldZone)
        }

        /**
         * Sacrifice a permanent.
         */
        fun sacrificePermanent(state: GameState, playerId: EntityId, permanentId: EntityId): ExecutionResult {
            val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

            val permanentName = state.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"

            var newState = state.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)

            val events = listOf(
                PermanentsSacrificedEvent(playerId, listOf(permanentId), listOf(permanentName)),
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = permanentName,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )

            return ExecutionResult.success(newState, events)
        }

        /**
         * Discard a card.
         */
        fun discardCard(state: GameState, playerId: EntityId, cardId: EntityId): ExecutionResult {
            val handZone = ZoneKey(playerId, Zone.HAND)
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            var newState = state.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val events = listOf(
                CardsDiscardedEvent(playerId, listOf(cardId), listOf(cardName)),
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.HAND,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )

            return ExecutionResult.success(newState, events)
        }
    }
}
