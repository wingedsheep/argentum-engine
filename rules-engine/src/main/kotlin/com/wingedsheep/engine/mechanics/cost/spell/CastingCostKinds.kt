package com.wingedsheep.engine.mechanics.cost.spell

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.costs.ChoiceCostResolver
import com.wingedsheep.engine.handlers.costs.ForageCostResolver
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment

/*
 * Kinds for the casting-context-specific [AdditionalCost] subtypes — the costs that mean something
 * only while a spell is being cast.
 */

/** Life the caster can pay, and the CR 119.4 rejection when they can't. */
private fun rejectUnaffordableLife(check: SpellCostCheck, required: Int, message: () -> String): String? {
    val currentLife = check.state.lifeTotal(check.playerId) // CR 810.9a — team's shared total
    // CR 119.4 — you can't pay life unless you have at least that much
    return if (currentLife < required) message() else null
}

/**
 * Puts [amount] -1/-1 counters on [targetId] as a blight payment, with the first-counter-this-turn
 * record and the counters-added event.
 */
private fun blight(ledger: SpellCostLedger, targetId: EntityId, amount: Int) {
    val targetContainer = ledger.state.getEntity(targetId) ?: return
    val counters = targetContainer.get<CountersComponent>() ?: CountersComponent()
    val firstThisTurn = DamageUtils.isFirstCounterThisTurn(ledger.state, targetId)
    val firstOfTypeThisTurn =
        DamageUtils.isFirstCounterOfTypeThisTurn(ledger.state, targetId, CounterType.MINUS_ONE_MINUS_ONE)
    ledger.state = ledger.state.updateEntity(targetId) { c ->
        c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, amount))
    }
    ledger.state = DamageUtils.markCounterPlacedOnCreature(
        ledger.state, ledger.playerId, targetId, CounterType.MINUS_ONE_MINUS_ONE
    )
    ledger.events.add(CountersAddedEvent(
        entityId = targetId,
        counterType = CounterType.MINUS_ONE_MINUS_ONE,
        amount = amount,
        entityName = targetContainer.get<CardComponent>()?.name ?: "Creature",
        firstThisTurn = firstThisTurn,
        firstOfTypeThisTurn = firstOfTypeThisTurn,
        placedBy = ledger.playerId
    ))
}

/** The creature a blight payment names must be a creature you control that can take counters. */
private fun validateBlightTarget(check: SpellCostCheck, targetId: EntityId): String? {
    val state = check.state
    val projected = state.projectedState
    val container = state.getEntity(targetId)
        ?: return "Blight target not found: $targetId"
    container.get<CardComponent>()
        ?: return "Blight target is not a card: $targetId"
    if (projected.getController(targetId) != check.playerId) {
        return "You can only blight creatures you control"
    }
    if (targetId !in state.getBattlefield()) {
        return "Blight target is not on the battlefield: $targetId"
    }
    if (!projected.isCreature(targetId)) {
        return "Blight target must be a creature"
    }
    if (!projected.canReceiveCounters(targetId)) {
        return "Blight target can't have counters put on it"
    }
    return null
}

/** Creatures [EntityId] controls that can take -1/-1 counters — the blight pool. */
private fun blightableCreatures(env: SpellCostEnumeration): List<EntityId> {
    val projected = env.state.projectedState
    return projected.getBattlefieldControlledBy(env.playerId)
        .filter { projected.isCreature(it) && projected.canReceiveCounters(it) }
}

/**
 * "This spell costs 3 life more to cast for each target" (Phyrexian Purge). Always payable at
 * enumeration time — a caster with too little life can choose fewer (or zero, when the requirement
 * is optional) targets; the amount is computed from the chosen targets at cast time.
 */
