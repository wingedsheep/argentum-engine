package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.PutOnTopOfLibraryEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.SearchLibraryEffect
import com.wingedsheep.rulesengine.ability.ShuffleLibraryEffect
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.Supertype
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.decision.CardOption
import com.wingedsheep.rulesengine.decision.ChooseCards
import com.wingedsheep.rulesengine.decision.PlayerDecision
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.EffectContinuation
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlin.reflect.KClass

/**
 * Handler for ShuffleLibraryEffect.
 * Shuffles a player's library.
 */
class ShuffleLibraryHandler : BaseEffectHandler<ShuffleLibraryEffect>() {
    override val effectClass: KClass<ShuffleLibraryEffect> = ShuffleLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: ShuffleLibraryEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        val newState = state.shuffleLibrary(targetPlayerId)
        return result(newState, EffectEvent.LibraryShuffled(targetPlayerId))
    }
}

/**
 * Handler for SearchLibraryEffect.
 *
 * Implements a multi-step search flow:
 * 1. **Search Phase** - Find all cards in library matching the filter
 * 2. **Choice Phase** - If multiple matches and no explicit selection, return a PendingDecision
 * 3. **Resolution Phase** - Move selected cards to destination (via continuation or explicit selection)
 * 4. **Shuffle Phase** - Shuffle library if required
 *
 * This allows the game loop to present choices to the player and collect their selection
 * before completing the effect.
 */
class SearchLibraryHandler : BaseEffectHandler<SearchLibraryEffect>() {
    override val effectClass: KClass<SearchLibraryEffect> = SearchLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: SearchLibraryEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)
        val library = state.getZone(libraryZone)

        // Step 1: Find all cards in library matching the filter
        val matchingCards = library.filter { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            cardComponent != null && matchesFilter(cardComponent.definition, effect.filter)
        }

        // No matching cards found - complete immediately
        if (matchingCards.isEmpty()) {
            return completeSearchWithNoMatches(state, playerId, effect)
        }

        // Step 2: Determine if we need player choice or can complete immediately
        return when {
            // Explicit selection provided - complete with those cards
            effect.selectedCardIds != null -> {
                val validSelection = effect.selectedCardIds.filter { it in matchingCards }.take(effect.count)
                completeSearch(state, playerId, validSelection, effect)
            }

            // Only one match and count is 1 - auto-select
            matchingCards.size == 1 && effect.count == 1 -> {
                completeSearch(state, playerId, matchingCards, effect)
            }

            // Multiple matches - need player choice
            else -> {
                createPendingDecision(state, playerId, matchingCards, effect, context)
            }
        }
    }

    /**
     * Create a pending decision for the player to choose which cards to take.
     * Includes a continuation that will complete the search when called.
     */
    private fun createPendingDecision(
        state: GameState,
        playerId: EntityId,
        matchingCards: List<EntityId>,
        effect: SearchLibraryEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Build card options with display information
        val cardOptions = matchingCards.map { cardId ->
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            val def = cardComponent?.definition
            CardOption(
                entityId = cardId,
                name = def?.name ?: "Unknown",
                typeLine = def?.typeLine?.toString(),
                manaCost = def?.manaCost?.toString()
            )
        }

        val sourceName = context.sourceId.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.definition?.name
        }

        val decision = ChooseCards.create(
            playerId = playerId,
            description = "Choose ${if (effect.count == 1) "a card" else "up to ${effect.count} cards"} to put ${effect.destination.description}",
            cards = cardOptions,
            minCount = 0,  // Player can "fail to find" even if matches exist
            maxCount = effect.count,
            mayChooseNone = true,  // MTG allows failing to find
            sourceEntityId = context.sourceId,
            sourceName = sourceName,
            filterDescription = effect.filter.description
        )

        // Create continuation that will complete the search with the player's selection
        val continuation = EffectContinuation { selectedIds ->
            // Validate selection against matching cards
            val validSelection = selectedIds.filter { it in matchingCards }.take(effect.count)
            completeSearch(state, playerId, validSelection, effect)
        }

        // Return partial result with pending decision
        return ExecutionResult(
            state = state,
            events = listOf(EffectEvent.LibrarySearched(playerId, matchingCards.size, effect.filter.description)),
            pendingDecision = decision,
            continuation = continuation
        )
    }

    /**
     * Complete the search by moving selected cards to the destination.
     */
    private fun completeSearch(
        state: GameState,
        playerId: EntityId,
        selectedCards: List<EntityId>,
        effect: SearchLibraryEffect
    ): ExecutionResult {
        val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)

        if (selectedCards.isEmpty()) {
            return completeSearchWithNoMatches(state, playerId, effect)
        }

        // Move selected cards to destination
        var currentState = state
        val events = mutableListOf<EffectEvent>()

        events.add(EffectEvent.LibrarySearched(playerId, selectedCards.size, effect.filter.description))

        // For TOP_OF_LIBRARY with shuffle, we need to shuffle first, then put on top
        // This matches cards like Personal Tutor: "shuffle, then put that card on top"
        if (effect.destination == SearchDestination.TOP_OF_LIBRARY && effect.shuffleAfter) {
            // Remove all selected cards from library first
            for (cardId in selectedCards) {
                currentState = currentState.removeFromZone(cardId, libraryZone)
            }

            // Shuffle the library (without the selected cards)
            currentState = currentState.shuffleLibrary(playerId)
            events.add(EffectEvent.LibraryShuffled(playerId))

            // Now put selected cards on top of library (in reverse order so first selected is on top)
            for (cardId in selectedCards.reversed()) {
                val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
                currentState = currentState.addToZoneAt(cardId, libraryZone, 0)
                events.add(EffectEvent.CardMovedToZone(cardId, cardName, "top of library"))
            }

            return result(currentState, events)
        }

        // Standard flow for other destinations
        for (cardId in selectedCards) {
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

            // Remove from library
            currentState = currentState.removeFromZone(cardId, libraryZone)

            // Add to destination
            val (destZoneId, zoneName) = getDestinationZone(effect.destination, playerId)
            if (effect.destination == SearchDestination.TOP_OF_LIBRARY) {
                // Put on top of library (position 0)
                currentState = currentState.addToZoneAt(cardId, destZoneId, 0)
            } else {
                currentState = currentState.addToZone(cardId, destZoneId)
            }

            // Apply tapped if entering battlefield tapped
            if (effect.entersTapped && effect.destination == SearchDestination.BATTLEFIELD) {
                currentState = currentState.updateEntity(cardId) { it.with(TappedComponent) }
            }

            events.add(EffectEvent.CardMovedToZone(cardId, cardName, zoneName))
        }

        // Shuffle library if required (for non-TOP_OF_LIBRARY destinations)
        if (effect.shuffleAfter) {
            currentState = currentState.shuffleLibrary(playerId)
            events.add(EffectEvent.LibraryShuffled(playerId))
        }

        return result(currentState, events)
    }

    /**
     * Complete search when no cards match (or player chose none).
     */
    private fun completeSearchWithNoMatches(
        state: GameState,
        playerId: EntityId,
        effect: SearchLibraryEffect
    ): ExecutionResult {
        return if (effect.shuffleAfter) {
            val shuffledState = state.shuffleLibrary(playerId)
            result(
                shuffledState,
                listOf(
                    EffectEvent.LibrarySearched(playerId, 0, effect.filter.description),
                    EffectEvent.LibraryShuffled(playerId)
                )
            )
        } else {
            result(state, EffectEvent.LibrarySearched(playerId, 0, effect.filter.description))
        }
    }

    private fun getDestinationZone(destination: SearchDestination, playerId: EntityId): Pair<ZoneId, String> {
        return when (destination) {
            SearchDestination.HAND -> ZoneId(ZoneType.HAND, playerId) to "hand"
            SearchDestination.BATTLEFIELD -> ZoneId.BATTLEFIELD to "battlefield"
            SearchDestination.GRAVEYARD -> ZoneId(ZoneType.GRAVEYARD, playerId) to "graveyard"
            SearchDestination.TOP_OF_LIBRARY -> ZoneId(ZoneType.LIBRARY, playerId) to "top of library"
        }
    }

    companion object {
        /**
         * Check if a card definition matches the given filter.
         */
        fun matchesFilter(definition: CardDefinition, filter: CardFilter): Boolean {
            return when (filter) {
                is CardFilter.AnyCard -> true

                is CardFilter.CreatureCard -> definition.isCreature

                is CardFilter.LandCard -> definition.isLand

                is CardFilter.BasicLandCard -> {
                    definition.isLand && definition.typeLine.supertypes.contains(Supertype.BASIC)
                }

                is CardFilter.SorceryCard -> definition.isSorcery

                is CardFilter.InstantCard -> definition.isInstant

                is CardFilter.HasSubtype -> {
                    definition.typeLine.subtypes.any { subtype ->
                        subtype.value.equals(filter.subtype, ignoreCase = true)
                    }
                }

                is CardFilter.HasColor -> {
                    definition.colors.contains(filter.color)
                }

                is CardFilter.And -> {
                    filter.filters.all { matchesFilter(definition, it) }
                }

                is CardFilter.Or -> {
                    filter.filters.any { matchesFilter(definition, it) }
                }
            }
        }
    }
}

