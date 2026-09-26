package com.wingedsheep.engine.mechanics.cost.spell

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.handlers.costs.CostAtomAmounts
import com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/*
 * Kinds for the shared cost atoms ([CostAtom]) when an [AdditionalCost.Atom] carries them as a
 * spell's additional cost. The same atoms are paid in other contexts (activated abilities, "unless
 * you pay") by `CostHandler` and `CostPaymentService`; these objects are only the casting-context
 * half.
 */

/** The phrase an additional-cost atom leads its clause with ("Sacrifice a Goblin"). */
private val CostAtom.leadingDescription: String get() = description.replaceFirstChar { it.uppercase() }

/** The caster's hand minus the spell being cast, narrowed to [filter]. */
internal fun handCandidates(env: SpellCostEnumeration, filter: GameObjectFilter): List<EntityId> {
    val handCards = env.state.getZone(ZoneKey(env.playerId, Zone.HAND)).filter { it != env.castCardId }
    if (filter == GameObjectFilter.Any) return handCards
    val predicateContext = PredicateContext(controllerId = env.playerId)
    return handCards.filter {
        env.predicateEvaluator.matches(env.state, env.state.projectedState, it, filter, predicateContext)
    }
}

/**
 * Moves each of [cardIds] from the payer's [fromZone] to their exile, stamping the origin zone the
 * way `ZoneTransitionService` does so a later "return it to its previous zone" knows where it was.
 */
internal fun exileFromOwnZone(ledger: SpellCostLedger, cardIds: List<EntityId>, fromZone: Zone) {
    for (cardId in cardIds) {
        val card = ledger.state.getEntity(cardId)?.get<CardComponent>() ?: continue
        val sourceZone = ZoneKey(ledger.playerId, fromZone)
        val exileZone = ZoneKey(ledger.playerId, Zone.EXILE)

        var state = ledger.state.removeFromZone(sourceZone, cardId)
        val oldObjectRef = state.objectRef(cardId)
        state = state.addToZone(exileZone, cardId)
        state = state.updateEntity(cardId) { c -> c.with(ExiledFromZoneComponent(fromZone)) }
        ledger.state = state

        ledger.events.add(ZoneChangeEvent(
            entityId = cardId,
            entityName = card.name,
            fromZone = fromZone,
            toZone = Zone.EXILE,
            ownerId = ledger.playerId,
            oldObject = oldObjectRef,
            newObject = state.objectRef(cardId)
        ))
    }
}

/** "Sacrifice a creature" (Natural Order). */
internal object SacrificeCostKind : SpellCostKind<CostAtom.Sacrifice> {
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.Sacrifice, costHandler: CostHandler) =
        costHandler.findMatchingPermanentsUnified(state, payerId, cost.filter).size >= cost.count

    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.Sacrifice, offer: SpellCostOffer): Boolean {
        val validSacTargets = candidates(env, cost)
        offer.sacrificeTargets.addAll(validSacTargets)
        return validSacTargets.size >= cost.count
    }

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.Sacrifice) =
        env.costUtils.findSacrificeTargets(env.state, env.playerId, cost)

    override fun selectionCount(cost: CostAtom.Sacrifice) = cost.selectionCount

    override fun present(env: SpellCostEnumeration, cost: CostAtom.Sacrifice, candidates: List<EntityId>) =
        "Sacrifice" to AdditionalCostData(
            description = cost.leadingDescription,
            costType = "SacrificePermanent",
            validSacrificeTargets = candidates,
            sacrificeCount = cost.count,
        )

    override fun selectionSupplied(cost: CostAtom.Sacrifice, payment: AdditionalCostPayment) =
        payment.sacrificedPermanents.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: CostAtom.Sacrifice): String? {
        val state = check.state
        val projected = state.projectedState
        val sacrificed = check.payment?.sacrificedPermanents ?: emptyList()
        val filterDesc = cost.filter.description
        if (sacrificed.size < cost.count) {
            return "You must sacrifice ${cost.count} $filterDesc to cast this spell"
        }
        for (permId in sacrificed) {
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
            // Use unified filter with projected state
            val context = PredicateContext(controllerId = check.playerId)
            if (!check.predicateEvaluator.matches(state, projected, permId, cost.filter, context)) {
                return "${permCard.name} doesn't match the required filter: $filterDesc"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.Sacrifice): String? {
        // Snapshot projected subtypes and P/T before zone change
        // (Rule 113.7a / 608.2h — "as it last existed on the battlefield")
        val sacrificed = ledger.payment.sacrificedPermanents
        ledger.sacrificedSnapshots.addAll(captureEntitySnapshots(sacrificed, ledger.state.projectedState))
        for (permId in sacrificed) {
            if (ledger.state.getEntity(permId) == null) continue
            ledger.sacrifice(permId)
        }
        return null
    }
}

/** "Sacrifice all creatures you control" (Soulblast) — every matching permanent, nothing chosen. */
internal object SacrificeAllCostKind : SpellCostKind<CostAtom.SacrificeAll> {
    // Controlling none of them sacrifices nothing, so this is always payable (CR 118.3).
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.SacrificeAll, costHandler: CostHandler) = true

    // The caster picks nothing, so the client submits no payment for it.
    override fun paysUnprompted(cost: CostAtom.SacrificeAll) = true

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.SacrificeAll): String? {
        val all = ledger.costHandler.sacrificeAllCandidates(ledger.state, cost, ledger.playerId)
        // Snapshot before any of them leaves (CR 608.2h) — "the sacrificed creatures' total power".
        ledger.sacrificedSnapshots.addAll(captureEntitySnapshots(all, ledger.state.projectedState))
        for (permId in all) ledger.sacrifice(permId)
        return null
    }
}

