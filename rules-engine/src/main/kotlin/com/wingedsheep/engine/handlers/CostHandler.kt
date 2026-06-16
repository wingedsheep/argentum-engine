package com.wingedsheep.engine.handlers
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
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
     * Check if a mana cost can be paid from a player's mana pool, considering ability-payment context.
     */
    fun canPayManaCost(manaPool: ManaPool, cost: ManaCost, spellContext: SpellPaymentContext?): Boolean {
        return manaPool.canPay(cost, spellContext)
    }

    /**
     * Check if an ability cost can be paid.
     */
    fun canPayAbilityCost(
        state: GameState,
        cost: AbilityCost,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        abilityContext: SpellPaymentContext? = null,
    ): Boolean {
        return when (cost) {
            is AbilityCost.Free -> true
            is AbilityCost.Atom -> canPayAtom(state, cost.atom, sourceId, controllerId, manaPool, abilityContext)
            is AbilityCost.Tap -> {
                !state.getEntity(sourceId)!!.has<TappedComponent>()
            }
            is AbilityCost.Untap -> {
                state.getEntity(sourceId)!!.has<TappedComponent>()
            }
            is AbilityCost.PayXLife -> {
                // X can be 0, so this is always payable as long as the player has a life total.
                // maxAffordableX is capped by life total in calculateMaxAffordableX.
                state.getEntity(controllerId)?.has<LifeTotalComponent>() == true
            }
            is AbilityCost.SacrificeChosenCreatureType -> {
                val chosenType = state.getEntity(sourceId)?.chosenCreatureType()
                    ?: return false
                val filter = GameObjectFilter.Creature.withSubtype(chosenType)
                findMatchingPermanentsUnified(state, controllerId, filter).isNotEmpty()
            }
            is AbilityCost.DiscardHand -> {
                // You can always discard your hand, even if it's empty
                true
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
            is AbilityCost.DiscardLastDrawnThisTurn -> {
                // Per the Jandor's Ring rulings: "If you do not have the card still in your hand,
                // you can't pay the cost." Track is populated by every CardsDrawnEvent emit site
                // during a turn and cleared at the turn boundary.
                val tracked = state.lastCardDrawnThisTurnByPlayer[controllerId] ?: return false
                state.getZone(ZoneKey(controllerId, Zone.HAND)).contains(tracked)
            }
            is AbilityCost.SacrificeSelf -> {
                // Source must be on the battlefield
                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                state.getZone(battlefieldZone).contains(sourceId)
            }
            is AbilityCost.ExileSelf -> {
                // Source must exist (can be on battlefield or in graveyard for graveyard abilities)
                state.getEntity(sourceId) != null
            }
            is AbilityCost.ExileGrantingPermanent -> {
                // Granter is resolved from the static-grant lookup at activation time.
                // If the ability is enumerable, the granter is on the battlefield; if it has
                // since left, ActivateAbilityHandler's lookup would have failed before payment.
                true
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
                // Check summoning sickness on the attached creature. Read creature-ness and
                // haste from projected state so a Vehicle / animated permanent currently
                // being a creature is gated correctly.
                if (state.projectedState.isCreature(attachedId)) {
                    val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                    val hasHaste = state.projectedState.hasKeyword(attachedId, com.wingedsheep.sdk.core.Keyword.HASTE)
                    if (hasSummoningSickness && !hasHaste) return false
                }
                true
            }
            is AbilityCost.RemoveXPlusOnePlusOneCounters -> {
                // X can be 0, so this is always payable as long as there are creatures
                // maxAffordableX is capped by total +1/+1 counters in LegalActionsCalculator
                true
            }
            is AbilityCost.RemovePlusOnePlusOneCounters -> {
                val available = findMatchingPermanentsUnified(state, controllerId, cost.filter)
                    .sumOf {
                        state.getEntity(it)?.get<CountersComponent>()
                            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                    }
                available >= cost.count
            }
            is AbilityCost.RemoveCounterFromSelf -> {
                val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                val counterType = resolveNamedCounterType(cost.counterType)
                (counters?.getCount(counterType) ?: 0) >= cost.count
            }
            is AbilityCost.Forage -> {
                // Can forage if: 3+ cards in graveyard OR a Food artifact on battlefield
                val graveyardSize = state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)).size
                val projected = state.projectedState
                val hasFood = state.getBattlefield().any { permId ->
                    state.getEntity(permId) ?: return@any false
                    projected.getController(permId) == controllerId &&
                        projected.hasSubtype(permId, Subtype.FOOD.value)
                }
                graveyardSize >= 3 || hasFood
            }
            is AbilityCost.Blight -> {
                // Blight requires at least one creature you control that can have -1/-1 counters
                // put on it. A creature that "can't have counters put on it" (Blossombind) is not
                // a legal blight target — CR 614.17b: the cost's event can't happen.
                val projected = state.projectedState
                projected.getBattlefieldControlledBy(controllerId)
                    .any { projected.isCreature(it) && projected.canReceiveCounters(it) }
            }
            is AbilityCost.RemoveCountersFromAmongFilteredPermanents ->
                com.wingedsheep.engine.handlers.costs.RemoveCountersFromAmongFilteredPermanentsCostHandler
                    .canPay(state, cost, controllerId)
            is AbilityCost.Craft -> {
                // Source on battlefield + enough materials in the unified BF+GY pool (CR 702.167a-b).
                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                if (!state.getZone(battlefieldZone).contains(sourceId)) return false
                findCraftMaterialCandidates(state, controllerId, cost.filter, sourceId).size >= cost.minCount
            }
            is AbilityCost.Composite -> {
                cost.costs.all { canPayAbilityCost(state, it, sourceId, controllerId, manaPool, abilityContext) }
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
        choices: CostPaymentChoices = CostPaymentChoices(),
        abilityContext: SpellPaymentContext? = null,
    ): CostPaymentResult {
        return when (cost) {
            is AbilityCost.Free -> {
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.Atom -> payAtom(state, cost.atom, sourceId, controllerId, manaPool, choices, abilityContext)
            is AbilityCost.Tap -> {
                val newState = state.updateEntity(sourceId) { it.with(TappedComponent) }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Untap -> {
                val newState = state.updateEntity(sourceId) { it.without<TappedComponent>() }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.PayXLife -> {
                val amount = choices.xValue
                if (amount == 0) {
                    CostPaymentResult.success(state, manaPool)
                } else {
                    val (newState, event) = DamageUtils.loseLife(state, controllerId, amount, LifeChangeReason.PAYMENT)
                    if (event == null) return CostPaymentResult.failure("Player has no life total")
                    CostPaymentResult.success(newState, manaPool, events = listOf(event))
                }
            }
            is AbilityCost.SacrificeChosenCreatureType -> {
                val chosenType = state.getEntity(sourceId)?.chosenCreatureType()
                    ?: return CostPaymentResult.failure("No creature type chosen")
                val sacrificeFilter = GameObjectFilter.Creature.withSubtype(chosenType)
                paySacrificeList(
                    state, choices.sacrificeChoices, sacrificeFilter,
                    requiredCount = 1, excludeSelf = false, sourceId, controllerId, manaPool
                )
            }
            is AbilityCost.DiscardHand -> {
                val cardsInHand = state.getZone(ZoneKey(controllerId, Zone.HAND))
                if (cardsInHand.isEmpty()) {
                    return CostPaymentResult.success(state, manaPool)
                }
                val result = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .discardCards(state, controllerId, cardsInHand)
                CostPaymentResult.success(result.state, manaPool, result.events)
            }
            is AbilityCost.ExileXFromGraveyard -> {
                val xCount = choices.xValue
                if (xCount == 0) {
                    CostPaymentResult.success(state, manaPool)
                } else {
                    exileCardsFromZone(state, controllerId, Zone.GRAVEYARD, xCount, cost.filter, choices.exileChoices, manaPool)
                }
            }
            is AbilityCost.DiscardSelf -> {
                // Discard the source card from its owner's hand.
                val sourceContainer = state.getEntity(sourceId)
                    ?: return CostPaymentResult.failure("Source card not found")
                val ownerId = sourceContainer.get<OwnerComponent>()?.playerId
                    ?: sourceContainer.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Source card has no owner")

                if (!state.getZone(ZoneKey(ownerId, Zone.HAND)).contains(sourceId)) {
                    return CostPaymentResult.failure("Source card is not in its owner's hand")
                }

                val result = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .discardCard(state, ownerId, sourceId)
                CostPaymentResult.success(result.state, manaPool, result.events)
            }
            is AbilityCost.DiscardLastDrawnThisTurn -> {
                // The engine picks the card from the per-player tracker — no player choice. canPay
                // already verifies the tracked entity is still in the controller's hand; recheck
                // here defensively in case payment is re-entered after state change.
                val tracked = state.lastCardDrawnThisTurnByPlayer[controllerId]
                    ?: return CostPaymentResult.failure("You haven't drawn a card this turn")
                if (!state.getZone(ZoneKey(controllerId, Zone.HAND)).contains(tracked)) {
                    return CostPaymentResult.failure("The card you drew last this turn is no longer in your hand")
                }
                val result = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .discardCard(state, controllerId, tracked)
                CostPaymentResult.success(result.state, manaPool, result.events)
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
            is AbilityCost.ExileGrantingPermanent -> {
                val granterId = choices.granterId
                    ?: return CostPaymentResult.failure("Granting permanent not provided for cost")
                state.getEntity(granterId)
                    ?: return CostPaymentResult.failure("Granting permanent not found")

                val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    state, granterId, Zone.EXILE
                )

                CostPaymentResult.success(transitionResult.state, manaPool, transitionResult.events)
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return CostPaymentResult.failure("Source is not attached to a creature")
                val newState = state.updateEntity(attachedId) { it.with(TappedComponent) }
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
            is AbilityCost.RemovePlusOnePlusOneCounters -> {
                if (choices.counterRemovalChoices.isNotEmpty()) {
                    removeCountersFromPermanentsWithChoices(
                        state, controllerId, cost.filter, cost.count, choices.counterRemovalChoices, manaPool
                    )
                } else {
                    removeCountersFromPermanents(state, controllerId, cost.filter, cost.count, manaPool)
                }
            }
            is AbilityCost.RemoveCounterFromSelf -> {
                val counterType = resolveNamedCounterType(cost.counterType)
                val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                    ?: return CostPaymentResult.failure("Source has no counters")
                val currentCount = counters.getCount(counterType)
                if (currentCount < cost.count) {
                    return CostPaymentResult.failure("Source has only $currentCount ${cost.counterType} counters, needs ${cost.count}")
                }
                val newState = state.updateEntity(sourceId) { container ->
                    val c = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(c.withRemoved(counterType, cost.count))
                }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Forage -> {
                val projected = state.projectedState
                val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
                val graveyardCards = state.getZone(graveyardZone)
                val foods = state.getBattlefield().filter { permId ->
                    state.getEntity(permId) ?: return@filter false
                    projected.getController(permId) == controllerId &&
                        projected.hasSubtype(permId, Subtype.FOOD.value)
                }

                // Player explicitly picked 3 graveyard cards to exile — honor that path.
                val playerExile = choices.exileChoices.takeIf { it.size >= 3 && it.all { id -> id in graveyardCards } }
                if (playerExile != null) {
                    val toExile = playerExile.take(3)
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
                } else if (foods.isNotEmpty()) {
                    // Sacrifice a Food (respecting player's choice if provided).
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
                } else if (graveyardCards.size >= 3) {
                    val toExile = graveyardCards.take(3)
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
                } else {
                    CostPaymentResult.failure("Cannot forage: need 3 cards in graveyard or a Food")
                }
            }
            is AbilityCost.Blight -> {
                val targetId = choices.blightChoices.firstOrNull()
                    ?: return CostPaymentResult.failure("No blight target chosen")
                val targetContainer = state.getEntity(targetId)
                    ?: return CostPaymentResult.failure("Blight target not found")
                val projected = state.projectedState
                if (projected.getController(targetId) != controllerId || !projected.isCreature(targetId)) {
                    return CostPaymentResult.failure("Blight target must be a creature you control")
                }
                if (!projected.canReceiveCounters(targetId)) {
                    return CostPaymentResult.failure("Blight target can't have counters put on it")
                }
                val counters = targetContainer.get<CountersComponent>() ?: CountersComponent()
                val firstThisTurn = com.wingedsheep.engine.handlers.effects.DamageUtils
                    .isFirstCounterThisTurn(state, targetId)
                val withCounters = state.updateEntity(targetId) { c ->
                    c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, cost.amount))
                }
                val newState = com.wingedsheep.engine.handlers.effects.DamageUtils
                    .markCounterPlacedOnCreature(withCounters, controllerId, targetId)
                val targetName = targetContainer.get<CardComponent>()?.name ?: "Creature"
                val events = listOf<GameEvent>(
                    CountersAddedEvent(
                        entityId = targetId,
                        counterType = Counters.MINUS_ONE_MINUS_ONE,
                        amount = cost.amount,
                        entityName = targetName,
                        firstThisTurn = firstThisTurn
                    )
                )
                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.RemoveCountersFromAmongFilteredPermanents ->
                com.wingedsheep.engine.handlers.costs.RemoveCountersFromAmongFilteredPermanentsCostHandler
                    .pay(state, cost, controllerId, manaPool, choices.counterRemovalChoices)
            is AbilityCost.Craft -> payCraftCost(state, cost, sourceId, controllerId, manaPool, choices)
            is AbilityCost.Composite -> {
                var currentState = state
                var currentPool = manaPool
                val allEvents = mutableListOf<GameEvent>()
                for (subCost in cost.costs) {
                    val result = payAbilityCost(currentState, subCost, sourceId, controllerId, currentPool, choices, abilityContext)
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

    // =============================================================================================
    // Shared CostAtom payment — the one place activated-ability costs and (eventually) other cost
    // contexts dispatch the payable things that mean the same everywhere (§3.2 "one cost language").
    // =============================================================================================

    /** Affordability check for a single [CostAtom] paid as an activated-ability cost. */
    private fun canPayAtom(
        state: GameState,
        atom: CostAtom,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        abilityContext: SpellPaymentContext?,
    ): Boolean = when (atom) {
        is CostAtom.Mana -> canPayManaCost(manaPool, atom.cost, abilityContext)
        is CostAtom.PayLife -> {
            // CR 810.9a — affordability uses the team's shared total in Two-Headed Giant.
            val life = state.lifeTotal(controllerId)
            // CR 119.4 — a player may pay life only if their life total is >= the payment.
            // Paying down to exactly 0 is legal; the state-based action checker handles the loss.
            life >= atom.amount
        }
        is CostAtom.Sacrifice -> {
            val candidates = findMatchingPermanentsUnified(state, controllerId, atom.filter)
            val eligible = if (atom.excludeSelf) candidates.filter { it != sourceId } else candidates
            eligible.size >= atom.count
        }
        is CostAtom.Discard -> {
            val handZone = ZoneKey(controllerId, Zone.HAND)
            findMatchingCardsUnified(state, state.getZone(handZone), atom.filter, controllerId).size >= atom.count
        }
        is CostAtom.ExileFrom -> {
            val zone = ZoneKey(controllerId, atom.zone)
            findMatchingCardsUnified(state, state.getZone(zone), atom.filter, controllerId).size >= atom.count
        }
        is CostAtom.TapPermanents -> {
            val candidates = findUntappedMatchingPermanentsUnified(state, controllerId, atom.filter)
                .let { targets -> if (atom.excludeSelf) targets.filter { it != sourceId } else targets }
            candidates.size >= atom.count
        }
        is CostAtom.ReturnToHand ->
            findMatchingPermanentsUnified(state, controllerId, atom.filter).size >= atom.count
        is CostAtom.RevealFromHand -> {
            val handZone = ZoneKey(controllerId, Zone.HAND)
            findMatchingCardsUnified(state, state.getZone(handZone), atom.filter, controllerId).size >= atom.count
        }
    }

    /** Perform payment for a single [CostAtom] paid as an activated-ability cost. */
    private fun payAtom(
        state: GameState,
        atom: CostAtom,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        choices: CostPaymentChoices,
        abilityContext: SpellPaymentContext?,
    ): CostPaymentResult = when (atom) {
        is CostAtom.Mana -> {
            val newPool = payManaCost(manaPool, atom.cost, abilityContext)
                ?: return CostPaymentResult.failure("Cannot pay mana cost")
            CostPaymentResult.success(state, newPool)
        }
        is CostAtom.PayLife -> {
            val (newState, event) = DamageUtils.loseLife(state, controllerId, atom.amount, LifeChangeReason.PAYMENT)
            if (event == null) return CostPaymentResult.failure("Player has no life total")
            CostPaymentResult.success(newState, manaPool, events = listOf(event))
        }
        is CostAtom.Sacrifice -> paySacrificeList(
            state, choices.sacrificeChoices, atom.filter,
            requiredCount = atom.count, excludeSelf = atom.excludeSelf, sourceId, controllerId, manaPool
        )
        is CostAtom.Discard -> {
            var workState = state
            val toDiscard: List<EntityId> = if (atom.random) {
                // Engine chooses the discarded cards at random — no player selection.
                val eligible = findMatchingCardsUnified(
                    state, state.getZone(ZoneKey(controllerId, Zone.HAND)), atom.filter, controllerId
                )
                if (eligible.size < atom.count) {
                    return CostPaymentResult.failure("Not enough cards to discard at random")
                }
                val (shuffled, advanced) = state.nextRandom { shuffle(eligible) }
                workState = advanced
                shuffled.take(atom.count)
            } else {
                if (choices.discardChoices.size < atom.count) {
                    return CostPaymentResult.failure("Must choose ${atom.count} card(s) to discard")
                }
                choices.discardChoices.take(atom.count)
            }
            val result = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                .discardCards(workState, controllerId, toDiscard)
            CostPaymentResult.success(result.state, manaPool, result.events)
        }
        is CostAtom.ExileFrom ->
            exileCardsFromZone(state, controllerId, atom.zone, atom.count, atom.filter, choices.exileChoices, manaPool)
        is CostAtom.TapPermanents -> payTapPermanents(state, atom, sourceId, controllerId, manaPool, choices)
        is CostAtom.ReturnToHand -> payReturnToHand(state, atom, controllerId, manaPool, choices)
        is CostAtom.RevealFromHand ->
            // No activated-ability cost reveals from hand today; revealing changes no zone, so this
            // is a no-op success kept for atom exhaustiveness (the PayCost reveal path emits the
            // CardsRevealedEvent through CostPaymentService).
            CostPaymentResult.success(state, manaPool)
    }

    /**
     * Sacrifice [requiredCount] permanents from [sacrificeChoices], each validated against [filter]
     * (and excluded from being the source when [excludeSelf]). Shared by the [CostAtom.Sacrifice]
     * atom and the chosen-creature-type sacrifice cost so both go through one cleanup path.
     */
    private fun paySacrificeList(
        state: GameState,
        sacrificeChoices: List<EntityId>,
        filter: GameObjectFilter,
        requiredCount: Int,
        excludeSelf: Boolean,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
    ): CostPaymentResult {
        // When no choice was supplied, auto-pick from the legal candidates — but ONLY when the
        // choice is forced (candidates <= requiredCount). When candidates > requiredCount it's a
        // real choice that ActivateAbilityHandler pauses for; we should never silently take the
        // first N. This mirrors exileCardsFromZone's auto-pick for empty exile choices, and lets
        // the forced case (e.g. exactly one artifact) resolve without a decision while breaking the
        // AI's infinite "Not enough sacrifice targets chosen" loop.
        val toSacrificeList = if (sacrificeChoices.isEmpty()) {
            val candidates = findMatchingCardsUnified(state, state.getBattlefield(controllerId), filter, controllerId)
                .let { if (excludeSelf) it.filter { id -> id != sourceId } else it }
            if (candidates.size < requiredCount) {
                return CostPaymentResult.failure("Not enough sacrifice targets chosen (need $requiredCount, got ${candidates.size})")
            }
            if (candidates.size > requiredCount) {
                // A real choice reached the cost path with no selection — fail rather than guess.
                return CostPaymentResult.failure("Not enough sacrifice targets chosen (need $requiredCount, got 0)")
            }
            candidates.take(requiredCount)
        } else {
            sacrificeChoices.take(requiredCount)
        }
        if (toSacrificeList.size < requiredCount) {
            return CostPaymentResult.failure("Not enough sacrifice targets chosen (need $requiredCount, got ${toSacrificeList.size})")
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

            if (!predicateEvaluator.matches(state, projected, toSacrifice, filter, context)) {
                return CostPaymentResult.failure("Sacrifice target does not match the required filter")
            }
            // Validate excludeSelf: "sacrifice another creature" cannot sacrifice the source.
            if (excludeSelf && toSacrifice == sourceId) {
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
        return CostPaymentResult.success(newState, manaPool, events)
    }

    /** Pay a [CostAtom.TapPermanents] atom from the chosen tap targets, re-validating each. */
    private fun payTapPermanents(
        state: GameState,
        atom: CostAtom.TapPermanents,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        choices: CostPaymentChoices,
    ): CostPaymentResult {
        val toTap = choices.tapChoices
        if (toTap.size < atom.count) {
            return CostPaymentResult.failure("Not enough permanents chosen to tap (need ${atom.count}, got ${toTap.size})")
        }
        if (atom.excludeSelf && sourceId in toTap) {
            return CostPaymentResult.failure("Cannot tap self for this cost")
        }

        // Defense in depth: the enumerator only offers untapped, controlled, matching
        // permanents (CostEnumerationUtils.findAbilityTapTargets), but the chosen ids
        // arrive on the action from the client — re-validate here so a malformed action
        // can't "pay" a tap cost by re-tapping an already-tapped or ineligible permanent
        // (Station, Cryptic Gateway). Filter matching uses projected state (CR 613).
        val projected = state.projectedState
        val context = PredicateContext(controllerId = controllerId)
        for (permanentId in toTap) {
            val entity = state.getEntity(permanentId)
                ?: return CostPaymentResult.failure("Permanent to tap no longer exists")
            if (permanentId !in state.getBattlefield()) {
                return CostPaymentResult.failure("Permanent to tap is not on the battlefield")
            }
            if (projected.getController(permanentId) != controllerId) {
                return CostPaymentResult.failure("Can only tap permanents you control")
            }
            if (entity.has<TappedComponent>()) {
                return CostPaymentResult.failure("Permanent to tap is already tapped")
            }
            if (!predicateEvaluator.matches(state, projected, permanentId, atom.filter, context)) {
                return CostPaymentResult.failure("Permanent to tap does not match ${atom.filter.description}")
            }
        }

        var newState = state
        for (permanentId in toTap) {
            newState = newState.updateEntity(permanentId) { it.with(TappedComponent) }
        }
        return CostPaymentResult.success(newState, manaPool)
    }

    /** Pay a [CostAtom.ReturnToHand] atom from the chosen bounce targets, validating each. */
    private fun payReturnToHand(
        state: GameState,
        atom: CostAtom.ReturnToHand,
        controllerId: EntityId,
        manaPool: ManaPool,
        choices: CostPaymentChoices,
    ): CostPaymentResult {
        if (choices.bounceChoices.size < atom.count) {
            return CostPaymentResult.failure("Not enough permanents chosen to bounce (need ${atom.count}, got ${choices.bounceChoices.size})")
        }

        val toBounceList = choices.bounceChoices.take(atom.count)
        val context = PredicateContext(controllerId = controllerId)
        var newState = state
        val allEvents = mutableListOf<GameEvent>()

        for (toBounce in toBounceList) {
            newState.getEntity(toBounce)
                ?: return CostPaymentResult.failure("Bounce target not found")

            // Validate the chosen bounce target matches the required filter
            if (!predicateEvaluator.matches(newState, newState.projectedState, toBounce, atom.filter, context)) {
                return CostPaymentResult.failure("Bounce target does not match the required filter")
            }

            // Delegate zone movement to ZoneTransitionService for full cleanup
            val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                newState, toBounce, Zone.HAND
            )
            newState = transitionResult.state
            allEvents.addAll(transitionResult.events)
        }

        return CostPaymentResult.success(newState, manaPool, allEvents)
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
            is AdditionalCost.Atom -> when (val atom = cost.atom) {
                is CostAtom.Sacrifice ->
                    findMatchingPermanentsUnified(state, controllerId, atom.filter).size >= atom.count
                is CostAtom.Discard ->
                    findMatchingCardsUnified(state, state.getZone(ZoneKey(controllerId, Zone.HAND)), atom.filter, controllerId).size >= atom.count
                is CostAtom.PayLife -> {
                    // CR 810.9a — affordability uses the team's shared total in Two-Headed Giant.
                    val life = state.lifeTotal(controllerId)
                    // CR 119.4 — a player may pay life only if their life total is >= the payment.
                    life >= atom.amount
                }
                is CostAtom.ExileFrom ->
                    findMatchingCardsUnified(state, state.getZone(ZoneKey(controllerId, atom.zone)), atom.filter, controllerId).size >= atom.count
                is CostAtom.TapPermanents ->
                    findUntappedMatchingPermanentsUnified(state, controllerId, atom.filter).size >= atom.count
                // Mana / return-to-hand / reveal are not produced as spell additional costs today.
                is CostAtom.Mana, is CostAtom.ReturnToHand, is CostAtom.RevealFromHand -> false
            }
            is AdditionalCost.PayLifePerTarget -> {
                // Always payable: choosing zero targets pays zero life. Per-target life
                // is validated against the chosen target count at CastSpellHandler time.
                true
            }
            is AdditionalCost.ExileVariableCards -> {
                val zone = ZoneKey(controllerId, cost.fromZone.toZone())
                findMatchingCardsUnified(state, state.getZone(zone), cost.filter, controllerId).size >= cost.minCount
            }
            is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                // Always payable - sacrificing 0 creatures is valid
                true
            }
            is AdditionalCost.Forage -> {
                // Can forage if: 3+ cards in graveyard OR a Food artifact on battlefield
                val graveyardSize = state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)).size
                val projected = state.projectedState
                val hasFood = state.getBattlefield().any { permId ->
                    state.getEntity(permId) ?: return@any false
                    projected.getController(permId) == controllerId &&
                        projected.hasSubtype(permId, Subtype.FOOD.value)
                }
                graveyardSize >= 3 || hasFood
            }
            is AdditionalCost.Behold -> {
                // Can behold if matching permanent on battlefield or matching card in hand
                val projected = state.projectedState
                val predicateContext = PredicateContext(controllerId = controllerId)
                val hasBattlefieldMatch = projected.getBattlefieldControlledBy(controllerId).any { permId ->
                    predicateEvaluator.matches(state, projected, permId, cost.filter, predicateContext)
                }
                val hasHandMatch = state.getHand(controllerId).any { cardId ->
                    predicateEvaluator.matches(state, state.projectedState, cardId, cost.filter, predicateContext)
                }
                hasBattlefieldMatch || hasHandMatch
            }
            is AdditionalCost.ExileFromStorage -> {
                // Payability determined by the preceding cost that populated the storage
                true
            }
            is AdditionalCost.BlightOrPay -> {
                // Always payable: player can always choose the "pay mana" path
                true
            }
            is AdditionalCost.BlightVariable -> {
                // X = 0 is always legal (default minCount = 0); higher minCounts
                // require a creature you control whose toughness >= minCount.
                if (cost.minCount <= 0) {
                    true
                } else {
                    val projected = state.projectedState
                    state.getBattlefield().any { permId ->
                        projected.getController(permId) == controllerId &&
                            projected.isCreature(permId) &&
                            (projected.getToughness(permId) ?: 0) >= cost.minCount
                    }
                }
            }
            is AdditionalCost.BeholdOrPay -> {
                // Always payable: player can always choose the "pay mana" path
                true
            }
            is AdditionalCost.RemoveCountersFromYourCreatures -> {
                val projected = state.projectedState
                val total = state.getBattlefield().sumOf { permId ->
                    if (projected.getController(permId) != controllerId) return@sumOf 0
                    if (!projected.isCreature(permId)) return@sumOf 0
                    state.getEntity(permId)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0
                }
                total >= cost.totalCount
            }
            is AdditionalCost.Composite -> {
                // All steps must be payable
                cost.steps.all { canPayAdditionalCost(state, it, controllerId) }
            }
            is AdditionalCost.ChooseEntity -> {
                // Payable iff at least one entity in the searched zones matches the filter.
                findChooseEntityCandidates(state, cost, controllerId).isNotEmpty()
            }
        }
    }

    /**
     * Enumerate every entity the caster could legally pick for an
     * [AdditionalCost.ChooseEntity] step. For each (zone, filter) entry in
     * [AdditionalCost.ChooseEntity.zoneFilters], iterate the caster's slice of
     * that zone and apply the filter — projected state for the battlefield,
     * base state for hidden / card zones (matching the Behold convention).
     *
     * Shared so the enumerator, validation, and payment paths all see the same
     * candidate set.
     */
    fun findChooseEntityCandidates(
        state: GameState,
        cost: AdditionalCost.ChooseEntity,
        controllerId: EntityId,
    ): List<EntityId> {
        val projected = state.projectedState
        val ctx = PredicateContext(controllerId = controllerId)
        return cost.zoneFilters.flatMap { (zone, filter) ->
            when (zone) {
                Zone.BATTLEFIELD -> projected.getBattlefieldControlledBy(controllerId)
                    .filter { predicateEvaluator.matches(state, projected, it, filter, ctx) }
                else -> state.getZone(ZoneKey(controllerId, zone))
                    .filter { predicateEvaluator.matches(state, state.projectedState, it, filter, ctx) }
            }
        }
    }

    /**
     * Exile a specified number of cards matching [filter] from the controller's [fromZone].
     * Uses provided exile choices if available, otherwise auto-selects. Ability exile costs are
     * graveyard-only today (Costs.ExileFromGraveyard → CostAtom.ExileFrom(GRAVEYARD)); the zone
     * parameter keeps the helper honest for any future non-graveyard ability exile.
     */
    private fun exileCardsFromZone(
        state: GameState,
        controllerId: EntityId,
        fromZone: Zone,
        count: Int,
        filter: GameObjectFilter,
        exileChoices: List<EntityId>,
        manaPool: ManaPool
    ): CostPaymentResult {
        val sourceZone = ZoneKey(controllerId, fromZone)
        val validCards = findMatchingCardsUnified(state, state.getZone(sourceZone), filter, controllerId)

        if (validCards.size < count) {
            return CostPaymentResult.failure("Not enough cards in ${fromZone.name.lowercase()} to exile")
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
            newState = newState.removeFromZone(sourceZone, cardId)
            newState = newState.addToZone(exileZone, cardId)
            events.add(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardName,
                    fromZone = fromZone,
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

    /**
     * Fixed-count counter removal across permanents you control matching [filter].
     * Auto-distributes when the player did not supply a choice map, preferring
     * permanents with the most counters first.
     */
    private fun removeCountersFromPermanents(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter,
        count: Int,
        manaPool: ManaPool
    ): CostPaymentResult {
        val candidates = findMatchingPermanentsUnified(state, controllerId, filter)
            .mapNotNull { id ->
                val n = state.getEntity(id)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                if (n > 0) id to n else null
            }
            .sortedByDescending { it.second }

        val available = candidates.sumOf { it.second }
        if (available < count) {
            return CostPaymentResult.failure(
                "Not enough +1/+1 counters to remove (need $count, have $available)"
            )
        }

        var newState = state
        var remaining = count
        for ((id, have) in candidates) {
            if (remaining <= 0) break
            val toRemove = minOf(remaining, have)
            newState = newState.updateEntity(id) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withRemoved(CounterType.PLUS_ONE_PLUS_ONE, toRemove))
            }
            remaining -= toRemove
        }
        return CostPaymentResult.success(newState, manaPool)
    }

    private fun removeCountersFromPermanentsWithChoices(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter,
        count: Int,
        choices: Map<EntityId, Int>,
        manaPool: ManaPool
    ): CostPaymentResult {
        val totalChosen = choices.values.sum()
        if (totalChosen != count) {
            return CostPaymentResult.failure(
                "Counter removal total ($totalChosen) does not match required count ($count)"
            )
        }
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        var newState = state
        for ((permId, toRemove) in choices) {
            if (toRemove <= 0) continue
            val container = state.getEntity(permId)
                ?: return CostPaymentResult.failure("Permanent not found for counter removal")
            if (container.get<ControllerComponent>()?.playerId != controllerId) {
                return CostPaymentResult.failure("Cannot remove counters from a permanent you don't control")
            }
            if (!predicateEvaluator.matches(state, projected, permId, filter, context)) {
                return CostPaymentResult.failure("Permanent does not match the cost's filter")
            }
            val available = container.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            if (available < toRemove) {
                return CostPaymentResult.failure(
                    "Permanent does not have enough +1/+1 counters (need $toRemove, have $available)"
                )
            }
            newState = newState.updateEntity(permId) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withRemoved(CounterType.PLUS_ONE_PLUS_ONE, toRemove))
            }
        }
        return CostPaymentResult.success(newState, manaPool)
    }

    /**
     * Find the combined BF+GY pool of legal Craft materials (CR 702.167a-b):
     * permanents the activator controls **and** cards in their graveyard that match [filter].
     * The source permanent itself is never a valid material — it's exiled separately by the
     * paired self-exile clause.
     */
    private fun findCraftMaterialCandidates(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter,
        sourceId: EntityId
    ): List<EntityId> {
        val battlefield = findMatchingPermanentsUnified(state, controllerId, filter)
            .filter { it != sourceId }
        val graveyardCards = findMatchingCardsUnified(
            state, state.getZone(ZoneKey(controllerId, Zone.GRAVEYARD)), filter, controllerId
        )
        return battlefield + graveyardCards
    }

    private fun payCraftCost(
        state: GameState,
        cost: AbilityCost.Craft,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        choices: CostPaymentChoices
    ): CostPaymentResult {
        val candidates = findCraftMaterialCandidates(state, controllerId, cost.filter, sourceId).toSet()
        // Materials are a player choice (CR 702.167a-b): the activator picks which permanents
        // and/or graveyard cards to exile. No silent auto-pick — callers must supply the
        // chosen IDs via CostPaymentChoices.exileChoices (web client routes them through the
        // CraftMaterialOverlay; game-server callers populate the field directly).
        val chosen = choices.exileChoices
        if (chosen.isEmpty()) {
            return CostPaymentResult.failure(
                "Craft requires the activator to choose at least ${cost.minCount} " +
                    "${cost.filter.description}(s) to exile via costPayment.exiledCards"
            )
        }
        if (chosen.size < cost.minCount) {
            return CostPaymentResult.failure(
                "Craft requires at least ${cost.minCount} ${cost.filter.description}(s) to exile"
            )
        }
        if (chosen.any { it !in candidates }) {
            return CostPaymentResult.failure("Craft material is not a legal candidate")
        }
        if (chosen.contains(sourceId)) {
            return CostPaymentResult.failure("Craft material cannot include the source permanent")
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // 1. Exile each chosen material from its zone via the standard zone-transition pipeline
        //    (LTB triggers, attachment cleanup, last-known info — all handled there).
        for (materialId in chosen) {
            val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                newState, materialId, Zone.EXILE
            )
            newState = transition.state
            events.addAll(transition.events)
        }

        // 2. Exile the source itself. The Craft resolution effect will lift it back to the
        //    battlefield as its back face; the materials remain in exile so the back face's
        //    CDA can keep reading their power.
        val selfTransition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
            newState, sourceId, Zone.EXILE
        )
        newState = selfTransition.state
        events.addAll(selfTransition.events)

        // 3. Record the materials on the source's CraftedFromExiledComponent so the resolution
        //    effect can re-attach the component on battlefield re-entry (it's stripped on
        //    every battlefield entry by applyBattlefieldEntry per Rule 400.7).
        newState = newState.updateEntity(sourceId) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent(chosen))
        }

        return CostPaymentResult.success(newState, manaPool, events)
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
            predicateEvaluator.matches(state, projected, entityId, filter, context)
        }.keys.toList()
    }

    // `internal` (not private) so the TapXPermanents cost-choice pause in ActivateAbilityHandler
    // can offer the same untapped-permanent candidate set used to enumerate the tap cost. Keeps
    // the pause and the rest of the cost machinery reading one definition of "tappable here".
    internal fun findUntappedMatchingPermanentsUnified(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        return state.entities.filter { (entityId, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            !container.has<TappedComponent>() &&
            predicateEvaluator.matches(state, projected, entityId, filter, context)
        }.keys.toList()
    }

    // `internal` (not private) so the activated-ability cost-choice pause in
    // ActivateAbilityHandler can offer exactly the candidate set this matcher accepts at payment
    // time — see exileCardsFromGraveyard above. Keeps the pause and payment in lockstep instead
    // of re-deriving the filter match in two places.
    internal fun findMatchingCardsUnified(
        state: GameState,
        cardIds: List<EntityId>,
        filter: GameObjectFilter,
        controllerId: EntityId
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        return cardIds.filter { cardId ->
            predicateEvaluator.matches(state, projected, cardId, filter, context)
        }
    }

    private fun resolveNamedCounterType(name: String): CounterType {
        return when (name) {
            "+1/+1" -> CounterType.PLUS_ONE_PLUS_ONE
            "-1/-1" -> CounterType.MINUS_ONE_MINUS_ONE
            else -> try {
                CounterType.valueOf(name.uppercase().replace(' ', '_'))
            } catch (_: IllegalArgumentException) {
                CounterType.PLUS_ONE_PLUS_ONE
            }
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
    val counterRemovalChoices: Map<EntityId, Int> = emptyMap(),
    val blightChoices: List<EntityId> = emptyList(),
    /**
     * The permanent that granted the activated ability being paid for, when the
     * ability comes from a static grant (e.g., GrantActivatedAbility scoped to the attached creature).
     * Read by the executor for [AbilityCost.ExileGrantingPermanent].
     */
    val granterId: EntityId? = null
)