/**
 * Handler for PutOnTopOfLibraryEffect.
 * Puts a card on top of its owner's library.
 * Commonly used for death triggers like "When this creature dies, put it on top of its owner's library."
 */
class PutOnTopOfLibraryHandler : BaseEffectHandler<PutOnTopOfLibraryEffect>() {
    override val effectClass: KClass<PutOnTopOfLibraryEffect> = PutOnTopOfLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: PutOnTopOfLibraryEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Resolve the target entity
        val targetId = when (effect.target) {
            EffectTarget.Self -> context.sourceId
            else -> context.targets.firstOrNull()?.let {
                when (it) {
                    is ChosenTarget.Permanent -> it.entityId
                    is ChosenTarget.Card -> it.cardId
                    else -> null
                }
            } ?: context.sourceId
        }

        // Get the card's owner to determine which library
        val cardComponent = state.getEntity(targetId)?.get<CardComponent>()
        val ownerId = cardComponent?.ownerId ?: context.controllerId
        val cardName = cardComponent?.definition?.name ?: "Unknown"

        // Find current zone and remove from it
        var currentState = state
        val currentZone = state.findZone(targetId)
        if (currentZone != null) {
            currentState = currentState.removeFromZone(targetId, currentZone)
        }

        // Add to top of library
        val libraryZone = ZoneId(ZoneType.LIBRARY, ownerId)
        currentState = currentState.addToZoneAt(targetId, libraryZone, 0)

        return result(
            currentState,
            EffectEvent.CardMovedToZone(targetId, cardName, "top of library")
        )
    }
}