/** "Discard a card" (Force of Will). */
internal object DiscardCostKind : SpellCostKind<CostAtom.Discard> {
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.Discard, costHandler: CostHandler) =
        costHandler.findMatchingCardsUnified(state, state.getZone(ZoneKey(payerId, Zone.HAND)), cost.filter, payerId)
            .size >= cost.count

    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.Discard, offer: SpellCostOffer): Boolean {
        val validDiscards = candidates(env, cost)
        offer.discardTargets = validDiscards
        offer.discardCount = cost.count
        return validDiscards.size >= cost.count
    }

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.Discard) = handCandidates(env, cost.filter)

    override fun selectionCount(cost: CostAtom.Discard) = cost.selectionCount

    override fun present(env: SpellCostEnumeration, cost: CostAtom.Discard, candidates: List<EntityId>) =
        "Discard" to AdditionalCostData(
            description = cost.leadingDescription,
            costType = "DiscardCard",
            validDiscardTargets = candidates,
            discardCount = cost.count,
        )

    override fun selectionSupplied(cost: CostAtom.Discard, payment: AdditionalCostPayment) =
        payment.discardedCards.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: CostAtom.Discard): String? {
        val state = check.state
        val discarded = check.payment?.discardedCards ?: emptyList()
        if (discarded.size < cost.count) {
            return "You must discard ${cost.count} card(s) to cast this spell"
        }
        val handCards = state.getZone(ZoneKey(check.playerId, Zone.HAND))
        val context = PredicateContext(controllerId = check.playerId)
        for (cardId in discarded) {
            if (cardId !in handCards) {
                return "Card to discard is not in your hand"
            }
            if (cardId == check.action.cardId) {
                return "Cannot discard the spell being cast"
            }
            if (cost.filter != GameObjectFilter.Any &&
                !check.predicateEvaluator.matches(state, state.projectedState, cardId, cost.filter, context)
            ) {
                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                return "$cardName doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.Discard): String? {
        val discardedCards = ledger.payment.discardedCards
        ledger.discardedAsCostCards.addAll(discardedCards)
        // Through the shared discard path so a card-intrinsic discard replacement (madness,
        // CR 702.35a) applies to a card discarded as an additional cost of casting a spell.
        val discardResult = ledger.zones.discardCards(ledger.state, ledger.playerId, discardedCards)
        ledger.state = discardResult.state
        ledger.events.addAll(discardResult.events)
        return null
    }
}

/** "Discard your hand." The payer selects nothing — every card goes. */
internal object DiscardHandCostKind : SpellCostKind<CostAtom.DiscardHand> {
    // An empty hand discards nothing, so this is always payable (CR 118.3).
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.DiscardHand, costHandler: CostHandler) = true

    // Every card goes, so the caster picks nothing and the client submits no payment for it.
    override fun paysUnprompted(cost: CostAtom.DiscardHand) = true

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.DiscardHand): String? {
        // Every card at once, through the same shared discard path as the counted variant, so
        // madness (CR 702.35a) applies to each of them.
        val hand = ledger.state.getZone(ZoneKey(ledger.playerId, Zone.HAND)).toList()
        if (hand.isNotEmpty()) {
            ledger.discardedAsCostCards.addAll(hand)
            val discardResult = ledger.zones.discardCards(ledger.state, ledger.playerId, hand)
            ledger.state = discardResult.state
            ledger.events.addAll(discardResult.events)
        }
        return null
    }
}