internal object PayLifePerTargetCostKind : SpellCostKind<AdditionalCost.PayLifePerTarget> {
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.PayLifePerTarget, costHandler: CostHandler) = true

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.PayLifePerTarget): String? {
        val required = lifeToPay(check, cost)
        return rejectUnaffordableLife(check, required) {
            "Not enough life to pay $required life for ${check.action.targets.size} targets"
        }
    }

    override fun lifeToPay(check: SpellCostCheck, cost: AdditionalCost.PayLifePerTarget) =
        cost.amountPerTarget * check.action.targets.size
}

/** "As an additional cost to cast this spell, pay X life." */
internal object PayXLifeCostKind : SpellCostKind<AdditionalCost.PayXLife> {
    // X = 0 is always legal (default minCount = 0); a higher minCount requires enough life to pay it.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.PayXLife, costHandler: CostHandler) =
        cost.minCount <= 0 || state.lifeTotal(payerId) >= cost.minCount

    // Surface the cap (current life total) so the client can bound the X slider (0..payXLifeMaxX).
    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.PayXLife, offer: SpellCostOffer): Boolean {
        val currentLife = env.state.lifeTotal(env.playerId)
        offer.payXLifeCost = cost
        offer.payXLifeMaxX = currentLife
        return currentLife >= cost.minCount
    }

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.PayXLife): String? {
        val amount = check.payment?.payXLifeAmount ?: 0
        if (amount < cost.minCount) {
            return "Pay X life: X must be at least ${cost.minCount} (got $amount)"
        }
        if (amount < 0) {
            return "Pay X life: X cannot be negative"
        }
        val currentLife = check.state.lifeTotal(check.playerId)
        if (amount > currentLife) {
            return "Pay X life: X ($amount) cannot exceed your life total ($currentLife)"
        }
        return null
    }

    override fun lifeToPay(check: SpellCostCheck, cost: AdditionalCost.PayXLife) = check.payment?.payXLifeAmount ?: 0
}

/** "Pay life equal to this spell's mana value." */
internal object PayLifeEqualToManaValueCostKind : SpellCostKind<AdditionalCost.PayLifeEqualToManaValueOfSpell> {
    // Affordability is per-cast (it depends on the cast card's mana value), so it is checked at
    // validation, not at this generic gate.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.PayLifeEqualToManaValueOfSpell, costHandler: CostHandler) = true

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.PayLifeEqualToManaValueOfSpell): String? {
        val required = lifeToPay(check, cost)
        return rejectUnaffordableLife(check, required) { "Not enough life to pay $required life (its mana value)" }
    }

    override fun lifeToPay(check: SpellCostCheck, cost: AdditionalCost.PayLifeEqualToManaValueOfSpell) =
        check.state.getEntity(check.action.cardId)?.get<CardComponent>()?.manaCost?.cmc ?: 0
}

