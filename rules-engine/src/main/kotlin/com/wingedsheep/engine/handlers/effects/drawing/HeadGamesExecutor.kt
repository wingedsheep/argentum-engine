package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.HeadGamesEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for HeadGamesEffect.
 *
 * "Target opponent puts the cards from their hand on top of their library.
 * Search that player's library for that many cards. The player puts those
 * cards into their hand, then shuffles."
 *
 * Two-step process:
 * 1. Move opponent's hand to top of library, count how many cards were moved.
 * 2. Controller searches opponent's library for that many cards (all cards visible).
 *    Selected cards go to opponent's hand, then opponent's library is shuffled.
 */
class HeadGamesExecutor : EffectExecutor<HeadGamesEffect> {

    override val effectType: KClass<HeadGamesEffect> = HeadGamesEffect::class

    override fun execute(
        state: GameState,
        effect: HeadGamesEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for Head Games")

        val controllerId = context.controllerId
        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val libraryZone = ZoneKey(targetPlayerId, Zone.LIBRARY)
        val hand = state.getZone(handZone)

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // If opponent's hand is empty, nothing happens
        if (hand.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        val handSize = hand.size
        val events = mutableListOf<GameEvent>()

        // Step 1: Move all cards from opponent's hand to top of library
        var newState = state
        val currentLibrary = newState.getZone(libraryZone)
        // Put hand cards on top, then existing library below
        newState = newState.copy(
            zones = newState.zones +
                (handZone to emptyList()) +
                (libraryZone to hand + currentLibrary)
        )

        for (cardId in hand) {
            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardComponent?.name ?: "Unknown",
                    fromZone = Zone.HAND,
                    toZone = Zone.LIBRARY,
                    ownerId = targetPlayerId
                )
            )
        }

        // Step 2: Controller searches opponent's library for that many cards
        val library = newState.getZone(libraryZone)

        // If library is empty (shouldn't happen since we just moved hand there), just return
        if (library.isEmpty()) {
            return ExecutionResult.success(newState, events)
        }

        // Build card info map for the UI - ALL library cards are visible to controller
        val cardInfoMap = library.associateWith { cardId ->
            val container = newState.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = cardComponent?.imageUri
            )
        }

        val maxSelections = minOf(handSize, library.size)

        // Create the decision for the controller to pick cards
        val decisionId = UUID.randomUUID().toString()
        val decision = SearchLibraryDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Search opponent's library for ${if (maxSelections == 1) "a card" else "up to $maxSelections cards"} to put in their hand",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = library,
            minSelections = 0, // Can "fail to find"
            maxSelections = maxSelections,
            cards = cardInfoMap,
            filterDescription = "card"
        )

        // Create continuation to resume after controller selects
        val continuation = HeadGamesContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            targetPlayerId = targetPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            searchCount = maxSelections
        )

        val stateWithDecision = newState.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        events.add(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = controllerId,
                decisionType = "SEARCH_LIBRARY",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            events
        )
    }
}