/** "Exile a creature card from your graveyard" — exile [CostAtom.ExileFrom.count] cards from a zone. */
internal object ExileFromCostKind : SpellCostKind<CostAtom.ExileFrom> {
    // A spell's additional cost has no source permanent, so `excludeSelf` has nothing to exclude here.
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.ExileFrom, costHandler: CostHandler): Boolean {
        val perZone = costHandler.exileCandidatesByOwner(state, cost, payerId, sourceId = null).values.map { it.size }
        return if (cost.singleZone) perZone.any { it >= cost.count } else perZone.sum() >= cost.count
    }

    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.ExileFrom, offer: SpellCostOffer): Boolean {
        val validExileTargets = env.costUtils.findExileTargets(
            env.state, env.playerId, cost.filter, cost.zone,
            cost.anyPlayersZone, cost.singleZone, cost.count,
        )
        offer.exileTargets = validExileTargets
        offer.exileMinCount = cost.count
        return validExileTargets.size >= cost.count
    }

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.ExileFrom) =
        env.costUtils.findExileTargets(env.state, env.playerId, cost.filter, cost.zone)
            .filter { it != env.castCardId }

    override fun selectionCount(cost: CostAtom.ExileFrom) = cost.selectionCount

    // Exile costs identify their source zone so the client opens the matching picker instead of
    // assuming every exile payment comes from the graveyard.
    override fun present(env: SpellCostEnumeration, cost: CostAtom.ExileFrom, candidates: List<EntityId>): Pair<String, AdditionalCostData>? {
        val (label, costType) = when (cost.zone) {
            Zone.GRAVEYARD -> "Exile from graveyard" to "ExileFromGraveyard"
            Zone.HAND -> "Exile from hand" to "ExileFromHand"
            else -> return null
        }
        return label to AdditionalCostData(
            description = cost.leadingDescription,
            costType = costType,
            validExileTargets = candidates,
            exileMinCount = cost.count,
            exileMaxCount = cost.count,
        )
    }

    override fun selectionSupplied(cost: CostAtom.ExileFrom, payment: AdditionalCostPayment) =
        payment.exiledCards.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: CostAtom.ExileFrom): String? {
        val state = check.state
        val exiled = check.payment?.exiledCards ?: emptyList()
        val zoneDesc = cost.zone.name.lowercase()
        if (exiled.size < cost.count) {
            return "You must exile ${cost.count} ${cost.filter.description}(s) from your $zoneDesc"
        }
        val zoneCards = state.getZone(ZoneKey(check.playerId, cost.zone))
        val context = PredicateContext(controllerId = check.playerId)
        for (cardId in exiled) {
            if (cardId !in zoneCards) {
                return "Card to exile is not in your $zoneDesc"
            }
            if (!check.predicateEvaluator.matches(state, state.projectedState, cardId, cost.filter, context)) {
                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                return "$cardName doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.ExileFrom): String? {
        val exiledCards = ledger.payment.exiledCards
        ledger.exiledAsCostCards.addAll(exiledCards)
        // Rule 113.7a — freeze what a permanent last was while it is still on the battlefield; a
        // token won't be readable at all by resolution.
        if (cost.zone == Zone.BATTLEFIELD) {
            ledger.exiledAsCostSnapshots.addAll(captureEntitySnapshots(exiledCards, ledger.state))
        }
        exileFromOwnZone(ledger, exiledCards, cost.zone)
        ledger.exiledCardCount = exiledCards.size
        return null
    }
}

/**
 * Collect evidence N as a *mandatory* cast cost (CR 701.59a, Urgent Necropsy). A *sum* gate, so the
 * count of cards is irrelevant; [CollectEvidenceResolver] owns the legality rule so enumeration,
 * validation and payment can't drift.
 */
internal object CollectEvidenceCostKind : SpellCostKind<CostAtom.CollectEvidence> {
    // CR 701.59b — an optional collect-evidence cast cost that can't be reached simply isn't
    // offered as a second cast action.
    //
    // A *target-derived* threshold has no price yet: this check runs before the caster announces
    // targets (CR 601.2c), and the cost isn't determined until 601.2f. It therefore withholds
    // judgement rather than guessing — the cast is offered, and an unreachable one is caught when
    // the submitted targets are validated, which is exactly the 601.2e/733 rewind the printed
    // ruling describes.
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.CollectEvidence, costHandler: CostHandler) =
        CostAtomAmounts.dependsOnTargets(cost.amount) ||
            CollectEvidenceResolver.canCollect(state, payerId, CostAtomAmounts.evaluate(state, cost.amount), predicateEvaluator = costHandler.predicateEvaluator)

    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.CollectEvidence, offer: SpellCostOffer): Boolean {
        offer.collectEvidenceCost = cost
        // CR 701.59b fails closed on a threshold that is already known: a graveyard that can't
        // reach it means the spell can't be cast at all. A *target-derived* threshold isn't known
        // yet — targets are announced at CR 601.2c and the cost isn't determined until 601.2f — so
        // the cast is offered and an unreachable choice of targets is rejected at validation, which
        // is the 601.2e rewind the printed ruling describes.
        return CostAtomAmounts.dependsOnTargets(cost.amount) ||
            CollectEvidenceResolver.canCollect(
                env.state, env.playerId,
                CostAtomAmounts.evaluate(env.state, cost.amount),
                excludeCardId = env.castCardId,
                predicateEvaluator = env.predicateEvaluator
            )
    }

    // The sum-gated graveyard costs: the pool is the whole graveyard and the binding constraint is
    // a summed measure, not a count — so `selectionCount` can't express it and `canPayFrom`
    // consults the resolver instead of counting candidates.
    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.CollectEvidence) =
        CollectEvidenceResolver.candidates(env.state, env.playerId, excludeCardId = env.castCardId, predicateEvaluator = env.predicateEvaluator).cards

    override fun selectionCount(cost: CostAtom.CollectEvidence) = cost.selectionCount

    override fun canPayFrom(env: SpellCostEnumeration, cost: CostAtom.CollectEvidence, candidates: List<EntityId>) =
        CollectEvidenceResolver.canCollect(
            env.state, env.playerId,
            CostAtomAmounts.evaluate(env.state, cost.amount),
            excludeCardId = env.castCardId,
            predicateEvaluator = env.predicateEvaluator
        )

    // Collect evidence names its amount, because the amount *is* the choice — "Collect evidence
    // 10" reads the way the card is printed where a bare "Collect evidence" would not (CR 701.59).
    override fun present(env: SpellCostEnumeration, cost: CostAtom.CollectEvidence, candidates: List<EntityId>): Pair<String, AdditionalCostData>? {
        val info = CollectEvidenceResolver.costInfo(
            env.state, env.playerId,
            CostAtomAmounts.evaluate(env.state, cost.amount),
            excludeCardId = env.castCardId,
            predicateEvaluator = env.predicateEvaluator,
        ) ?: return null
        // This rail is an *alternative* cast cost (Conspiracy Unraveler), enumerated before any
        // target is announced, so a target-derived threshold would price at 0 here. Nothing prints
        // one on this rail; the atom's own description is used so that if something ever does, the
        // label reads "Collect evidence X" rather than a fabricated number.
        return cost.leadingDescription to info
    }

    override fun selectionSupplied(cost: CostAtom.CollectEvidence, payment: AdditionalCostPayment) =
        payment.exiledCards.isNotEmpty()

    // A GameAction is client-supplied: never trust the submitted selection.
    override fun validate(check: SpellCostCheck, cost: CostAtom.CollectEvidence): String? {
        val exiled = check.payment?.exiledCards ?: emptyList()
        // CR 601.2f — the threshold is determined here, from the targets announced at 601.2c, and
        // then locked in. Urgent Necropsy's ruling spells the consequence out: if the graveyard
        // can't reach it, the caster can't choose to collect evidence at all, so this rejection
        // *is* the 601.2e illegal-cast rewind rather than a discount.
        val required = CostAtomAmounts.evaluate(check.state, cost.amount, check.action.xValue, check.action.targets)
        if (!CollectEvidenceResolver.isLegalSelection(check.state, check.playerId, required, exiled, predicateEvaluator = check.predicateEvaluator)) {
            return "You must exile cards with total mana value $required or " +
                "greater from your graveyard to collect evidence $required"
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.CollectEvidence): String? {
        val collected = CollectEvidenceResolver.collect(
            ledger.zones,
            state = ledger.state,
            playerId = ledger.playerId,
            // Priced from the same targets validate read, so the payment can't drift from the
            // check that allowed it.
            amount = CostAtomAmounts.evaluate(ledger.state, cost.amount, ledger.action.xValue, ledger.action.targets),
            chosenCards = ledger.payment.exiledCards,
            sourceName = ledger.cardDefinitionName ?: "Collect evidence",
        )
        if (collected is CollectEvidenceResolver.Result.Success) {
            ledger.state = collected.state
            ledger.events.addAll(collected.events)
        }
        return null
    }
}

