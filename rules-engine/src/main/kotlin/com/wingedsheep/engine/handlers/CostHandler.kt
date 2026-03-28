package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Validates and pays costs for spells and abilities.
 */
class CostHandler(
) {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Check if a mana cost can be paid from a player's mana pool.
     */
    fun canPayManaCost(manaPool: ManaPool, cost: ManaCost): Boolean {
        return manaPool.canPay(cost)
    }

    /**
     * Pay a mana cost from a player's mana pool.
     * Returns the new mana pool or null if can't pay.
     */
    fun payManaCost(manaPool: ManaPool, cost: ManaCost, spellContext: SpellPaymentContext? = null): ManaPool? {
        return manaPool.pay(cost, spellContext)
    }

    /**
     * Check if an ability cost can be paid.
     */
    fun canPayAbilityCost(
        state: GameState,
        cost: AbilityCost,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool
    ): Boolean {
        return when (cost) {
            is AbilityCost.Free -> true
            is AbilityCost.Tap -> {
                !state.getEntity(sourceId)!!.has<TappedComponent>()
            }
            is AbilityCost.Untap -> {
                state.getEntity(sourceId)!!.has<TappedComponent>()
            }
            is AbilityCost.Mana -> {
                canPayManaCost(manaPool, cost.cost)
            }
            is AbilityCost.PayLife -> {
                val life = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
                life > cost.amount // Can pay if life is > cost (not <= 0 after)
            }
            is AbilityCost.Sacrifice -> {
                val candidates = findMatchingPermanentsUnified(state, controllerId, cost.filter)
                val eligible = if (cost.excludeSelf) candidates.filter { it != sourceId } else candidates
                eligible.size >= cost.count
            }
            is AbilityCost.SacrificeChosenCreatureType -> {
                val chosenType = state.getEntity(sourceId)?.get<ChosenCreatureTypeComponent>()?.creatureType
                    ?: return false
                val filter = GameObjectFilter.Creature.withSubtype(chosenType)
                findMatchingPermanentsUnified(state, controllerId, filter).isNotEmpty()
            }
            is AbilityCost.Discard -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                findMatchingCardsUnified(state, state.getZone(handZone), cost.filter, controllerId).isNotEmpty()
            }
            is AbilityCost.DiscardHand -> {
                // You can always discard your hand, even if it's empty
                true
            }
            is AbilityCost.ExileFromGraveyard -> {
                val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
                findMatchingCardsUnified(state, state.getZone(graveyardZone), cost.filter, controllerId).size >= cost.count
            }
            is AbilityCost.ExileXFromGraveyard -> {
                // X can be 0, so this is always payable as long as graveyard exists
                // maxAffordableX is capped by graveyard size in LegalActionsCalculator
                true
            }
            is AbilityCost.DiscardSelf -> {
                // Card must be in hand
                val handZone = ZoneKey(controllerId, Zone.HAND)
                state.getZone(handZone).contains(sourceId)
            }
            is AbilityCost.SacrificeSelf -> {
                // Source must be on the battlefield
                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                state.getZone(battlefieldZone).contains(sourceId)
            }
            is AbilityCost.ExileSelf -> {
                // Source must be on the battlefield
                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                state.getZone(battlefieldZone).contains(sourceId)
            }
            is AbilityCost.TapPermanents -> {
                val candidates = findUntappedMatchingPermanentsUnified(state, controllerId, cost.filter)
                    .let { targets -> if (cost.excludeSelf) targets.filter { it != sourceId } else targets }
                candidates.size >= cost.count
            }
            is AbilityCost.TapXPermanents -> {
                // X can be 0, so this is always payable
                // maxAffordableX is capped by untapped permanent count in LegalActionsCalculator
                true
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return false
                val attachedEntity = state.getEntity(attachedId) ?: return false
                if (attachedEntity.has<TappedComponent>()) return false
                // Check summoning sickness on the attached creature
                val card = attachedEntity.get<CardComponent>()
                if (card != null && card.typeLine.isCreature) {
                    val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                    val hasHaste = card.baseKeywords.contains(com.wingedsheep.sdk.core.Keyword.HASTE)
                    if (hasSummoningSickness && !hasHaste) return false
                }
                true
            }
            is AbilityCost.ReturnToHand -> {
                findMatchingPermanentsUnified(state, controllerId, cost.filter).size >= cost.count
            }
            is AbilityCost.RemoveXPlusOnePlusOneCounters -> {
                // X can be 0, so this is always payable as long as there are creatures
                // maxAffordableX is capped by total +1/+1 counters in LegalActionsCalculator
                true
            }
            is AbilityCost.RemoveCounterFromSelf -> {
                val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                val counterType = resolveNamedCounterType(cost.counterType)
                (counters?.getCount(counterType) ?: 0) > 0
            }
            is AbilityCost.Forage -> {
                // Can forage if: 3+ cards in graveyard OR a Food artifact on battlefield
                val graveyardSize = state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)).size
                val hasFood = state.getBattlefield().any { permId ->
                    val permContainer = state.getEntity(permId) ?: return@any false
                    val permCard = permContainer.get<CardComponent>() ?: return@any false
                    val permController = permContainer.get<ControllerComponent>()?.playerId
                    permController == controllerId && permCard.typeLine.hasSubtype(Subtype.FOOD)
                }
                graveyardSize >= 3 || hasFood
            }
            is AbilityCost.Composite -> {
                cost.costs.all { canPayAbilityCost(state, it, sourceId, controllerId, manaPool) }
            }
            is AbilityCost.Loyalty -> {
                // Check if we have enough loyalty to pay the cost
                // Positive changes (like +1) can always be paid
                // Negative changes (like -2) require at least that many counters
                if (cost.change >= 0) {
                    true
                } else {
                    val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                    val currentLoyalty = counters?.getCount(CounterType.LOYALTY) ?: 0
                    currentLoyalty >= -cost.change
                }
            }
        }
    }

    /**
     * Pay an ability cost.
     */
    fun payAbilityCost(
        state: GameState,
        cost: AbilityCost,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        choices: CostPaymentChoices = CostPaymentChoices()
    ): CostPaymentResult {
        return when (cost) {
            is AbilityCost.Free -> {
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.Tap -> {
                val newState = state.updateEntity(sourceId) { it.with(TappedComponent) }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Untap -> {
                val newState = state.updateEntity(sourceId) { it.without<TappedComponent>() }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Mana -> {
                val newPool = payManaCost(manaPool, cost.cost)
                    ?: return CostPaymentResult.failure("Cannot pay mana cost")
                CostPaymentResult.success(state, newPool)
            }
            is AbilityCost.PayLife -> {
                val currentLife = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life
                    ?: return CostPaymentResult.failure("Player has no life total")
                val newState = state.updateEntity(controllerId) { container ->
                    container.with(LifeTotalComponent(currentLife - cost.amount))
                }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Sacrifice, is AbilityCost.SacrificeChosenCreatureType -> {
                val requiredCount = when (cost) {
                    is AbilityCost.Sacrifice -> cost.count
                    else -> 1
                }
                val toSacrificeList = choices.sacrificeChoices.take(requiredCount)
                if (toSacrificeList.size < requiredCount) {
                    return CostPaymentResult.failure("Not enough sacrifice targets chosen (need $requiredCount, got ${toSacrificeList.size})")
                }

                // Validate the chosen sacrifice matches the required filter
                val sacrificeFilter = when (cost) {
                    is AbilityCost.Sacrifice -> cost.filter
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = state.getEntity(sourceId)?.get<ChosenCreatureTypeComponent>()?.creatureType
                            ?: return CostPaymentResult.failure("No creature type chosen")
                        GameObjectFilter.Creature.withSubtype(chosenType)
                    }
                    else -> null
                }

                val context = PredicateContext(controllerId = controllerId)
                val projected = state.projectedState

                var newState = state
                val events = mutableListOf<GameEvent>()

                for (toSacrifice in toSacrificeList) {
                    val sacrificeContainer = newState.getEntity(toSacrifice)
                        ?: return CostPaymentResult.failure("Sacrifice target not found")
                    val sacrificeController = sacrificeContainer.get<ControllerComponent>()?.playerId
                        ?: return CostPaymentResult.failure("Sacrifice target has no controller")
                    val sacrificeName = sacrificeContainer.get<CardComponent>()?.name ?: "Unknown"

                    if (sacrificeFilter != null) {
                        if (!predicateEvaluator.matchesWithProjection(state, projected, toSacrifice, sacrificeFilter, context)) {
                            return CostPaymentResult.failure("Sacrifice target does not match the required filter")
                        }
                    }
                    // Validate excludeSelf: "sacrifice another creature" cannot sacrifice the source
                    if (cost is AbilityCost.Sacrifice && cost.excludeSelf && toSacrifice == sourceId) {
                        return CostPaymentResult.failure("Sacrifice target does not match the required filter")
                    }

                    // Capture AttachedToComponent before zone transition — needed by effects that
                    // read the enchanted creature from the sacrificed aura at resolution time.
                    val attachedTo = sacrificeContainer.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()

                    // Track Food sacrifice before zone transition
                    newState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.trackFoodSacrifice(newState, listOf(toSacrifice), sacrificeController)

                    // Delegate zone movement to ZoneTransitionService for full cleanup
                    val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                        newState, toSacrifice, Zone.GRAVEYARD
                    )
                    newState = transitionResult.state

                    // Restore AttachedToComponent for effects that need it at resolution time
                    if (attachedTo != null) {
                        newState = newState.updateEntity(toSacrifice) { c -> c.with(attachedTo) }
                    }

                    events.add(PermanentsSacrificedEvent(sacrificeController, listOf(toSacrifice), listOf(sacrificeName)))
                    events.addAll(transitionResult.events)
                }

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.Discard -> {
                val toDiscard = choices.discardChoices.firstOrNull()
                    ?: return CostPaymentResult.failure("No discard target chosen")

                val discardContainer = state.getEntity(toDiscard)
                    ?: return CostPaymentResult.failure("Card to discard not found")
                val discardOwner = discardContainer.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Card to discard has no owner")

                val handZone = ZoneKey(discardOwner, Zone.HAND)
                val graveyardZone = ZoneKey(discardOwner, Zone.GRAVEYARD)

                var newState = state.removeFromZone(handZone, toDiscard)
                newState = newState.addToZone(graveyardZone, toDiscard)

                val discardCardName = discardContainer.get<CardComponent>()?.name ?: "Card"
                val events = listOf(
                    CardsDiscardedEvent(discardOwner, listOf(toDiscard), listOf(discardCardName)),
                    ZoneChangeEvent(
                        entityId = toDiscard,
                        entityName = discardContainer.get<CardComponent>()?.name ?: "Unknown",
                        fromZone = Zone.HAND,
                        toZone = Zone.GRAVEYARD,
                        ownerId = discardOwner
                    )
                )

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.DiscardHand -> {
                // Discard all cards from the controller's hand
                val handZone = ZoneKey(controllerId, Zone.HAND)
                val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
                val cardsInHand = state.getZone(handZone)

                if (cardsInHand.isEmpty()) {
                    return CostPaymentResult.success(state, manaPool)
                }

                var newState = state
                val events = mutableListOf<GameEvent>()
                val discardedIds = mutableListOf<com.wingedsheep.sdk.model.EntityId>()

                for (cardId in cardsInHand) {
                    val cardContainer = newState.getEntity(cardId) ?: continue
                    val cardName = cardContainer.get<CardComponent>()?.name ?: "Unknown"

                    newState = newState.removeFromZone(handZone, cardId)
                    newState = newState.addToZone(graveyardZone, cardId)
                    discardedIds.add(cardId)

                    events.add(
                        ZoneChangeEvent(
                            entityId = cardId,
                            entityName = cardName,
                            fromZone = Zone.HAND,
                            toZone = Zone.GRAVEYARD,
                            ownerId = controllerId
                        )
                    )
                }

                if (discardedIds.isNotEmpty()) {
                    val discardedNames = discardedIds.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                    events.add(0, CardsDiscardedEvent(controllerId, discardedIds, discardedNames))
                }

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.ExileFromGraveyard -> {
                exileCardsFromGraveyard(state, controllerId, cost.count, cost.filter, choices.exileChoices, manaPool)
            }
            is AbilityCost.ExileXFromGraveyard -> {
                val xCount = choices.xValue
                if (xCount == 0) {
                    CostPaymentResult.success(state, manaPool)
                } else {
                    exileCardsFromGraveyard(state, controllerId, xCount, cost.filter, choices.exileChoices, manaPool)
                }
            }
            is AbilityCost.DiscardSelf -> {
                // TODO: Discard self
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.SacrificeSelf -> {
                // Sacrifice the source permanent
                val sourceContainer = state.getEntity(sourceId)
                    ?: return CostPaymentResult.failure("Source permanent not found")
                val sourceController = sourceContainer.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Source permanent has no controller")
                val sourceName = sourceContainer.get<CardComponent>()?.name ?: "Unknown"

                // Capture AttachedToComponent before zone transition — needed by effects like
                // GrantToEnchantedCreatureTypeGroupEffect (Crown cycle) that read the enchanted
                // creature from the source at resolution time.
                val attachedTo = sourceContainer.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()

                // Track Food sacrifice before zone transition
                var preState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.trackFoodSacrifice(state, listOf(sourceId), sourceController)

                // Delegate zone movement to ZoneTransitionService for full cleanup
                val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    preState, sourceId, Zone.GRAVEYARD
                )

                // Restore AttachedToComponent on the entity in graveyard for effects that need it
                var newState = transitionResult.state
                if (attachedTo != null) {
                    newState = newState.updateEntity(sourceId) { c -> c.with(attachedTo) }
                }

                val events = mutableListOf<GameEvent>()
                events.add(PermanentsSacrificedEvent(sourceController, listOf(sourceId)))
                events.addAll(transitionResult.events)

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.ExileSelf -> {
                // Exile the source permanent
                state.getEntity(sourceId)
                    ?: return CostPaymentResult.failure("Source permanent not found")

                // Delegate zone movement to ZoneTransitionService for full cleanup
                val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    state, sourceId, Zone.EXILE
                )

                CostPaymentResult.success(transitionResult.state, manaPool, transitionResult.events)
            }
            is AbilityCost.ReturnToHand -> {
                if (choices.bounceChoices.size < cost.count) {
                    return CostPaymentResult.failure("Not enough permanents chosen to bounce (need ${cost.count}, got ${choices.bounceChoices.size})")
                }

                val toBounceList = choices.bounceChoices.take(cost.count)
                val context = PredicateContext(controllerId = controllerId)
                var newState = state
                val allEvents = mutableListOf<GameEvent>()

                for (toBounce in toBounceList) {
                    newState.getEntity(toBounce)
                        ?: return CostPaymentResult.failure("Bounce target not found")

                    // Validate the chosen bounce target matches the required filter
                    if (!predicateEvaluator.matches(newState, toBounce, cost.filter, context)) {
                        return CostPaymentResult.failure("Bounce target does not match the required filter")
                    }

                    // Delegate zone movement to ZoneTransitionService for full cleanup
                    val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                        newState, toBounce, Zone.HAND
                    )
                    newState = transitionResult.state
                    allEvents.addAll(transitionResult.events)
                }

                CostPaymentResult.success(newState, manaPool, allEvents)
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return CostPaymentResult.failure("Source is not attached to a creature")
                val newState = state.updateEntity(attachedId) { it.with(TappedComponent) }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.TapPermanents -> {
                val toTap = choices.tapChoices
                if (toTap.size < cost.count) {
                    return CostPaymentResult.failure("Not enough permanents chosen to tap (need ${cost.count}, got ${toTap.size})")
                }
                if (cost.excludeSelf && sourceId in toTap) {
                    return CostPaymentResult.failure("Cannot tap self for this cost")
                }

                var newState = state
                for (permanentId in toTap) {
                    newState = newState.updateEntity(permanentId) { it.with(TappedComponent) }
                }

                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.TapXPermanents -> {
                val xCount = choices.xValue
                if (xCount == 0) {
                    CostPaymentResult.success(state, manaPool)
                } else {
                    val toTap = choices.tapChoices
                    if (toTap.size < xCount) {
                        return CostPaymentResult.failure("Not enough permanents chosen to tap (need $xCount, got ${toTap.size})")
                    }

                    var newState = state
                    for (permanentId in toTap.take(xCount)) {
                        newState = newState.updateEntity(permanentId) { it.with(TappedComponent) }
                    }

                    CostPaymentResult.success(newState, manaPool)
                }
            }
            is AbilityCost.RemoveXPlusOnePlusOneCounters -> {
                val xCount = choices.xValue
                if (xCount == 0) {
                    CostPaymentResult.success(state, manaPool)
                } else if (choices.counterRemovalChoices.isNotEmpty()) {
                    removeCountersFromCreaturesWithChoices(state, controllerId, xCount, choices.counterRemovalChoices, manaPool)
                } else {
                    removeCountersFromCreatures(state, controllerId, xCount, manaPool)
                }
            }
            is AbilityCost.RemoveCounterFromSelf -> {
                val counterType = resolveNamedCounterType(cost.counterType)
                val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                    ?: return CostPaymentResult.failure("Source has no counters")
                val currentCount = counters.getCount(counterType)
                if (currentCount <= 0) {
                    return CostPaymentResult.failure("Source has no ${cost.counterType} counters to remove")
                }
                val newState = state.updateEntity(sourceId) { container ->
                    val c = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(c.withRemoved(counterType, 1))
                }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Forage -> {
                // Forage: sacrifice a Food if available, otherwise exile 3 cards from graveyard
                val foods = state.getBattlefield().filter { permId ->
                    val permContainer = state.getEntity(permId) ?: return@filter false
                    val permCard = permContainer.get<CardComponent>() ?: return@filter false
                    val permController = permContainer.get<ControllerComponent>()?.playerId
                    permController == controllerId && permCard.typeLine.hasSubtype(Subtype.FOOD)
                }

                if (foods.isNotEmpty()) {
                    // Sacrifice a Food (auto-select first available)
                    val foodId = choices.sacrificeChoices.firstOrNull()?.takeIf { it in foods } ?: foods.first()
                    val foodName = state.getEntity(foodId)?.get<CardComponent>()?.name ?: "Food"
                    val foodController = state.getEntity(foodId)?.get<ControllerComponent>()?.playerId ?: controllerId

                    var newState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.trackFoodSacrifice(state, listOf(foodId), foodController)
                    val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                        newState, foodId, Zone.GRAVEYARD
                    )
                    val events = mutableListOf<GameEvent>()
                    events.add(PermanentsSacrificedEvent(foodController, listOf(foodId), listOf(foodName)))
                    events.addAll(transitionResult.events)
                    CostPaymentResult.success(transitionResult.state, manaPool, events)
                } else {
                    // Exile 3 cards from graveyard
                    val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
                    val graveyardCards = state.getZone(graveyardZone)
                    if (graveyardCards.size < 3) {
                        return CostPaymentResult.failure("Cannot forage: need 3 cards in graveyard or a Food")
                    }
                    val toExile = (choices.exileChoices.takeIf { it.size >= 3 }?.take(3)
                        ?: graveyardCards.take(3))
                    var newState = state
                    val events = mutableListOf<GameEvent>()
                    for (cardId in toExile) {
                        val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                            newState, cardId, Zone.EXILE
                        )
                        newState = transitionResult.state
                        events.addAll(transitionResult.events)
                    }
                    CostPaymentResult.success(newState, manaPool, events)
                }
            }
            is AbilityCost.Composite -> {
                var currentState = state
                var currentPool = manaPool
                val allEvents = mutableListOf<GameEvent>()
                for (subCost in cost.costs) {
                    val result = payAbilityCost(currentState, subCost, sourceId, controllerId, currentPool, choices)
                    if (!result.success) return result
                    currentState = result.newState!!
                    currentPool = result.newManaPool!!
                    allEvents.addAll(result.events)
                }
                CostPaymentResult.success(currentState, currentPool, allEvents)
            }
            is AbilityCost.Loyalty -> {
                // Adjust loyalty counters based on the cost change
                val newState = state.updateEntity(sourceId) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    val newCounters = if (cost.change >= 0) {
                        counters.withAdded(CounterType.LOYALTY, cost.change)
                    } else {
                        counters.withRemoved(CounterType.LOYALTY, -cost.change)
                    }
                    container.with(newCounters)
                }
                CostPaymentResult.success(newState, manaPool)
            }
        }
    }

    /**
     * Check if additional costs can be paid.
     */
    fun canPayAdditionalCost(
        state: GameState,
        cost: AdditionalCost,
        controllerId: EntityId
    ): Boolean {
        return when (cost) {
            is AdditionalCost.SacrificePermanent -> {
                findMatchingPermanentsUnified(state, controllerId, cost.filter).size >= cost.count
            }
            is AdditionalCost.DiscardCards -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                findMatchingCardsUnified(state, state.getZone(handZone), cost.filter, controllerId).size >= cost.count
            }
            is AdditionalCost.PayLife -> {
                val life = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
                life > cost.amount
            }
            is AdditionalCost.ExileCards -> {
                val zone = ZoneKey(controllerId, cost.fromZone.toZone())
                findMatchingCardsUnified(state, state.getZone(zone), cost.filter, controllerId).size >= cost.count
            }
            is AdditionalCost.ExileVariableCards -> {
                val zone = ZoneKey(controllerId, cost.fromZone.toZone())
                findMatchingCardsUnified(state, state.getZone(zone), cost.filter, controllerId).size >= cost.minCount
            }
            is AdditionalCost.TapPermanents -> {
                findUntappedMatchingPermanentsUnified(state, controllerId, cost.filter).size >= cost.count
            }
            is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                // Always payable - sacrificing 0 creatures is valid
                true
            }
            is AdditionalCost.Forage -> {
                // Can forage if: 3+ cards in graveyard OR a Food artifact on battlefield
                val graveyardSize = state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)).size
                val hasFood = state.getBattlefield().any { permId ->
                    val permContainer = state.getEntity(permId) ?: return@any false
                    val permCard = permContainer.get<CardComponent>() ?: return@any false
                    val permController = permContainer.get<ControllerComponent>()?.playerId
                    permController == controllerId && permCard.typeLine.hasSubtype(Subtype.FOOD)
                }
                graveyardSize >= 3 || hasFood
            }
        }
    }

    /**
     * Exile a specified number of cards from the controller's graveyard.
     * Uses provided exile choices if available, otherwise auto-selects.
     */
    private fun exileCardsFromGraveyard(
        state: GameState,
        controllerId: EntityId,
        count: Int,
        filter: GameObjectFilter,
        exileChoices: List<EntityId>,
        manaPool: ManaPool
    ): CostPaymentResult {
        val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
        val validCards = findMatchingCardsUnified(state, state.getZone(graveyardZone), filter, controllerId)

        if (validCards.size < count) {
            return CostPaymentResult.failure("Not enough cards in graveyard to exile")
        }

        // Use exile choices if provided, otherwise auto-select
        val toExile = if (exileChoices.isNotEmpty()) {
            exileChoices.take(count)
        } else {
            validCards.take(count)
        }

        var newState = state
        val events = mutableListOf<GameEvent>()
        val exileZone = ZoneKey(controllerId, Zone.EXILE)

        for (cardId in toExile) {
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
            newState = newState.removeFromZone(graveyardZone, cardId)
            newState = newState.addToZone(exileZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = Zone.GRAVEYARD,
                    toZone = Zone.EXILE,
                    ownerId = controllerId
                )
            )
        }

        return CostPaymentResult.success(newState, manaPool, events)
    }

    /**
     * Remove X +1/+1 counters from among creatures the player controls.
     * Auto-distributes removal across creatures, preferring those with the most counters.
     */
    private fun removeCountersFromCreatures(
        state: GameState,
        controllerId: EntityId,
        count: Int,
        manaPool: ManaPool
    ): CostPaymentResult {
        // Find all creatures with +1/+1 counters controlled by this player
        val creaturesWithCounters = state.entities.filter { (_, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            container.get<CardComponent>()?.typeLine?.isCreature == true &&
            (container.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) > 0
        }.map { (entityId, container) ->
            entityId to (container.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0)
        }.sortedByDescending { it.second } // Prefer creatures with the most counters

        val totalAvailable = creaturesWithCounters.sumOf { it.second }
        if (totalAvailable < count) {
            return CostPaymentResult.failure("Not enough +1/+1 counters to remove (need $count, have $totalAvailable)")
        }

        var newState = state
        var remaining = count

        for ((creatureId, available) in creaturesWithCounters) {
            if (remaining <= 0) break
            val toRemove = minOf(remaining, available)
            newState = newState.updateEntity(creatureId) { container ->
                val counters = container.get<CountersComponent>() ?: CountersComponent()
                container.with(counters.withRemoved(CounterType.PLUS_ONE_PLUS_ONE, toRemove))
            }
            remaining -= toRemove
        }

        return CostPaymentResult.success(newState, manaPool)
    }

    /**
     * Remove +1/+1 counters from creatures using the player's chosen distribution.
     * Validates that each creature is controlled by the player and has enough counters.
     */
    private fun removeCountersFromCreaturesWithChoices(
        state: GameState,
        controllerId: EntityId,
        count: Int,
        choices: Map<EntityId, Int>,
        manaPool: ManaPool
    ): CostPaymentResult {
        val totalChosen = choices.values.sum()
        if (totalChosen != count) {
            return CostPaymentResult.failure("Counter removal total ($totalChosen) does not match X ($count)")
        }

        var newState = state
        for ((creatureId, toRemove) in choices) {
            if (toRemove <= 0) continue
            val container = state.getEntity(creatureId)
                ?: return CostPaymentResult.failure("Creature not found for counter removal")
            if (container.get<ControllerComponent>()?.playerId != controllerId) {
                return CostPaymentResult.failure("Cannot remove counters from a creature you don't control")
            }
            if (container.get<CardComponent>()?.typeLine?.isCreature != true) {
                return CostPaymentResult.failure("Can only remove +1/+1 counters from creatures")
            }
            val available = container.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            if (available < toRemove) {
                return CostPaymentResult.failure("Creature does not have enough +1/+1 counters (need $toRemove, have $available)")
            }
            newState = newState.updateEntity(creatureId) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withRemoved(CounterType.PLUS_ONE_PLUS_ONE, toRemove))
            }
        }

        return CostPaymentResult.success(newState, manaPool)
    }

    // Helper functions

    private fun findMatchingPermanentsUnified(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        return state.entities.filter { (entityId, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, context)
        }.keys.toList()
    }

    private fun findUntappedMatchingPermanentsUnified(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        return state.entities.filter { (entityId, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            !container.has<TappedComponent>() &&
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, context)
        }.keys.toList()
    }

    private fun findMatchingCardsUnified(
        state: GameState,
        cardIds: List<EntityId>,
        filter: GameObjectFilter,
        controllerId: EntityId
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        return cardIds.filter { cardId ->
            predicateEvaluator.matchesWithProjection(state, projected, cardId, filter, context)
        }
    }

    private fun resolveNamedCounterType(name: String): CounterType {
        return try {
            CounterType.valueOf(name.uppercase().replace(' ', '_'))
        } catch (_: IllegalArgumentException) {
            CounterType.PLUS_ONE_PLUS_ONE
        }
    }

    private fun com.wingedsheep.sdk.scripting.CostZone.toZone(): Zone = when (this) {
        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
    }
}

/**
 * Result of paying a cost.
 */
data class CostPaymentResult(
    val success: Boolean,
    val newState: GameState?,
    val newManaPool: ManaPool?,
    val error: String?,
    val events: List<GameEvent> = emptyList()
) {
    companion object {
        fun success(state: GameState, manaPool: ManaPool, events: List<GameEvent> = emptyList()) =
            CostPaymentResult(true, state, manaPool, null, events)

        fun failure(error: String) =
            CostPaymentResult(false, null, null, error)
    }
}

/**
 * Player choices for paying costs.
 */
data class CostPaymentChoices(
    val sacrificeChoices: List<EntityId> = emptyList(),
    val discardChoices: List<EntityId> = emptyList(),
    val exileChoices: List<EntityId> = emptyList(),
    val tapChoices: List<EntityId> = emptyList(),
    val bounceChoices: List<EntityId> = emptyList(),
    val xValue: Int = 0,
    val counterRemovalChoices: Map<EntityId, Int> = emptyMap()
)
