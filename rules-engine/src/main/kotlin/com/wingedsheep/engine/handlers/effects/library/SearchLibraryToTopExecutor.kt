package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchLibraryToTopEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SearchLibraryToTopEffect.
 * "Search your library for a [filter] card, then shuffle and put that card on top"
 *
 * This executor handles the tutor pattern where the library is shuffled BEFORE
 * the selected card is placed on top (so the card remains on top).
 *
 * This differs from SearchLibraryEffect with TOP_OF_LIBRARY destination which
 * places the card on top THEN shuffles (which would shuffle the card back in).
 *
 * Used by cards like Cruel Tutor and Personal Tutor.
 */
class SearchLibraryToTopExecutor : EffectExecutor<SearchLibraryToTopEffect> {

    override val effectType: KClass<SearchLibraryToTopEffect> = SearchLibraryToTopEffect::class

    override fun execute(
        state: GameState,
        effect: SearchLibraryToTopEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val library = state.getZone(libraryZone)

        // If library is empty, just shuffle and return
        if (library.isEmpty()) {
            return handleEmptyOrNoMatches(state, libraryZone)
        }

        // Filter cards that match the criteria
        val matchingCards = library.filter { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            matchesFilter(cardComponent, effect.filter)
        }

        // If no cards match, shuffle and return
        // Player "fails to find" - this is legal in MTG
        if (matchingCards.isEmpty()) {
            return handleEmptyOrNoMatches(state, libraryZone)
        }

        // Build card info map for the UI
        val cardInfoMap = matchingCards.associateWith { cardId ->
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

        val decision = SearchLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Search your library for ${effect.filter.description}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = matchingCards,
            minSelections = 0, // Can always "fail to find"
            maxSelections = 1,
            cards = cardInfoMap,
            filterDescription = effect.filter.description
        )

        // Create continuation to resume after player selects
        val continuation = SearchLibraryToTopContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            filter = effect.filter,
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
     * Shuffle and return success.
     */
    private fun handleEmptyOrNoMatches(
        state: GameState,
        libraryZone: ZoneKey
    ): ExecutionResult {
        val library = state.getZone(libraryZone).shuffled()
        val newZones = state.zones + (libraryZone to library)
        return ExecutionResult.success(
            state.copy(zones = newZones),
            listOf(LibraryShuffledEvent(libraryZone.ownerId))
        )
    }

    /**
     * Check if a card matches the given filter.
     */
    private fun matchesFilter(card: CardComponent?, filter: CardFilter): Boolean {
        if (card == null) return false

        return when (filter) {
            is CardFilter.AnyCard -> true
            is CardFilter.CreatureCard -> card.typeLine.isCreature
            is CardFilter.LandCard -> card.typeLine.isLand
            is CardFilter.BasicLandCard -> card.typeLine.isBasicLand
            is CardFilter.SorceryCard -> card.typeLine.isSorcery
            is CardFilter.InstantCard -> card.typeLine.isInstant
            is CardFilter.HasSubtype -> card.typeLine.hasSubtype(Subtype(filter.subtype))
            is CardFilter.HasColor -> card.colors.contains(filter.color)
            is CardFilter.And -> filter.filters.all { matchesFilter(card, it) }
            is CardFilter.Or -> filter.filters.any { matchesFilter(card, it) }
            is CardFilter.PermanentCard -> card.typeLine.isPermanent
            is CardFilter.NonlandPermanentCard -> card.typeLine.isPermanent && !card.typeLine.isLand
            is CardFilter.ManaValueAtMost -> card.manaCost.cmc <= filter.maxManaValue
            is CardFilter.Not -> !matchesFilter(card, filter.filter)
        }
    }
}