/**
 * Exile cards from your graveyard with a summed measure — the filtered generalization behind collect
 * evidence. An activated-ability cost: it only reaches the casting context through an alternative
 * cost's presentation rail.
 */
internal object ExileFromGraveyardForTotalCostKind : SpellCostKind<CostAtom.ExileFromGraveyardForTotal> {
    // Not payable as a *spell's* additional cost today: nothing offers this atom in a cast context,
    // and the cast-time picker has no sum-gated exile mode to raise, so an unreachable one would be
    // offered and then fail at payment. Fails closed until a printed card needs it, matching the
    // "prefer absent to unpayable" rule collect evidence follows.
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.ExileFromGraveyardForTotal, costHandler: CostHandler) = false

    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.ExileFromGraveyardForTotal, offer: SpellCostOffer) = true

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.ExileFromGraveyardForTotal) =
        GraveyardTotalExileResolver
            .candidates(env.state, env.playerId, cost.measure, cost.filter, excludeCardId = env.castCardId, predicateEvaluator = env.predicateEvaluator).cards

    override fun selectionCount(cost: CostAtom.ExileFromGraveyardForTotal) = cost.selectionCount

    override fun canPayFrom(env: SpellCostEnumeration, cost: CostAtom.ExileFromGraveyardForTotal, candidates: List<EntityId>) =
        GraveyardTotalExileResolver
            .canPay(env.state, env.playerId, cost.measure, cost.minTotal, cost.filter, excludeCardId = env.castCardId, predicateEvaluator = env.predicateEvaluator)

    override fun present(env: SpellCostEnumeration, cost: CostAtom.ExileFromGraveyardForTotal, candidates: List<EntityId>): Pair<String, AdditionalCostData>? {
        val info = GraveyardTotalExileResolver.costInfo(env.state, env.playerId, cost, excludeCardId = env.castCardId, predicateEvaluator = env.predicateEvaluator)
            ?: return null
        return "Exile from graveyard" to info
    }

    // Never offered as a spell's additional cost (see canPay), so no payment can satisfy it here.
    override fun selectionSupplied(cost: CostAtom.ExileFromGraveyardForTotal, payment: AdditionalCostPayment) = false
}

