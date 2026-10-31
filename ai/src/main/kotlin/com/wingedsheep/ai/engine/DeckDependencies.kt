package com.wingedsheep.ai.engine

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CompanionEligibility
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * "Does this spell do anything in *this* deck?" — the deck-building reading of a card whose whole
 * effect hangs on the deck holding some kind of card.
 *
 * Loyal Tutor ("search your library for a planeswalker card") and Entrust the Spark ("you may
 * sacrifice a planeswalker. If you do, search for a planeswalker card") rate fine in a vacuum, but in
 * a deck with no planeswalkers they are blank cards that sit in hand all game (2026-10-15 games
 * 577f03e0, c13e39d1). The question is read off the card's structure, never its name:
 *
 *  - **What the spell gathers from our own zones** — a library search, "planeswalkers you control"
 *    for a sacrifice — and the `feasibility` of a "you may …" gate;
 *  - **Additional costs it can't be cast without** — "as an additional cost, sacrifice a X" or
 *    "behold a X" with no "or pay" alternative;
 *  - **A required target among our own permanents** — "target X you control".
 *
 * A requirement is met when the deck holds at least [MIN_SUPPORT] other cards that match it. A card
 * is [inert] only when *every* part of its effect hangs on an unmet requirement: Sphinx's Approach's
 * "four cards named Sphinx's Approach" rider doesn't make it dead, because it draws two cards first.
 *
 * Conservative on purpose: only instants and sorceries are judged (a permanent has a body or a
 * static to fall back on), and only filters made of card types and subtypes are read — a filter
 * this can't read is "met", so an unfamiliar shape never cuts a card.
 *
 * The softer question — a payoff that *works* here but has too little fuel, Blood Artist with
 * nothing to sacrifice — is [com.wingedsheep.ai.engine.knowledge.DeckSynergy]'s: such a card is
 * *underfed* rather than inert, and loses part of its value instead of its slot.
 */
internal object DeckDependencies {

    /** Matching cards a requirement needs in the rest of the deck for the card to be live. */
    const val MIN_SUPPORT = 3

    /** Whether [card] would do nothing in a deck whose other nonland cards are [deck]. */
    fun inert(card: CardDefinition, deck: List<CardDefinition>): Boolean {
        if (card.typeLine.isPermanent || card.cardFaces.isNotEmpty()) return false
        val others = deck.filter { it.name != card.name }
        fun met(filter: GameObjectFilter) = supported(filter, others)

        if (card.script.additionalCosts.any { !costPayable(it, ::met) }) return true
        val unmetTarget = card.script.targetRequirements.any { req ->
            req is TargetObject && !req.optional && req.minCount > 0 &&
                req.filter.zone == Zone.BATTLEFIELD &&
                req.filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou &&
                !met(req.filter.baseFilter)
        }
        if (unmetTarget) return true
        val effect = card.script.spellEffect ?: return false
        return state(effect, ::met) == Reach.INERT
    }

    /** How an effect fares in the deck: does something, is plumbing, or can't happen. */
    private enum class Reach { ACTIVE, NEUTRAL, INERT }

    private fun combine(parts: List<Reach>): Reach = when {
        parts.any { it == Reach.ACTIVE } -> Reach.ACTIVE
        parts.any { it == Reach.INERT } -> Reach.INERT
        else -> Reach.NEUTRAL
    }

    private fun state(effect: Effect, met: (GameObjectFilter) -> Boolean): Reach = when (effect) {
        is CompositeEffect -> if (effect.effects.isEmpty()) Reach.NEUTRAL else combine(effect.effects.map { state(it, met) })
        is GatedEffect -> gated(effect, met)
        is GatherCardsEffect -> {
            val filter = ownZoneFilter(effect.source)
            if (filter != null && !met(filter)) Reach.INERT else Reach.NEUTRAL
        }
        is SelectFromCollectionEffect, is MoveCollectionEffect, is RevealCollectionEffect,
        is ShuffleLibraryEffect, EmitLibrarySearchedEventEffect -> Reach.NEUTRAL
        else -> Reach.ACTIVE
    }

    private fun gated(effect: GatedEffect, met: (GameObjectFilter) -> Boolean): Reach {
        val otherwise = effect.otherwise?.let { state(it, met) }
        val gateBlocked = when (val gate = effect.gate) {
            is Gate.DoAction -> state(gate.action, met) == Reach.INERT
            is Gate.MayDecide -> (gate.feasibility as? FeasibilityCheck.ControlsPermanentMatching)
                ?.let { !met(it.filter) } ?: false
            else -> false
        }
        if (gateBlocked) return otherwise ?: Reach.INERT
        val action = (effect.gate as? Gate.DoAction)?.let { state(it.action, met) }
        return combine(listOfNotNull(action, state(effect.then, met), otherwise))
    }

    /** The filter of a gather over one of our own zones, or null when it reaches elsewhere. */
    private fun ownZoneFilter(source: CardSource): GameObjectFilter? = when (source) {
        is CardSource.FromZone -> source.filter.takeIf { source.player == Player.You }
        is CardSource.BattlefieldMatching -> source.filter.takeIf { source.player == Player.You }
        is CardSource.ControlledPermanents -> source.filter.takeIf { source.player == Player.You }
        else -> null
    }

    private fun costPayable(cost: AdditionalCost, met: (GameObjectFilter) -> Boolean): Boolean = when (cost) {
        is AdditionalCost.Atom -> (cost.atom as? CostAtom.Sacrifice)?.let { met(it.filter) } ?: true
        is AdditionalCost.Behold -> met(cost.filter)
        is AdditionalCost.Composite -> cost.steps.all { costPayable(it, met) }
        else -> true
    }

    /**
     * Whether [others] hold [MIN_SUPPORT] cards matching [filter]. Lands always count as supported
     * (the mana base is added after the spells), and so does any filter this can't read.
     */
    private fun supported(filter: GameObjectFilter, others: List<CardDefinition>): Boolean {
        if (!readable(filter) || mentionsLand(filter)) return true
        val printed = filter.copy(statePredicates = emptyList(), controllerPredicate = null)
        return others.count { CompanionEligibility.matchesPrinted(it, printed) } >= MIN_SUPPORT
    }

    private fun readable(filter: GameObjectFilter): Boolean =
        filter.cardPredicates.all(::readable) && filter.anyOf.all(::readable)

    private fun readable(predicate: CardPredicate): Boolean = when (predicate) {
        is CardPredicate.And -> predicate.predicates.all(::readable)
        is CardPredicate.Or -> predicate.predicates.all(::readable)
        is CardPredicate.Not -> readable(predicate.predicate)
        CardPredicate.IsCreature, CardPredicate.IsLand, CardPredicate.IsArtifact, CardPredicate.IsEnchantment,
        CardPredicate.IsPlaneswalker, CardPredicate.IsBattle, CardPredicate.IsInstant, CardPredicate.IsSorcery,
        CardPredicate.IsPermanent, CardPredicate.IsNonland, CardPredicate.IsNoncreature, CardPredicate.IsLegendary,
        CardPredicate.IsBasicLand, is CardPredicate.HasSubtype, is CardPredicate.HasAnyOfSubtypes -> true
        else -> false
    }

    private fun mentionsLand(filter: GameObjectFilter): Boolean =
        filter.cardPredicates.any(::mentionsLand) || filter.anyOf.any(::mentionsLand)

    private fun mentionsLand(predicate: CardPredicate): Boolean = when (predicate) {
        is CardPredicate.And -> predicate.predicates.any(::mentionsLand)
        is CardPredicate.Or -> predicate.predicates.any(::mentionsLand)
        is CardPredicate.Not -> false
        CardPredicate.IsLand, CardPredicate.IsBasicLand, CardPredicate.IsPermanent -> true
        is CardPredicate.HasSubtype -> predicate.subtype.value in Subtype.ALL_BASIC_LAND_TYPES
        is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes.any { it.value in Subtype.ALL_BASIC_LAND_TYPES }
        else -> false
    }
}
