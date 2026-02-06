package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.CardPredicate
import com.wingedsheep.sdk.scripting.RevealAndOpponentChoosesEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for RevealAndOpponentChoosesEffect.
 *
 * "Reveal the top N cards of your library. An opponent chooses a creature card
 * from among them. Put that card onto the battlefield and the rest into your graveyard."
 *
 * This executor:
 * 1. Gets the top N cards from the controller's library
 * 2. Filters for cards matching the filter (e.g., creatures)
 * 3. If matching cards exist, creates a SelectCardsDecision for the opponent
 * 4. Pushes a RevealAndOpponentChoosesContinuation to resume after selection
 * 5. If no matching cards, all revealed cards go to graveyard
 */
class RevealAndOpponentChoosesExecutor : EffectExecutor<RevealAndOpponentChoosesEffect> {

    override val effectType: KClass<RevealAndOpponentChoosesEffect> = RevealAndOpponentChoosesEffect::class

    override fun execute(
        state: GameState,
        effect: RevealAndOpponentChoosesEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val libraryZone = ZoneKey(controllerId, Zone.LIBRARY)
        val library = state.getZone(libraryZone)

        // Get top N cards from library
        val topCards = library.take(effect.count)

        // If no cards to reveal, just return success
        if (topCards.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        // Build card info for all revealed cards
        val cardInfoMap = topCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = cardComponent?.imageUri
            )
        }

        // Emit reveal event
        val cardNames = topCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Unknown" }
        val imageUris = topCards.map { state.getEntity(it)?.get<CardComponent>()?.imageUri }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val revealEvent = CardsRevealedEvent(
            revealingPlayerId = controllerId,
            cardIds = topCards,
            cardNames = cardNames,
            imageUris = imageUris,
            source = sourceName
        )

        // Filter for cards matching the effect filter (e.g., creatures)
        val matchingCards = topCards.filter { cardId ->
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            cardComponent != null && matchesFilter(cardComponent, effect)
        }

        // If no matching cards, all go to graveyard
        if (matchingCards.isEmpty()) {
            return moveAllToGraveyard(state, controllerId, topCards, libraryZone, listOf(revealEvent))
        }

        // Find an opponent to make the choice
        val opponentId = context.opponentId
            ?: state.turnOrder.firstOrNull { it != controllerId }
            ?: return moveAllToGraveyard(state, controllerId, topCards, libraryZone, listOf(revealEvent))

        // Create the decision for the opponent
        val decisionId = UUID.randomUUID().toString()

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = opponentId,
            prompt = "Choose a ${effect.filter.description} card to put onto the battlefield",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = matchingCards,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            cardInfo = cardInfoMap.filterKeys { it in matchingCards }
        )

        // Create continuation
        val continuation = RevealAndOpponentChoosesContinuation(
            decisionId = decisionId,
            controllerId = controllerId,
            opponentId = opponentId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            allCards = topCards,
            creatureCards = matchingCards
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                revealEvent,
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = opponentId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Check if a card matches the effect's filter based on card predicates.
     */
    private fun matchesFilter(cardComponent: CardComponent, effect: RevealAndOpponentChoosesEffect): Boolean {
        val typeLine = cardComponent.typeLine ?: return false
        return effect.filter.cardPredicates.all { predicate ->
            when (predicate) {
                is CardPredicate.IsCreature -> typeLine.isCreature
                is CardPredicate.IsArtifact -> typeLine.isArtifact
                is CardPredicate.IsEnchantment -> typeLine.isEnchantment
                is CardPredicate.IsLand -> typeLine.isLand
                is CardPredicate.IsInstant -> typeLine.isInstant
                is CardPredicate.IsSorcery -> typeLine.isSorcery
                is CardPredicate.Or -> predicate.predicates.any { sub ->
                    when (sub) {
                        is CardPredicate.IsCreature -> typeLine.isCreature
                        is CardPredicate.IsArtifact -> typeLine.isArtifact
                        is CardPredicate.IsEnchantment -> typeLine.isEnchantment
                        is CardPredicate.IsLand -> typeLine.isLand
                        is CardPredicate.IsInstant -> typeLine.isInstant
                        is CardPredicate.IsSorcery -> typeLine.isSorcery
                        else -> false
                    }
                }
                else -> false
            }
        }
    }

    /**
     * Move all revealed cards to graveyard (when no matching cards found).
     */
    private fun moveAllToGraveyard(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        cards: List<com.wingedsheep.sdk.model.EntityId>,
        libraryZone: ZoneKey,
        priorEvents: List<GameEvent>
    ): ExecutionResult {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        val events = priorEvents.toMutableList()
        var newState = state

        for (cardId in cards) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.LIBRARY,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