/** "Tap an untapped artifact you control" (Zahid, Guardian of the Great Door). */
internal object TapPermanentsCostKind : SpellCostKind<CostAtom.TapPermanents> {
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.TapPermanents, costHandler: CostHandler) =
        costHandler.findUntappedMatchingPermanentsUnified(state, payerId, cost.filter).size >= cost.count

    // Mirrors ReturnToHand's selection model — permanents you control, chosen by the caster — but
    // the payment taps instead of bouncing.
    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.TapPermanents, offer: SpellCostOffer): Boolean {
        val validTapTargets = env.costUtils.findAbilityTapTargets(env.state, env.playerId, cost.filter)
            .let { if (cost.excludeSelf) it.filter { id -> id != env.castCardId } else it }
        offer.tapTargets = validTapTargets
        offer.tapCount = cost.count
        return validTapTargets.size >= cost.count
    }

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.TapPermanents) =
        env.costUtils.findAbilityTapTargets(
            env.state, env.playerId, cost.filter, if (cost.excludeSelf) env.castCardId else null
        )

    override fun selectionCount(cost: CostAtom.TapPermanents) = cost.selectionCount

    override fun present(env: SpellCostEnumeration, cost: CostAtom.TapPermanents, candidates: List<EntityId>) =
        "Tap" to AdditionalCostData(
            description = cost.leadingDescription,
            costType = "TapPermanents",
            validTapTargets = candidates,
            tapCount = cost.count,
        )

    override fun selectionSupplied(cost: CostAtom.TapPermanents, payment: AdditionalCostPayment) =
        payment.tappedPermanents.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: CostAtom.TapPermanents): String? {
        val state = check.state
        val projected = state.projectedState
        val tapped = check.payment?.tappedPermanents ?: emptyList()
        if (tapped.size < cost.count) {
            return "You must tap ${cost.count} ${cost.filter.description}(s) to cast this spell"
        }
        val context = PredicateContext(controllerId = check.playerId)
        for (permId in tapped) {
            val permContainer = state.getEntity(permId)
                ?: return "Tapped permanent not found: $permId"
            val permCard = permContainer.get<CardComponent>()
                ?: return "Tapped entity is not a card: $permId"
            if (projected.getController(permId) != check.playerId) {
                return "You can only tap permanents you control"
            }
            if (permContainer.has<TappedComponent>()) {
                return "${permCard.name} is already tapped"
            }
            if (permId !in state.getBattlefield()) {
                return "Tapped permanent is not on the battlefield: $permId"
            }
            if (!check.predicateEvaluator.matches(state, projected, permId, cost.filter, context)) {
                return "${permCard.name} doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.TapPermanents): String? {
        for (permId in ledger.payment.tappedPermanents) {
            val (tappedState, tapEvent) = tap(ledger.state, permId)
            ledger.state = tappedState
            tapEvent?.let(ledger.events::add)
        }
        return null
    }
}

/** "Return a permanent you control to its owner's hand" (Fear of Isolation). */
internal object ReturnToHandCostKind : SpellCostKind<CostAtom.ReturnToHand> {
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.ReturnToHand, costHandler: CostHandler): Boolean {
        val battlefield = state.getBattlefield()
        val pool = if (cost.youControl) battlefield.filter { state.projectedState.getController(it) == payerId } else battlefield
        return costHandler.findMatchingCardsUnified(state, pool, cost.filter, payerId).size >= cost.count
    }

    // "As an additional cost to cast this spell, return [count] permanent(s) matching [filter] you
    // control to its owner's hand." Permanents you control, no destruction.
    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.ReturnToHand, offer: SpellCostOffer): Boolean {
        val validBounceTargets = candidates(env, cost)
        offer.bounceTargets = validBounceTargets
        offer.bounceCount = cost.count
        return validBounceTargets.size >= cost.count
    }

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.ReturnToHand) =
        env.costUtils.findAbilityBounceTargets(env.state, env.playerId, cost.filter, cost.youControl)

    override fun selectionCount(cost: CostAtom.ReturnToHand) = cost.selectionCount

    override fun present(env: SpellCostEnumeration, cost: CostAtom.ReturnToHand, candidates: List<EntityId>) =
        "Return to hand" to AdditionalCostData(
            description = cost.leadingDescription,
            costType = "BouncePermanent",
            validBounceTargets = candidates,
            bounceCount = cost.count,
        )

    override fun selectionSupplied(cost: CostAtom.ReturnToHand, payment: AdditionalCostPayment) =
        payment.bouncedPermanents.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: CostAtom.ReturnToHand): String? {
        val state = check.state
        val projected = state.projectedState
        val bounced = check.payment?.bouncedPermanents ?: emptyList()
        if (bounced.size < cost.count) {
            return "You must return ${cost.count} ${cost.filter.description}(s) you control to its owner's hand to cast this spell"
        }
        val context = PredicateContext(controllerId = check.playerId)
        for (permId in bounced) {
            val permContainer = state.getEntity(permId)
                ?: return "Returned permanent not found: $permId"
            val permCard = permContainer.get<CardComponent>()
                ?: return "Returned entity is not a card: $permId"
            if (projected.getController(permId) != check.playerId) {
                return "You can only return permanents you control"
            }
            if (permId !in state.getBattlefield()) {
                return "Returned permanent is not on the battlefield: $permId"
            }
            if (!check.predicateEvaluator.matches(state, projected, permId, cost.filter, context)) {
                return "${permCard.name} doesn't match the required filter: ${cost.filter.description}"
            }
        }
        return null
    }

    // ZoneTransitionService.moveToZone handles attached auras/equipment and tokens ceasing to exist.
    override fun pay(ledger: SpellCostLedger, cost: CostAtom.ReturnToHand): String? {
        for (permId in ledger.payment.bouncedPermanents) {
            val tr = ledger.zones.moveToZone(ledger.state, permId, Zone.HAND)
            ledger.state = tr.state
            ledger.events.addAll(tr.events)
        }
        return null
    }
}