/** "Exile X cards from your graveyard" — at least [AdditionalCost.ExileVariableCards.minCount]. */
internal object ExileVariableCardsCostKind : SpellCostKind<AdditionalCost.ExileVariableCards> {
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.ExileVariableCards, costHandler: CostHandler): Boolean {
        val zone = ZoneKey(payerId, cost.fromZone.toZone())
        return costHandler.findMatchingCardsUnified(state, state.getZone(zone), cost.filter, payerId).size >= cost.minCount
    }

    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.ExileVariableCards, offer: SpellCostOffer): Boolean {
        val validExileTargets = env.costUtils.findExileTargets(env.state, env.playerId, cost.filter, cost.fromZone.toZone())
        offer.exileTargets = validExileTargets
        offer.exileMinCount = cost.minCount
        return validExileTargets.size >= cost.minCount
    }

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.ExileVariableCards): String? {
        val state = check.state
        val exiled = check.payment?.exiledCards ?: emptyList()
        if (exiled.size < cost.minCount) {
            return "You must exile at least ${cost.minCount} ${cost.filter.description}(s) from your ${cost.fromZone.description}"
        }
        val zoneCards = state.getZone(ZoneKey(check.playerId, cost.fromZone.toZone()))
        val context = PredicateContext(controllerId = check.playerId)
        for (cardId in exiled) {
            if (cardId !in zoneCards) {
                return "Card to exile is not in your ${cost.fromZone.description}"
            }
            if (!check.predicateEvaluator.matches(state, state.projectedState, cardId, cost.filter, context)) {
                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                return "$cardName doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.ExileVariableCards): String? {
        val exiledCards = ledger.payment.exiledCards
        exileFromOwnZone(ledger, exiledCards, cost.fromZone.toZone())
        ledger.exiledCardCount = exiledCards.size
        return null
    }
}

/**
 * "You may sacrifice any number of creatures; this spell costs {2} less for each" (Torgaar). The
 * reduction is part of the total cost (`CastCostTotaller`), priced from the declared sacrifices.
 */
internal object SacrificeForCostReductionCostKind : SpellCostKind<AdditionalCost.SacrificeCreaturesForCostReduction> {
    // Always payable — sacrificing 0 creatures is valid.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.SacrificeCreaturesForCostReduction, costHandler: CostHandler) = true

    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.SacrificeCreaturesForCostReduction, offer: SpellCostOffer): Boolean {
        offer.variableSacrificeTargets = env.costUtils.findVariableSacrificeTargets(env.state, env.playerId, cost.filter)
        offer.variableSacrificeReduction = cost.costReductionPerCreature
        return true
    }

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.SacrificeCreaturesForCostReduction): String? {
        val state = check.state
        val projected = state.projectedState
        for (permId in check.payment?.sacrificedPermanents ?: emptyList()) {
            val permContainer = state.getEntity(permId)
                ?: return "Sacrificed permanent not found: $permId"
            val permCard = permContainer.get<CardComponent>()
                ?: return "Sacrificed entity is not a card: $permId"
            if (projected.getController(permId) != check.playerId) {
                return "You can only sacrifice permanents you control"
            }
            if (permId !in state.getBattlefield()) {
                return "Sacrificed permanent is not on the battlefield: $permId"
            }
            val context = PredicateContext(controllerId = check.playerId)
            if (!check.predicateEvaluator.matches(state, projected, permId, cost.filter, context)) {
                return "${permCard.name} doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.SacrificeCreaturesForCostReduction): String? {
        val sacrificed = ledger.payment.sacrificedPermanents
        ledger.sacrificedSnapshots.addAll(captureEntitySnapshots(sacrificed, ledger.state.projectedState))
        for (permId in sacrificed) {
            if (ledger.state.getEntity(permId) == null) continue
            ledger.sacrifice(permId)
        }
        return null
    }
}

/** Forage as an additional cost to cast (Feed the Cycle's forage mode): exile three cards or sacrifice a Food. */
internal object ForageCostKind : SpellCostKind<AdditionalCost.Forage> {
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.Forage, costHandler: CostHandler) =
        ForageCostResolver.canPay(state, payerId)

    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.Forage, offer: SpellCostOffer) =
        ForageCostResolver.canPay(env.state, env.playerId)

    // Honors the player's mode + card/Food choice, falling back to a legal auto-payment otherwise.
    // The spell is cast from hand here, so it's not in the exile pool — no exclusion needed.
    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.Forage): String? =
        when (val forageResult = ForageCostResolver.pay(
            ledger.zones,
            ledger.state, ledger.playerId,
            exileChoices = ledger.payment.exiledCards,
            sacrificeChoices = ledger.payment.sacrificedPermanents,
        )) {
            is ForageCostResolver.Result.Success -> {
                ledger.state = forageResult.state
                ledger.events.addAll(forageResult.events)
                null
            }
            is ForageCostResolver.Result.Failure -> forageResult.reason
        }
}

/**
 * "Blight N or pay {M}" — the blight leg puts -1/-1 counters on a creature you control; declining it
 * pays the alternative mana instead.
 */
