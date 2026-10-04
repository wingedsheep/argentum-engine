package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.mechanics.DisturbCasts
import com.wingedsheep.engine.mechanics.EmergeCasts
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.MayhemGrants
import com.wingedsheep.engine.mechanics.EscapeCasts
import com.wingedsheep.engine.mechanics.MiracleGrants
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.WebSlinging
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MiracleWindowComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/** The mana a cast charges and the X actually paid as mana at payment time. */
internal data class ComputedCastCost(val cost: ManaCost, val paymentXValue: Int)

/**
 * The "determine total cost" stage of casting a spell (CR 601.2f): the cost the caster locks in once
 * the spell, its modes, targets and additional costs have been announced.
 *
 * One implementation for every caller. Before it existed `validate` and `execute` each carried their
 * own copy of this pipeline, and they had drifted: validation priced splice costs that execution
 * never charged, and execution charged per-mode mana that validation never checked.
 *
 * The order is the rule's: the base (the mana cost, or the alternative cost that replaces it),
 * then additions (kicker, per-target tax, mode costs, declined or-pay legs, waterbend, cost
 * increases, splice), then reductions known from the declared payment. Reductions that are *paid*
 * by tapping or exiling (delve, convoke, improvise, waterbend taps, harmonize) are applied by the
 * payer — [validationCost] prices them for the affordability check.
 */