/**
 * A variable-count permanent cost — Teamwork N's "tap any number of creatures you control with
 * total power N or more" (CR 702.194a). Enumerated on the optional-cost rail, not here.
 */
internal object VariablePermanentsCostKind : SpellCostKind<CostAtom.VariablePermanents> {
    // Payable when the payer has enough candidates to clear both floors. No `sourceId` is passed —
    // a *spell's* additional cost has no source permanent on the battlefield to exclude (teamwork
    // sets `excludeSelf = false` anyway).
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.VariablePermanents, costHandler: CostHandler) =
        VariablePermanentsCost.canPay(state, payerId, cost, predicateEvaluator = costHandler.predicateEvaluator)

    override fun selectionSupplied(cost: CostAtom.VariablePermanents, payment: AdditionalCostPayment) =
        payment.variableCostPermanents.isNotEmpty()

    override fun validate(check: SpellCostCheck, cost: CostAtom.VariablePermanents): String? {
        val state = check.state
        val projected = state.projectedState
        val chosen = check.payment?.variableCostPermanents ?: emptyList()
        val verb = VariablePermanentsCost.verb(cost.action)
        if (chosen.size != chosen.distinct().size) {
            return "The same permanent can't be chosen twice to $verb for this spell"
        }
        if (chosen.size < cost.minCount) {
            return "You must $verb at least ${cost.minCount} ${cost.filter.description}(s) to cast this spell"
        }
        val context = PredicateContext(controllerId = check.playerId)
        for (permId in chosen) {
            val permContainer = state.getEntity(permId)
                ?: return "Permanent to $verb not found: $permId"
            val permCard = permContainer.get<CardComponent>()
                ?: return "Entity to $verb is not a card: $permId"
            if (permId !in state.getBattlefield()) {
                return "Permanent to $verb is not on the battlefield: $permId"
            }
            if (projected.getController(permId) != check.playerId) {
                return "You can only $verb permanents you control"
            }
            // CR 701.26a — only untapped permanents can be tapped. Summoning sickness (CR 302.6)
            // governs the {T} symbol, not a tap paid as a cost, so a creature that entered this
            // turn may still pay (as with crew, CR 702.122b).
            if (cost.action == PermanentCostAction.TAP && permContainer.has<TappedComponent>()) {
                return "${permCard.name} is already tapped"
            }
            if (!check.predicateEvaluator.matches(state, projected, permId, cost.filter, context)) {
                return "${permCard.name} doesn't match the required filter: ${cost.filter.description}"
            }
        }
        // The measure floor — Teamwork N's "with total power N or more" (CR 702.194a), summed from
        // projected power so a lord bonus counts.
        if (cost.minMeasure > 0) {
            val measured = VariablePermanentsCost.measure(state, cost.xMeasure, chosen)
            if (measured < cost.minMeasure) {
                return "The permanents you chose have ${VariablePermanentsCost.measureName(cost.xMeasure)} " +
                    "$measured; ${cost.minMeasure} or more is required"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.VariablePermanents): String? {
        // Validation already re-checked control, filter, and the measure floor.
        val chosen = ledger.payment.variableCostPermanents
        when (cost.action) {
            PermanentCostAction.TAP -> {
                // The tap carries its *cause* ([TapReason]), which is what lets "whenever this
                // becomes tapped to pay a teamwork cost" (Agent Maria Hill) tell a teamwork tap apart
                // from an attack, crew, or mana tap — all of which are also performed by the
                // creature's own controller, so `tappedById` can't separate them. The cause comes
                // from the *declared cast-choice slot*, not from the atom: `VariablePermanents(TAP)`
                // is a generic atom any mechanic may reuse, and it is teamwork's declaration
                // (CR 601.2b / 702.194a) that makes this a teamwork tap. Stamped only on the cost the
                // declared optional ability actually contributed, so a card's own printed tap cost
                // isn't relabelled by an unrelated declaration. Tapping itself goes through
                // [VariablePermanentsCost.tapAll] — the single tap site for this atom, shared with
                // the activated-ability payer in `CostHandler`.
                val reason = if (AdditionalCost.Atom(cost) in ledger.declaredSlotCosts) {
                    TapReason.forChoiceSlot(ledger.action.declaredCostSlot)
                } else {
                    TapReason.UNSPECIFIED
                }
                val (tappedState, tapEvents) = VariablePermanentsCost.tapAll(ledger.state, chosen, reason)
                ledger.state = tappedState
                ledger.events.addAll(tapEvents)
            }
            PermanentCostAction.SACRIFICE -> {
                ledger.sacrificedSnapshots.addAll(captureEntitySnapshots(chosen, ledger.state.projectedState))
                for (permId in chosen) {
                    if (ledger.state.getEntity(permId) == null) continue
                    ledger.sacrifice(permId)
                }
            }
            PermanentCostAction.EXILE -> for (permId in chosen) {
                val tr = ledger.zones.moveToZone(ledger.state, permId, Zone.EXILE)
                ledger.state = tr.state
                ledger.events.addAll(tr.events)
            }
        }
        return null
    }
}

/**
 * "Reveal an Elf card from your hand" (Wren's Run Vanquisher). The cards stay in hand (CR 701.20b):
 * the caster picks which to publish and nothing moves.
 */
internal object RevealFromHandCostKind : SpellCostKind<CostAtom.RevealFromHand> {
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.RevealFromHand, costHandler: CostHandler) =
        costHandler.findMatchingCardsUnified(state, state.getZone(ZoneKey(payerId, Zone.HAND)), cost.filter, payerId)
            .size >= cost.count

    // The spell itself is on its way to the stack, so it is excluded from its own candidate pool.
    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.RevealFromHand, offer: SpellCostOffer): Boolean {
        val validReveals = candidates(env, cost)
        offer.revealTargets = validReveals
        offer.revealCount = cost.count
        return validReveals.size >= cost.count
    }

    override fun candidates(env: SpellCostEnumeration, cost: CostAtom.RevealFromHand) = handCandidates(env, cost.filter)

    override fun selectionCount(cost: CostAtom.RevealFromHand) = cost.selectionCount

    // Its own pool/count fields rather than behold's: behold also offers battlefield permanents
    // (CR 701.4a), which can never pay this.
    override fun present(env: SpellCostEnumeration, cost: CostAtom.RevealFromHand, candidates: List<EntityId>) =
        "Reveal" to AdditionalCostData(
            description = cost.leadingDescription,
            costType = "RevealCard",
            validRevealTargets = candidates,
            revealCount = cost.count,
        )

    override fun selectionSupplied(cost: CostAtom.RevealFromHand, payment: AdditionalCostPayment) =
        payment.revealedCards.isNotEmpty()

    // The chosen cards must be in the caster's hand and match the filter. They stay there, so this
    // validates a selection rather than a zone change.
    override fun validate(check: SpellCostCheck, cost: CostAtom.RevealFromHand): String? {
        val state = check.state
        val revealed = check.payment?.revealedCards ?: emptyList()
        val filterDesc = cost.filter.description
        if (revealed.size < cost.count) {
            return "You must reveal ${cost.count} $filterDesc from your hand to cast this spell"
        }
        val hand = state.getZone(ZoneKey(check.playerId, Zone.HAND))
        val revealContext = PredicateContext(controllerId = check.playerId)
        for (revealedId in revealed) {
            val card = state.getEntity(revealedId)?.get<CardComponent>()
                ?: return "Revealed card not found: $revealedId"
            if (revealedId !in hand) {
                return "${card.name} is not in your hand"
            }
            if (!check.predicateEvaluator.matches(state, state.projectedState, revealedId, cost.filter, revealContext)) {
                return "${card.name} doesn't match the required filter: $filterDesc"
            }
        }
        return null
    }

    // Revealing publishes the cards and moves nothing, so paying is the event alone — the cards
    // stay in hand and are still castable later.
    override fun pay(ledger: SpellCostLedger, cost: CostAtom.RevealFromHand): String? {
        val revealed = ledger.payment.revealedCards
        if (revealed.isNotEmpty()) {
            ledger.events.add(
                CardsRevealedEvent(
                    revealingPlayerId = ledger.playerId,
                    cardIds = revealed,
                    cardNames = revealed.map { ledger.state.getEntity(it)?.get<CardComponent>()?.name ?: "Unknown" },
                    source = ledger.state.getEntity(ledger.action.cardId)?.get<CardComponent>()?.name,
                )
            )
        }
        return null
    }
}

