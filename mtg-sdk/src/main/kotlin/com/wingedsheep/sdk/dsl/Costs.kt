package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * Facade object providing convenient access to cost types.
 *
 * Three cost families live here behind a single namespace:
 * - [Costs] top-level members wrap [AbilityCost] (activated-ability costs).
 * - [Costs.additional] wraps [AdditionalCost] (extra costs paid while casting a spell).
 * - [Costs.pay] wraps [PayCost] (payable costs for "unless you …" / morph / choice mechanics).
 *
 * Usage:
 * ```kotlin
 * Costs.Tap
 * Costs.Mana("{2}")
 * Costs.Sacrifice(Filters.Creature)
 * Costs.Composite(Costs.Tap, Costs.Sacrifice(Filters.Self))
 * Costs.additional.SacrificePermanent(Filters.Creature)
 * Costs.pay.PayLife(3)
 * ```
 */
object Costs {

    // =========================================================================
    // Simple Costs
    // =========================================================================

    /**
     * No cost ({0}) — the ability is free to activate.
     */
    val Free: AbilityCost = AbilityCost.Free

    /**
     * Tap this permanent ({T}).
     */
    val Tap: AbilityCost = AbilityCost.Tap

    /**
     * Untap this permanent ({Q}).
     */
    val Untap: AbilityCost = AbilityCost.Untap

    // =========================================================================
    // Mana Costs
    // =========================================================================

    /**
     * Pay mana cost from string (e.g., "{2}{B}").
     */
    fun Mana(cost: String): AbilityCost =
        AbilityCost.Mana(ManaCost.parse(cost))

    /**
     * Pay mana cost from ManaCost object.
     */
    fun Mana(cost: ManaCost): AbilityCost =
        AbilityCost.Mana(cost)

    // =========================================================================
    // Life Costs
    // =========================================================================

    /**
     * Pay life.
     */
    fun PayLife(amount: Int): AbilityCost =
        AbilityCost.PayLife(amount)

    /**
     * Pay X life, where X is the value chosen for the ability's `{X}` mana cost
     * (e.g. "{X}{B}, {T}, Pay X life: ..." on Krumar Initiate).
     */
    val PayXLife: AbilityCost = AbilityCost.PayXLife

    // =========================================================================
    // Sacrifice Costs
    // =========================================================================

