package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
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
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val executeEffect: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null
) : EffectExecutor<PayOrSufferEffect> {

    override val effectType: KClass<PayOrSufferEffect> = PayOrSufferEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for pay or suffer effect")

        // Resolve who must pay — defaults to controller but can be the opponent (e.g., "target opponent loses 3 life unless they sacrifice")
        val payingPlayerId = context.resolvePlayerTarget(effect.player)
            ?: context.controllerId

        // Find source card info
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source has no card component")

        return when (val cost = effect.cost) {
            is PayCost.Discard -> handleDiscardCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            is PayCost.Sacrifice -> handleSacrificeCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            is PayCost.PayLife -> handlePayLifeCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            is PayCost.Mana -> handleManaCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            is PayCost.Exile -> handleExileCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            is PayCost.Choice -> handleChoiceCost(state, effect, context, cost, sourceId, sourceCard.name, payingPlayerId)
            is PayCost.ReturnToHand -> ExecutionResult.error(state, "ReturnToHand payment for PayOrSuffer not yet implemented")
            is PayCost.RevealCard -> ExecutionResult.error(state, "RevealCard payment for PayOrSuffer not yet implemented")
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
        val prompt = buildDiscardPrompt(cost, sourceName, effect.suffer)

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
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets
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
        val prompt = "Discard ${if (cost.count == 1) "a card" else "${cost.count} cards"} at random to keep $sourceName?"

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
            random = true,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets
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
        val prompt = buildSacrificePrompt(cost, sourceName, effect.suffer)

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
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true  // Use battlefield targeting UI instead of modal
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
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets
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
            filter = GameObjectFilter.Any, // Not used for life payment
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets
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
     * Handle an exile cost - player must exile cards from a zone to avoid suffer effect.
     */
    private fun handleExileCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Exile,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): ExecutionResult {
        val validCards = findValidCardsInZone(state, controllerId, cost.filter, cost.zone)

        if (validCards.size < cost.count) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        val prompt = buildExilePrompt(cost, sourceName, effect.suffer)

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

        val continuation = PayOrSufferContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.EXILE,
            sufferEffect = effect.suffer,
            requiredCount = cost.count,
            filter = cost.filter,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            zone = cost.zone
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Handle a mana cost - player must pay mana to avoid suffer effect.
     */
    private fun handleManaCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Mana,
        sourceId: EntityId,
        sourceName: String,
        controllerId: EntityId
    ): ExecutionResult {
        // Check if the player can pay the mana cost
        val manaSolver = ManaSolver(cardRegistry)
        if (!manaSolver.canPay(state, controllerId, cost.cost)) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        // Create a yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val consequence = describeConsequence(effect.suffer, sourceName)
        val prompt = "Pay ${cost.cost} or $consequence?"

        val decision = YesNoDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Pay ${cost.cost}",
            noText = "Accept consequence"
        )

        val continuation = PayOrSufferContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            costType = PayOrSufferCostType.MANA,
            sufferEffect = effect.suffer,
            requiredCount = 0,
            filter = GameObjectFilter.Any,
            random = false,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets,
            manaCost = cost.cost
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
     * Handle a choice cost - player picks which cost to pay from multiple options.
     */
    private fun handleChoiceCost(
        state: GameState,
        effect: PayOrSufferEffect,
        context: EffectContext,
        cost: PayCost.Choice,
        sourceId: EntityId,
        sourceName: String,
        payingPlayerId: EntityId
    ): ExecutionResult {
        // Build available options: only include costs the player can actually pay
        val availableOptions = mutableListOf<Pair<Int, String>>()
        for ((index, option) in cost.options.withIndex()) {
            if (canPayCost(state, payingPlayerId, option, sourceId)) {
                availableOptions.add(index to option.description.replaceFirstChar { it.uppercase() })
            }
        }

        // Always add the suffer option
        val sufferDescription = effect.suffer.description.replaceFirstChar { it.uppercase() }

        val optionLabels = availableOptions.map { it.second } + sufferDescription

        // If no avoidance options are available, automatically suffer
        if (availableOptions.isEmpty()) {
            return executeSufferEffect(state, effect.suffer, context)
        }

        val decisionId = UUID.randomUUID().toString()
        val prompt = "Choose one:"

        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = payingPlayerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = optionLabels
        )

        val continuation = PayOrSufferChoiceContinuation(
            decisionId = decisionId,
            playerId = payingPlayerId,
            sourceId = sourceId,
            sourceName = sourceName,
            options = availableOptions.map { cost.options[it.first] },
            sufferEffect = effect.suffer,
            targets = context.targets,
            namedTargets = context.pipeline.namedTargets
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = payingPlayerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = prompt
                )
            )
        )
    }

    /**
     * Check if a player can pay a specific cost.
     */
    private fun canPayCost(
        state: GameState,
        playerId: EntityId,
        cost: PayCost,
        sourceId: EntityId
    ): Boolean {
        return when (cost) {
            is PayCost.Discard -> findValidCardsInHand(state, playerId, cost.filter).size >= cost.count
            is PayCost.Sacrifice -> findValidPermanentsOnBattlefield(state, playerId, cost.filter, sourceId).size >= cost.count
            is PayCost.PayLife -> {
                val life = state.getEntity(playerId)?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life ?: 0
                life > cost.amount
            }
            is PayCost.Mana -> ManaSolver(cardRegistry).canPay(state, playerId, cost.cost)
            is PayCost.Exile -> findValidCardsInZone(state, playerId, cost.filter, cost.zone).size >= cost.count
            is PayCost.Choice -> cost.options.any { canPayCost(state, playerId, it, sourceId) }
            is PayCost.ReturnToHand -> false
            is PayCost.RevealCard -> false
        }
    }

    /**
     * Find all cards in hand that match the specified filter.
     */
    private fun findValidCardsInHand(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)
        val context = PredicateContext(controllerId = playerId)

        return hand.filter { cardId ->
            predicateEvaluator.matches(state, cardId, filter, context)
        }
    }

    /**
     * Find all cards in a given zone that match the specified filter.
     */
    private fun findValidCardsInZone(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        zone: Zone
    ): List<EntityId> {
        if (zone == Zone.BATTLEFIELD) {
            return BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, filter.youControl(), PredicateContext(controllerId = playerId)
            )
        }
        val zoneKey = ZoneKey(playerId, zone)
        val cards = state.getZone(zoneKey)
        val context = PredicateContext(controllerId = playerId)
        return cards.filter { cardId ->
            predicateEvaluator.matches(state, cardId, filter, context)
        }
    }

    /**
     * Find all permanents on the battlefield that match the filter.
     * Excludes the source permanent itself.
     */
    private fun findValidPermanentsOnBattlefield(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        sourceId: EntityId
    ): List<EntityId> {
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl(), PredicateContext(controllerId = playerId), excludeSelfId = sourceId
        )
    }

    /**
     * Execute the suffer effect.
     */
    private fun executeSufferEffect(
        state: GameState,
        sufferEffect: Effect,
        context: EffectContext
    ): ExecutionResult {
        // Use injected executor if available, otherwise handle common cases
        if (executeEffect != null) {
            return executeEffect.invoke(state, sufferEffect, context)
        }

        // Fallback: Handle the most common suffer effects directly
        return when (sufferEffect) {
            is SacrificeSelfEffect -> {
                // Handle "sacrifice this" - the most common suffer effect
                val sourceId = context.sourceId ?: return ExecutionResult.success(state)
                val controllerId = context.controllerId
                sacrificePermanent(state, controllerId, sourceId)
            }
            is SacrificeEffect -> {
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
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        // Check if the permanent is still on the battlefield
        if (permanentId !in state.getZone(battlefieldZone)) {
            return ExecutionResult.success(state)
        }

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
     * Build prompt for discard cost.
     */
    private fun buildDiscardPrompt(cost: PayCost.Discard, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            val article = if (desc == "card") "a" else if (desc.first().lowercaseChar() in "aeiou") "an" else "a"
            "$article $desc"
        } else {
            "${cost.count} ${desc}s"
        }
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Discard $typeText or $consequence"
    }

    /**
     * Build prompt for sacrifice cost.
     */
    private fun buildSacrificePrompt(cost: PayCost.Sacrifice, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            "${if (desc.first().lowercaseChar() in "aeiou") "an" else "a"} $desc"
        } else {
            "${cost.count} ${desc}s"
        }
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Sacrifice $typeText or $consequence"
    }

    /**
     * Build prompt for exile cost.
     */
    private fun buildExilePrompt(cost: PayCost.Exile, sourceName: String, sufferEffect: Effect): String {
        val desc = cost.filter.description
        val typeText = if (cost.count == 1) {
            "${if (desc.first().lowercaseChar() in "aeiou") "an" else "a"} $desc"
        } else {
            "${cost.count} ${desc}s"
        }
        val zoneName = cost.zone.name.lowercase()
        val consequence = describeConsequence(sufferEffect, sourceName)
        return "Exile $typeText from your $zoneName or $consequence"
    }

    /**
     * Describe the consequence of not paying the cost.
     */
    private fun describeConsequence(sufferEffect: Effect, sourceName: String): String {
        return when (sufferEffect) {
            is SacrificeSelfEffect,
            is SacrificeEffect -> "sacrifice $sourceName"
            else -> sufferEffect.description
        }
    }

    companion object {
        private val predicateEvaluatorStatic = PredicateEvaluator()

        /**
         * Execute the random discard after player confirmed.
         */
        fun executeRandomDiscard(
            state: GameState,
            playerId: EntityId,
            filter: GameObjectFilter,
            count: Int
        ): ExecutionResult {
            val handZone = ZoneKey(playerId, Zone.HAND)
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            val hand = state.getZone(handZone)
            val context = PredicateContext(controllerId = playerId)

            // Filter valid cards
            val validCards = hand.filter { cardId ->
                predicateEvaluatorStatic.matches(state, cardId, filter, context)
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
                        fromZone = Zone.HAND,
                        toZone = Zone.GRAVEYARD,
                        ownerId = playerId
                    )
                )
            }

            val discardNames = cardsToDiscard.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(0, CardsDiscardedEvent(playerId, cardsToDiscard, discardNames))

            return ExecutionResult.success(newState, events)
        }
    }
}