internal class CastCostTotaller(
    private val cardRegistry: CardRegistry,
    private val costCalculator: CostCalculator,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val zoneResolver: CastZoneResolver,
    private val predicateEvaluator: PredicateEvaluator,
) {

    /**
     * The total cost of casting [action] (CR 601.2f), before tap/exile payment reductions. Null when
     * the cast names an alternative cost that isn't available to it, or the card has no mana cost to
     * pay (CR 202.1b / 118.6) — `validate` rejects the cast in both cases.
     */
    fun totalCost(
        state: GameState,
        action: CastSpell,
        cardDef: CardDefinition?,
        cardComponent: CardComponent,
        playForFree: Boolean,
        castingFromCommandZone: Boolean,
    ): ManaCost? {
        var effectiveCost = baseCost(state, action, cardDef, cardComponent, playForFree, castingFromCommandZone)
            ?: return null

        // Add kicker/offspring mana cost if kicked (only for mana-based kicker/offspring; not
        // applicable with alternative costs).
        if (!playForFree && !action.useAlternativeCost) {
            // Summed over every declared cost: "Kicker [A] and/or [B]" kicked with both pays both
            // (CR 702.33b); a single-cost declaration is the one-element case.
            val kickerManaCost = optionalCostsManaPaid(declaredOptionalCosts(action, cardDef), action.declaredCostTimes)
            if (kickerManaCost != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerManaCost.symbols)
            }
        }

        // "This spell costs {W}{U} more to cast for each target beyond the first" (Officious
        // Interrogation). Charged outside every `playForFree` / alternative-cost / face-down base,
        // because a cost *increase* is not part of the cost those bases waive or replace
        // (CR 601.2f) — the card's own ruling spells it out: a free cast still owes the tax for
        // targets beyond the first. `calculateEffectiveCost` already applied it on the ordinary
        // path, so only the bases that bypassed it are topped up here.
        if (cardDef != null && !com.wingedsheep.engine.mechanics.BestowCasts.selected(action) &&
            (playForFree || action.useAlternativeCost || action.castFaceDown)) {
            effectiveCost = effectiveCost + costCalculator.selfPerTargetTax(
                cardDef, action.targets.map { it.toEntityId() }
            )
        }

        // Per-mode additional mana cost (e.g., Feed the Cycle "pay {B}" mode). With choose-N
        // (rule 700.2h), the additional mana cost of every chosen mode stacks, and escalate's
        // per-extra-mode mana (CR 702.120a) is added for each mode beyond the first.
        if (cardDef != null && action.chosenModes.isNotEmpty()) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            if (modalEffect != null) {
                for (modeIndex in action.chosenModes) {
                    val modeManaCost = modalEffect.modes.getOrNull(modeIndex)?.additionalManaCost ?: continue
                    effectiveCost = effectiveCost + ManaCost.parse(modeManaCost)
                }
                val perExtraMode = modalEffect.additionalManaCostPerExtraMode
                if (perExtraMode != null) {
                    repeat((action.chosenModes.size - 1).coerceAtLeast(0)) {
                        effectiveCost = effectiveCost + ManaCost.parse(perExtraMode)
                    }
                }
            }
        }

        // Fold in the "… or pay {N}" alternative mana for every or-pay cost whose non-mana leg the
        // caster declined.
        if (cardDef != null && !playForFree) {
            effectiveCost = SpellCosts.applyManaSurcharges(
                effectiveCost, cardDef.script.additionalCosts, action.additionalCostPayment
            )
        }

        // Spell-level waterbend additional cost (Avatar: The Last Airbender). Adds the waterbend
        // amount {N} (or {X}) as generic mana; the tapped artifacts/creatures in alternativePayment
        // reduce that generic at payment, capped at N.
        if (cardDef != null && !playForFree) {
            val waterbendAmount = spellWaterbendAmount(cardDef, action)
            if (waterbendAmount > 0) {
                effectiveCost = effectiveCost + ManaCost.parse("{$waterbendAmount}")
            }
        }

        // Airbend: a fixed alternative cost ({2}) is paid *instead of* the printed cost — it
        // replaces the base. A cost increase (e.g. Soul Partition's tax, or a Thalia-style "costs
        // {1} more") is not part of the cost it replaces, so it still applies on top: an airbended
        // card cast under a {1}-tax costs {3}, not {2}.
        if (!playForFree) {
            val fixedAltCost = state.getEntity(action.cardId)
                ?.get<PlayWithFixedAlternativeManaCostComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (fixedAltCost != null) {
                effectiveCost = fixedAltCost.fixedCost
            }
            // Apply runtime mana tax from exile permissions (e.g., Soul Partition) on top of
            // whichever base applies (printed cost, or the fixed alternative above).
            val runtimeCostIncrease = state.getEntity(action.cardId)
                ?.get<PlayWithCostIncreaseComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostIncrease != null) {
                effectiveCost = effectiveCost + runtimeCostIncrease.cost
            }
        }

        // Splice (CR 702.47a): every revealed splice card's cost is an *additional* cost, so it lands
        // on top of whatever is paying for the spell itself — a free cast and an alternative cost both
        // waive only the mana cost, never the additional costs (CR 601.2f–h). Added after the airbend
        // branch above, which *replaces* effectiveCost outright and would otherwise wipe it.
        if (action.splicedCardIds.isNotEmpty()) {
            effectiveCost = SpliceCasts.addSpliceCosts(effectiveCost, state, action.splicedCardIds, cardRegistry)
        }

        // "You may pay any amount of mana" as an additional cost (Chorus of the Conclave) — like
        // splice, on top of whatever pays for the spell itself; a free or alternative cast still owes
        // it (CR 601.2f). Legality is checked in validation.
        if (action.additionalManaForCounters > 0) {
            effectiveCost = effectiveCost + ManaCost.parse("{${action.additionalManaForCounters}}")
        }

        // Sacrifice-for-cost-reduction (Torgaar): the declared sacrifices take generic mana off.
        if (cardDef != null && action.additionalCostPayment != null) {
            for (cost in cardDef.script.additionalCosts) {
                if (cost is AdditionalCost.SacrificeCreaturesForCostReduction) {
                    val reduction = action.additionalCostPayment.sacrificedPermanents.size * cost.costReductionPerCreature
                    if (reduction > 0) {
                        effectiveCost = effectiveCost.reduceGeneric(reduction)
                    }
                }
            }
        }

        return effectiveCost
    }

    /**
     * The mana validation checks the caster can pay: [totalCost] with the tap/exile payment
     * reductions (delve, convoke, waterbend, improvise) applied, plus the X actually paid as mana.
     */
    fun validationCost(
        state: GameState,
        action: CastSpell,
        cardDef: CardDefinition?,
        cardComponent: CardComponent,
        playForFree: Boolean,
        castingFromCommandZone: Boolean,
    ): ComputedCastCost? {
        val effectiveCost = totalCost(state, action, cardDef, cardComponent, playForFree, castingFromCommandZone)
            ?: return null

        // Account for Delve/Convoke reduction before validating payment
        val costAfterAltPayment = if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            alternativePaymentHandler.calculateReducedCost(
                effectiveCost,
                action.alternativePayment,
                cardDef,
                state,
                action.playerId,
                action.cardId
            )
        } else {
            effectiveCost
        }

        // Account for waterbend (Avatar): tapped artifacts/creatures reduce the waterbend generic,
        // capped at the waterbend amount. Two sources: a spell-level `waterbend {N}` additional cost
        // (capped so taps never eat the printed generic) and Hama's fixed-alternative waterbend cost
        // (the whole {mana value} is reducible). Only one is ever non-zero for a given cast.
        val validateWaterbendCap = (if (cardDef != null) spellWaterbendAmount(cardDef, action) else 0) +
            fixedAltWaterbendAmount(state, action, playForFree)
        val costAfterWaterbend = if (!playForFree && action.alternativePayment != null &&
            action.alternativePayment.tapForGenericPermanents.isNotEmpty() && validateWaterbendCap > 0
        ) {
            alternativePaymentHandler.calculateReducedCostForWaterbend(
                costAfterAltPayment, action.alternativePayment, validateWaterbendCap
            )
        } else {
            costAfterAltPayment
        }

        // Account for improvise (CR 702.126): each tapped artifact pays {1} of the generic in the
        // spell's *total* cost, with no cap beyond that generic. Shares the tap-for-generic carrier
        // with waterbend, and no card has both — the cap above being 0 is what tells them apart.
        val costAfterImprovise = if (!playForFree && cardDef != null && validateWaterbendCap == 0 &&
            action.alternativePayment != null && action.alternativePayment.tapForGenericPermanents.isNotEmpty()
        ) {
            alternativePaymentHandler.calculateReducedCostForImprovise(
                costAfterWaterbend, action.alternativePayment, cardDef, state, action.playerId, action.cardId
            )
        } else {
            costAfterWaterbend
        }

        return ComputedCastCost(costAfterImprovise, paymentXValue(state, action, cardDef, effectiveCost))
    }

    /**
     * The X to charge as mana (≤ [CastSpell.xValue]). Convoke taps for {1}, delved cards and a
     * harmonize creature's power all pay generic mana, and the announced X is generic mana in the
     * total cost (CR 601.2f) — so the reduction left over beyond the printed generic comes off the
     * X mana paid (see [alternativePaymentXValue]). For a
     * "waterbend {X}" spell the X is already materialized as generic in the cost (and reduced by the
     * waterbend taps), so it must NOT also be charged as {X} mana. The effect's X is untouched.
     */
    fun paymentXValue(state: GameState, action: CastSpell, cardDef: CardDefinition?, totalCost: ManaCost): Int =
        if (!totalCost.hasX || cardDef?.script?.spellWaterbend?.isX == true) 0
        else alternativePaymentXValue(state, action, cardDef, totalCost)

    // ---------------------------------------------------------------------------------------------
    // The base
    // ---------------------------------------------------------------------------------------------

    private fun baseCost(
        state: GameState,
        action: CastSpell,
        cardDef: CardDefinition?,
        cardComponent: CardComponent,
        playForFree: Boolean,
        castingFromCommandZone: Boolean,
    ): ManaCost? {
        // Split-layout (CR 709.3a) — only the chosen half is evaluated for legality. When
        // `faceIndex` is set, the cost is the face's printed mana cost passed through the standard
        // battlefield cost-modifier pipeline (CR 118.9a applies cost modifiers to the chosen half
        // just like to a normal cast).
        val faceManaCostOverride: ManaCost? = action.faceIndex?.let { idx -> cardDef?.cardFaces?.getOrNull(idx)?.manaCost }
        return when {
            playForFree -> ManaCost.ZERO
            faceManaCostOverride != null && cardDef != null ->
                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, faceManaCostOverride, action.playerId)
            action.useAlternativeCost && cardDef != null ->
                alternativeBases.firstNotNullOfOrNull { (type, base) ->
                    if (action.altAllows(type)) base(AlternativeBaseQuery(state, action, cardDef)) else null
                }
            // CR 708.4 — a face-down spell costs {3} (morph / disguise), before modifiers.
            action.castFaceDown -> costCalculator.calculateFaceDownCost(state, action.playerId)
            cardDef != null -> {
                // CR 202.1b/118.6: a card printed with genuinely no mana cost (Ancestral Vision)
                // represents an unpayable cost and can't be cast this way — the bases above already
                // cover the alternative costs and free-cast permissions that CAN play it (Suspend
                // routes through a completely separate free-cast pipeline). `hasNoManaCost` (not
                // `manaCost` itself) is the DSL-authored signal — a printed {0} stays normally
                // castable, and test fixtures often build `ManaCost.ZERO` directly to mean "free".
                if (cardDef.hasNoManaCost) return null
                costCalculator.calculateEffectiveCost(
                    state,
                    cardDef,
                    action.playerId,
                    action.targets.map { it.toEntityId() },
                    fromZone = if (castingFromCommandZone) Zone.COMMAND else castSourceZone(state, action.cardId),
                    // Price the branch the player actually announced — a "costs {2} less to cast if
                    // it's bargained" reduction is gated on the declaration (CR 702.166).
                    declaredCostSlot = action.declaredCostSlot,
                )
            }
            else -> cardComponent.manaCost
        }
    }

    private class AlternativeBaseQuery(val state: GameState, val action: CastSpell, val cardDef: CardDefinition) {
        val playerId: EntityId get() = action.playerId
        val cardId: EntityId get() = action.cardId
    }

    private fun AlternativeBaseQuery.priced(base: ManaCost): ManaCost =
        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, base, playerId)

    /**
     * The alternative costs that can replace a spell's mana cost (CR 118.9), in the order they are
     * tried. Each is consulted only when the cast permits its [AlternativeCostType]
     * ([CastSpell.altAllows]), so an explicit choice (e.g. evoke) isn't overridden by a
     * higher-priority cost that also happens to be legal (e.g. a granted warp); with no choice
     * recorded every gate is open and this is the priority order. Each base answers null when its
     * cost isn't available to this cast, and a cast with no available base is rejected — a specific
     * alternative cost whose own permission gate failed never falls back to an unrelated one.
     *
     * Every base runs through the alternative-base cost-modifier pipeline, so battlefield cost
     * modifiers apply to it.
     */
    private val alternativeBases: List<Pair<AlternativeCostType, AlternativeBaseQuery.() -> ManaCost?>> = listOf(
        // Flashback — printed, granted per-entity by Archmage's Newt, or granted to the whole
        // graveyard by a battlefield static (Iroh, Grand Lotus).
        AlternativeCostType.FLASHBACK to {
            FlashbackGrants.effectiveFlashback(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)
                ?.takeIf { zoneResolver.hasFlashbackPermission(state, playerId, cardId) }
                ?.let { priced(it.cost) }
        },
        // Harmonize — printed or granted at runtime (Songcrafter Mage). The per-creature power
        // reduction is applied afterward via alternativePayment.
        AlternativeCostType.HARMONIZE to {
            HarmonizeGrants.effectiveHarmonize(state, cardId, cardDef)
                ?.takeIf { zoneResolver.hasHarmonizePermission(state, playerId, cardId) }
                ?.let { priced(it.cost) }
        },
        // Mayhem (CR 702.187) — cast from graveyard for its mayhem cost.
        AlternativeCostType.MAYHEM to {
            MayhemGrants.effectiveMayhem(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)
                ?.takeIf { zoneResolver.hasMayhemPermission(state, playerId, cardId) }
                ?.let { priced(it.cost) }
        },
        // Escape (CR 702.138a) — cast from graveyard for its escape mana; the non-mana half is
        // owed as an additional cost (CastAdditionalCosts).
        AlternativeCostType.ESCAPE to {
            EscapeCasts.printedEscape(cardDef)
                ?.takeIf { zoneResolver.hasEscapePermission(state, playerId, cardId) }
                ?.let { priced(it.cost) }
        },
        // Disturb (CR 702.146a) — printed on the front face, which is also the face the cost-modifier
        // pipeline is applied against (the spell's mana value comes from the front face, CR 712.8c).
        AlternativeCostType.DISTURB to {
            DisturbCasts.printedDisturb(cardDef)
                ?.takeIf { zoneResolver.disturbCastFace(state, playerId, cardId) != null }
                ?.let { priced(it.cost) }
        },
        // Modal DFC back face (CR 712.11b) — you pay that face's *own* printed mana cost, not an
        // alternative one; unlike disturb the base is the back face's cost, because CR 712.8f gives
        // a modal back face its own mana value.
        AlternativeCostType.MODAL_BACK_FACE to {
            zoneResolver.modalBackCastFace(state, playerId, cardId)?.let { priced(it.manaCost) }
        },
        // Warp (hand only — CR 702.185a). Re-casts from exile pay the regular mana cost. Printed warp
        // wins; a battlefield grant ([GrantWarpToCardsInHand]) supplies the cost otherwise.
        AlternativeCostType.WARP to {
            WarpGrants.effectiveWarp(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)
                ?.takeIf { zoneResolver.hasWarpPermission(state, playerId, cardId) }
                ?.let { priced(it.cost) }
        },
        // Sneak (CR 702.190 — the mana portion; the bounce is paid separately). Printed Sneak, or a
        // granted graveyard sneak (Ninja Teen: "creature cards in your graveyard have sneak {3}{B}").
        AlternativeCostType.SNEAK to {
            SneakWindow.effectiveSneakCost(state, cardDef, cardId, playerId, cardRegistry)?.let { priced(it) }
        },
        // Web-slinging (CR 702.188 — an alternative cost bundling a return-a-tapped-creature payment,
        // cast at the spell's normal timing).
        AlternativeCostType.WEB_SLINGING to {
            WebSlinging.effectiveWebSlinging(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)
                ?.let { priced(it.cost) }
        },
        AlternativeCostType.BESTOW to {
            if (action.alternativeCostType == AlternativeCostType.BESTOW) {
                cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Bestow>().firstOrNull()?.let {
                    costCalculator.calculateEffectiveCost(state, cardDef, playerId,
                        chosenTargets = action.targets.map { target -> target.toEntityId() },
                        fromZone = if (zoneResolver.hasCommanderCastPermission(state, playerId, cardId)) Zone.COMMAND else castSourceZone(state, cardId),
                        declaredCostSlot = action.declaredCostSlot,
                        baseCost = action.xValue?.let { x -> it.cost.withXAs(x) } ?: it.cost)
                }
            } else null
        },
        AlternativeCostType.EVOKE to {
            cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()?.let { priced(it.cost) }
        },
        // Emerge (CR 702.119a) — the emerge cost, then reduced by an amount of *generic* mana equal
        // to the sacrificed creature's mana value. The reduction lands after the cost-modifier
        // pipeline because it is a cost reduction (CR 601.2f applies increases before reductions),
        // and the creature is still on the battlefield here: it is sacrificed only as the total cost
        // is paid (CR 601.2h), which execute() does after mana payment.
        AlternativeCostType.EMERGE to {
            EmergeCasts.effectiveEmerge(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)?.let {
                EmergeCasts.reduceForSacrifice(
                    priced(it.cost), state, action.additionalCostPayment?.sacrificedPermanents?.firstOrNull()
                )
            }
        },
        // Dash (CR 702.109 — hand only, printed only for now).
        AlternativeCostType.DASH to {
            cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Dash>().firstOrNull()
                ?.takeIf { zoneResolver.hasDashPermission(state, playerId, cardId) }
                ?.let { priced(it.cost) }
        },
        AlternativeCostType.IMPENDING to {
            cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Impending>().firstOrNull()?.let { priced(it.cost) }
        },
        // Cleave (CR 702.148 — an alternative cost; the brackets-removed text variant is chosen
        // structurally at resolution, not here).
        AlternativeCostType.CLEAVE to {
            cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cleave>().firstOrNull()?.let { priced(it.cost) }
        },
        // Overload (CR 702.96 — an alternative cost; the "each" variant is swapped in at resolution).
        AlternativeCostType.OVERLOAD to {
            cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Overload>().firstOrNull()?.let { priced(it.cost) }
        },
        // Miracle (CR 702.94 — printed or granted in hand, window-gated). The window component is
        // present only when the card was drawn as the first card this turn; without it, the miracle
        // alternative cost is unavailable.
        AlternativeCostType.MIRACLE to {
            if (state.getEntity(cardId)?.has<MiracleWindowComponent>() == true) {
                MiracleGrants.effectiveMiracle(state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator)
                    ?.let { priced(it.cost) }
            } else null
        },
        // A card's own alternative cost (e.g., Zahid's {3}{U} + tap an artifact).
        AlternativeCostType.SELF_ALTERNATIVE to {
            cardDef.script.selfAlternativeCost?.let { priced(it.manaCost) }
        },
        // A battlefield-granted alternative cost (e.g., Jodah's {W}{U}{B}{R}{G}). Only the mana half
        // is priced here; the grant's non-mana half (Conspiracy Unraveler's "collect evidence 10") is
        // paid with the other additional costs.
        AlternativeCostType.GRANTED to {
            costCalculator.findAlternativeCastingCosts(state, playerId, cardDef).firstOrNull()?.let { priced(it.manaCost) }
        },
    )

    // ---------------------------------------------------------------------------------------------
    // Waterbend and harmonize
    // ---------------------------------------------------------------------------------------------

    /**
     * The waterbend amount this cast adds to its mana cost (Avatar: The Last Airbender), or 0 when
     * the spell has no waterbend additional cost, or its *optional* cost was declined. For
     * "waterbend {X}" the amount is the cast-time X ([CastSpell.xValue]).
     */
    fun spellWaterbendAmount(cardDef: CardDefinition, action: CastSpell): Int {
        val wb = cardDef.script.spellWaterbend ?: return 0
        val paid = !wb.optional || action.wasWaterbendPaid
        if (!paid) return 0
        return if (wb.isX) (action.xValue ?: 0) else wb.amount
    }

    /**
     * The generic amount of a waterbend-flagged *fixed alternative* cost this cast can pay by
     * tapping artifacts/creatures, or 0 when the cast has no such cost. Hama, the Bloodbender exiles
     * a card and grants a `PlayWithFixedAlternativeManaCostComponent(waterbend = true)` whose whole
     * fixed cost is `{mana value}` generic and entirely waterbend-reducible (CR 701.67). Unlike a
     * spell-level `waterbend {N}` additional cost — which is capped so taps never eat the printed
     * generic — the fixed alternative cost *replaces* the printed cost, so the cap is the whole cost.
     */
    fun fixedAltWaterbendAmount(state: GameState, action: CastSpell, playForFree: Boolean): Int {
        if (playForFree) return 0
        val comp = state.getEntity(action.cardId)
            ?.get<PlayWithFixedAlternativeManaCostComponent>()
            ?.takeIf { it.controllerId == action.playerId && it.waterbend }
            ?: return 0
        return comp.fixedCost.genericAmount
    }

    /**
     * The X still owed as mana once the tap/exile payments have paid their share of it.
     *
     * CR 601.2f: the total cost includes the announced X as generic mana, so a payment that pays
     * generic mana — a convoke tap for {1} (CR 702.51a), a delved card, harmonize's power — pays
     * the X-derived generic as well as the printed generic. The payer charges X separately from
     * [totalCost]'s symbols, and `ManaCost.reduceGeneric` only eats the generic symbols, so the
     * reduction left over after the printed generic comes off the X mana here.
     *
     * With several {X} symbols (no current convoke/delve/harmonize card) the per-symbol X rounds
     * *up*, so payment never charges less than the total cost; the rounding can only strand a tap.
     */
    private fun alternativePaymentXValue(
        state: GameState,
        action: CastSpell,
        cardDef: CardDefinition?,
        totalCost: ManaCost,
    ): Int {
        val xValue = action.xValue ?: 0
        if (xValue <= 0 || cardDef == null) return xValue
        val payment = action.alternativePayment ?: return xValue
        val reduction = alternativePaymentHandler.genericReductionForSpell(
            state, payment, action.playerId, cardDef, action.cardId,
            // Harmonize (printed or granted — the handler checks both) also needs the cast's
            // graveyard permission.
            harmonizeAllowed = payment.harmonizeCreature != null &&
                zoneResolver.hasHarmonizePermission(state, action.playerId, action.cardId),
            // Improvise taps pay the announced X too, unless a waterbend cost claims them.
            improviseAllowed = spellWaterbendAmount(cardDef, action) == 0,
        )
        val leftover = (reduction - totalCost.genericAmount).coerceAtLeast(0)
        if (leftover == 0) return xValue
        val xCount = totalCost.xCount.coerceAtLeast(1)
        val xMana = (xValue * xCount - leftover).coerceAtLeast(0)
        return (xMana + xCount - 1) / xCount
    }

    /**
     * The zone the card is being cast from, used to apply cast-from-zone cost modifiers (e.g. Aven
     * Interrupter's "spells your opponents cast from graveyards or exile cost {2} more"). A spell
     * card still occupies its source zone when the cost is computed (it hasn't moved to the stack
     * yet); null once it has. Commander casts are handled separately via `Zone.COMMAND`.
     */
    fun castSourceZone(state: GameState, cardId: EntityId): Zone? {
        for (ownerId in state.turnOrder) {
            for (zone in listOf(Zone.HAND, Zone.GRAVEYARD, Zone.EXILE, Zone.LIBRARY)) {
                if (cardId in state.getZone(ZoneKey(ownerId, zone))) return zone
            }
        }
        return null
    }
}
