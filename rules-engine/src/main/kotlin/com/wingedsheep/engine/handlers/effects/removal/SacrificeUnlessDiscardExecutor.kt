package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SacrificeUnlessDiscardEffect
import kotlin.reflect.KClass

/**
 * Executor for SacrificeUnlessDiscardEffect.
 *
 * "When [this creature] enters the battlefield, sacrifice it unless you discard a [card type]."
 *
 * Examples:
 * - Mercenary Knight: "sacrifice it unless you discard a creature card"
 * - Thundering Wurm: "sacrifice it unless you discard a land card"
 * - Pillaging Horde: "sacrifice it unless you discard a card at random"
 *
 * The player is presented with a selection of valid cards to discard from their hand.
 * If they select exactly the required count, those cards are discarded and the source survives.
 * If they select 0 (or don't have enough), the source is sacrificed instead.
 */
class SacrificeUnlessDiscardExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<SacrificeUnlessDiscardEffect> {

    override val effectType: KClass<SacrificeUnlessDiscardEffect> =
        SacrificeUnlessDiscardEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeUnlessDiscardEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for sacrifice unless discard effect")

        val controllerId = context.controllerId

        // Find source card info
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source has no card component")

        // Check if the source is still on the battlefield (could have been removed by other effects)
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        if (sourceId !in state.getZone(battlefieldZone)) {
            // Source is no longer on battlefield - nothing to do
            return ExecutionResult.success(state)
        }

        // Handle random discard separately (e.g., Pillaging Horde)
        if (effect.random) {
            return handleRandomDiscard(state, controllerId, sourceId, sourceCard.name, effect.discardFilter)
        }

        // Find all valid cards in hand that match the filter
        val validCards = findValidCards(state, controllerId, effect.discardFilter)

        // If the player doesn't have any matching cards, automatically sacrifice the source
        if (validCards.isEmpty()) {
            return sacrificeSource(state, controllerId, sourceId, sourceCard.name)
        }

        // Player has at least one valid card - present the decision
        // Use minSelections = 0 to allow declining (which sacrifices the source)
        val prompt = buildPrompt(effect.discardFilter, sourceCard.name)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceCard.name,
            prompt = prompt,
            options = validCards,
            minSelections = 0,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = SacrificeUnlessDiscardContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceCard.name,
            discardFilter = effect.discardFilter,
            requiredCount = 1
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle random discard variant (e.g., Pillaging Horde).
     */
    private fun handleRandomDiscard(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        sourceName: String,
        discardFilter: CardFilter
    ): ExecutionResult {
        val validCards = findValidCards(state, playerId, discardFilter)

        // If no valid cards, sacrifice the source
        if (validCards.isEmpty()) {
            return sacrificeSource(state, playerId, sourceId, sourceName)
        }

        // Randomly discard one card
        val cardToDiscard = validCards.random()

        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        var newState = state.removeFromZone(handZone, cardToDiscard)
        newState = newState.addToZone(graveyardZone, cardToDiscard)

        val cardName = newState.getEntity(cardToDiscard)?.get<CardComponent>()?.name ?: "Unknown"

        val events = listOf(
            CardsDiscardedEvent(playerId, listOf(cardToDiscard)),
            ZoneChangeEvent(
                entityId = cardToDiscard,
                entityName = cardName,
                fromZone = ZoneType.HAND,
                toZone = ZoneType.GRAVEYARD,
                ownerId = playerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Find all cards in hand that match the specified filter.
     */
    private fun findValidCards(
        state: GameState,
        playerId: EntityId,
        filter: CardFilter
    ): List<EntityId> {
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val hand = state.getZone(handZone)

        return hand.filter { cardId ->
            val container = state.getEntity(cardId) ?: return@filter false
            val card = container.get<CardComponent>() ?: return@filter false

            matchesFilter(card, filter)
        }
    }

    /**
     * Check if a card matches the specified filter.
     */
    private fun matchesFilter(card: CardComponent, filter: CardFilter): Boolean {
        return when (filter) {
            is CardFilter.AnyCard -> true
            is CardFilter.CreatureCard -> card.typeLine.isCreature
            is CardFilter.LandCard -> card.typeLine.isLand
            is CardFilter.BasicLandCard -> card.typeLine.isBasicLand
            is CardFilter.SorceryCard -> card.typeLine.isSorcery
            is CardFilter.InstantCard -> card.typeLine.isInstant
            is CardFilter.PermanentCard -> card.typeLine.isPermanent
            is CardFilter.NonlandPermanentCard -> card.typeLine.isPermanent && !card.typeLine.isLand
            is CardFilter.HasSubtype -> Subtype.of(filter.subtype) in card.typeLine.subtypes
            is CardFilter.HasColor -> filter.color in card.colors
            is CardFilter.ManaValueAtMost -> (card.manaCost?.cmc ?: 0) <= filter.maxManaValue
            is CardFilter.And -> filter.filters.all { matchesFilter(card, it) }
            is CardFilter.Or -> filter.filters.any { matchesFilter(card, it) }
            is CardFilter.Not -> !matchesFilter(card, filter.filter)
        }
    }

    /**
     * Build the prompt message for the decision.
     */
    private fun buildPrompt(filter: CardFilter, sourceName: String): String {
        val typeText = "a ${filter.description}"
        return "Discard $typeText to keep $sourceName, or skip to sacrifice $sourceName"
    }

    companion object {
        /**
         * Sacrifice the source permanent.
         */
        fun sacrificeSource(
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId,
            sourceName: String
        ): ExecutionResult {
            val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
            val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

            var newState = state.removeFromZone(battlefieldZone, sourceId)
            newState = newState.addToZone(graveyardZone, sourceId)

            val events = listOf(
                PermanentsSacrificedEvent(playerId, listOf(sourceId)),
                ZoneChangeEvent(
                    entityId = sourceId,
                    entityName = sourceName,
                    fromZone = ZoneType.BATTLEFIELD,
                    toZone = ZoneType.GRAVEYARD,
                    ownerId = playerId
                )
            )

            return ExecutionResult.success(newState, events)
        }
    }
}
