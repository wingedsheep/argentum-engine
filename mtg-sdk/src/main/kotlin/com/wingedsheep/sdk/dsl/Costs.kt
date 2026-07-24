package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs.Composite
import com.wingedsheep.sdk.dsl.Costs.Mana
import com.wingedsheep.sdk.dsl.Costs.RemoveCounters
import com.wingedsheep.sdk.dsl.Costs.RemoveXCounters
import com.wingedsheep.sdk.dsl.Costs.additional.Behold
import com.wingedsheep.sdk.dsl.Costs.additional.ExileFromStorage
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
        AbilityCost.Atom(CostAtom.Mana(ManaCost.parse(cost)))

    /**
     * Pay mana cost from ManaCost object.
     */
    fun Mana(cost: ManaCost): AbilityCost =
        AbilityCost.Atom(CostAtom.Mana(cost))

    // =========================================================================
    // Life Costs
    // =========================================================================

    /**
     * Pay life.
     */
    fun PayLife(amount: Int): AbilityCost =
        AbilityCost.Atom(CostAtom.PayLife(amount))

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
        AbilityCost.Atom(CostAtom.Sacrifice(filter))

    /**
     * Sacrifice another permanent matching the filter (excludes the source permanent).
     */
    fun SacrificeAnother(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Atom(CostAtom.Sacrifice(filter, excludeSelf = true))

    /**
     * Sacrifice multiple permanents matching the filter.
     *
     * @param distinctNames when true the sacrificed permanents must all have different names
     *   ("sacrifice three artifact tokens with different names" — Transmutation Font).
     */
    fun SacrificeMultiple(
        count: Int,
        filter: GameObjectFilter = GameObjectFilter.Any,
        distinctNames: Boolean = false
    ): AbilityCost =
        AbilityCost.Atom(CostAtom.Sacrifice(filter, count = count, distinctNames = distinctNames))

    // =========================================================================
    // Discard Costs
    // =========================================================================

    /**
     * Discard a card (any card).
     */
    val DiscardCard: AbilityCost = AbilityCost.Atom(CostAtom.Discard())

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
    ): AbilityCost = AbilityCost.Atom(CostAtom.Discard(count, filter, random = atRandom))

    /**
     * Discard [count] cards chosen at random (e.g. Meteor Storm's "Discard two cards at random").
     */
    fun DiscardAtRandom(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Atom(CostAtom.Discard(count, filter, random = true))

    /**
     * Discard your entire hand.
     */
    val DiscardHand: AbilityCost = AbilityCost.DiscardHand

    /**
     * Discard this card (for cycling and similar abilities).
     */
    val DiscardSelf: AbilityCost = AbilityCost.DiscardSelf

    /**
     * Discard the specific card you drew most recently this turn (Jandor's Ring).
     * Unpayable when you haven't drawn a card this turn, or the tracked card has
     * since left your hand. The engine resolves the card automatically — no player
     * selection is required at payment time.
     */
    val DiscardLastDrawnThisTurn: AbilityCost = AbilityCost.DiscardLastDrawnThisTurn

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
     * Sacrifice the permanent that granted this activated ability (e.g., the Equipment
     * granting the ability to its equipped creature, like Deconstruction Hammer). The
     * self-sacrifice sibling of [ExileGrantingPermanent]; the granter is resolved at
     * activation time, so it sacrifices exactly the granting permanent (CR 201.5a).
     */
    val SacrificeGrantingPermanent: AbilityCost = AbilityCost.SacrificeGrantingPermanent

    /**
     * Tap the permanent that granted this activated ability — "Tap Fishing Pole" inside the
     * ability the Equipment grants its equipped creature. Compose with [Tap] when the printed cost
     * taps both the host (`{T}`) and the granter.
     */
    val TapGrantingPermanent: AbilityCost = AbilityCost.TapGrantingPermanent

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
        AbilityCost.Atom(CostAtom.ExileFrom(Zone.GRAVEYARD, filter, count))

    /**
     * Exile X cards from graveyard, where X is the ability's X value.
     */
    fun ExileXFromGraveyard(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ExileXFromGraveyard(filter)

    /**
     * Exile one or more permanents matching [filter] you control (variable count, at least
     * [minCount]); with [excludeSelf] the ability's own source is excluded ("one or more *other*
     * …"). The exiled set's **total mana value** becomes the ability's X value — read it with
     * `DynamicAmount.XValue` (e.g. to bound a reanimation target "with mana value X or less").
     * Backs Fabrication Foundry's "Exile one or more other artifacts you control with total mana
     * value X" activation cost. Pair with a sorcery-speed timing rule where the card demands it.
     */
    fun ExilePermanents(
        filter: GameObjectFilter = GameObjectFilter.Any,
        minCount: Int = 1,
        excludeSelf: Boolean = true
    ): AbilityCost = AbilityCost.Atom(CostAtom.ExilePermanents(filter, minCount, excludeSelf))

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
     * Set [excludeSelf] for "tap N other untapped … you control" (excludes the source permanent).
     */
    fun TapPermanents(
        count: Int,
        filter: GameObjectFilter = GameObjectFilter.Creature,
        excludeSelf: Boolean = false
    ): AbilityCost = AbilityCost.Atom(CostAtom.TapPermanents(count, filter, excludeSelf))

    /**
     * Tap another untapped permanent you control (e.g., "Tap another untapped permanent you control").
     * Excludes the source permanent from the tap candidates.
     * Pass `GameObjectFilter.Creature` to restrict to creatures, etc.
     */
    fun TapAnotherPermanent(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Atom(CostAtom.TapPermanents(count = 1, filter = filter, excludeSelf = true))

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
        AbilityCost.Atom(CostAtom.ReturnToHand(filter, count))

    // =========================================================================
    // Counter Removal Costs
    // =========================================================================

    /**
     * Remove a fixed number of +1/+1 counters from among permanents you control matching
     * [filter]. Use this for fixed-count costs that aren't creature-only (e.g., Iron Spider:
     * "Remove two +1/+1 counters from among artifacts you control"). Use
     * [RemoveXCounters] for player-chosen X.
     * Delegates to [RemoveCounters].
     */
    fun RemovePlusOnePlusOneCounters(filter: GameObjectFilter, count: Int): AbilityCost =
        AbilityCost.Atom(CostAtom.RemoveCounters("+1/+1", DynamicAmount.Fixed(count), filter))

    /**
     * Remove one or more counters of the specified type from this permanent.
     * Used for artifacts with charge/gem counters as activation costs.
     * Delegates to [RemoveCounters] with [self] = true.
     */
    fun RemoveCounterFromSelf(counterType: String?, count: Int = 1): AbilityCost =
        AbilityCost.Atom(CostAtom.RemoveCounters(counterType, DynamicAmount.Fixed(count), self = true))

    /**
     * Put [count] counters of [counterType] on this permanent as part of the activation cost —
     * "{T}, Put a page counter on this artifact: Scry 1" (Mazemind Tome). The accruing mirror of
     * [RemoveCounterFromSelf]; always payable, since it costs the player nothing they must have.
     */
    fun PutCounterOnSelf(counterType: String, count: Int = 1): AbilityCost =
        AbilityCost.Atom(CostAtom.PutCountersOnSelf(counterType, count))

    /**
     * Remove [count] counters of the specified [counterType] (or any type when null)
     * from among permanents matching [filter] you control. When [counterType] is null
     * (default), counters of any type may be removed in any combination.
     *
     * Examples:
     * - `Costs.RemoveCounters(count = 2, counterType = "+1/+1", filter = Filters.Artifact)`
     *   — "Remove two +1/+1 counters from among artifacts you control"
     * - `Costs.RemoveCounters(count = 3, filter = Filters.Creature)`
     *   — "Remove three counters from among creatures you control" (any type)
     */
    fun RemoveCounters(
        count: Int = 1,
        counterType: String? = null,
        filter: GameObjectFilter = GameObjectFilter.Permanent
    ): AbilityCost = AbilityCost.Atom(CostAtom.RemoveCounters(counterType, DynamicAmount.Fixed(count), filter))

    /**
     * Remove X counters of any type from among creatures you control.
     * X is the value chosen for this ability's variable cost.
     */
    fun RemoveXCounters(
            counterType: String? = null,
            count: DynamicAmount = DynamicAmount.XValue,
            filter: GameObjectFilter = GameObjectFilter.Permanent
        ): AbilityCost = AbilityCost.Atom(CostAtom.RemoveCounters(counterType, count, filter))

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
    fun Craft(filter: GameObjectFilter, minCount: Int = 1, maxCount: Int? = null): AbilityCost =
        AbilityCost.Craft(filter, minCount, maxCount)

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
            AdditionalCost.Atom(CostAtom.Sacrifice(filter, count))

        /**
         * Return [count] permanents matching [filter] you control to their owner's hand
         * (Fear of Isolation — "As an additional cost to cast this spell, return a permanent
         * you control to its owner's hand").
         */
        fun ReturnToHand(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): AdditionalCost =
            AdditionalCost.Atom(CostAtom.ReturnToHand(filter, count))

        /** Discard [count] cards matching [filter] (Force of Will). */
        fun DiscardCards(count: Int = 1, filter: GameObjectFilter = GameObjectFilter.Any): AdditionalCost =
            AdditionalCost.Atom(CostAtom.Discard(count, filter))

        /** Pay [amount] life. */
        fun PayLife(amount: Int): AdditionalCost = AdditionalCost.Atom(CostAtom.PayLife(amount))

        /** Pay [amountPerTarget] life for each target chosen by this spell (Phyrexian Purge). */
        fun PayLifePerTarget(amountPerTarget: Int): AdditionalCost =
            AdditionalCost.PayLifePerTarget(amountPerTarget)

        /**
         * Pay life equal to the cast spell's mana value — the substitute cost for "pay life equal
         * to its mana value rather than pay its mana cost" (Valgavoth, Terror Eater). Pair with a
         * play-from-exile grant whose mana cost is waived (`withoutPayingManaCost = true`).
         */
        val PayLifeEqualToManaValueOfSpell: AdditionalCost = AdditionalCost.PayLifeEqualToManaValueOfSpell

        /** Exile [count] cards matching [filter] from [fromZone]. */
        fun ExileCards(
            count: Int = 1,
            filter: GameObjectFilter = GameObjectFilter.Any,
            fromZone: CostZone = CostZone.GRAVEYARD
        ): AdditionalCost = AdditionalCost.Atom(CostAtom.ExileFrom(fromZone.toZone(), filter, count))

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

        /**
         * Cost-vs-cost — the caster pays exactly one of [options] ("discard a card **or** sacrifice a
         * permanent"; Souls of the Lost). For options that are each independently payable non-mana
         * costs; use the `*OrPay` family instead when one branch pays extra *mana*. See
         * [AdditionalCost.Choice].
         */
        fun Choice(vararg options: AdditionalCost): AdditionalCost = AdditionalCost.Choice(options.toList())

        /** Blight X — put X -1/-1 counters on a creature you control (X declared at cast time, min [minCount]). */
        fun BlightVariable(minCount: Int = 0): AdditionalCost = AdditionalCost.BlightVariable(minCount)

        /**
         * Pay X life — the caster declares X at cast time and pays X life (X declared at cast time,
         * min [minCount]). X is surfaced to the spell's effects via the resolution context's X value
         * (read by `DynamicAmount.XValue` / `CardPredicate.ManaValueAtMostX`). The card must not also
         * carry an `{X}` mana cost (they share the same X slot).
         */
        fun PayXLife(minCount: Int = 0): AdditionalCost = AdditionalCost.PayXLife(minCount)

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

        /**
         * Exile [exileCount] cards matching [filter] from your graveyard, or pay
         * [alternativeManaCost] instead (Soaring Stoneglider).
         */
        fun ExileFromGraveyardOrPay(
            exileCount: Int,
            alternativeManaCost: String,
            filter: GameObjectFilter = GameObjectFilter.Any,
        ): AdditionalCost =
            AdditionalCost.ExileFromGraveyardOrPay(exileCount, alternativeManaCost, filter)

        /**
         * Sacrifice [count] permanent(s) matching [filter] you control, or pay
         * [alternativeManaCost] instead (Louisoix's Sacrifice).
         */
        fun SacrificeOrPay(
            filter: GameObjectFilter = GameObjectFilter.Any,
            alternativeManaCost: String,
            count: Int = 1,
        ): AdditionalCost =
            AdditionalCost.SacrificeOrPay(filter, alternativeManaCost, count)

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

        /**
         * Remove [count] counters of the specified [counterType] (or any type when null)
         * from among permanents matching [filter] you control, as an additional cost to
         * cast a spell.
         */
        fun RemoveCounters(
            count: Int = 1,
            counterType: String? = null,
            filter: GameObjectFilter = GameObjectFilter.Permanent
        ): AdditionalCost = AdditionalCost.Atom(CostAtom.RemoveCounters(counterType, DynamicAmount.Fixed(count), filter))

        /** Tap [count] untapped permanents matching [filter] you control. */
        fun TapPermanents(count: Int = 1, filter: GameObjectFilter = GameObjectFilter.Creature): AdditionalCost =
            AdditionalCost.Atom(CostAtom.TapPermanents(count, filter))

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
        fun Mana(cost: ManaCost): PayCost = PayCost.Atom(CostAtom.Mana(cost))

        /** Pay a mana cost parsed from a string (e.g. "{2}{U}"). */
        fun Mana(cost: String): PayCost = PayCost.Atom(CostAtom.Mana(ManaCost.parse(cost)))

        /** Pay the source permanent's own mana cost (Essence Leak). */
        val OwnManaCost: PayCost = PayCost.OwnManaCost

        /** Discard [count] cards matching [filter] (optionally [random]). */
        fun Discard(
            filter: GameObjectFilter = GameObjectFilter.Any,
            count: Int = 1,
            random: Boolean = false
        ): PayCost = PayCost.Atom(CostAtom.Discard(count, filter, random))

        /** Sacrifice [count] permanents matching [filter]. */
        fun Sacrifice(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.Sacrifice(filter, count))

        /** Pay [amount] life. */
        fun PayLife(amount: Int): PayCost = PayCost.Atom(CostAtom.PayLife(amount))

        /** Exile [count] cards matching [filter] from [zone]. */
        fun Exile(
            filter: GameObjectFilter = GameObjectFilter.Any,
            zone: Zone = Zone.HAND,
            count: Int = 1
        ): PayCost = PayCost.Atom(CostAtom.ExileFrom(zone, filter, count))

        /** Reveal [count] cards matching [filter] from hand. */
        fun RevealCard(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.RevealFromHand(filter, count))

        /** Choose one of [options] to pay. */
        fun Choice(options: List<PayCost>): PayCost = PayCost.Choice(options)

        /** Return [count] permanents matching [filter] you control to their owner's hand. */
        fun ReturnToHand(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.ReturnToHand(filter, count))

        /** Tap [count] untapped permanents matching [filter] you control. */
        fun Tap(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.TapPermanents(count, filter))

        /**
         * Remove [count] counters of the specified [counterType] (or any type when null)
         * from among permanents matching [filter] you control.
         */
        fun RemoveCounters(
            count: Int = 1,
            counterType: String? = null,
            filter: GameObjectFilter = GameObjectFilter.Permanent
        ): PayCost = PayCost.Atom(CostAtom.RemoveCounters(counterType, DynamicAmount.Fixed(count), filter))
    }
}
