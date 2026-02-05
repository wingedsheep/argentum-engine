package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.LookAtOpponentLibraryEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for LookAtOpponentLibraryEffect.
 *
 * "Look at the top N cards of target opponent's library. Put X of them into that
 * player's graveyard and the rest on top of their library in any order."
 *
 * Used for cards like Cruel Fate.
 *
 * This executor handles library manipulation by:
 * 1. Getting the top N cards from the opponent's library
 * 2. Creating a SelectCardsDecision for choosing which cards go to graveyard
 * 3. Pushing a LookAtOpponentLibraryContinuation to resume after selection
 * 4. After graveyard selection, if there are remaining cards, creating a reorder decision
 *
 * Special cases:
 * - Empty library: Return success immediately (nothing to look at)
 * - Fewer cards than N: Show all available cards
 * - All cards go to graveyard: Skip the reorder step
 */
class LookAtOpponentLibraryExecutor : EffectExecutor<LookAtOpponentLibraryEffect> {

    override val effectType: KClass<LookAtOpponentLibraryEffect> = LookAtOpponentLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtOpponentLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        // The opponent is the target of this spell
        val opponentId = context.opponentId ?: return ExecutionResult.error(
            state, "No opponent found for LookAtOpponentLibrary effect"
        )

        val controllerId = context.controllerId

        val libraryZone = ZoneKey(opponentId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // If library is empty, nothing to look at
        if (library.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Get top N cards (or fewer if library is smaller)
        val count = minOf(effect.count, library.size)
        val topCards = library.take(count)

        // If we need to put all cards in graveyard (or more than available), just do it directly
        if (effect.toGraveyard >= topCards.size) {
            return putAllInGraveyard(state, opponentId, topCards, context)
        }

        // Build card info map for the UI (controller can see opponent's cards during this effect)
        // Note: imageUri is null here - the server layer enriches this with metadata from the registry
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

        // Create the decision for selecting cards to put in graveyard
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Look at the top $count cards of opponent's library. " +
                "Choose ${effect.toGraveyard} to put into their graveyard.",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = topCards,
            minSelections = effect.toGraveyard,
            maxSelections = effect.toGraveyard,
            ordered = false,
            cardInfo = cardInfoMap
        )

        // Create continuation to resume after player selects cards for graveyard
        val continuation = LookAtOpponentLibraryContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            opponentId = opponentId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            allCards = topCards,
            toGraveyard = effect.toGraveyard
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
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Handle the case where all looked-at cards go to graveyard.
     */
    private fun putAllInGraveyard(
        state: GameState,
        opponentId: EntityId,
        cards: List<EntityId>,
        context: EffectContext
    ): ExecutionResult {
        val libraryZone = ZoneKey(opponentId, Zone.LIBRARY)
        val graveyardZone = ZoneKey(opponentId, Zone.GRAVEYARD)

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (cardId in cards) {
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)

            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.GRAVEYARD,
                    ownerId = opponentId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
