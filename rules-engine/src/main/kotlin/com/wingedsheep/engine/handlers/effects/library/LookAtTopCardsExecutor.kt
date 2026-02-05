package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LookAtTopCardsEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for LookAtTopCardsEffect.
 * "Look at the top N cards of your library. Put X of them into your hand
 * and the rest into your graveyard."
 *
 * This executor:
 * 1. Gets the top N cards from the controller's library
 * 2. Creates a SelectCardsDecision with embedded card info (since library is hidden)
 * 3. Pushes a LookAtTopCardsContinuation to resume after selection
 * 4. The continuation handler moves selected cards to hand and rest to graveyard
 */
class LookAtTopCardsExecutor : EffectExecutor<LookAtTopCardsEffect> {

    override val effectType: KClass<LookAtTopCardsEffect> = LookAtTopCardsEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtTopCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // Get top N cards from library
        val topCards = library.take(effect.count)

        // If no cards to look at, just return success
        if (topCards.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        // If fewer cards than keepCount, player keeps all of them
        val actualKeepCount = minOf(effect.keepCount, topCards.size)

        // If keepCount equals or exceeds available cards, no choice needed
        // Just put all cards in hand (or as many as we can keep)
        if (actualKeepCount >= topCards.size) {
            return moveAllToHand(state, playerId, topCards, libraryZone)
        }

        // Build card info for the UI (library cards are normally hidden)
        val cardInfoMap = topCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        // Create the decision
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose ${actualKeepCount} card${if (actualKeepCount != 1) "s" else ""} to put into your hand",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = topCards,
            minSelections = actualKeepCount,
            maxSelections = actualKeepCount,
            ordered = false,
            cardInfo = cardInfoMap
        )

        // Create continuation to resume after player selects
        val continuation = LookAtTopCardsContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            allCards = topCards,
            keepCount = actualKeepCount,
            restToGraveyard = effect.restToGraveyard
        )

        // Push continuation and return paused state
        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Move all looked-at cards directly to hand (when no choice is needed).
     */
    private fun moveAllToHand(
        state: GameState,
        playerId: EntityId,
        cards: List<EntityId>,
        libraryZone: ZoneKey
    ): ExecutionResult {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val events = mutableListOf<GameEvent>()
        var newState = state

        for (cardId in cards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.HAND,
                    ownerId = playerId
                )
            )
        }

        events.add(0, CardsDrawnEvent(playerId, cards.size, cards))
        return ExecutionResult.success(newState, events)
    }
}
