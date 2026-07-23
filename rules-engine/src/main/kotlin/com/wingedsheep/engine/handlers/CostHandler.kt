package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Validates and pays costs for spells and abilities.
 */
class CostHandler {

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
        /**
         * The permanent whose static ability granted [cost]'s ability to [sourceId], when this is a
         * granted ability. Only [AbilityCost.TapGrantingPermanent] needs it — the granter has to be
         * untapped for that cost to be payable. Null (the default) means "granter unknown", which
         * defers the check to payment time, matching the Exile/Sacrifice granter costs.
         */
        granterId: EntityId? = null,
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
            is AbilityCost.SacrificeGrantingPermanent -> {
                // Same granter-resolution contract as ExileGrantingPermanent above.
                true
            }
            is AbilityCost.TapGrantingPermanent -> {
                // Unlike its exile/sacrifice siblings this one has a real payability gate: a
                // granter that is already tapped can't be tapped again, which is what stops a
                // Fishing Pole from being milked more than once per untap. When the caller
                // couldn't resolve the granter we fall through to payment-time validation.
                if (granterId == null) true
                else {
                    val granter = state.getEntity(granterId)
                    granter != null && granterId in state.getBattlefield() && !granter.has<TappedComponent>()
                }
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
            is AbilityCost.Forage ->
                com.wingedsheep.engine.handlers.costs.ForageCostResolver.canPay(state, controllerId)
            is AbilityCost.Blight -> {
                // Blight requires at least one creature you control that can have -1/-1 counters
                // put on it. A creature that "can't have counters put on it" (Blossombind) is not
                // a legal blight target — CR 614.17b: the cost's event can't happen.
                val projected = state.projectedState
                projected.getBattlefieldControlledBy(controllerId)
                    .any { projected.isCreature(it) && projected.canReceiveCounters(it) }
            }
            is AbilityCost.Craft -> {
                // Source on battlefield + enough materials in the unified BF+GY pool (CR 702.167a-b).
                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                if (!state.getZone(battlefieldZone).contains(sourceId)) return false
                val candidates = findCraftMaterialCandidates(state, controllerId, cost.filter, sourceId)
                if (candidates.size < cost.minCount) return false
                // Heterogeneous per-slot craft (Throne of the Grim Captain): the count is necessary
                // but not sufficient — the candidates must admit a matching that saturates every
                // slot (four Vampires can't cover Dinosaur/Merfolk/Pirate/Vampire). CR 702.167.
                if (cost.slots.isNotEmpty() &&
                    !com.wingedsheep.engine.handlers.costs.CraftSlotMatching.canSatisfyAllSlots(
                        cost.slots, candidates
                    ) { materialId, slotFilter -> craftMaterialMatchesSlot(state, controllerId, materialId, slotFilter) }
                ) return false
                true
            }
            is AbilityCost.Composite -> {
                cost.costs.all { canPayAbilityCost(state, it, sourceId, controllerId, manaPool, abilityContext, granterId) }
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
                // Route through the tap atom so the "{T}:" cost emits its TappedEvent — every
                // payAbilityCost caller gets the event for free (ActivateAbilityHandler used to
                // re-derive it by hand, silently dropping it on any other payment path).
                val (newState, event) = tap(state, sourceId)
                CostPaymentResult.success(newState, manaPool, listOfNotNull(event))
            }
            is AbilityCost.Untap -> {
                // Route through the untap atom so a "{Q}:" cost emits UntappedEvent and a stun
                // counter correctly replaces the untap (CR 122.1d) instead of being ignored.
                val (newState, events) = untapOrConsumeStun(state, sourceId)
                CostPaymentResult.success(newState, manaPool, events)
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
                val result = ZoneTransitionService
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

                val result = ZoneTransitionService
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
                val result = ZoneTransitionService
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
                val attachedTo = sourceContainer.get<AttachedToComponent>()

                // Track Food sacrifice before zone transition
                val preState = ZoneTransitionService.trackPermanentSacrifice(state, listOf(sourceId), sourceController)

                // Delegate zone movement to ZoneTransitionService for full cleanup
                val transitionResult = ZoneTransitionService.moveToZone(
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
                val transitionResult = ZoneTransitionService.moveToZone(
                    state, sourceId, Zone.EXILE
                )

                CostPaymentResult.success(transitionResult.state, manaPool, transitionResult.events)
            }
            is AbilityCost.ExileGrantingPermanent -> {
                val granterId = choices.granterId
                    ?: return CostPaymentResult.failure("Granting permanent not provided for cost")
                state.getEntity(granterId)
                    ?: return CostPaymentResult.failure("Granting permanent not found")

                val transitionResult = ZoneTransitionService.moveToZone(
                    state, granterId, Zone.EXILE
                )

                CostPaymentResult.success(transitionResult.state, manaPool, transitionResult.events)
            }
            is AbilityCost.TapGrantingPermanent -> {
                val granterId = choices.granterId
                    ?: return CostPaymentResult.failure("Granting permanent not provided for cost")
                val granter = state.getEntity(granterId)
                    ?: return CostPaymentResult.failure("Granting permanent not found")
                if (granterId !in state.getBattlefield()) {
                    return CostPaymentResult.failure("Granting permanent is not on the battlefield")
                }
                if (granter.has<TappedComponent>()) {
                    return CostPaymentResult.failure("Granting permanent is already tapped")
                }
                // Through the shared tap helper so the TappedEvent fires — a "whenever this becomes
                // tapped" trigger must see an Equipment tapped to pay a granted ability's cost.
                val (newState, event) = tap(state, granterId)
                CostPaymentResult.success(newState, manaPool, listOfNotNull(event))
            }
            is AbilityCost.SacrificeGrantingPermanent -> {
                val granterId = choices.granterId
                    ?: return CostPaymentResult.failure("Granting permanent not provided for cost")
                val granter = state.getEntity(granterId)
                    ?: return CostPaymentResult.failure("Granting permanent not found")
                val granterController = granter.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Granting permanent has no controller")

                // Capture AttachedToComponent before the zone transition so a granted effect that
                // reads the sacrificed granter's attachment at resolution time still sees it —
                // mirrors the SacrificeSelf branch.
                val attachedTo = granter.get<AttachedToComponent>()

                // Track Food/dies-with-counter sacrifice bookkeeping before the zone transition,
                // then move to the graveyard — mirrors the SacrificeSelf branch, but on the granter.
                val preState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .trackPermanentSacrifice(state, listOf(granterId), granterController)
                val transitionResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    preState, granterId, Zone.GRAVEYARD
                )

                var newState = transitionResult.state
                if (attachedTo != null) {
                    newState = newState.updateEntity(granterId) { c -> c.with(attachedTo) }
                }

                val events = mutableListOf<GameEvent>()
                events.add(PermanentsSacrificedEvent(granterController, listOf(granterId)))
                events.addAll(transitionResult.events)

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return CostPaymentResult.failure("Source is not attached to a creature")
                val (newState, event) = tap(state, attachedId)
                CostPaymentResult.success(newState, manaPool, listOfNotNull(event))
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
                    val events = mutableListOf<GameEvent>()
                    for (permanentId in toTap.take(xCount)) {
                        val (tappedState, event) = tap(newState, permanentId)
                        newState = tappedState
                        event?.let(events::add)
                    }

                    CostPaymentResult.success(newState, manaPool, events)
                }
            }
            is AbilityCost.Forage -> {
                // Unified forage payment — honors the player's mode + card/Food choice, falling
                // back to a legal auto-payment only when no valid choice was supplied. See
                // [com.wingedsheep.engine.handlers.costs.ForageCostResolver].
                when (val result = com.wingedsheep.engine.handlers.costs.ForageCostResolver.pay(
                    state, controllerId,
                    exileChoices = choices.exileChoices,
                    sacrificeChoices = choices.sacrificeChoices,
                )) {
                    is com.wingedsheep.engine.handlers.costs.ForageCostResolver.Result.Success ->
                        CostPaymentResult.success(result.state, manaPool, result.events)
                    is com.wingedsheep.engine.handlers.costs.ForageCostResolver.Result.Failure ->
                        CostPaymentResult.failure(result.reason)
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
                val firstThisTurn = DamageUtils
                    .isFirstCounterThisTurn(state, targetId)
                val withCounters = state.updateEntity(targetId) { c ->
                    c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, cost.amount))
                }
                val newState = DamageUtils
                    .markCounterPlacedOnCreature(withCounters, controllerId, targetId)
                val targetName = targetContainer.get<CardComponent>()?.name ?: "Creature"
                val events = listOf<GameEvent>(
                    CountersAddedEvent(
                        entityId = targetId,
                        counterType = Counters.MINUS_ONE_MINUS_ONE,
                        amount = cost.amount,
                        entityName = targetName,
                        firstThisTurn = firstThisTurn,
                        placedBy = controllerId
                    )
                )
                CostPaymentResult.success(newState, manaPool, events)
            }
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
            if (atom.distinctNames) {
                // "Sacrifice N ... with different names" — need at least N distinctly-named candidates.
                eligible.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }.toSet().size >= atom.count
            } else {
                eligible.size >= atom.count
            }
        }
        is CostAtom.ExilePermanents -> {
            val candidates = findMatchingPermanentsUnified(state, controllerId, atom.filter)
            val eligible = if (atom.excludeSelf) candidates.filter { it != sourceId } else candidates
            eligible.size >= atom.minCount
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
        // Adding counters takes nothing away, so there is never a reason it can't be paid — this
        // is what keeps Mazemind Tome activatable on the very activation that exiles it.
        is CostAtom.PutCountersOnSelf -> true
        is CostAtom.RemoveCounters -> {
            if (atom.self) {
                val counters = state.getEntity(sourceId)?.get<CountersComponent>() ?: return false
                val ct = atom.counterType?.let { resolveNamedCounterType(it) }
                val needed = getAtomCount(atom.count)
                if (needed <= 0) return true
                if (ct != null) counters.getCount(ct) >= needed
                else counters.counters.values.sum() >= needed
            } else {
                val counterType = atom.counterType?.let { resolveNamedCounterType(it) }
                val projected = state.projectedState
                val ctx = PredicateContext(controllerId = controllerId)
                val needed = getAtomCount(atom.count)
                if (needed <= 0) return true
                val total = projected.getBattlefieldControlledBy(controllerId).sumOf { entityId ->
                    if (!predicateEvaluator.matches(state, projected, entityId, atom.filter, ctx)) return@sumOf 0
                    val counters = state.getEntity(entityId)?.get<CountersComponent>() ?: return@sumOf 0
                    if (counterType != null) counters.getCount(counterType)
                    else counters.counters.values.sum()
                }
                total >= needed
            }
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
            requiredCount = atom.count, excludeSelf = atom.excludeSelf, sourceId, controllerId, manaPool,
            distinctNames = atom.distinctNames
        )
        is CostAtom.ExilePermanents -> payExilePermanentsList(
            state, choices.exileChoices, atom.filter,
            minCount = atom.minCount, excludeSelf = atom.excludeSelf, sourceId, controllerId, manaPool
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
            val result = ZoneTransitionService
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
        is CostAtom.PutCountersOnSelf -> {
            // Counters put on as a cost are an ordinary counter placement (CR 121.6), so they run
            // through the same chokepoint as the AddCounters effect: the "can't have counters put
            // on it" gate (CR 614.1 / Solemnity), the placement replacements (Hardened Scales,
            // Doubling Season), and the first-placement-this-turn marker. A permanent that can't
            // receive counters still pays the cost — nothing is owed and the activation stands.
            val counterType = resolveNamedCounterType(atom.counterType)
            if (!state.projectedState.canReceiveCounters(sourceId)) {
                CostPaymentResult.success(state, manaPool)
            } else {
                val current = state.getEntity(sourceId)?.get<CountersComponent>() ?: CountersComponent()
                val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                    state, sourceId, counterType, atom.count, placerId = controllerId
                )
                val firstThisTurn = DamageUtils.isFirstCounterThisTurn(state, sourceId)
                val newState = state.updateEntity(sourceId) { c ->
                    c.with(current.withAdded(counterType, modifiedCount))
                }.let { DamageUtils.markCounterPlacedOnCreature(it, controllerId, sourceId) }
                val entityName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: ""
                CostPaymentResult.success(
                    newState,
                    manaPool,
                    events = listOf(
                        CountersAddedEvent(
                            sourceId, atom.counterType, modifiedCount, entityName,
                            firstThisTurn, placedBy = controllerId,
                        )
                    ),
                )
            }
        }
        is CostAtom.RemoveCounters -> {
            val counterType = atom.counterType?.let { resolveNamedCounterType(it) }
            val requiredCount = getAtomCount(atom.count, choices)
            var newState = state
            val events = mutableListOf<GameEvent>()

            if (atom.self) {
                // Remove from source permanent (self-cost)
                val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                    ?: return CostPaymentResult.failure("Source has no counters")
                if (counterType != null) {
                    val current = counters.getCount(counterType)
                    if (current < requiredCount) {
                        return CostPaymentResult.failure("Source has only $current ${atom.counterType} counters, need $requiredCount")
                    }
                    newState = newState.updateEntity(sourceId) { c ->
                        c.with(counters.withRemoved(counterType, requiredCount))
                    }
                    events.add(CountersRemovedEvent(sourceId, atom.counterType!!, requiredCount, "Source"))
                } else {
                    // Self with any-type: use distributedCounterRemovals
                    val removals = choices.distributedCounterRemovals
                    for (removal in removals) {
                        if (removal.count <= 0) continue
                        val resolvedType = resolveNamedCounterType(removal.counterType)
                        val source = state.getEntity(sourceId)?.get<CountersComponent>() ?: continue
                        newState = newState.updateEntity(sourceId) { c ->
                            c.with(source.withRemoved(resolvedType, removal.count))
                        }
                        events.add(CountersRemovedEvent(sourceId, removal.counterType, removal.count, "Source"))
                    }
                }
            } else {
                val removals = choices.distributedCounterRemovals
                val totalChosen = removals.sumOf { it.count }
                if (totalChosen != requiredCount) {
                    return CostPaymentResult.failure(
                        "Counter removal total ($totalChosen) does not match required count ($requiredCount)"
                    )
                }
                val execution = CostPaymentService.applyDistributedCounterRemovals(
                    newState, controllerId, atom, removals
                )
                if (!execution.success) return CostPaymentResult.failure("Counter removal validation failed")
                newState = execution.state
                events.addAll(execution.events)
            }
            CostPaymentResult.success(newState, manaPool, events)
        }
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
        distinctNames: Boolean = false,
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
            // "With different names" requires a real choice (ActivateAbilityHandler always pauses
            // for it), so an empty selection here means the cost wasn't paid — never auto-pick.
            if (candidates.size > requiredCount || distinctNames) {
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
        // "Sacrifice N ... with different names" — the chosen permanents must be pairwise distinct.
        if (distinctNames) {
            val names = toSacrificeList.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
            if (names.size != toSacrificeList.size || names.toSet().size != names.size) {
                return CostPaymentResult.failure("Sacrificed permanents must all have different names")
            }
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
            val attachedTo = sacrificeContainer.get<AttachedToComponent>()

            // Track Food sacrifice before zone transition
            newState = ZoneTransitionService.trackPermanentSacrifice(newState, listOf(toSacrifice), sacrificeController)

            // Delegate zone movement to ZoneTransitionService for full cleanup
            val transitionResult = ZoneTransitionService.moveToZone(
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

    /**
     * Pay a [CostAtom.ExilePermanents] variable-count cost: exile every permanent the player chose
     * ([exileChoices]), re-validating that each matches [filter], is controlled by the activator,
     * and — when [excludeSelf] — isn't the ability's own source. At least [minCount] must be chosen.
     * Unlike the fixed-count sacrifice / exile-from-zone atoms this exiles *all* selected permanents
     * (the count is the player's choice, CR 601.2b). Permanents move to exile via
     * [ZoneTransitionService.moveToZone], so attached Auras fall off, tokens cease to exist, and
     * leaves-the-battlefield triggers fire.
     */
    private fun payExilePermanentsList(
        state: GameState,
        exileChoices: List<EntityId>,
        filter: GameObjectFilter,
        minCount: Int,
        excludeSelf: Boolean,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
    ): CostPaymentResult {
        // With no selection supplied, auto-pick ONLY when the choice is forced (exactly minCount
        // eligible). A real choice (more eligible than minCount) is paused for by
        // ActivateAbilityHandler; never silently guess which permanents to exile.
        val toExile: List<EntityId> = if (exileChoices.isEmpty()) {
            val candidates = findMatchingCardsUnified(state, state.getBattlefield(controllerId), filter, controllerId)
                .let { if (excludeSelf) it.filter { id -> id != sourceId } else it }
            if (candidates.size < minCount) {
                return CostPaymentResult.failure("Not enough permanents to exile (need $minCount, got ${candidates.size})")
            }
            if (candidates.size > minCount) {
                return CostPaymentResult.failure("No permanents chosen to exile (need at least $minCount)")
            }
            candidates
        } else {
            exileChoices
        }
        if (toExile.size < minCount) {
            return CostPaymentResult.failure("Not enough permanents chosen to exile (need $minCount, got ${toExile.size})")
        }
        val context = PredicateContext(controllerId = controllerId)
        val projected = state.projectedState
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (id in toExile) {
            val container = newState.getEntity(id)
                ?: return CostPaymentResult.failure("Permanent to exile not found")
            val itsController = container.get<ControllerComponent>()?.playerId
                ?: return CostPaymentResult.failure("Permanent to exile has no controller")
            if (itsController != controllerId) {
                return CostPaymentResult.failure("Can only exile permanents you control")
            }
            if (!predicateEvaluator.matches(state, projected, id, filter, context)) {
                return CostPaymentResult.failure("Permanent to exile does not match the required filter")
            }
            if (excludeSelf && id == sourceId) {
                return CostPaymentResult.failure("Cannot exile the source permanent for this cost")
            }
            val transitionResult = ZoneTransitionService.moveToZone(newState, id, Zone.EXILE)
            newState = transitionResult.state
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
        val events = mutableListOf<GameEvent>()
        for (permanentId in toTap) {
            // The tap atom emits the TappedEvent so "whenever this becomes tapped" triggers fire
            // when a creature is tapped to pay a cost (Station, Cryptic Gateway). Open-coding the
            // mutation here used to drop the event (cf. the declare-attackers fix in
            // AttackPhaseManager.commitAttackDeclaration).
            val (tappedState, event) = tap(newState, permanentId)
            newState = tappedState
            event?.let(events::add)
        }
        return CostPaymentResult.success(newState, manaPool, events)
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
            val transitionResult = ZoneTransitionService.moveToZone(
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
                is CostAtom.RemoveCounters -> {
                    val needed = getAtomCount(atom.count)
                    if (needed <= 0) true
                    else {
                        val counterType = atom.counterType?.let { resolveNamedCounterType(it) }
                        val projected = state.projectedState
                        val ctx = PredicateContext(controllerId = controllerId)
                        val total = projected.getBattlefieldControlledBy(controllerId).sumOf { entityId ->
                            if (!predicateEvaluator.matches(state, projected, entityId, atom.filter, ctx)) return@sumOf 0
                            val counters = state.getEntity(entityId)?.get<CountersComponent>() ?: return@sumOf 0
                            if (counterType != null) counters.getCount(counterType)
                            else counters.counters.values.sum()
                        }
                        total >= needed
                    }
                }
                // Mana / return-to-hand / reveal / put-counters-on-self / exile-permanents are not
                // produced as spell additional costs today (put-counters-on-self is inherently
                // ability-scoped — a spell on the stack has no permanent to put the counters on; and
                // ExilePermanents is an activated-ability cost only).
                is CostAtom.Mana, is CostAtom.ReturnToHand, is CostAtom.RevealFromHand,
                is CostAtom.PutCountersOnSelf, is CostAtom.ExilePermanents -> false
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
            is AdditionalCost.Forage ->
                com.wingedsheep.engine.handlers.costs.ForageCostResolver.canPay(state, controllerId)
            is AdditionalCost.Choice ->
                // Cost-vs-cost: payable if at least one option can be paid.
                cost.options.any { canPayAdditionalCost(state, it, controllerId) }
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
            is AdditionalCost.PayXLife -> {
                // X = 0 is always legal (default minCount = 0); a higher minCount requires
                // enough life to pay it.
                cost.minCount <= 0 || state.lifeTotal(controllerId) >= cost.minCount
            }
            is AdditionalCost.PayLifeEqualToManaValueOfSpell -> {
                // Affordability is per-cast (depends on the cast card's mana value), so it is
                // checked at CastSpellHandler time, not here. Always "payable" at this generic gate.
                true
            }
            is AdditionalCost.BeholdOrPay -> {
                // Always payable: player can always choose the "pay mana" path
                true
            }
            is AdditionalCost.ExileFromGraveyardOrPay -> {
                // Always payable: player can always choose the "pay mana" path
                true
            }
            is AdditionalCost.SacrificeOrPay -> {
                // Always payable: player can always choose the "pay mana" path
                true
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

    /**
     * Edge predicate for heterogeneous Craft slot matching (CR 702.167): may [materialId] fill a
     * slot whose material filter is [slotFilter]? Uses the same projected-state matcher as the
     * unified candidate gathering, so a battlefield permanent's projected subtypes/type line are
     * honored while a graveyard card falls back to its printed characteristics.
     */
    private fun craftMaterialMatchesSlot(
        state: GameState,
        controllerId: EntityId,
        materialId: EntityId,
        slotFilter: GameObjectFilter
    ): Boolean = predicateEvaluator.matches(
        state, state.projectedState, materialId, slotFilter, PredicateContext(controllerId = controllerId)
    )

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
        val maxCount = cost.maxCount
        if (maxCount != null && chosen.size > maxCount) {
            // Exact-count crafts ("Craft with artifact") exile exactly maxCount materials —
            // costs are paid exactly as written (CR 118.8), no over-payment.
            return CostPaymentResult.failure(
                "Craft accepts at most $maxCount ${cost.filter.description}(s) to exile"
            )
        }
        if (chosen.any { it !in candidates }) {
            return CostPaymentResult.failure("Craft material is not a legal candidate")
        }
        if (chosen.contains(sourceId)) {
            return CostPaymentResult.failure("Craft material cannot include the source permanent")
        }
        // Heterogeneous per-slot craft (Throne of the Grim Captain): the chosen set must admit a
        // matching that fills every slot with a distinct material (CR 702.167). min == max ==
        // slots.size is already enforced above, so a full matching here uses each chosen material
        // exactly once. Rejects e.g. four Vampires for a Dinosaur/Merfolk/Pirate/Vampire craft.
        if (cost.slots.isNotEmpty() &&
            !com.wingedsheep.engine.handlers.costs.CraftSlotMatching.canSatisfyAllSlots(
                cost.slots, chosen.toList()
            ) { materialId, slotFilter -> craftMaterialMatchesSlot(state, controllerId, materialId, slotFilter) }
        ) {
            return CostPaymentResult.failure(
                "Chosen Craft materials cannot fill each of the required material slots"
            )
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // 1. Exile each chosen material from its zone via the standard zone-transition pipeline
        //    (LTB triggers, attachment cleanup, last-known info — all handled there). Flag the move
        //    as a craft-material exile so a SELF "exiled from the battlefield while you're activating
        //    a craft ability" trigger (Market Gnome) fires on materials that left the battlefield.
        for (materialId in chosen) {
            val transition = ZoneTransitionService.moveToZone(
                newState, materialId, Zone.EXILE,
                options = com.wingedsheep.engine.handlers.effects.ZoneEntryOptions(craftMaterial = true)
            )
            newState = transition.state
            events.addAll(transition.events)
        }

        // 2. Exile the source itself. The Craft resolution effect will lift it back to the
        //    battlefield as its back face; the materials remain in exile so the back face's
        //    CDA can keep reading their power.
        val selfTransition = ZoneTransitionService.moveToZone(
            newState, sourceId, Zone.EXILE
        )
        newState = selfTransition.state
        events.addAll(selfTransition.events)

        // 3. Record the materials on the source's CraftedFromExiledComponent so the resolution
        //    effect can re-attach the component on battlefield re-entry (it's stripped on
        //    every battlefield entry by applyBattlefieldEntry per Rule 400.7).
        newState = newState.updateEntity(sourceId) { c ->
            c.with(CraftedFromExiledComponent(chosen))
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

    /**
     * Extract the effective count from a [DynamicAmount] in a cost atom.
     * For [DynamicAmount.Fixed], returns the fixed value.
     * For [DynamicAmount.XValue], returns [CostPaymentChoices.xValue] (or 0 when no choices).
     * Scaffolds for other dynamic variants (returns 0 — caller handles zero as "not required").
     */
    private fun getAtomCount(amount: DynamicAmount, choices: CostPaymentChoices? = null): Int = when (amount) {
        is DynamicAmount.Fixed -> amount.amount
        is DynamicAmount.XValue -> choices?.xValue ?: 0
        else -> 0
    }

    private fun resolveNamedCounterType(name: String): CounterType {
        return resolveCounterType(name)
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
    /**
     * Per-entity counter removals with explicit counter type.
     * Each entry carries the entity, the counter type name, and the count to remove.
     */
    val distributedCounterRemovals: List<DistributedCounterRemoval> = emptyList(),
    val blightChoices: List<EntityId> = emptyList(),
    /**
     * The permanent that granted the activated ability being paid for, when the
     * ability comes from a static grant (e.g., GrantActivatedAbility scoped to the attached creature).
     * Read by the executor for [AbilityCost.ExileGrantingPermanent].
     */
    val granterId: EntityId? = null
)