/**
 * "Remove three counters from among creatures you control" (Dawnhand Dissident's granted cost). The
 * client sends the typed per-entity, per-counter-type removals
 * ([AdditionalCostPayment.distributedCounterRemovals]), so the player explicitly picks which counter
 * types come off each creature.
 */
internal object RemoveCountersCostKind : SpellCostKind<CostAtom.RemoveCounters> {
    private fun fixedCount(cost: CostAtom.RemoveCounters): Int = (cost.count as? DynamicAmount.Fixed)?.amount ?: 0

    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.RemoveCounters, costHandler: CostHandler): Boolean {
        val needed = fixedCount(cost)
        if (needed <= 0) return true
        val counterType = cost.counterType?.let { it }
        val projected = state.projectedState
        val ctx = PredicateContext(controllerId = payerId)
        val total = projected.getBattlefieldControlledBy(payerId).sumOf { entityId ->
            if (!costHandler.predicateEvaluator.matches(state, projected, entityId, cost.filter, ctx)) return@sumOf 0
            val counters = state.getEntity(entityId)?.get<CountersComponent>() ?: return@sumOf 0
            if (counterType != null) counters.getCount(counterType) else counters.counters.values.sum()
        }
        return total >= needed
    }

    // The pool is counters, not objects, so affordability sums what the matching permanents carry
    // rather than counting candidates.
    override fun canPayFrom(env: SpellCostEnumeration, cost: CostAtom.RemoveCounters, candidates: List<EntityId>): Boolean {
        val needed = fixedCount(cost)
        return needed <= 0 || counterPool(env, cost).sumOf { it.availableCounters } >= needed
    }

    override fun present(env: SpellCostEnumeration, cost: CostAtom.RemoveCounters, candidates: List<EntityId>) =
        "Remove counters" to AdditionalCostData(
            description = cost.description.replaceFirstChar { it.uppercase() },
            costType = "RemoveCounters",
            counterRemovalCreatures = counterPool(env, cost),
            distributedCounterRemovalTotal = fixedCount(cost),
        )

    private fun counterPool(env: SpellCostEnumeration, cost: CostAtom.RemoveCounters) =
        env.costUtils.buildRemoveCountersPermanents(env.state, env.playerId, cost.filter, cost.counterType)

    override fun validate(check: SpellCostCheck, cost: CostAtom.RemoveCounters): String? {
        val state = check.state
        val projected = state.projectedState
        val needed = fixedCount(cost)
        val removals = check.payment?.distributedCounterRemovals ?: emptyList()
        val total = removals.sumOf { it.count }
        if (total < needed) {
            val phrase = if (needed == 1) "1 counter from a" else "$needed counters from among"
            val plural = if (needed == 1) "" else "s"
            return "You must remove $phrase ${cost.filter.description}$plural you control to cast this spell"
        }
        val demanded = mutableMapOf<Pair<EntityId, CounterType>, Int>()
        for (removal in removals) {
            if (removal.count <= 0) {
                return "Counter removal count must be positive"
            }
            val permContainer = state.getEntity(removal.entityId)
                ?: return "Counter removal target not found: ${removal.entityId}"
            permContainer.get<CardComponent>()
                ?: return "Counter removal target is not a card: ${removal.entityId}"
            if (projected.getController(removal.entityId) != check.playerId) {
                return "You can only remove counters from permanents you control"
            }
            if (removal.entityId !in state.getBattlefield()) {
                return "Counter removal target is not on the battlefield"
            }
            val ctx = PredicateContext(controllerId = check.playerId)
            if (!check.predicateEvaluator.matches(state, projected, removal.entityId, cost.filter, ctx)) {
                val permName = state.getEntity(removal.entityId)?.get<CardComponent>()?.name ?: "Permanent"
                return "$permName doesn't match the required filter: ${cost.filter.description}"
            }
            val key = removal.entityId to CounterType.of(removal.counterType)
            demanded[key] = (demanded[key] ?: 0) + removal.count
        }
        for ((key, demandedCount) in demanded) {
            val (entityId, counterType) = key
            val actual = state.getEntity(entityId)?.get<CountersComponent>()?.getCount(counterType) ?: 0
            if (actual < demandedCount) {
                return "Creature does not have $demandedCount ${counterType.printed} counters to remove"
            }
        }
        return null
    }

    override fun pay(ledger: SpellCostLedger, cost: CostAtom.RemoveCounters): String? {
        for (removal in ledger.payment.distributedCounterRemovals) {
            val container = ledger.state.getEntity(removal.entityId) ?: continue
            val existing = container.get<CountersComponent>() ?: continue
            val resolvedType = CounterType.of(removal.counterType)
            ledger.state = ledger.state.updateEntity(removal.entityId) { c ->
                c.with(existing.withRemoved(resolvedType, removal.count))
            }
            ledger.events.add(CountersRemovedEvent(
                entityId = removal.entityId,
                counterType = resolvedType,
                amount = removal.count,
                entityName = container.get<CardComponent>()?.name ?: "Permanent"
            ))
        }
        return null
    }
}

