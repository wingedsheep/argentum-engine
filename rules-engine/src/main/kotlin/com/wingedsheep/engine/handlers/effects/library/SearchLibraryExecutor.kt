package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SearchLibraryEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SearchLibraryEffect.
 * "Search your library for a [filter] card and put it [destination]"
 *
 * This executor handles library search by:
 * 1. Getting the controller's library contents
 * 2. Filtering cards that match the search criteria
 * 3. Creating a SearchLibraryDecision with embedded card info
 * 4. Pushing a SearchLibraryContinuation to resume after selection
 *
 * Special cases:
 * - Empty library: Shuffle (if configured), return success with empty result
 * - No matches: Player can "fail to find", shuffle and return
 * - Fewer matches than count: maxSelections = min(count, matchingCards.size)
 */
class SearchLibraryExecutor : EffectExecutor<SearchLibraryEffect> {

    override val effectType: KClass<SearchLibraryEffect> = SearchLibraryEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: SearchLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val library = state.getZone(libraryZone)

        // If library is empty, just shuffle (if configured) and return
        if (library.isEmpty()) {
            return handleEmptyOrNoMatches(state, libraryZone, effect.shuffleAfter)
        }

        // Filter cards that match the criteria
        val predicateContext = PredicateContext.fromEffectContext(context)
        val matchingCards = library.filter { cardId ->
            predicateEvaluator.matches(state, cardId, effect.filter, predicateContext)
        }

        // If no cards match, shuffle (if configured) and return
        // Player "fails to find" - this is legal in MTG
        if (matchingCards.isEmpty()) {
            return handleEmptyOrNoMatches(state, libraryZone, effect.shuffleAfter)
        }

        // Build card info map for the UI
        val cardInfoMap = matchingCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null // Could be populated from card definition metadata
            )
        }

        // Calculate selection bounds
        val maxSelections = minOf(effect.count, matchingCards.size)

        // Create the decision
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val filterDescription = effect.filter.description

        val decision = SearchLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Search your library for ${if (effect.count == 1) "a" else "up to ${effect.count}"} $filterDescription",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = matchingCards,
            minSelections = 0, // Can always "fail to find"
            maxSelections = maxSelections,
            cards = cardInfoMap,
            filterDescription = filterDescription
        )

        // Create continuation to resume after player selects
        val continuation = SearchLibraryContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            filter = effect.filter,
            count = effect.count,
            destination = effect.destination,
            entersTapped = effect.entersTapped,
            shuffleAfter = effect.shuffleAfter,
            reveal = effect.reveal
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
                    decisionType = "SEARCH_LIBRARY",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Handle the case when library is empty or no cards match the filter.
     * Shuffle if configured and return success.
     */
    private fun handleEmptyOrNoMatches(
        state: GameState,
        libraryZone: ZoneKey,
        shuffleAfter: Boolean
    ): ExecutionResult {
        if (!shuffleAfter) {
            return ExecutionResult.success(state)
        }

        val library = state.getZone(libraryZone).shuffled()
        val newZones = state.zones + (libraryZone to library)
        return ExecutionResult.success(
            state.copy(zones = newZones),
            listOf(LibraryShuffledEvent(libraryZone.ownerId))
        )
    }

}