    /**
     * Sacrifice a permanent matching the filter.
     */
    fun Sacrifice(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Sacrifice(filter)

    /**
     * Sacrifice another permanent matching the filter (excludes the source permanent).
     */
    fun SacrificeAnother(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Sacrifice(filter, excludeSelf = true)

    /**
     * Sacrifice multiple permanents matching the filter.
     */
    fun SacrificeMultiple(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Sacrifice(filter, count = count)

    // =========================================================================
    // Discard Costs
    // =========================================================================

    /**
     * Discard a card (any card).
     */
    val DiscardCard: AbilityCost = AbilityCost.Discard()

    /**
     * Discard one or more cards matching the filter.
     *
     * @param count how many cards to discard
     * @param atRandom when true, the engine picks the discarded cards at random (no player choice)
     */
    fun Discard(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        atRandom: Boolean = false
    ): AbilityCost = AbilityCost.Discard(filter, count, atRandom)

    /**
     * Discard [count] cards chosen at random (e.g. Meteor Storm's "Discard two cards at random").
     */
    fun DiscardAtRandom(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Discard(filter, count, atRandom = true)

    /**
     * Discard your entire hand.
     */
    val DiscardHand: AbilityCost = AbilityCost.DiscardHand

    /**
     * Discard this card (for cycling and similar abilities).
     */
    val DiscardSelf: AbilityCost = AbilityCost.DiscardSelf

    /**
     * Sacrifice this permanent (for abilities that sacrifice themselves as cost).
     */
    val SacrificeSelf: AbilityCost = AbilityCost.SacrificeSelf

    /**
     * Exile this creature (for abilities that exile themselves as cost).
     */
    val ExileSelf: AbilityCost = AbilityCost.ExileSelf

    /**
     * Exile the permanent that granted this activated ability (e.g., the equipment
     * granting the ability to its equipped creature, like The Dominion Bracelet).
     */
    val ExileGrantingPermanent: AbilityCost = AbilityCost.ExileGrantingPermanent

    /**
     * Sacrifice a creature of the type chosen when this permanent entered the battlefield.
     * Used by cards like Doom Cannon.
     */
    val SacrificeChosenCreatureType: AbilityCost = AbilityCost.SacrificeChosenCreatureType

    /**
     * Tap the creature this aura is attached to.
     * Used by auras that grant activated abilities to enchanted creature (e.g., Lavamancer's Skill).
     */
    val TapAttachedCreature: AbilityCost = AbilityCost.TapAttachedCreature

    // =========================================================================
    // Exile Costs
    // =========================================================================

    /**
     * Exile cards from graveyard.
     */
    fun ExileFromGraveyard(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ExileFromGraveyard(count, filter)

    /**
     * Exile X cards from graveyard, where X is the ability's X value.
     */
    fun ExileXFromGraveyard(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ExileXFromGraveyard(filter)

    // =========================================================================
    // Loyalty Costs
    // =========================================================================

    /**
     * Add loyalty counters (positive) or remove them (negative).
     */
    fun Loyalty(change: Int): AbilityCost =
        AbilityCost.Loyalty(change)

    // =========================================================================
    // Tap Permanents Costs
    // =========================================================================

    /**
     * Tap permanents you control (e.g., "Tap five untapped Clerics you control").
     */
    fun TapPermanents(count: Int, filter: GameObjectFilter = GameObjectFilter.Creature): AbilityCost =
        AbilityCost.TapPermanents(count, filter)

    /**
     * Tap another untapped permanent you control (e.g., "Tap another untapped permanent you control").
     * Excludes the source permanent from the tap candidates.
     * Pass `GameObjectFilter.Creature` to restrict to creatures, etc.
     */
    fun TapAnotherPermanent(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.TapPermanents(count = 1, filter = filter, excludeSelf = true)

    /**
     * Tap X permanents you control, where X is the ability's chosen X value.
     * Example: "Tap X untapped Knights you control" for Aryel, Knight of Windgrace.
     */
    fun TapXPermanents(filter: GameObjectFilter = GameObjectFilter.Creature): AbilityCost =
        AbilityCost.TapXPermanents(filter)

    // =========================================================================
    // Return to Hand Costs
    // =========================================================================

    /**
     * Return a permanent you control matching the filter to its owner's hand.
     */
    fun ReturnToHand(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): AbilityCost =
        AbilityCost.ReturnToHand(filter, count)

    // =========================================================================
    // Counter Removal Costs
    // =========================================================================

    /**
     * Remove X +1/+1 counters from among creatures you control.
     * X is chosen by the player; the engine auto-distributes counter removal.
     */
    val RemoveXPlusOnePlusOneCounters: AbilityCost = AbilityCost.RemoveXPlusOnePlusOneCounters

    /**
     * Remove a fixed number of +1/+1 counters from among permanents you control matching
     * [filter]. Use this for fixed-count costs that aren't creature-only (e.g., Iron Spider:
     * "Remove two +1/+1 counters from among artifacts you control"). Use
     * [RemoveXPlusOnePlusOneCounters] for player-chosen X.
     */
    fun RemovePlusOnePlusOneCounters(filter: GameObjectFilter, count: Int): AbilityCost =
        AbilityCost.RemovePlusOnePlusOneCounters(filter, count)

    /**
     * Remove one or more counters of the specified type from this permanent.
     * Used for artifacts with charge/gem counters as activation costs.
     */
    fun RemoveCounterFromSelf(counterType: String, count: Int = 1): AbilityCost =
        AbilityCost.RemoveCounterFromSelf(counterType, count)

    // =========================================================================
    // Composite Costs
    // =========================================================================

    /**
     * Combine multiple costs.
     */
    fun Composite(vararg costs: AbilityCost): AbilityCost =
        AbilityCost.Composite(costs.toList())

    /**
     * Combine multiple costs from a list.
     */
    fun Composite(costs: List<AbilityCost>): AbilityCost =
        AbilityCost.Composite(costs)

    /**
     * Forage: exile three cards from your graveyard or sacrifice a Food.
     */
    fun Forage(): AbilityCost = AbilityCost.Forage

    /**
     * Blight N: put N -1/-1 counters on a creature you control.
     * Used as part of an activated ability cost (e.g., "{T}, Blight 1: ...").
     */
    fun Blight(amount: Int): AbilityCost = AbilityCost.Blight(amount)

    /**
     * Craft (CR 702.167a) — the "Exile this permanent, Exile [filter] from among permanents
     * you control and/or [filter] cards from your graveyard" portion of the Craft activated
     * ability. Combine with [Mana] inside [Composite] to express the full cost shape:
     *
     * ```kotlin
     * Costs.Composite(Costs.Mana("{4}{R}"), Costs.Craft(Filters.Dinosaur))
     * ```
     */
    fun Craft(filter: GameObjectFilter, minCount: Int = 1): AbilityCost =
        AbilityCost.Craft(filter, minCount)

    // =========================================================================
    // Additional Costs (paid while casting a spell) — wraps AdditionalCost
    // =========================================================================

    /**
     * Facade for [AdditionalCost] — extra costs declared on a spell and paid as it is cast
     * (sacrifice, discard, pay life, behold, blight, …). Prefer these factories over raw
     * `AdditionalCost.*` construction so the underlying type can evolve behind the facade.
     */
    object additional {

        /** Sacrifice [count] permanents matching [filter] (Natural Order). */
        fun SacrificePermanent(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): AdditionalCost =
            AdditionalCost.SacrificePermanent(filter, count)

        /** Discard [count] cards matching [filter] (Force of Will). */
        fun DiscardCards(count: Int = 1, filter: GameObjectFilter = GameObjectFilter.Any): AdditionalCost =
            AdditionalCost.DiscardCards(count, filter)

        /** Pay [amount] life. */
        fun PayLife(amount: Int): AdditionalCost = AdditionalCost.PayLife(amount)

        /** Pay [amountPerTarget] life for each target chosen by this spell (Phyrexian Purge). */
        fun PayLifePerTarget(amountPerTarget: Int): AdditionalCost =
            AdditionalCost.PayLifePerTarget(amountPerTarget)

        /** Exile [count] cards matching [filter] from [fromZone]. */
        fun ExileCards(
            count: Int = 1,
            filter: GameObjectFilter = GameObjectFilter.Any,
            fromZone: CostZone = CostZone.GRAVEYARD
        ): AdditionalCost = AdditionalCost.ExileCards(count, filter, fromZone)

        /** Exile a variable number (at least [minCount]) of cards matching [filter] from [fromZone] (Chill Haunting). */
        fun ExileVariableCards(
            minCount: Int = 1,
            filter: GameObjectFilter = GameObjectFilter.Any,
            fromZone: CostZone = CostZone.GRAVEYARD
        ): AdditionalCost = AdditionalCost.ExileVariableCards(minCount, filter, fromZone)

        /** Sacrifice any number of [filter] permanents, each reducing generic cost by [costReductionPerCreature]. */
        fun SacrificeCreaturesForCostReduction(
            filter: GameObjectFilter = GameObjectFilter.Creature,
            costReductionPerCreature: Int = 2
        ): AdditionalCost = AdditionalCost.SacrificeCreaturesForCostReduction(filter, costReductionPerCreature)

        /** Forage (exile three cards from your graveyard or sacrifice a Food). */
        val Forage: AdditionalCost = AdditionalCost.Forage

        /** Blight X — put X -1/-1 counters on a creature you control (X declared at cast time, min [minCount]). */
        fun BlightVariable(minCount: Int = 0): AdditionalCost = AdditionalCost.BlightVariable(minCount)

        /** Blight [blightAmount] or pay [alternativeManaCost] instead. */
        fun BlightOrPay(blightAmount: Int, alternativeManaCost: String): AdditionalCost =
            AdditionalCost.BlightOrPay(blightAmount, alternativeManaCost)

        /** Behold [count] cards matching [filter], recording them under [storeAs]. */
        fun Behold(
            filter: GameObjectFilter = GameObjectFilter.Any,
            count: Int = 1,
            storeAs: String = "beheld"
        ): AdditionalCost = AdditionalCost.Behold(filter, count, storeAs)

        /** Behold a [filter] card or pay [alternativeManaCost] instead. */
        fun BeholdOrPay(
            filter: GameObjectFilter = GameObjectFilter.Any,
            alternativeManaCost: String,
            storeAs: String = "beheld"
        ): AdditionalCost = AdditionalCost.BeholdOrPay(filter, alternativeManaCost, storeAs)

        /** "Behold a [filter] and exile it" — [Behold] + [ExileFromStorage] composed. */
        fun BeholdAndExile(
            filter: GameObjectFilter,
            count: Int = 1,
            storeAs: String = "beheld"
        ): AdditionalCost = AdditionalCost.BeholdAndExile(filter, count, storeAs)

        /** Exile cards from pipeline storage key [from], optionally linking them to the source. */
        fun ExileFromStorage(from: String, linkToSource: Boolean = false): AdditionalCost =
            AdditionalCost.ExileFromStorage(from, linkToSource)

        /** Group multiple additional costs into one logical cost (steps run in order). */
        fun Composite(steps: List<AdditionalCost>): AdditionalCost = AdditionalCost.Composite(steps)

        /** Remove [totalCount] counters from among creatures you control (Dawnhand Dissident). */
        fun RemoveCountersFromYourCreatures(totalCount: Int): AdditionalCost =
            AdditionalCost.RemoveCountersFromYourCreatures(totalCount)

        /** Tap [count] untapped permanents matching [filter] you control. */
        fun TapPermanents(count: Int = 1, filter: GameObjectFilter = GameObjectFilter.Creature): AdditionalCost =
            AdditionalCost.TapPermanents(count, filter)

        /** Choose one entity across [zoneFilters] without moving it, recording it under [storeAs] (Close Encounter). */
        fun ChooseEntity(
            zoneFilters: Map<Zone, GameObjectFilter> = mapOf(Zone.BATTLEFIELD to GameObjectFilter.Any),
            storeAs: String = "chosen",
            captureSnapshot: Boolean = false,
            descriptionOverride: String? = null
        ): AdditionalCost = AdditionalCost.ChooseEntity(zoneFilters, storeAs, captureSnapshot, descriptionOverride)
    }

    // =========================================================================
    // Payable Costs ("unless you …", morph, choice) — wraps PayCost
    // =========================================================================

    /**
     * Facade for [PayCost] — a payable cost used by morph face-up costs, "unless you …"
     * mechanics, and player-choice costs. Prefer these factories over raw `PayCost.*`.
     */
    object pay {

        /** Pay a mana cost. */
        fun Mana(cost: ManaCost): PayCost = PayCost.Mana(cost)

        /** Pay a mana cost parsed from a string (e.g. "{2}{U}"). */
        fun Mana(cost: String): PayCost = PayCost.Mana(ManaCost.parse(cost))

        /** Pay the source permanent's own mana cost (Essence Leak). */
        val OwnManaCost: PayCost = PayCost.OwnManaCost

        /** Discard [count] cards matching [filter] (optionally [random]). */
        fun Discard(
            filter: GameObjectFilter = GameObjectFilter.Any,
            count: Int = 1,
            random: Boolean = false
        ): PayCost = PayCost.Discard(filter, count, random)

        /** Sacrifice [count] permanents matching [filter]. */
        fun Sacrifice(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Sacrifice(filter, count)

        /** Pay [amount] life. */
        fun PayLife(amount: Int): PayCost = PayCost.PayLife(amount)

        /** Exile [count] cards matching [filter] from [zone]. */
        fun Exile(
            filter: GameObjectFilter = GameObjectFilter.Any,
            zone: Zone = Zone.HAND,
            count: Int = 1
        ): PayCost = PayCost.Exile(filter, zone, count)

        /** Reveal [count] cards matching [filter] from hand. */
        fun RevealCard(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.RevealCard(filter, count)

        /** Choose one of [options] to pay. */
        fun Choice(options: List<PayCost>): PayCost = PayCost.Choice(options)

        /** Return [count] permanents matching [filter] you control to their owner's hand. */
        fun ReturnToHand(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.ReturnToHand(filter, count)

        /** Tap [count] untapped permanents matching [filter] you control. */
        fun Tap(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Tap(filter, count)
    }
}