/** "Pay 2 life" (Timeline Culler's warp). Auto-paid: the amount is fixed, so there is nothing to choose. */
internal object PayLifeCostKind : SpellCostKind<CostAtom.PayLife> {
    // CR 810.9a — affordability uses the team's shared total in Two-Headed Giant.
    // CR 119.4 — a player may pay life only if their life total is >= the payment.
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom.PayLife, costHandler: CostHandler) =
        state.lifeTotal(payerId) >= cost.amount

    // Mode-level and cast-level affordability gate, so "discard a card or pay 3 life" doesn't
    // surface a Pay-3-Life action to a caster with fewer than 3 life (Bitter Triumph). Validation
    // still backstops it.
    override fun enumerate(env: SpellCostEnumeration, cost: CostAtom.PayLife, offer: SpellCostOffer) =
        env.state.lifeTotal(env.playerId) >= cost.amount

    override fun validate(check: SpellCostCheck, cost: CostAtom.PayLife): String? {
        // CR 119.4 — you can't pay life unless you have at least that much (CR 810.9a — team total)
        if (check.state.lifeTotal(check.playerId) < cost.amount) {
            return "Not enough life to pay ${cost.amount} life"
        }
        return null
    }

    override fun lifeToPay(check: SpellCostCheck, cost: CostAtom.PayLife) = cost.amount
}

/**
 * The atoms that are never a spell's additional cost: mana isn't produced as one; put-counters and
 * reveal-the-noted-type are ability-scoped (a spell on the stack has no permanent to accrue counters
 * on, nor one carrying a secret note); a spell is attached to nothing, so unattaching is ability-only;
 * putting cards from hand on the library (Leashling) is only printed on abilities;
 * mill and exile-from-library-top are activated-ability costs. Never payable here, so validation and
 * payment have nothing to do.
 */
internal object AbilityOnlyAtomCostKind : SpellCostKind<CostAtom> {
    override fun canPay(state: GameState, payerId: EntityId, cost: CostAtom, costHandler: CostHandler) = false
}
