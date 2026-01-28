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
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.PayOrSufferEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PayOrSufferEffect.
 *
 * Generic "unless" effect handler for punisher mechanics:
 * "Do [suffer], unless you [cost]."
 *
 * Examples:
 * - Thundering Wurm: "When ~ enters the battlefield, sacrifice it unless you discard a land card."
 * - Primeval Force: "When ~ enters the battlefield, sacrifice it unless you sacrifice three Forests."
 *
 * The player is presented with a selection of valid options to pay the cost.
 * If they select exactly the required count, the cost is paid and the suffer effect is avoided.
 * If they select 0 (or don't have enough), the suffer effect is executed.
 */
class PayOrSufferExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val executeEffect: ((GameState, com.wingedsheep.sdk.scripting.Effect, EffectContext) -> ExecutionResult)? = null
) : EffectExecutor<PayOrSufferEffect> {

    override val effectType: KClass<PayOrSufferEffect> = PayOrSufferEffect::class

    override fun execute(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for pay or suffer effect")

        val controllerId = context.controllerId

        // Find source card info
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source has no card component")

        return when (val cost = effect.cost) {
            is PayCost.Discard -> handleDiscardCost(state, effect, context, cost, sourceId, sourceCard.name, controllerId)
            is PayCost.Sacrifice -> handleSacrificeCost(state, effect, context, cost, sourceId, sourceCard.name, controllerId)
            is PayCost.PayLife -> handlePayLifeCost(state, effect, context, cost, sourceId, sourceCard.name, controllerId)
        }
    }

    /**
     * Handle a discard cost - player must discard cards matching filter to avoid suffer effect.
     */
    private fun handleDiscardCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Discard,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): ExecutionResult {
        // Handle random discard separately
        if (cost.random) {
            return handleRandomDiscard(state, effect, context, cost, sourceId, sourceName, controllerId)
        }

        // Find all valid cards in hand that match the filter
        val validCards = findValidCardsInHand(state, controllerId, cost.filter)

        // If the player doesn't have enough matching cards, automatically execute suffer effect
        if (validCards.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Player has at least enough valid cards - present the decision
        val prompt = buildDiscardPrompt(cost, sourceName)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validCards,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.DISCARD,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle random discard variant.
     * Prompts the player with a yes/no choice.
     */
    private fun handleRandomDiscard(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Discard,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): ExecutionResult {
        val validCards = findValidCardsInHand(state, controllerId, cost.filter)

        // If no valid cards, execute suffer effect automatically
        if (validCards.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Create a yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val prompt = "Discard ${if (cost.count == 1) "a card" else "${cost.count} cards"} at random to avoid ${effect.suffer.description}?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Discard",
            noText = "Accept consequence"
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.DISCARD,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = true
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "YES_NO",
                    prompt = prompt
                )
            )
        )
    }

    /**
     * Handle a sacrifice cost - player must sacrifice permanents matching filter to avoid suffer effect.
     */
    private fun handleSacrificeCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Sacrifice,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): ExecutionResult {
        // Find all valid permanents on the battlefield that the player controls
        val validPermanents = findValidPermanentsOnBattlefield(state, controllerId, cost.filter, sourceId)

        // If the player doesn't have enough permanents, automatically execute suffer effect
        if (validPermanents.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Player has enough - present the decision
        val prompt = buildSacrificePrompt(cost, sourceName)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.SACRIFICE,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle a pay life cost - player must pay life to avoid suffer effect.
     */
    private fun handlePayLifeCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.PayLife,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): ExecutionResult {
        // Check if player has enough life to pay (must have more than the cost)
        val playerContainer = state.getEntity(controllerId)
        val playerLife = playerContainer?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0

        // If player doesn't have enough life to pay and survive, execute suffer effect
        if (playerLife <= cost.amount) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Create a yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val prompt = "Pay ${cost.amount} life to avoid ${effect.suffer.description}?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Pay life",
            noText = "Accept consequence"
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.PAY_LIFE,
            sufferEffect = effect.suffer,
            requiredCount = cost.amount,
            filter = CardFilter.AnyCard, // Not used for life payment
            random = false
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "YES_NO",
                    prompt = prompt
                )
            )
        )
    }

    /**
     * Find all cards in hand that match the specified filter.
     */
    private fun findValidCardsInHand(
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
     * Find all permanents on the battlefield that match the filter.
     * Excludes the source permanent itself.
     */
    private fun findValidPermanentsOnBattlefield(
        state: GameState,
        playerId: EntityId,
        filter: CardFilter,
        sourceId: EntityId
    ): List<EntityId> {
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val battlefield = state.getZone(battlefieldZone)

        return battlefield.filter { permanentId ->
            if (permanentId == sourceId) return@filter false

            val container = state.getEntity(permanentId) ?: return@filter false
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
     * Execute the suffer effect.
     */
    private fun executeSufferEffect(
        state: GameState,
        sufferEffect: com.wingedsheep.sdk.scripting.Effect,
        context: EffectContext
    ): ExecutionResult {
        // Use injected executor if available, otherwise handle common cases
        if (executeEffect != null) {
            return executeEffect.invoke(state, sufferEffect, context)
        }

        // Fallback: Handle the most common suffer effects directly
        return when (sufferEffect) {
            is com.wingedsheep.sdk.scripting.SacrificeSelfEffect -> {
                // Handle "sacrifice this" - the most common suffer effect
                val sourceId = context.sourceId ?: return ExecutionResult.success(state)
                val controllerId = context.controllerId
                sacrificePermanent(state, controllerId, sourceId)
            }
            is com.wingedsheep.sdk.scripting.SacrificeEffect -> {
                // Handle "sacrifice this" when using SacrificeEffect
                // For PayOrSufferEffect, we assume it means sacrifice self
                val sourceId = context.sourceId ?: return ExecutionResult.success(state)
                val controllerId = context.controllerId
                sacrificePermanent(state, controllerId, sourceId)
            }
            else -> {
                // For other effects, we'd need the full executor registry
                // This should be handled by the registry in practice
                ExecutionResult.error(state, "Cannot execute suffer effect: ${sufferEffect::class.simpleName}")
            }
        }
    }

    /**
     * Sacrifice a permanent.
     */
    private fun sacrificePermanent(
        state: GameState,
        playerId: EntityId,
        permanentId: EntityId
    ): ExecutionResult {
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        // Check if the permanent is still on the battlefield
        if (permanentId !in state.getZone(battlefieldZone)) {
            return ExecutionResult.success(state)
        }

        val permanentName = state.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"

        var newState = state.removeFromZone(battlefieldZone, permanentId)
        newState = newState.addToZone(graveyardZone, permanentId)

        val events = listOf(
            PermanentsSacrificedEvent(playerId, listOf(permanentId)),
            ZoneChangeEvent(
                entityId = permanentId,
                entityName = permanentName,
                fromZone = ZoneType.BATTLEFIELD,
                toZone = ZoneType.GRAVEYARD,
                ownerId = playerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Build prompt for discard cost.
     */
    private fun buildDiscardPrompt(cost: PayCost.Discard, sourceName: String): String {
        val typeText = if (cost.count == 1) {
            when (cost.filter) {
                CardFilter.AnyCard -> "a card"
                CardFilter.LandCard -> "a land card"
                CardFilter.CreatureCard -> "a creature card"
                else -> "a ${cost.filter.description}"
            }
        } else {
            "${cost.count} ${cost.filter.description}${if (cost.filter != CardFilter.AnyCard) "s" else " cards"}"
        }
        return "Discard $typeText to keep $sourceName, or skip to accept the consequence"
    }

    /**
     * Build prompt for sacrifice cost.
     */
    private fun buildSacrificePrompt(cost: PayCost.Sacrifice, sourceName: String): String {
        val typeText = if (cost.count == 1) {
            val desc = cost.filter.description
            "${if (desc.first().lowercaseChar() in "aeiou") "an" else "a"} $desc"
        } else {
            "${cost.count} ${cost.filter.description}s"
        }
        return "Sacrifice $typeText to keep $sourceName, or skip to accept the consequence"
    }

    companion object {
        /**
         * Execute the random discard after player confirmed.
         */
        fun executeRandomDiscard(
            state: GameState,
            playerId: EntityId,
            filter: CardFilter,
            count: Int
        ): ExecutionResult {
            val handZone = ZoneKey(playerId, ZoneType.HAND)
            val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)
            val hand = state.getZone(handZone)

            // Filter valid cards
            val validCards = hand.filter { cardId ->
                val container = state.getEntity(cardId) ?: return@filter false
                val card = container.get<CardComponent>() ?: return@filter false
                matchesFilterStatic(card, filter)
            }

            if (validCards.isEmpty()) {
                return ExecutionResult.success(state)
            }

            // Randomly select cards to discard
            val cardsToDiscard = validCards.shuffled().take(count)
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (cardId in cardsToDiscard) {
                val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
                newState = newState.removeFromZone(handZone, cardId)
                newState = newState.addToZone(graveyardZone, cardId)
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = ZoneType.HAND,
                        toZone = ZoneType.GRAVEYARD,
                        ownerId = playerId
                    )
                )
            }

            events.add(0, CardsDiscardedEvent(playerId, cardsToDiscard))

            return ExecutionResult.success(newState, events)
        }

        private fun matchesFilterStatic(card: CardComponent, filter: CardFilter): Boolean {
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
                is CardFilter.And -> filter.filters.all { matchesFilterStatic(card, it) }
                is CardFilter.Or -> filter.filters.any { matchesFilterStatic(card, it) }
                is CardFilter.Not -> !matchesFilterStatic(card, filter.filter)
            }
        }
    }
}
