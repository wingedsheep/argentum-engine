package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
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
import com.wingedsheep.sdk.scripting.costs.CardMeasure
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure
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

    /**
     * Exert this permanent (CR 701.43a) — it won't untap during your next untap step.
     */
    val Exert: AbilityCost = AbilityCost.Exert

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

    // =========================================================================
    // Mill Costs
    // =========================================================================

    /**
     * Mill a card as a cost — "{T}, Mill a card: Add {C}" (Deranged Assistant).
     *
     * Unpayable with an empty library (CR 701.17b), and takes no player selection: the milled card
     * is the top of the library.
     */
    val MillCard: AbilityCost = AbilityCost.Atom(CostAtom.Mill())

    /**
     * Mill [count] cards as a cost. Unpayable unless the library holds at least [count] cards
     * (CR 701.17b).
     */
    fun Mill(count: Int): AbilityCost = AbilityCost.Atom(CostAtom.Mill(count))

    /**
     * Exile the top [count] cards of your library as a cost — "{R}, Exile the top ten cards of
     * your library" (Arc-Slogger).
     *
     * The exile twin of [Mill]: no player selection (the cards are the top of the library), and
     * unpayable unless the library holds at least [count] cards (CR 118.3). Not to be confused
     * with [ExileFromGraveyard]-style *chosen*-card costs, which mean "choose N", not "the top N".
     */
    fun ExileTopOfLibrary(count: Int): AbilityCost =
        AbilityCost.Atom(CostAtom.ExileTopOfLibrary(count))

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
     * Return this permanent to its owner's hand (for abilities that bounce themselves as cost —
     * Maze's End). No player selection: the source is the only thing that moves. Contrast
     * [ReturnToHand], the choose-a-permanent-you-control bounce cost, which excludes the source.
     */
    val ReturnSelfToHand: AbilityCost = AbilityCost.ReturnSelfToHand

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

    /**
     * "Reveal the creature type you chose" — publish the secret creature type this permanent's
     * controller noted with `Effects.SecretlyChooseCreatureType(...)`, and hand it to the ability's
     * effect as `chosenValues["chosenCreatureType"]` (A Killer Among Us). Only the player who made
     * the note can pay it; see [CostAtom.RevealNotedCreatureType].
     */
    val RevealNotedCreatureType: AbilityCost = AbilityCost.Atom(CostAtom.RevealNotedCreatureType)

    // =========================================================================
    // Exile Costs
    // =========================================================================

    /**
     * Exile cards from graveyard.
     */
    fun ExileFromGraveyard(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Atom(CostAtom.ExileFrom(Zone.GRAVEYARD, filter, count))

    /**
     * Exile [count] cards matching [filter] from a *single* graveyard — any player's, but all
     * from the same one (Night Soil). The pool is every graveyard; the constraint is that the
     * chosen cards share an owner.
     */
    fun ExileFromSingleGraveyard(count: Int, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Atom(
            CostAtom.ExileFrom(Zone.GRAVEYARD, filter, count, anyPlayersZone = true, singleZone = true)
        )

    /**
     * Exile exactly [count] permanents matching [filter] from the battlefield as a cost —
     * "Exile a creature you control:" (City of Shadows).
     *
     * The fixed-count sibling of [ExilePermanents], which is variable-count and derives the
     * ability's X from what was exiled. Use this one whenever the card names a specific number and
     * nothing downstream reads an X.
     *
     * Pass a controller-scoped [filter] (`.youControl()`): the battlefield zone map is keyed by
     * **owner**, so the filter is what actually enforces "you control".
     */
    fun ExilePermanentsFixed(count: Int = 1, filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.Atom(CostAtom.ExileFrom(Zone.BATTLEFIELD, filter, count))

    /**
     * Exile X cards from graveyard, where X is the ability's X value.
     */
    fun ExileXFromGraveyard(filter: GameObjectFilter = GameObjectFilter.Any): AbilityCost =
        AbilityCost.ExileXFromGraveyard(filter)

    /**
     * Collect evidence [amount] (CR 701.59) — exile any number of cards from your graveyard with
     * total mana value [amount] or greater. Unlike [ExileFromGraveyard] the constraint is a floor on
     * the exiled cards' **total mana value**, not on their count. Per CR 701.59b the ability is not
     * activatable at all when the graveyard can't reach [amount].
     *
     * For the *linked* cast-time shape ("you may collect evidence N" + "if evidence was collected")
     * use the `collectEvidence()` DSL helper on [CardBuilder] instead — it rides the
     * optional-additional-cost rail so the declaration is observable.
     *
     * Set [linkToSource] when a later ability on the same permanent reads the cards this payment
     * exiled — "cards exiled with it" (Kylox's Voltstrider). They then land in the source's
     * linked-exile pile, which [com.wingedsheep.sdk.scripting.effects.CardSource.FromLinkedExile]
     * gathers back.
     */
    fun CollectEvidence(amount: Int, linkToSource: Boolean = false): AbilityCost =
        AbilityCost.Atom(CostAtom.CollectEvidence(amount, linkToSource))

    /**
     * Exile any number of cards matching [filter] from your graveyard whose summed [measure]
     * reaches [minTotal] — the unnamed, filtered generalization of [CollectEvidence]'s shape.
     *
     * Baron Helmut Zemo's boast cost ("Exile any number of black cards from your graveyard with
     * fifteen or more black mana symbols among their mana costs") is
     * `ExileFromGraveyardForTotal(15, CardMeasure.ColoredManaSymbols(listOf(Color.BLACK)),
     * Filters.Unified.withColor(Color.BLACK))`; prefer [ExileFromGraveyardForColoredSymbols], which
     * derives the colour filter from the same colours it counts.
     *
     * Like collect evidence, the count is free and the *sum* is the constraint, and the cost fails
     * closed: an ability whose graveyard can't reach [minTotal] isn't offered at all.
     */
    fun ExileFromGraveyardForTotal(
        minTotal: Int,
        measure: CardMeasure,
        filter: GameObjectFilter = GameObjectFilter.Any,
    ): AbilityCost = AbilityCost.Atom(
        CostAtom.ExileFromGraveyardForTotal(filter = filter, measure = measure, minTotal = minTotal)
    )

    /**
     * "Exile any number of [color] cards from your graveyard with [minSymbols] or more [color] mana
     * symbols among their mana costs" (Baron Helmut Zemo) — [ExileFromGraveyardForTotal] with the
     * pip measure and the matching colour filter derived from one list of colours, so the two can't
     * drift apart.
     *
     * The colour filter and the pip count are *not* redundant: colour is a characteristic while the
     * count reads printed pips, so the filter is what keeps a black card with no black pips (and a
     * blue card with a `{B/U}` pip) on the right side of the printed wording.
     */
    fun ExileFromGraveyardForColoredSymbols(
        minSymbols: Int,
        vararg colors: Color,
    ): AbilityCost {
        require(colors.isNotEmpty()) { "ExileFromGraveyardForColoredSymbols needs at least one color" }
        // `withAnyColor` (union), matching `ManaCost.coloredSymbolCount`'s union over the same
        // colours: a card contributing a pip of *any* requested colour is one the cost may spend.
        val colorFilter = GameObjectFilter.Any.withAnyColor(*colors)
        return ExileFromGraveyardForTotal(
            minTotal = minSymbols,
            measure = CardMeasure.ColoredManaSymbols(colors.toList()),
            filter = colorFilter,
        )
    }

    /**
     * Exile one or more permanents matching [filter] you control (variable count, at least
     * [minCount]); with [excludeSelf] the ability's own source is excluded ("one or more *other*
     * …"). By default the exiled set's **total mana value** becomes the ability's X value — read it
     * with `DynamicAmount.XValue` (e.g. to bound a reanimation target "with mana value X or less");
     * pass [xMeasure] `COUNT` for the "for each permanent exiled this way" shape instead.
     * Backs Fabrication Foundry's "Exile one or more other artifacts you control with total mana
     * value X" activation cost. Pair with a sorcery-speed timing rule where the card demands it.
     */
    fun ExilePermanents(
        filter: GameObjectFilter = GameObjectFilter.Any,
        minCount: Int = 1,
        excludeSelf: Boolean = true,
        xMeasure: VariableCostMeasure = VariableCostMeasure.TOTAL_MANA_VALUE,
        minMeasure: Int = 0
    ): AbilityCost = AbilityCost.Atom(
        CostAtom.VariablePermanents(
            filter, minCount, excludeSelf, PermanentCostAction.EXILE, xMeasure, minMeasure
        )
    )

    /**
     * Sacrifice one or more permanents matching [filter] you control (variable count, at least
     * [minCount]) — the sacrificing twin of [ExilePermanents]. The number sacrificed becomes the
     * ability's X value by default, so the resolving effect can scale with it via
     * `DynamicAmount.XValue` ("for each artifact sacrificed this way" — Radiant Lotus).
     *
     * [excludeSelf] defaults to false: the ability's own source is usually a legal choice for its
     * own cost (Radiant Lotus is an artifact and may sacrifice itself). Pass [xMeasure]
     * `TOTAL_MANA_VALUE` for the "with total mana value X" shape.
     */
    fun SacrificePermanents(
        filter: GameObjectFilter = GameObjectFilter.Any,
        minCount: Int = 1,
        excludeSelf: Boolean = false,
        xMeasure: VariableCostMeasure = VariableCostMeasure.COUNT,
        minMeasure: Int = 0
    ): AbilityCost = AbilityCost.Atom(
        CostAtom.VariablePermanents(
            filter, minCount, excludeSelf, PermanentCostAction.SACRIFICE, xMeasure, minMeasure
        )
    )

    /**
     * Tap one or more untapped permanents matching [filter] you control (variable count, at least
     * [minCount]) — the tapping twin of [ExilePermanents] and [SacrificePermanents], and the third
     * value of [PermanentCostAction] the other two already name.
     *
     * Tapping this way is a cost rather than the `{T}` symbol, so summoning sickness (CR 302.6)
     * does not apply and only untapped permanents may be chosen (CR 701.26a). Pass [minMeasure]
     * with [VariableCostMeasure.TOTAL_POWER] for Mossbridge Troll's "tap any number of untapped
     * creatures you control other than this creature with total power 10 or greater"; the
     * additional-cost twin of that shape is [Additional.TapForTotalPower].
     */
    fun TapPermanentsVariable(
        filter: GameObjectFilter = GameObjectFilter.Creature,
        minCount: Int = 1,
        excludeSelf: Boolean = false,
        xMeasure: VariableCostMeasure = VariableCostMeasure.COUNT,
        minMeasure: Int = 0
    ): AbilityCost = AbilityCost.Atom(
        CostAtom.VariablePermanents(
            filter, minCount, excludeSelf, PermanentCostAction.TAP, xMeasure, minMeasure
        )
    )

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
     *
     * Pass [self] for "remove any number of counters from ~" (The Astonishing Ant-Man), where the
     * counters come off the ability's own source. That takes the direct payment path; the default
     * filter-based form instead asks the player to distribute the removal across matching
     * permanents, which is wrong — and unpayable — for a self-scoped cost.
     */
    fun RemoveXCounters(
            counterType: String? = null,
            count: DynamicAmount = DynamicAmount.XValue,
            filter: GameObjectFilter = GameObjectFilter.Permanent,
            self: Boolean = false
        ): AbilityCost = AbilityCost.Atom(CostAtom.RemoveCounters(counterType, count, filter, self))

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
         * Tap any number of permanents matching [filter] you control whose **total projected
         * power** is [totalPower] or more — the "tap creatures for total power N" selection crew
         * and saddle already use, re-exposed as a spell's additional cost (Teamwork N,
         * CR 702.194a). The count is free; the power floor is the constraint.
         *
         * Tapping this way is a cost, not the `{T}` symbol, so summoning sickness (CR 302.6)
         * doesn't apply; only untapped permanents may be chosen (CR 701.26a).
         */
        fun TapForTotalPower(
            totalPower: Int,
            filter: GameObjectFilter = GameObjectFilter.Creature
        ): AdditionalCost = AdditionalCost.Atom(
            CostAtom.VariablePermanents(
                filter = filter,
                minCount = 0,
                excludeSelf = false,
                action = PermanentCostAction.TAP,
                xMeasure = VariableCostMeasure.TOTAL_POWER,
                minMeasure = totalPower
            )
        )

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
         * Collect evidence [amount] (CR 701.59a) — exile any number of cards from your graveyard
         * with total mana value [amount] or greater, as an additional cost to cast.
         *
         * This is the **mandatory** shape (Urgent Necropsy's "collect evidence X"). For the far more
         * common *optional linked* shape — "you may collect evidence N", read back by
         * `Conditions.WasEvidenceCollected` — use the `collectEvidence()` DSL helper on
         * [CardBuilder], which wraps this on the optional-additional-cost rail.
         */
        fun CollectEvidence(amount: Int): AdditionalCost =
            AdditionalCost.Atom(CostAtom.CollectEvidence(amount))

        /**
         * "Collect evidence X, where X is the total mana value of the permanents this spell
         * targets" — Urgent Necropsy, the one printed collect-evidence cost whose threshold is
         * derived rather than literal.
         *
         * X is determined once the targets are announced and before the cost is paid (CR 601.2c →
         * 601.2f → 601.2h), and the ruling that follows from CR 701.59b is that a caster who
         * cannot exile that much **can't choose to collect evidence at all** — so a target set the
         * graveyard can't pay for is an illegal cast (CR 601.2e), not a discounted one. The client
         * therefore runs its evidence picker *after* targeting for this cost, priced on what was
         * actually chosen.
         */
        val CollectEvidenceForTargetsTotalManaValue: AdditionalCost =
            AdditionalCost.Atom(CostAtom.CollectEvidence(CostAtom.CollectEvidence.TARGET_SUM))

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

        /**
         * Pay [cost] or pay [alternativeManaCost] instead — the general "do X or pay {N}" shape
         * ([AdditionalCost.OrPay]). [cost] must be a selection-carrying cost: a [Behold], or an
         * atom cost over sacrifice / discard / exile-from-a-zone / tap / return-to-hand. The named
         * shapes below are the printed wordings, and a new one is a one-line facade over this.
         */
        fun OrPay(cost: AdditionalCost, alternativeManaCost: String): AdditionalCost =
            AdditionalCost.OrPay(cost, alternativeManaCost)

        /** Behold a [filter] card or pay [alternativeManaCost] instead (Lys Alana Dignitary). */
        fun BeholdOrPay(
            filter: GameObjectFilter = GameObjectFilter.Any,
            alternativeManaCost: String,
            storeAs: String = "beheld"
        ): AdditionalCost = OrPay(
            AdditionalCost.Behold(filter, count = 1, storeAs = storeAs),
            alternativeManaCost
        )

        /**
         * Exile [exileCount] cards matching [filter] from your graveyard, or pay
         * [alternativeManaCost] instead (Soaring Stoneglider).
         */
        fun ExileFromGraveyardOrPay(
            exileCount: Int,
            alternativeManaCost: String,
            filter: GameObjectFilter = GameObjectFilter.Any,
        ): AdditionalCost = OrPay(
            AdditionalCost.Atom(CostAtom.ExileFrom(Zone.GRAVEYARD, filter, exileCount)),
            alternativeManaCost
        )

        /**
         * Sacrifice [count] permanent(s) matching [filter] you control, or pay
         * [alternativeManaCost] instead (Louisoix's Sacrifice).
         */
        fun SacrificeOrPay(
            filter: GameObjectFilter = GameObjectFilter.Any,
            alternativeManaCost: String,
            count: Int = 1,
        ): AdditionalCost = OrPay(
            AdditionalCost.Atom(CostAtom.Sacrifice(filter, count)),
            alternativeManaCost
        )

        /**
         * Discard [count] card(s) matching [filter] from your hand, or pay
         * [alternativeManaCost] instead (Pumpkin Bombardment — "discard a card or pay {2}").
         */
        fun DiscardOrPay(
            alternativeManaCost: String,
            filter: GameObjectFilter = GameObjectFilter.Any,
            count: Int = 1,
        ): AdditionalCost = OrPay(
            AdditionalCost.Atom(CostAtom.Discard(count, filter)),
            alternativeManaCost
        )

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

        /**
         * Any shared payable thing, lifted into this context — the generic wrapper the other two
         * cost contexts already publish (`AbilityCost.Atom`, `AdditionalCost.Atom`). Reach for a
         * named factory below where one exists; this is what a caller holding a [CostAtom] it did
         * not construct itself needs, and what keeps [CostAtom]'s "one cost language" reachable
         * from "unless you …" without a factory per atom.
         */
        fun Atom(atom: CostAtom): PayCost = PayCost.Atom(atom)

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

        /**
         * Sacrifice [count] permanents matching [filter]. The source is a legal choice when it
         * matches — "sacrifice it unless you sacrifice an artifact" on an artifact creature lets
         * you name the creature itself. Use [SacrificeAnother] for a printed "another".
         */
        fun Sacrifice(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.Sacrifice(filter, count))

        /** Sacrifice [count] permanents matching [filter] **other than the source** ("another"). */
        fun SacrificeAnother(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.Sacrifice(filter, count, excludeSelf = true))

        /** Pay [amount] life. */
        fun PayLife(amount: Int): PayCost = PayCost.Atom(CostAtom.PayLife(amount))

        /**
         * Pay life equal to a value computed when the cost is offered — "unless they pay life equal
         * to its mana value" (Wand of Ith). PayOrSuffer only; see [PayCost.DynamicLife].
         */
        fun PayDynamicLife(amount: DynamicAmount): PayCost = PayCost.DynamicLife(amount)

        /**
         * Put [count] counters of [counterType] on a permanent matching [filter] the payer
         * controls — Tourach's Chant's "unless they put a -1/-1 counter on a creature they
         * control". Unpayable when they control no matching permanent.
         */
        fun PutCountersOnPermanent(
            counterType: String,
            count: Int = 1,
            filter: GameObjectFilter = GameObjectFilter.Permanent
        ): PayCost = PayCost.Atom(CostAtom.PutCountersOnPermanent(counterType, count, filter))

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

        /**
         * Tap [count] untapped permanents matching [filter] you control. The source is a legal
         * choice when it matches and is untapped — Public Thoroughfare's and Command Bridge's
         * rulings both allow tapping the land itself. Use [TapAnother] for a printed "another".
         */
        fun Tap(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.TapPermanents(count, filter))

        /** Tap [count] untapped permanents matching [filter] **other than the source** ("another"). */
        fun TapAnother(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1): PayCost =
            PayCost.Atom(CostAtom.TapPermanents(count, filter, excludeSelf = true))

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