internal object BlightOrPayCostKind : SpellCostKind<AdditionalCost.BlightOrPay> {
    // Always payable: the caster can always decline the blight leg and pay the mana.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.BlightOrPay, costHandler: CostHandler) = true

    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.BlightOrPay, offer: SpellCostOffer): Boolean {
        offer.blightOrPayCost = cost
        offer.blightCreatures = blightableCreatures(env)
        return true
    }

    override fun manaSurcharge(cost: AdditionalCost.BlightOrPay, payment: AdditionalCostPayment?) =
        cost.alternativeManaCost.takeIf { payment?.blightTargets.isNullOrEmpty() }

    // The caster chose blight if blightTargets is non-empty; otherwise they chose the mana, which is
    // validated with the mana payment.
    override fun validate(check: SpellCostCheck, cost: AdditionalCost.BlightOrPay): String? {
        val blightTargets = check.payment?.blightTargets ?: emptyList()
        return if (blightTargets.isNotEmpty()) validateBlightTarget(check, blightTargets.first()) else null
    }

    // With no blight target the caster took the mana path — already folded into the total cost.
    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.BlightOrPay): String? {
        ledger.payment.blightTargets.firstOrNull()?.let { blight(ledger, it, cost.blightAmount) }
        return null
    }
}

/** "Blight X" — put X -1/-1 counters on a creature you control, X chosen by the caster. */
internal object BlightVariableCostKind : SpellCostKind<AdditionalCost.BlightVariable> {
    // X = 0 is always legal (default minCount = 0); higher minCounts require a creature you control
    // whose toughness >= minCount.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.BlightVariable, costHandler: CostHandler): Boolean {
        if (cost.minCount <= 0) return true
        val projected = state.projectedState
        return state.getBattlefield().any { permId ->
            projected.getController(permId) == payerId &&
                projected.isCreature(permId) &&
                (projected.getToughness(permId) ?: 0) >= cost.minCount
        }
    }

    // Surface the creature pool + cap so the client can prompt for X and a creature.
    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.BlightVariable, offer: SpellCostOffer): Boolean {
        val projected = env.state.projectedState
        val ownCreatures = blightableCreatures(env)
        val maxToughness = ownCreatures.maxOfOrNull { projected.getToughness(it) ?: 0 } ?: 0
        offer.blightVariableCost = cost
        offer.blightVariableCreatures = ownCreatures
        offer.blightVariableMaxX = maxToughness
        return maxToughness >= cost.minCount
    }

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.BlightVariable): String? {
        val projected = check.state.projectedState
        val amount = check.payment?.blightAmount ?: 0
        if (amount < cost.minCount) {
            return "Blight X must be at least ${cost.minCount} (got $amount)"
        }
        if (amount < 0) {
            return "Blight X cannot be negative"
        }
        val maxToughness = check.state.getBattlefield()
            .filter { permId ->
                projected.getController(permId) == check.playerId &&
                    projected.isCreature(permId) &&
                    projected.canReceiveCounters(permId)
            }
            .maxOfOrNull { projected.getToughness(it) ?: 0 } ?: 0
        if (amount > maxToughness) {
            return "Blight X ($amount) cannot exceed the greatest toughness among creatures you control ($maxToughness)"
        }
        if (amount > 0) {
            val blightTargets = check.payment?.blightTargets ?: emptyList()
            if (blightTargets.size != 1) {
                return "Blight X must target exactly one creature you control"
            }
            return validateBlightTarget(check, blightTargets.first())
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.BlightVariable): String? {
        val amount = ledger.payment.blightAmount
        if (amount > 0) {
            ledger.payment.blightTargets.firstOrNull()?.let { blight(ledger, it, amount) }
        }
        return null
    }
}

/**
 * "Behold a Dragon" (CR 701.4a) — choose a matching permanent you control or reveal a matching card
 * from your hand. The chosen cards are stored under [AdditionalCost.Behold.storeAs] for a following
 * [AdditionalCost.ExileFromStorage] or the resolving effect.
 */
