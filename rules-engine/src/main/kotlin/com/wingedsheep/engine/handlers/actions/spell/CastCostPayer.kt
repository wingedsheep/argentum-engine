package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.costs.ForageCostResolver
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.mechanics.EmergeCasts
import com.wingedsheep.engine.mechanics.EscalateCosts
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.WebSlinging
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostCheck
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostLedger
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.paymentSubtypesOf
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/** What paying a cast's costs produced, for the stages after it. */
internal class CastPayment(
    val payment: PaymentResult,
    /** Total mana spent on this cast — Expend's running total and the stack object's record. */
    val manaSpent: Int,
    /** The first mana-spent event, carrying the per-colour breakdown mana-spent triggers read. */
    val manaSpentEvent: ManaSpentEvent?,
    /** Cast from a graveyard through a forage permission (Osteomancer Adept) — the entry rider's cue. */
    val isForageCast: Boolean,
)

/** The alternative costs whose non-mana portion returns a creature you control to hand. */
internal class ReturnedForAlternativeCost(
    val wasSneaked: Boolean,
    /** The player or planeswalker the sneak-returned creature was attacking (CR 702.190b). */
    val sneakAttackDefenderId: EntityId?,
    val wasWebSlung: Boolean,
    /** The web-slinging-returned creature's own mana value (CR 118.9c). */
    val webSlungReturnedManaValue: Int,
)

/**
 * The "pay the total cost" stage of casting a spell (CR 601.2g–h), and the affordability check
 * validation makes against the same terms.
 *
 * Also owns the cast's *owed* additional costs — which ones the declared modes, optional costs,
 * alternative cost and cast permission add up to — since both validation and payment read them.
 */
