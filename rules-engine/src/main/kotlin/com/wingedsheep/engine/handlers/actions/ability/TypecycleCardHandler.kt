package com.wingedsheep.engine.handlers.actions.ability
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.CardCycledEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.tapForMana
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.core.ZoneChangeEvent
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
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventCycling
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome

/**
 * Handler for the TypecycleCard action.
 *
 * Typecycling (e.g., Swampcycling {2}) allows a player to pay a cost, discard the card,
 * and search their library for a card of the specified type, put it into their hand,
 * then shuffle. Typecycling triggers cycling abilities per MTG rules.
 */
class TypecycleCardHandler(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
    private val castPermissionUtils: com.wingedsheep.engine.legalactions.utils.CastPermissionUtils? = null
) : ActionHandler<TypecycleCard> {
    override val actionType: KClass<TypecycleCard> = TypecycleCard::class

    override fun validate(state: GameState, action: TypecycleCard): String? {
        if (!state.hasPriority(action.playerId)) {
            return "You don't have priority"
        }

        // Check if cycling is prevented by any permanent on the battlefield (e.g., Stabilizer)
        if (isCyclingPrevented(state)) {
            return "Cycling is prevented"
        }

        // Typecycling is an activated ability of the card in hand (CR 702.29e): an any-zone
        // "players can't activate abilities" (Yuriko, Blade of the Mighty) forbids it.
        if (castPermissionUtils?.isActivationPreventedForPlayer(state, action.cardId, action.playerId) == true) {
            return "An effect prevents you from activating that ability right now"
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
                    val (tappedState, tapEvents) = tapForMana(currentState, sourceId, action.playerId)
                    currentState = tappedState
                    events.addAll(tapEvents)
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

        // Discard the card to pay the typecycling cost (CR 702.29a), through the shared discard
        // path so "whenever you discard" payoffs see it (Magmakin Artillerist) *and* a
        // card-intrinsic discard replacement applies (madness, CR 702.35a). Both events land
        // before CardCycledEvent, so a card that triggers on both (CR 702.29d) sees them in the
        // order they happened.
        val discardResult = zones.discardCards(currentState, action.playerId, listOf(action.cardId), asCyclingCost = true)
        currentState = discardResult.state
        events.addAll(discardResult.events)

        // Emit cycling event (typecycling triggers cycling abilities per MTG rules)
        events.add(CardCycledEvent(action.playerId, action.cardId, cardComponent.name))

        currentState = currentState.tick()

        // Search library for a card matching the typecycling variant's filter
        val searchEffect = Patterns.Library.searchLibrary(
            filter = variant.searchFilter,
            count = 1,
            reveal = true
        )

        val effectContext = EffectContext(
            sourceId = action.cardId,
            controllerId = action.playerId,
        )

        val searchResult = effectExecutorRegistry.execute(currentState, searchEffect, effectContext)
        if (searchResult.outcome is Outcome.Paused) {
            return ExecutionResult.propagatePause(
                searchResult.state,
                events + searchResult.events
            )
        }
        currentState = searchResult.newState
        events.addAll(searchResult.events)

        // Typecycling doesn't change priority
        return ExecutionResult.success(currentState, events)
    }

    /**
     * Unified view of the typecycling-family keywords. Plain `Cycling` (no search filter)
     * is filtered out — that goes through `CycleCardHandler`. Typed cycling shares the
     * same cost-pay/discard/search pipeline; only the search filter and display prefix differ.
     */
    private data class TypecyclingVariant(
        val cost: com.wingedsheep.sdk.core.ManaCost,
        val searchFilter: GameObjectFilter,
        val description: String
    )

    private fun findTypecyclingVariant(cardDef: com.wingedsheep.sdk.model.CardDefinition): TypecyclingVariant? {
        val cycling = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Cycling>()
            .firstOrNull { it.searchFilter != null }
            ?: return null
        return TypecyclingVariant(
            cost = cycling.cost,
            searchFilter = cycling.searchFilter!!,
            description = cycling.displayPrefix
        )
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
                services.zones,
                services.cardRegistry,
                services.manaSolver,
                services.effectExecutorRegistry,
                services.manaAbilitySideEffectExecutor,
                services.castPermissionUtils
            )
        }
    }
}