internal object BeholdCostKind : SpellCostKind<AdditionalCost.Behold> {
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.Behold, costHandler: CostHandler): Boolean {
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = payerId)
        val hasBattlefieldMatch = projected.getBattlefieldControlledBy(payerId).any { permId ->
            costHandler.predicateEvaluator.matches(state, projected, permId, cost.filter, predicateContext)
        }
        val hasHandMatch = state.getHand(payerId).any { cardId ->
            costHandler.predicateEvaluator.matches(state, state.projectedState, cardId, cost.filter, predicateContext)
        }
        return hasBattlefieldMatch || hasHandMatch
    }

    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.Behold, offer: SpellCostOffer): Boolean {
        val allTargets = candidates(env, cost)
        offer.beholdTargets = allTargets
        offer.beholdCount = cost.count
        return allTargets.size >= cost.count
    }

    // Behold spans battlefield (projected — continuous effects matter) *and* hand in one pool; the
    // spell being cast is excluded from its own hand pool.
    override fun candidates(env: SpellCostEnumeration, cost: AdditionalCost.Behold): List<EntityId> {
        val state = env.state
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = env.playerId)
        val evaluator = env.predicateEvaluator
        val battlefieldMatches = projected.getBattlefieldControlledBy(env.playerId).filter { permId ->
            evaluator.matches(state, projected, permId, cost.filter, predicateContext)
        }
        val handMatches = state.getZone(ZoneKey(env.playerId, Zone.HAND))
            .filter { it != env.castCardId }
            .filter { evaluator.matches(state, projected, it, cost.filter, predicateContext) }
        return battlefieldMatches + handMatches
    }

    override fun selectionCount(cost: AdditionalCost.Behold) = cost.count

    override fun present(env: SpellCostEnumeration, cost: AdditionalCost.Behold, candidates: List<EntityId>) =
        "Behold" to AdditionalCostData(
            description = cost.description,
            costType = "Behold",
            validBeholdTargets = candidates,
            beholdCount = cost.count,
        )

    override fun selectionSupplied(cost: AdditionalCost.Behold, payment: AdditionalCostPayment) =
        payment.beheldCards.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.Behold): String? {
        val state = check.state
        val projected = state.projectedState
        val chosen = check.payment?.beheldCards ?: emptyList()
        if (chosen.size < cost.count) {
            return "You must behold ${cost.count} ${cost.filter.description}(s)"
        }
        val handCards = state.getZone(ZoneKey(check.playerId, Zone.HAND))
        val battlefieldCards = state.getBattlefield()
        val context = PredicateContext(controllerId = check.playerId)
        for (cardId in chosen) {
            val inHand = cardId in handCards && cardId != check.action.cardId
            val onBattlefield = cardId in battlefieldCards && projected.getController(cardId) == check.playerId
            if (!inHand && !onBattlefield) {
                return "Beheld card must be a card in your hand or a permanent you control"
            }
            if (!check.predicateEvaluator.matches(state, projected, cardId, cost.filter, context)) {
                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                return "$cardName doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.Behold): String? {
        // Store beheld card IDs in the pipeline for downstream costs/effects
        val chosen = ledger.payment.beheldCards
        ledger.beheldCards.addAll(chosen)
        ledger.costPipelineCollections[cost.storeAs] = chosen

        // Behold reveals the chosen card(s) to all players
        if (chosen.isNotEmpty()) {
            val state = ledger.state
            val cardNames = chosen.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
            val imageUris = chosen.map { id ->
                val defId = state.getEntity(id)?.get<CardComponent>()?.cardDefinitionId
                defId?.let { ledger.cardRegistry.getCard(it)?.metadata?.imageUri }
            }
            val battlefield = state.getBattlefield()
            ledger.events.add(CardsRevealedEvent(
                revealingPlayerId = ledger.playerId,
                cardIds = chosen,
                cardNames = cardNames,
                imageUris = imageUris,
                source = ledger.castCardName,
                // Deliver to the revealing player when the beheld card is on the battlefield
                // (public info) so their client can show the behold pulse. Suppress when revealing
                // from hand — the caster already knows and the reveal overlay would be redundant.
                revealToSelf = chosen.any { it in battlefield }
            ))
        }
        return null
    }
}

/**
 * Exile the cards a preceding cost stored (behold-and-exile). Payability and validation belong to
 * the cost that populated the storage.
 */
internal object ExileFromStorageCostKind : SpellCostKind<AdditionalCost.ExileFromStorage> {
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.ExileFromStorage, costHandler: CostHandler) = true

    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.ExileFromStorage): String? {
        val cardsToExile = ledger.costPipelineCollections[cost.from] ?: emptyList()
        for (cardId in cardsToExile) {
            val cardContainer = ledger.state.getEntity(cardId) ?: continue
            val card = cardContainer.get<CardComponent>() ?: continue

            // Determine source zone (could be battlefield or hand)
            val controllerId = cardContainer.get<ControllerComponent>()?.playerId ?: ledger.playerId
            val ownerId = card.ownerId ?: ledger.playerId
            val sourceZone = if (cardId in ledger.state.getBattlefield()) {
                ZoneKey(controllerId, Zone.BATTLEFIELD)
            } else {
                ZoneKey(ledger.playerId, Zone.HAND)
            }
            val exileZone = ZoneKey(ownerId, Zone.EXILE)

            var state = ledger.state.removeFromZone(sourceZone, cardId)
            val oldObjectRef = state.objectRef(cardId)
            state = state.addToZone(exileZone, cardId)
            // Record the origin zone the way ZoneTransitionService does. This path can exile from the
            // battlefield *or* from hand and (just below) links the result to the spell, so it is the
            // one direct-`addToZone` site that can feed a *linked*-exile pile: without the stamp a
            // later CR 610.3 "return it to its previous zone" would put a hand card onto the
            // battlefield via ToZoneExiledFrom's fallback.
            state = state.updateEntity(cardId) { c -> c.with(ExiledFromZoneComponent(sourceZone.zoneType)) }
            ledger.state = state

            ledger.events.add(ZoneChangeEvent(
                entityId = cardId,
                entityName = card.name,
                fromZone = sourceZone.zoneType,
                toZone = Zone.EXILE,
                ownerId = ownerId,
                oldObject = oldObjectRef,
                newObject = state.objectRef(cardId)
            ))
        }
        // Link exiled cards to the spell entity for leaves-the-battlefield triggers
        if (cost.linkToSource && cardsToExile.isNotEmpty()) {
            ledger.state = ledger.state.updateEntity(ledger.action.cardId) { c ->
                c.with(LinkedExileComponent(exiledIds = cardsToExile))
            }
        }
        return null
    }
}