internal class CastCostPayer(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val costHandler: CostHandler,
    private val costCalculator: CostCalculator,
    private val manaSolver: ManaSolver,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val paymentProcessor: CastPaymentProcessor,
    private val castCostTotaller: CastCostTotaller,
    private val zoneResolver: CastZoneResolver,
    private val castPermissionUtils: CastPermissionUtils,
    private val conditionEvaluator: ConditionEvaluator,
    private val predicateEvaluator: PredicateEvaluator,
) {

    // ---------------------------------------------------------------------------------------------
    // Owed additional costs (CR 601.2b / 601.2f)
    // ---------------------------------------------------------------------------------------------

    /**
     * The additional costs a modal spell owes for the modes it chose: per-mode overrides where the
     * chosen modes declare them (rule 700.2h — they stack), card-level costs otherwise, plus the
     * non-mana escalate cost when the card has one ([EscalateCosts.additionalCostFor]).
     */
    fun additionalCostsForModes(cardDef: CardDefinition, action: CastSpell): List<AdditionalCost> {
        if (action.chosenModes.isEmpty()) return cardDef.script.additionalCosts
        val modalEffect = cardDef.script.spellEffect as? ModalEffect ?: return cardDef.script.additionalCosts

        val perModeOverrides = action.chosenModes.mapNotNull { modeIndex ->
            modalEffect.modes.getOrNull(modeIndex)?.additionalCosts
        }
        val base = if (perModeOverrides.isEmpty()) cardDef.script.additionalCosts else perModeOverrides.flatten()
        val escalate = EscalateCosts.additionalCostFor(modalEffect, action.chosenModes.size)
        return if (escalate == null) base else base + escalate
    }

    /**
     * The non-mana half of the optional cost the caster *declared* (kicker, bargain, teamwork —
     * `action.declaredCostSlot`). Kept apart from the card's printed additional costs because only
     * it carries the declared mechanic's identity, which is what names a tap's cause
     * ([com.wingedsheep.sdk.scripting.TapReason.forChoiceSlot]).
     */
    fun declaredSlotCost(action: CastSpell, cardDef: CardDefinition?): AdditionalCost? =
        declaredOptionalCosts(action, cardDef).firstOrNull { it.additionalCost != null }?.additionalCost

    /**
     * Every additional cost this cast owes: the card's (or its chosen modes'), the declared optional
     * cost's non-mana half, the chosen alternative cost's bundled costs, and the costs a cast
     * permission attaches.
     */
    fun owedAdditionalCosts(state: GameState, action: CastSpell, cardDef: CardDefinition?): List<AdditionalCost> = buildList {
        if (cardDef != null) addAll(additionalCostsForModes(cardDef, action))
        declaredSlotCost(action, cardDef)?.let { add(it) }
        if (action.useAlternativeCost && cardDef != null) {
            // Each bundled additional cost is gated by the chosen alternative-cost type so a
            // collision (e.g. granted warp on a card also being evoked) doesn't drag in the
            // unchosen cost's bundled additional cost.
            val selfAltCost = cardDef.script.selfAlternativeCost
            if (selfAltCost != null && action.altAllows(AlternativeCostType.SELF_ALTERNATIVE)) addAll(selfAltCost.additionalCosts)
            // A battlefield-granted alternative cost's non-mana half (Conspiracy Unraveler's
            // "collect evidence 10"). The mana half was already substituted for the spell's mana
            // cost; this is the rest of the same cost, so it is paid by the ordinary additional-cost
            // kinds — which is also what makes it validate and surface a picker like every other
            // selection cost.
            if (action.altAllows(AlternativeCostType.GRANTED)) {
                costCalculator.findAlternativeCastingCosts(state, action.playerId)
                    .firstOrNull()?.let { addAll(it.additionalCosts) }
            }
            // Flashback's bundled additional cost (e.g., Behold three Elementals)
            if (action.altAllows(AlternativeCostType.FLASHBACK) &&
                zoneResolver.hasFlashbackPermission(state, action.playerId, action.cardId)
            ) {
                cardDef.keywordAbilities
                    .filterIsInstance<KeywordAbility.Flashback>()
                    .firstOrNull()
                    ?.additionalCost
                    ?.let { add(it) }
            }
            // Warp's bundled additional cost (e.g., "Pay 2 life" on Timeline Culler). Use
            // [WarpGrants] so granted warps ([GrantWarpToCardsInHand]) participate too — currently
            // they carry no additional cost, but routing through the same helper keeps the seam.
            if (action.altAllows(AlternativeCostType.WARP) &&
                zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)
            ) {
                WarpGrants.effectiveWarp(state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator)
                    ?.additionalCost
                    ?.let { add(it) }
            }
        }
        // Runtime additional costs from entity component (e.g., The Infamous Cruelclaw)
        state.getEntity(action.cardId)
            ?.get<PlayWithAdditionalCostComponent>()
            ?.takeIf { it.controllerId == action.playerId }
            ?.let { addAll(it.additionalCosts) }

        // Linked-exile granter additional cost (e.g., Dawnhand Dissident's "remove three counters
        // from among creatures you control")
        zoneResolver.findLinkedExileGranter(state, action.playerId, action.cardId)
            ?.additionalCost?.let { add(it) }

        // Self-referential MayCastSelfFromZones grant's additional cost (e.g. Alien Symbiosis'
        // "by discarding a card")
        zoneResolver.findMayCastSelfFromZoneAbility(state, action.playerId, action.cardId)
            ?.additionalCost?.let { add(it) }

        // Gwenom: pay-life additional cost for a spell cast from the top of the library.
        zoneResolver.topOfLibraryAlternativeGrant(state, action.playerId, action.cardId)
            ?.additionalCost?.let { add(it) }
    }

    // ---------------------------------------------------------------------------------------------
    // Paying (CR 601.2g–h)
    // ---------------------------------------------------------------------------------------------

    /**
     * Pays everything the cast owes into [ledger], in the order the rules put it: the additional
     * costs and optional additional costs, the tap/exile payments that reduce the mana (delve,
     * convoke, harmonize, waterbend, improvise), the mana itself, then the costs paid as the mana is
     * (emerge's sacrifice, forage from a graveyard, the life taxes). Returns the payment on success,
     * or why it can't be paid.
     */
    fun pay(
        ledger: SpellCostLedger,
        cardComponent: CardComponent,
        cardDef: CardDefinition?,
        totalCost: ManaCost,
        owedCosts: List<AdditionalCost>,
        playForFree: Boolean,
    ): CastPaymentOutcome {
        val action = ledger.action
        payAdditionalCosts(ledger, owedCosts)?.let { return CastPaymentOutcome.Failed(it) }
        payConspire(ledger)
        payCasualty(ledger)

        // The X charged as mana (see CastCostTotaller.paymentXValue); action.xValue — the effect's
        // X — is untouched.
        val paymentXValue = castCostTotaller.paymentXValue(ledger.state, action, cardDef, totalCost)
        var cost = payWithPermanentsAndCards(ledger, totalCost, cardDef, playForFree)

        // "Mana of any type can be spent" — relax colored requirements for cast-from-exile
        // permissions that carry the flag (Taster of Wares, Cruelclaw's Heist).
        if (isCastWithAnyManaType(ledger.state, action)) {
            cost = cost.relaxColors()
        }
        val paymentResult = paymentProcessor.processPayment(
            ledger.state, action, cost, cardComponent.name, paymentXValue,
            spellPaymentContext(ledger.state, action, cardComponent), xManaRestriction(action, cardDef)
        )
        if (paymentResult.error != null) {
            return CastPaymentOutcome.Failed(paymentResult.error)
        }
        ledger.state = paymentResult.state
        ledger.events.addAll(paymentResult.events)

        payEmergeSacrifice(ledger, cardDef)
        val manaSpent = recordManaSpent(ledger, paymentResult)

        // Forage from a graveyard via MayCastCreaturesFromGraveyardWithForageComponent (e.g.,
        // Osteomancer Adept). See payGraveyardForage.
        val isForageCast = zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(
            ledger.state, action.playerId, action.cardId, cardComponent
        ) && action.cardId in ledger.state.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD))
        if (isForageCast) {
            payGraveyardForage(ledger)?.let { return CastPaymentOutcome.Failed(it) }
        }
        payGraveyardLifeCost(ledger)
        payTargetLifeTaxes(ledger)

        return CastPaymentOutcome.Paid(
            CastPayment(
                payment = paymentResult,
                manaSpent = manaSpent,
                manaSpentEvent = paymentResult.events.filterIsInstance<ManaSpentEvent>().firstOrNull(),
                isForageCast = isForageCast,
            )
        )
    }

    /**
     * Pays [costs] into [ledger]: first the life every life cost takes — fixed by the cast, so it is
     * paid whether or not the client sent a payment object — then each cost in order: from the
     * submitted selection, or — for a cost that selects nothing — unprompted. Returns an error to
     * abort the cast.
     */
    private fun payAdditionalCosts(ledger: SpellCostLedger, costs: List<AdditionalCost>): String? {
        for (cost in costs) {
            val lifeToPay = SpellCosts.lifeToPay(SpellCostCheck(ledger.state, ledger.action, costHandler, predicateEvaluator), cost)
            if (lifeToPay == 0) continue
            val (afterPayment, paymentEvents) =
                LifePaymentService.pay(zones, ledger.state, ledger.playerId, lifeToPay) ?: continue
            ledger.state = afterPayment
            ledger.events.addAll(paymentEvents)
        }
        val submitted = ledger.action.additionalCostPayment != null
        for (cost in costs) {
            if (!submitted && !SpellCosts.paysUnprompted(cost)) continue
            SpellCosts.pay(ledger, cost)?.let { return it }
        }
        return null
    }

    /**
     * Conspire's optional additional cost: tap the two chosen creatures (CR 702.78). Validated in
     * validate(); emits TappedEvent so "becomes tapped" self-triggers fire.
     */
    private fun payConspire(ledger: SpellCostLedger) {
        for (creatureId in ledger.action.conspiredCreatures) {
            val (tappedState, tapEvent) = tap(ledger.state, creatureId)
            ledger.state = tappedState
            tapEvent?.let(ledger.events::add)
        }
    }

    /**
     * Casualty's optional additional cost: sacrifice the chosen creature (CR 702.153). Routes through
     * the shared cost sacrifice so the leave-the-battlefield events are emitted for dies/leaves
     * triggers; the pre-sacrifice snapshot feeds the spell's own effect context (CR 608.2h / 113.7a).
     */
    private fun payCasualty(ledger: SpellCostLedger) {
        val permId = ledger.action.casualtyCreature ?: return
        ledger.sacrificedSnapshots.addAll(captureEntitySnapshots(listOf(permId), ledger.state.projectedState))
        if (ledger.state.getEntity(permId) != null) {
            ledger.sacrifice(permId)
        }
    }

    /**
     * The payments that take mana off [cost] by tapping or exiling things — delve, convoke and
     * harmonize; waterbend, bounded by the waterbend amount; improvise — and the "whenever you
     * waterbend" record. Returns the mana still owed.
     */
    private fun payWithPermanentsAndCards(
        ledger: SpellCostLedger,
        totalCost: ManaCost,
        cardDef: CardDefinition?,
        playForFree: Boolean,
    ): ManaCost {
        val action = ledger.action
        var cost = totalCost
        val alternativePayment = action.alternativePayment

        // Delve / convoke / harmonize
        if (alternativePayment != null && !alternativePayment.isEmpty && cardDef != null) {
            val result = alternativePaymentHandler.apply(
                ledger.state, cost, alternativePayment, action.playerId, cardDef, action.cardId
            )
            cost = result.reducedCost
            ledger.state = result.newState
            ledger.events.addAll(result.events)
        }

        // Waterbend (Avatar): tap the chosen artifacts/creatures, each paying {1} of the waterbend
        // generic, bounded by the waterbend amount. Sums the spell-level `waterbend {N}` additional
        // cost and Hama's fixed-alternative waterbend cost (only one is ever non-zero). > 0 exactly
        // when a waterbend cost is actually being paid on this cast (an optional "you may
        // waterbend" that was declined yields 0).
        val waterbendPaidAmount = (if (cardDef != null) castCostTotaller.spellWaterbendAmount(cardDef, action) else 0) +
            castCostTotaller.fixedAltWaterbendAmount(ledger.state, action, playForFree)
        if (waterbendPaidAmount > 0 && alternativePayment != null && alternativePayment.tapForGenericPermanents.isNotEmpty()) {
            val result = alternativePaymentHandler.applyWaterbendForSpell(
                ledger.state, cost, alternativePayment, action.playerId, waterbendPaidAmount
            )
            cost = result.reducedCost
            ledger.state = result.newState
            ledger.events.addAll(result.events)
        }

        // Improvise (CR 702.126a): tap the chosen artifacts, each paying {1} of the generic in the
        // spell's total cost. Unlike waterbend there is no separate amount to cap at — improvise is
        // not a cost of its own (CR 702.126b) — so it runs only when no waterbend cost claimed the
        // taps, and the handler re-checks the keyword before tapping anything.
        if (waterbendPaidAmount == 0 && !playForFree && cardDef != null &&
            alternativePayment != null && alternativePayment.tapForGenericPermanents.isNotEmpty()
        ) {
            val result = alternativePaymentHandler.applyImproviseForSpell(
                ledger.state, cost, alternativePayment, action.playerId, cardDef
            )
            cost = result.reducedCost
            ledger.state = result.newState
            ledger.events.addAll(result.events)
        }

        // CR 701.67c: paying a spell's waterbend cost (however paid — taps above and/or plain mana)
        // fires "whenever you waterbend". A later payment failure rolls the cast (and this event)
        // back, so emitting here is safe.
        if (waterbendPaidAmount > 0) {
            val (bendState, bendEvent) = BendEvents.record(ledger.state, action.playerId, BendType.WATER)
            ledger.state = bendState
            ledger.events.add(bendEvent)
        }
        return cost
    }

    /**
     * Emerge (CR 702.119a/c): the chosen creature is sacrificed *as the total cost is paid*
     * (CR 601.2h), which is why this runs after the mana payment rather than with the additional
     * costs — mana abilities are activated first (CR 601.2f–g), so the creature can legally be
     * tapped for mana toward its own emerge cost before it dies. Its mana value was already taken
     * off the generic portion of the total cost while it was on the battlefield.
     */
    private fun payEmergeSacrifice(ledger: SpellCostLedger, cardDef: CardDefinition?) {
        val action = ledger.action
        if (!action.useAlternativeCost || !action.altAllows(AlternativeCostType.EMERGE) ||
            cardDef == null || EmergeCasts.printedEmerge(cardDef) == null
        ) return
        val emergeSacrifice = action.additionalCostPayment?.sacrificedPermanents?.firstOrNull() ?: return
        if (ledger.state.getEntity(emergeSacrifice) == null) return
        // The [GameState] overload: besides last-known P/T it freezes the creature's *name*, which is
        // what lets the stack card and the game log say which body paid for this cast once it is
        // gone (a sacrificed token leaves no entity to read a name off). The snapshot feeds "as it
        // last existed on the battlefield" reads (CR 608.2h) exactly like a scripted sacrifice cost.
        ledger.sacrificedSnapshots.addAll(captureEntitySnapshots(listOf(emergeSacrifice), ledger.state))
        ledger.sacrifice(emergeSacrifice)
    }

    /** Adds this cast's mana to the player's running total for the turn (Expend). */
    private fun recordManaSpent(ledger: SpellCostLedger, paymentResult: PaymentResult): Int {
        val manaSpent = paymentResult.events.filterIsInstance<ManaSpentEvent>().sumOf { it.total }
        if (manaSpent > 0) {
            ledger.state = ledger.state.updateEntity(ledger.playerId) { container ->
                val existing = container.get<ManaSpentOnSpellsThisTurnComponent>() ?: ManaSpentOnSpellsThisTurnComponent()
                container.with(existing.copy(totalSpent = existing.totalSpent + manaSpent))
            }
        }
        return manaSpent
    }

    /**
     * Forage paid to cast a creature from a graveyard (Osteomancer Adept). The spell being cast is
     * excluded from the exile pool — it has left the graveyard for the stack and can't be one of the
     * three cards it exiles to pay for itself. The player's mode + card/Food choice (when supplied
     * via additionalCostPayment) is honored; otherwise a legal mode is auto-paid. See
     * [ForageCostResolver].
     */
    private fun payGraveyardForage(ledger: SpellCostLedger): String? {
        val action = ledger.action
        return when (val forageResult = ForageCostResolver.pay(
            zones,
            ledger.state, action.playerId,
            exileChoices = action.additionalCostPayment?.exiledCards ?: emptyList(),
            sacrificeChoices = action.additionalCostPayment?.sacrificedPermanents ?: emptyList(),
            excludeCardId = action.cardId,
        )) {
            is ForageCostResolver.Result.Success -> {
                ledger.state = forageResult.state
                ledger.events.addAll(forageResult.events)
                null
            }
            is ForageCostResolver.Result.Failure -> forageResult.reason
        }
    }

    /** The life a graveyard-cast permission charges (e.g., Festival of Embers). */
    private fun payGraveyardLifeCost(ledger: SpellCostLedger) {
        val lifeCost = ledger.action.graveyardLifeCost
        if (lifeCost <= 0) return
        val currentLife = ledger.state.lifeTotal(ledger.playerId) // CR 810.9a — team's shared total
        val newLife = currentLife - lifeCost
        ledger.state = ledger.state.withLifeTotal(ledger.playerId, newLife)
        ledger.events.add(LifeChangedEvent(ledger.playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        ledger.state = DamageUtils.markLifeLostThisTurn(ledger.state, ledger.playerId, lifeCost)
    }

    /**
     * Life owed to opponents' ModifySpellCost abilities for what this spell targets (e.g. Terror of
     * the Peaks: "Spells your opponents cast that target this creature cost an additional 3 life to
     * cast.").
     */
    private fun payTargetLifeTaxes(ledger: SpellCostLedger) {
        val action = ledger.action
        if (action.targets.isEmpty()) return
        val additionalLifeCost = costCalculator.calculateAdditionalLifeCost(ledger.state, action.playerId, action.targets)
        if (additionalLifeCost <= 0) return
        LifePaymentService.pay(zones, ledger.state, action.playerId, additionalLifeCost)
            ?.let { (afterPayment, paymentEvents) ->
                ledger.state = afterPayment
                ledger.events.addAll(paymentEvents)
            }
    }

    /**
     * The non-mana portion of sneak and web-slinging: return a creature you control to its owner's
     * hand. The mana was paid with the rest of the cost.
     *
     * - Sneak (CR 702.190a) returns an unblocked attacker; the defender it was attacking is captured
     *   first so a resolving permanent spell can enter attacking the same player or planeswalker
     *   (CR 702.190b). Printed sneak, or a granted graveyard sneak (Ninja Teen). The card may
     *   already be on the stack, so the grant is detected via the player's battlefield
     *   (zone-independent) rather than the card's current zone.
     * - Web-slinging (CR 702.188a) returns a tapped creature; its own mana value is captured first
     *   (CR 118.9c — Scarlet Spider, Ben Reilly reads it).
     */
    fun returnForAlternativeCost(ledger: SpellCostLedger, cardDef: CardDefinition?): ReturnedForAlternativeCost {
        val action = ledger.action
        val wasSneaked = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.SNEAK) &&
            (cardDef.keywordAbilities.any { it.ninjutsuStyleCost != null } ||
                SneakWindow.graveyardSneakGrantCost(ledger.state, action.playerId, cardRegistry) != null)
        var sneakAttackDefenderId: EntityId? = null
        if (wasSneaked) {
            action.additionalCostPayment?.bouncedPermanents?.firstOrNull()?.let { bounceId ->
                sneakAttackDefenderId = ledger.state.getEntity(bounceId)?.get<AttackingComponent>()?.defenderId
                returnToHand(ledger, bounceId)
            }
        }

        val wasWebSlung = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.WEB_SLINGING) &&
            WebSlinging.effectiveWebSlinging(ledger.state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator) != null
        var webSlungReturnedManaValue = 0
        if (wasWebSlung) {
            action.additionalCostPayment?.bouncedPermanents?.firstOrNull()?.let { bounceId ->
                webSlungReturnedManaValue = ledger.state.getEntity(bounceId)?.get<CardComponent>()?.manaValue ?: 0
                returnToHand(ledger, bounceId)
            }
        }
        return ReturnedForAlternativeCost(wasSneaked, sneakAttackDefenderId, wasWebSlung, webSlungReturnedManaValue)
    }

    private fun returnToHand(ledger: SpellCostLedger, permId: EntityId) {
        val bounceResult = zones.moveToZone(ledger.state, permId, Zone.HAND)
        ledger.state = bounceResult.state
        ledger.events.addAll(bounceResult.events)
    }

    // ---------------------------------------------------------------------------------------------
    // The mana terms, shared with validation
    // ---------------------------------------------------------------------------------------------

    /**
     * What conditional mana is judged against when it pays for this spell. A face-down cast
     * (CR 708.2) is a nameless 2/2 creature spell regardless of what the card says, so it gets its
     * own context rather than the printed card's.
     */
    fun spellPaymentContext(state: GameState, action: CastSpell, cardComponent: CardComponent?): SpellPaymentContext? =
        if (action.castFaceDown) {
            SpellPaymentContext.faceDownCast(isFromHand = isCastFrom(state, action.cardId, Zone.HAND))
        } else if (cardComponent != null) {
            SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = action.declaredCostSlot == ChoiceSlot.KICKED,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = paymentSubtypesOf(cardComponent),
                isFromExile = isCastFrom(state, action.cardId, Zone.EXILE),
                isFromHand = isCastFrom(state, action.cardId, Zone.HAND),
                cardTypes = cardComponent.typeLine.cardTypes,
            )
        } else null

    /**
     * "Spend only [colors] on X" (Soul Burn). Uses the cast face's script for split/adventure cards,
     * otherwise the card's own.
     */
    fun xManaRestriction(action: CastSpell, cardDef: CardDefinition?): Set<Color> =
        (action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script } ?: cardDef?.script)
            ?.xManaRestriction ?: emptySet()

    /**
     * True if mana of any type may be spent on this spell's mana cost (CR 118.14 / 609.4b). Two
     * independent sources:
     *
     * 1. A [com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells] static controlled by the
     *    caster whose filter matches the card — the blanket "you can spend mana of any type to cast
     *    [these] spells" (Vizier of the Menagerie). Zone-agnostic, so it is checked first and covers
     *    hand and top-of-library casts too.
     * 2. A [com.wingedsheep.engine.state.permissions.MayPlayPermission] carrying the
     *    `withAnyManaType` rider. That is a *per-card* grant, so the card must currently be in a
     *    zone a may-play permission can grant casting from — exile (the card's owner's, which may be
     *    an opponent — e.g. Taster of Wares leaves the exiled card in the revealing player's exile)
     *    or a graveyard (per-card grants that leave the card in the graveyard — e.g. Tinybones, the
     *    Pickpocket lets you cast a targeted nonland permanent card from the damaged player's
     *    graveyard). An active permission must be granted to the casting player with its condition
     *    gate open, and the `withAnyManaType` flag must be set on at least one of them.
     */
    fun isCastWithAnyManaType(state: GameState, action: CastSpell): Boolean {
        if (castPermissionUtils.canSpendAnyManaTypeForSpell(state, action.playerId, action.cardId)) {
            return true
        }
        val inGrantableZone = isCastFrom(state, action.cardId, Zone.EXILE) || isCastFrom(state, action.cardId, Zone.GRAVEYARD)
        if (!inGrantableZone) return false
        if (state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
                .any { it.withAnyManaType }
        ) {
            return true
        }
        // 3. A linked-exile cast grant ([com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile])
        //    carrying the same rider — "you may cast the exiled card, and mana of any type can be
        //    spent to cast that spell" (Null Summoner). Only exile hosts a linked pile.
        return isCastFrom(state, action.cardId, Zone.EXILE) &&
            zoneResolver.findLinkedExileGranter(state, action.playerId, action.cardId)?.withAnyManaType == true
    }

    private fun isCastFrom(state: GameState, cardId: EntityId, zone: Zone): Boolean =
        state.turnOrder.any { ownerId -> cardId in state.getZone(ZoneKey(ownerId, zone)) }

    /**
     * Whether the caster can pay [cost] (with [paymentXValue] as the X paid in mana) the way their
     * [CastSpell.paymentStrategy] says they will; null when they can.
     */
    fun validateManaPayment(state: GameState, action: CastSpell, cost: ManaCost, paymentXValue: Int = action.xValue ?: 0): String? {
        val xValue = paymentXValue
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
        val spellCtx = spellPaymentContext(state, action, cardComponent)

        // "Mana of any type can be spent" — relax colored requirements when the cast permission
        // carries that flag (e.g. Taster of Wares, Cruelclaw's Heist).
        val effectiveCost = if (isCastWithAnyManaType(state, action)) cost.relaxColors() else cost

        // "Spend only [colors] on X" restriction (Soul Burn) — limits which mana can pay X.
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val xManaRestriction = xManaRestriction(action, cardDef)

        val validationCost = when (val strategy = action.paymentStrategy) {
            is PaymentStrategy.Explicit -> effectiveCost.withPhyrexianPaidByLife(strategy.phyrexianLifePayments)
                ?: return "Invalid Phyrexian mana payment"
            else -> effectiveCost
        }

        return when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                if (!manaSolver.canPay(state, action.playerId, validationCost, xValue, spellContext = spellCtx, xManaRestriction = xManaRestriction)) {
                    "Not enough mana to cast this spell"
                } else null
            }
            is PaymentStrategy.FromPool -> {
                if (!floatingPool(state, action.playerId).canPay(validationCost, spellCtx)) {
                    "Insufficient mana in pool to cast this spell"
                } else null
            }
            is PaymentStrategy.Explicit -> {
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceContainer = state.getEntity(sourceId)
                        ?: return "Mana source not found: $sourceId"
                    if (sourceContainer.has<TappedComponent>()) {
                        return "Mana source is already tapped: $sourceId"
                    }
                }
                // Mirror what [CastPaymentProcessor.autoPay] actually does: pay from the floating
                // pool first, then verify the chosen sources can cover the rest. Otherwise a player
                // who has already floated mana before clicking cast gets a false "Selected mana
                // sources cannot pay this spell's cost" because the validator demands the chosen
                // sources alone cover the full (post-convoke/delve) cost.
                val partial = floatingPool(state, action.playerId).payPartial(validationCost, spellCtx)
                val remainingCost = partial.remainingCost
                // Floating mana also covers the {X} portion (execution — explicitPay → autoPay —
                // spends it before tapping anything), so only ask the chosen sources for the X the
                // pool can't pay. Eligible restricted mana counts via ManaPool.xCoverage.
                val xSymbolCount = validationCost.xCount.coerceAtLeast(1)
                val totalXMana = xValue * xSymbolCount
                val xRemaining = totalXMana - partial.newPool.xCoverage(totalXMana, xManaRestriction, spellCtx)
                if (remainingCost.isEmpty() && xRemaining == 0) {
                    null
                } else {
                    val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                    val excluded = manaSolver.findAvailableManaSources(state, action.playerId)
                        .map { it.entityId }
                        .filter { it !in chosen }
                        .toSet()
                    if (manaSolver.solve(state, action.playerId, remainingCost, xRemaining, excludeSources = excluded, spellContext = spellCtx, xManaRestriction = xManaRestriction) == null) {
                        "Selected mana sources cannot pay this spell's cost"
                    } else null
                }
            }
        }
    }

    private fun floatingPool(state: GameState, playerId: EntityId): ManaPool {
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        return ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana
        )
    }
}

/** How paying a cast's costs went. */
internal sealed interface CastPaymentOutcome {
    class Paid(val payment: CastPayment) : CastPaymentOutcome

    /** The costs couldn't be paid; [reason] rejects the cast. */
    class Failed(val reason: String) : CastPaymentOutcome
}
