package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.core.TypecycleSearchContinuation
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventCycling
import kotlin.reflect.KClass

/**
 * Handler for the TypecycleCard action.
 *
 * Typecycling (e.g., Swampcycling {2}) allows a player to pay a cost, discard the card,
 * and search their library for a card of the specified type, put it into their hand,
 * then shuffle. Typecycling triggers cycling abilities per MTG rules.
 */
class TypecycleCardHandler(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
) : ActionHandler<TypecycleCard> {
    override val actionType: KClass<TypecycleCard> = TypecycleCard::class

    override fun validate(state: GameState, action: TypecycleCard): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        // Check if cycling is prevented by any permanent on the battlefield (e.g., Stabilizer)
        if (isCyclingPrevented(state)) {
            return "Cycling is prevented"
        }

        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        val handZone = ZoneKey(action.playerId, Zone.HAND)
        if (action.cardId !in state.getZone(handZone)) {
            return "Card is not in your hand"
        }

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return "Card definition not found"

        val variant = findTypecyclingVariant(cardDef)
            ?: return "This card doesn't have typecycling"

        if (action.paymentStrategy is PaymentStrategy.Explicit) {
            for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                val sourceContainer = state.getEntity(sourceId)
                    ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) {
                    return "Mana source is already tapped: $sourceId"
                }
            }
        } else if (!manaSolver.canPay(state, action.playerId, variant.cost)) {
            return "Not enough mana to typecycle this card"
        }

        return null
    }

    override fun execute(state: GameState, action: TypecycleCard): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return ExecutionResult.error(state, "Card definition not found")

        val variant = findTypecyclingVariant(cardDef)
            ?: return ExecutionResult.error(state, "This card doesn't have typecycling")

        var currentState = state
        val events = mutableListOf<GameEvent>()
        val ownerId = cardComponent.ownerId ?: action.playerId

        // Pay the typecycling cost - use floating mana first, then tap lands
        val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )

        val partialResult = pool.payPartial(variant.cost)
        val poolAfterPayment = partialResult.newPool
        val remainingCost = partialResult.remainingCost
        val manaSpentFromPool = partialResult.manaSpent

        var whiteSpent = manaSpentFromPool.white
        var blueSpent = manaSpentFromPool.blue
        var blackSpent = manaSpentFromPool.black
        var redSpent = manaSpentFromPool.red
        var greenSpent = manaSpentFromPool.green
        var colorlessSpent = manaSpentFromPool.colorless

        currentState = currentState.updateEntity(action.playerId) { c ->
            c.with(
                ManaPoolComponent(
                    white = poolAfterPayment.white,
                    blue = poolAfterPayment.blue,
                    black = poolAfterPayment.black,
                    red = poolAfterPayment.red,
                    green = poolAfterPayment.green,
                    colorless = poolAfterPayment.colorless
                )
            )
        }

        // Tap lands for remaining cost
        if (!remainingCost.isEmpty()) {
            if (action.paymentStrategy is PaymentStrategy.Explicit) {
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceName = currentState.getEntity(sourceId)
                        ?.get<CardComponent>()?.name ?: "Unknown"
                    currentState = currentState.updateEntity(sourceId) { c ->
                        c.with(TappedComponent)
                    }
                    events.add(TappedEvent(sourceId, sourceName))
                }
            } else {
                val solution = manaSolver.solve(currentState, action.playerId, remainingCost, 0)
                    ?: return ExecutionResult.error(state, "Not enough mana to typecycle")

                val (stateAfterTaps, tapEvents) = manaAbilitySideEffectExecutor
                    .tapSourcesWithSideEffects(currentState, solution, action.playerId)
                currentState = stateAfterTaps
                events.addAll(tapEvents)

                for ((_, production) in solution.manaProduced) {
                    when (production.color) {
                        Color.WHITE -> whiteSpent++
                        Color.BLUE -> blueSpent++
                        Color.BLACK -> blackSpent++
                        Color.RED -> redSpent++
                        Color.GREEN -> greenSpent++
                        null -> colorlessSpent += production.colorless
                    }
                }
            }
        }

        events.add(
            ManaSpentEvent(
                playerId = action.playerId,
                reason = "${variant.description} ${cardComponent.name}",
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )

        // Discard the card (move from hand to graveyard)
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)
        currentState = currentState.removeFromZone(handZone, action.cardId)
        currentState = currentState.addToZone(graveyardZone, action.cardId)

        events.add(
            ZoneChangeEvent(
                entityId = action.cardId,
                entityName = cardComponent.name,
                fromZone = Zone.HAND,
                toZone = Zone.GRAVEYARD,
                ownerId = ownerId
            )
        )

        // Emit cycling event (typecycling triggers cycling abilities per MTG rules)
        events.add(CardCycledEvent(action.playerId, action.cardId, cardComponent.name))

        currentState = currentState.tick()

        // Detect and process triggers from discard + cycling events before search
        val preTriggers = triggerDetector.detectTriggers(currentState, events)
        if (preTriggers.isNotEmpty()) {
            // Push search continuation BEFORE processing triggers, so it ends up below
            // any trigger continuations on the stack. After all triggers resolve,
            // checkForMoreContinuations() will find this and execute the search.
            val stateWithSearchContinuation = currentState.pushContinuation(
                TypecycleSearchContinuation(
                    playerId = action.playerId,
                    cardId = action.cardId,
                    searchFilter = variant.searchFilter,
                    abilityDescription = variant.description
                )
            )
            val triggerResult = triggerProcessor.processTriggers(stateWithSearchContinuation, preTriggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            // Triggers resolved synchronously — pop the search continuation and search inline
            val (_, stateAfterPop) = triggerResult.newState.popContinuation()
            currentState = stateAfterPop
            events.addAll(triggerResult.events)
        }

        // Search library for a card matching the typecycling variant's filter
        val searchEffect = EffectPatterns.searchLibrary(
            filter = variant.searchFilter,
            count = 1,
            reveal = true
        )

        val effectContext = EffectContext(
            sourceId = action.cardId,
            controllerId = action.playerId,
            opponentId = currentState.getOpponent(action.playerId)
        )

        val searchResult = effectExecutorRegistry.execute(currentState, searchEffect, effectContext)
        if (searchResult.isPaused) {
            return ExecutionResult.paused(
                searchResult.state,
                searchResult.pendingDecision!!,
                events + searchResult.events
            )
        }
        currentState = searchResult.newState
        events.addAll(searchResult.events)

        // Typecycling doesn't change priority
        return ExecutionResult.success(currentState, events)
    }

    /**
     * Unified view of the two typecycling-family keywords (classic `Typecycling` and
     * `BasicLandcycling`). Both share the same cost-pay/discard/search pipeline; only the
     * search filter and display name differ.
     */
    private data class TypecyclingVariant(
        val cost: com.wingedsheep.sdk.core.ManaCost,
        val searchFilter: GameObjectFilter,
        val description: String
    )

    private fun findTypecyclingVariant(cardDef: com.wingedsheep.sdk.model.CardDefinition): TypecyclingVariant? {
        cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Typecycling>().firstOrNull()?.let {
            return TypecyclingVariant(
                cost = it.cost,
                searchFilter = GameObjectFilter.Any.withSubtype(it.type),
                description = "${it.type}cycling"
            )
        }
        cardDef.keywordAbilities.filterIsInstance<KeywordAbility.BasicLandcycling>().firstOrNull()?.let {
            return TypecyclingVariant(
                cost = it.cost,
                searchFilter = GameObjectFilter.BasicLand,
                description = "Basic landcycling"
            )
        }
        return null
    }

    private fun isCyclingPrevented(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PreventCycling }) {
                return true
            }
        }
        return false
    }

    companion object {
        fun create(services: EngineServices): TypecycleCardHandler {
            return TypecycleCardHandler(
                services.cardRegistry,
                services.manaSolver,
                services.triggerDetector,
                services.triggerProcessor,
                services.effectExecutorRegistry,
                services.manaAbilitySideEffectExecutor
            )
        }
    }
}