/**
 * Choose one entity across zones as a cost ("choose a creature card in your graveyard or a creature
 * you control"). Choosing does not change zones.
 */
internal object ChooseEntityCostKind : SpellCostKind<AdditionalCost.ChooseEntity> {
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.ChooseEntity, costHandler: CostHandler) =
        costHandler.findChooseEntityCandidates(state, cost, payerId).isNotEmpty()

    // Search each (zone, filter) pair. Battlefield uses projected state (continuous effects matter);
    // hidden / card zones use base state, mirroring the Behold convention.
    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.ChooseEntity, offer: SpellCostOffer): Boolean {
        val state = env.state
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = env.playerId)
        val evaluator = env.predicateEvaluator
        val allTargets = cost.zoneFilters.flatMap { (zone, filter) ->
            when (zone) {
                Zone.BATTLEFIELD -> projected.getBattlefieldControlledBy(env.playerId)
                    .filter { evaluator.matches(state, projected, it, filter, predicateContext) }
                else -> state.getZone(ZoneKey(env.playerId, zone))
                    .filter { it != env.castCardId } // exclude the spell being cast
                    .filter { evaluator.matches(state, state.projectedState, it, filter, predicateContext) }
            }
        }
        offer.beholdTargets = allTargets
        offer.beholdCount = 1
        return allTargets.isNotEmpty()
    }

    override fun validate(check: SpellCostCheck, cost: AdditionalCost.ChooseEntity): String? {
        val chosen = check.payment?.beheldCards ?: emptyList()
        if (chosen.isEmpty()) {
            return "You must ${cost.description}"
        }
        if (chosen.size > 1) {
            return "You may only choose one entity for: ${cost.description}"
        }
        val entityId = chosen.first()
        if (entityId !in check.costHandler.findChooseEntityCandidates(check.state, cost, check.playerId)) {
            val name = check.state.getEntity(entityId)?.get<CardComponent>()?.name ?: entityId.toString()
            return "$name is not a valid choice for: ${cost.description}"
        }
        return null
    }

    // Record the chosen entity under the shared "chosen-as-additional-cost" storage and in the
    // pipeline under storeAs so the spell effect can reference it. With `captureSnapshot`, freeze a
    // power/toughness/subtype snapshot for battlefield choices so downstream effects can fall back to
    // LKI when the entity leaves between cost-pay and resolution (Rule 113.7a).
    override fun pay(ledger: SpellCostLedger, cost: AdditionalCost.ChooseEntity): String? {
        val chosen = ledger.payment.beheldCards
        if (chosen.isNotEmpty()) {
            ledger.beheldCards.addAll(chosen)
            ledger.costPipelineCollections[cost.storeAs] = chosen
            if (cost.captureSnapshot) {
                val battlefieldChosen = chosen.filter { it in ledger.state.getBattlefield() }
                if (battlefieldChosen.isNotEmpty()) {
                    ledger.chosenEntitySnapshots.addAll(
                        captureEntitySnapshots(battlefieldChosen, ledger.state.projectedState)
                    )
                }
            }
        }
        return null
    }
}

/**
 * Cost-vs-cost ("sacrifice a creature or discard a card"). [SpellCosts.reduceAlternatives] replaces
 * it with the option paid before validation and payment, so only the enumeration stages see it.
 */
internal object ChoiceCostKind : SpellCostKind<AdditionalCost.Choice> {
    // Payable if at least one option can be paid.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.Choice, costHandler: CostHandler) =
        cost.options.any { SpellCosts.canPay(state, it, payerId, costHandler) }

    // Castable only if at least one option is payable. The per-option legal actions are produced in
    // the enumerator's post-processing.
    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.Choice, offer: SpellCostOffer) =
        ChoiceCostResolver.costInfos(env.state, env.playerId, cost, env.costUtils, env.castCardId).isNotEmpty()
}

/**
 * Cost-vs-mana ("sacrifice a creature or pay {3}"). [SpellCosts.reduceAlternatives] replaces it with
 * its leg cost when the caster paid the leg, and drops it otherwise, so validation and payment never
 * see it; the declined leg's mana is its [manaSurcharge].
 */
internal object OrPayCostKind : SpellCostKind<AdditionalCost.OrPay> {
    // Always payable, because the caster can always decline the non-mana leg and fold the
    // alternative mana into the spell's cost instead. Whether the leg is *available* only decides
    // which cast paths the enumerator offers.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.OrPay, costHandler: CostHandler) = true

    // Surface the candidates for the leg path, whichever cost it carries.
    override fun enumerate(env: SpellCostEnumeration, cost: AdditionalCost.OrPay, offer: SpellCostOffer): Boolean {
        offer.orPayCost = cost
        offer.orPayTargets = SpellCosts.candidates(env, cost.cost)
        return true
    }

    override fun manaSurcharge(cost: AdditionalCost.OrPay, payment: AdditionalCostPayment?) =
        cost.alternativeManaCost.takeUnless { SpellCosts.selectionSupplied(cost.cost, payment) }
}

/** Several costs paid together (behold-and-exile). Flattened before every stage but affordability. */
internal object CompositeCostKind : SpellCostKind<AdditionalCost.Composite> {
    // All steps must be payable.
    override fun canPay(state: GameState, payerId: EntityId, cost: AdditionalCost.Composite, costHandler: CostHandler) =
        cost.steps.all { SpellCosts.canPay(state, it, payerId, costHandler) }
}
