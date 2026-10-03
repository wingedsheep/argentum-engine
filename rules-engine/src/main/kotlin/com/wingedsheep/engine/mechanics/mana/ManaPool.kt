package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import kotlinx.serialization.Serializable

/**
 * Represents a player's mana pool.
 * Tracks available mana that can be spent on costs.
 */
/**
 * Context about the spell being cast or the ability being activated, used to evaluate
 * mana spending restrictions.
 *
 * Spell-cast fields (isInstantOrSorcery, isKicked, isCreature, manaValue, hasXInCost,
 * subtypes, isFromExile, [cardTypes]) describe a spell being cast. When [isAbilityActivation]
 * is true, the context instead describes an activated ability being paid for, and
 * [abilitySourceCardTypes] (plus [subtypes] of the source) carry the source's type
 * information for restrictions that check it.
 */
@Serializable
data class SpellPaymentContext(
    val isInstantOrSorcery: Boolean = false,
    val isKicked: Boolean = false,
    val isCreature: Boolean = false,
    val manaValue: Int = 0,
    /**
     * True when the cost being paid contains the {X} mana symbol: the spell's mana cost for a
     * cast, the ability's mana cost for an activation (set by [buildAbilityPaymentContext]).
     * Restrictions that are spell-only ([ManaRestriction.SpellsWithManaValueAtLeast]) also require
     * `!isAbilityActivation`; [ManaRestriction.CostsContainingXOnly] reads it for both.
     */
    val hasXInCost: Boolean = false,
    /**
     * True when the spell being cast is colorless (CR 105.2c). Defaults to false so a cast path
     * that forgets to set it refuses [ManaRestriction.ColorlessSpellsOnly] mana rather than
     * letting it pay for a colored spell.
     */
    val isColorless: Boolean = false,
    val subtypes: Set<String> = emptySet(),
    val isFromExile: Boolean = false,
    /** True when the spell being cast is legendary (has the Legendary supertype). */
    val isLegendary: Boolean = false,
    /** Card types of the spell being cast (empty for non-spell contexts). */
    val cardTypes: Set<com.wingedsheep.sdk.core.CardType> = emptySet(),
    val isAbilityActivation: Boolean = false,
    /** Card types of the source whose ability is being activated (empty for spell-cast contexts). */
    val abilitySourceCardTypes: Set<com.wingedsheep.sdk.core.CardType> = emptySet(),
    /**
     * True when the spell is being cast from the caster's hand. Defaults to `true` because most
     * spell casts originate from hand and the standard [CastSpellEnumerator] path is hand-only;
     * cast-from-non-hand paths (top of library, exile, graveyard, command zone) must set this to
     * `false` so restrictions like [ManaRestriction.CastFromNonHandOnly] recognize the cast.
     *
     * The field is irrelevant for ability activations because every restriction that reads it
     * also requires `!isAbilityActivation`.
     */
    val isFromHand: Boolean = true,
    /**
     * True when the payment is for the turn-face-up special action (CR 707.9 / disguise's
     * turn-up). Lets restrictions like [ManaRestriction.TurnPermanentsFaceUpOnly] recognize
     * "spend this mana only to turn permanents face up" (Overgrown Zealot, Creeping Peeper).
     */
    val isTurnFaceUpAction: Boolean = false,
    /**
     * True when the payment is for the unlock-a-door special action (CR 709.5e). Lets
     * [ManaRestriction.UnlockDoorOnly] recognize "spend this mana only to ... unlock a door"
     * (Creeping Peeper).
     */
    val isUnlockDoorAction: Boolean = false,
    /**
     * True when the spell being cast is being cast **face down** for its morph (CR 702.37a) or
     * disguise (CR 702.168a) cost. Lets [ManaRestriction.FaceDownSpellsOnly] recognize "spend
     * this mana only to cast face-down spells" (Tin Street Gossip).
     *
     * A face-down spell has no name and no characteristics other than "2/2 creature" (CR 708.2),
     * so [faceDownCast] is the whole context for such a cast — see its factory below.
     */
    val isFaceDownCast: Boolean = false,
    /**
     * True when the payment is for an **equip** ability (CR 702.6) — the activation whose
     * `ActivatedAbility.isEquipAbility` flag is set. Always implies [isAbilityActivation]; it is a
     * strictly narrower fact about the same activation, which is why
     * [ManaRestriction.EquipAbilityActivationOnly] can't be expressed by the source's card type
     * (an Equipment's equip ability and its other activated abilities share one card type).
     *
     * Built only by [buildAbilityPaymentContext], which every ability-activation path funnels
     * through, so a new activation site can't forget it.
     */
    val isEquipAbilityActivation: Boolean = false,
    /**
     * True when colorless mana may be spent **as though it were mana of any color** on this
     * payment (CR 609.4b) — a per-spell permission carried by the cast grant that authorised it
     * ("you may spend colorless mana as though it were mana of any color to cast that spell",
     * Abstruse Appropriation). Every colored requirement (colored, hybrid, Phyrexian,
     * monocolored-hybrid pips) additionally accepts colorless mana; `{C}` and generic are
     * untouched, and colored mana still can't pay `{C}`. Unlike "mana of any type" this does not
     * rewrite the cost — it widens which mana may pay it — so it is applied in the pool's pip
     * matching and the auto-tap solver rather than to the [com.wingedsheep.sdk.core.ManaCost].
     */
    val colorlessAsAnyColor: Boolean = false,
) {
    init {
        require(!isEquipAbilityActivation || isAbilityActivation) {
            "isEquipAbilityActivation implies isAbilityActivation"
        }
    }

    companion object {
        /**
         * The payment context for a face-down cast (morph / disguise). CR 708.2: the spell has no
         * name, no mana cost, no color, and no card types or subtypes other than creature, with
         * power and toughness 2/2. Building it from the *printed* card would let restricted mana
         * keyed to the hidden card's characteristics (Cavern of Souls' chosen type, "creature
         * spells with mana value 4 or greater") pay for a cast that, as far as the rules are
         * concerned, has none of them.
         */
        fun faceDownCast(isFromHand: Boolean = true): SpellPaymentContext = SpellPaymentContext(
            isCreature = true,
            manaValue = 0,
            isColorless = true,
            cardTypes = setOf(com.wingedsheep.sdk.core.CardType.CREATURE),
            isFromHand = isFromHand,
            isFaceDownCast = true,
        )
    }

    /**
     * True when this payment is for *casting a spell*, as opposed to activating an ability, a
     * special action, or any other cost a player is asked to pay. Every spell has at least one
     * card type, and [cardTypes] is documented as empty for non-spell contexts, so the two
     * together are the signal. Read by the negative restrictions, which must let every non-cast
     * spend through rather than falling back to "not the thing I allow, so no".
     */
    val isSpellCast: Boolean get() = !isAbilityActivation && cardTypes.isNotEmpty()
}

/**
 * Check whether a mana restriction is satisfied by the spell being cast or ability being activated.
 */
fun ManaRestriction.isSatisfiedBy(context: SpellPaymentContext): Boolean = when (this) {
    is ManaRestriction.AnySpend -> true
    is ManaRestriction.InstantOrSorceryOnly -> !context.isAbilityActivation && context.isInstantOrSorcery
    is ManaRestriction.KickedSpellsOnly -> !context.isAbilityActivation && context.isKicked
    is ManaRestriction.SpellsWithManaValueAtLeast ->
        !context.isAbilityActivation &&
            (!creatureOnly || context.isCreature) &&
            (context.manaValue >= minManaValue || (orXInCost && context.hasXInCost))
    is ManaRestriction.CreatureSpellsOnly -> !context.isAbilityActivation && context.isCreature
    is ManaRestriction.LegendarySpellsOnly -> !context.isAbilityActivation && context.isLegendary
    is ManaRestriction.ColorlessSpellsOnly -> context.isSpellCast && context.isColorless
    is ManaRestriction.CostsContainingXOnly -> (context.isSpellCast || context.isAbilityActivation) && context.hasXInCost
    is ManaRestriction.SubtypeSpellsOrAbilitiesOnly ->
        (!creatureOnly || (!context.isAbilityActivation && context.isCreature)) &&
            context.subtypes.any { it.equals(subtype, ignoreCase = true) }
    is ManaRestriction.CastFromExileOnly -> !context.isAbilityActivation && context.isFromExile
    is ManaRestriction.CastFromNonHandOnly -> !context.isAbilityActivation && !context.isFromHand
    is ManaRestriction.TurnPermanentsFaceUpOnly -> context.isTurnFaceUpAction
    is ManaRestriction.FaceDownSpellsOnly -> !context.isAbilityActivation && context.isFaceDownCast
    is ManaRestriction.UnlockDoorOnly -> context.isUnlockDoorAction
    is ManaRestriction.AbilityActivationOnly -> context.isAbilityActivation
    is ManaRestriction.EquipAbilityActivationOnly -> context.isEquipAbilityActivation
    is ManaRestriction.AnyOf -> restrictions.any { it.isSatisfiedBy(context) }
    is ManaRestriction.AllOf -> restrictions.all { it.isSatisfiedBy(context) }
    is ManaRestriction.SpellsOnly -> context.isSpellCast
    is ManaRestriction.SubtypeSpellsOnly ->
        !context.isAbilityActivation &&
            subtypes.any { sub -> context.subtypes.any { it.equals(sub, ignoreCase = true) } }
    is ManaRestriction.CardTypeSpellsOrAbilitiesOnly ->
        if (context.isAbilityActivation) allowAbilities && (cardType in context.abilitySourceCardTypes) != negated
        else allowSpells && (cardType in context.cardTypes) != negated
    // Negative restriction: only a spell cast can ever violate it, so every other kind of
    // payment passes untouched.
    is ManaRestriction.CannotCastSpellsOtherThan ->
        !context.isSpellCast || cardTypes.any { it in context.cardTypes }
    // Negative restriction: only a spell cast from hand violates it.
    is ManaRestriction.CannotCastSpellsFromHand -> !context.isSpellCast || !context.isFromHand
}

/**
 * The provenance of the mana actually consumed by a single payment: for each producing-source
 * subtype, how many mana units carrying that subtype were spent ([bySubtype]); and the set of
 * producing-source entity ids whose mana was spent ([sourceIds]).
 *
 * Generalizes the old boolean "paid with Treasure mana" tag: Treasure is just one entry
 * (`bySubtype[Subtype.TREASURE]`). Drives:
 *  - `DynamicAmount.ManaSpentFromSubtype` (Bat Colony: "a Bat for each mana from a Cave spent to
 *    cast it") via the per-subtype counts stamped onto the resolving spell/permanent,
 *  - `SpellCastPredicate.PaidWithManaFromSubtype` (Alchemist's Talent: "paid with Treasure mana"),
 *  - `SpellCastPredicate.PaidWithManaFromSource` (Tecutlan / Barracks / Myriad Pools: "cast … using
 *    mana produced by this land").
 *
 * Counts are proportional approximations (mirroring the legacy Treasure counter): a mana unit from
 * a source with several subtypes contributes to each of them, so the per-subtype counts need not sum
 * to the total mana spent.
 */
@Serializable
data class SpentManaProvenance(
    val bySubtype: Map<com.wingedsheep.sdk.core.Subtype, Int> = emptyMap(),
    val sourceIds: Set<com.wingedsheep.sdk.model.EntityId> = emptySet(),
    /** Producing-source card type → mana units carrying it (Inga and Esika's "mana from creatures"). */
    val byCardType: Map<com.wingedsheep.sdk.core.CardType, Int> = emptyMap(),
    /** Units produced by a snow source — the "{S} spent" of CR 107.4h. Exact, not proportional. */
    val snow: Int = 0
) {
    val isEmpty: Boolean get() = bySubtype.isEmpty() && sourceIds.isEmpty() && byCardType.isEmpty() && snow == 0

    /**
     * The producing-source subtypes that had at least one mana unit spent. `bySubtype` only ever
     * holds positive counts (consumers add an entry only when they consume a unit), so this is just
     * its key set — named here so the cast/resolve paths don't each re-filter for `> 0`.
     */
    val spentSubtypes: Set<com.wingedsheep.sdk.core.Subtype> get() = bySubtype.keys

    /** Sum two snapshots (adding per-subtype and per-card-type counts, unioning source ids). */
    operator fun plus(other: SpentManaProvenance): SpentManaProvenance = when {
        isEmpty -> other
        other.isEmpty -> this
        else -> SpentManaProvenance(
            bySubtype = sumCounts(bySubtype, other.bySubtype),
            sourceIds = sourceIds + other.sourceIds,
            byCardType = sumCounts(byCardType, other.byCardType),
            snow = snow + other.snow
        )
    }

    companion object {
        private fun <K> sumCounts(a: Map<K, Int>, b: Map<K, Int>): Map<K, Int> =
            if (b.isEmpty()) a else a.toMutableMap().apply { b.forEach { (k, n) -> merge(k, n, Int::plus) } }

        /** One unit's worth of provenance for each tag (a mana unit per tag). */
        fun ofUnits(tags: List<com.wingedsheep.engine.state.components.player.ManaSourceTag>): SpentManaProvenance {
            if (tags.isEmpty()) return SpentManaProvenance()
            val bySubtype = mutableMapOf<com.wingedsheep.sdk.core.Subtype, Int>()
            val byCardType = mutableMapOf<com.wingedsheep.sdk.core.CardType, Int>()
            for (tag in tags) {
                tag.subtypes.forEach { bySubtype.merge(it, 1, Int::plus) }
                tag.cardTypes.forEach { byCardType.merge(it, 1, Int::plus) }
            }
            return SpentManaProvenance(bySubtype, tags.mapTo(mutableSetOf()) { it.sourceId }, byCardType, tags.count { it.isSnow })
        }

        /**
         * Provenance of the restricted units a payment consumed: the multiset difference between
         * the pool's restricted entries [before] and [after] the payment, read off each consumed
         * entry's [RestrictedManaEntry.source].
         */
        fun ofConsumedRestricted(before: List<RestrictedManaEntry>, after: List<RestrictedManaEntry>): SpentManaProvenance {
            if (before.none { it.source != null }) return SpentManaProvenance()
            val remaining = after.toMutableList()
            val consumed = before.filter { entry ->
                val idx = remaining.indexOf(entry)
                if (idx >= 0) { remaining.removeAt(idx); false } else true
            }
            return ofUnits(consumed.mapNotNull { it.source })
        }
    }
}

@Serializable
data class ManaPool(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0,
    val restrictedMana: List<RestrictedManaEntry> = emptyList(),
    /**
     * Provenance of the unrestricted mana currently floating in the pool. See
     * [com.wingedsheep.engine.state.components.player.ManaPoolComponent.manaBySubtype].
     */
    val manaBySubtype: Map<com.wingedsheep.sdk.core.Subtype, Int> = emptyMap(),
    val manaBySource: Map<com.wingedsheep.sdk.model.EntityId, Int> = emptyMap(),
    val manaByCardType: Map<com.wingedsheep.sdk.core.CardType, Int> = emptyMap(),
    /**
     * How many of the floating unrestricted units of each color came from a snow source — the
     * only mana that can pay a `{S}` pip (CR 107.4h). Unlike the proportional provenance counters
     * this is exact per color, because which unit pays `{S}` matters: invariant
     * `snowMana[c] <= get(c)` and [snowColorless] `<= colorless`. Spending a color for anything
     * but `{S}` spends its non-snow units first (see [spend]), so a snow unit survives as long as
     * any plain unit of its color could have been spent instead.
     */
    val snowMana: Map<Color, Int> = emptyMap(),
    val snowColorless: Int = 0,
    /** Ephemeral payment configuration, never stored in the player's mana component. */
    val spendingColors: Map<Color, Set<Color>> = emptyMap(),
    /** Payment-local selection memory, never stored in the player component. */
    val dischargedObligations: Set<String> = emptySet()
) {
    /**
     * Whether paying [cost] must go through the substitution-aware matcher: a player-wide
     * [spendingColors] permission, or a per-payment [SpellPaymentContext.colorlessAsAnyColor]
     * with at least one colored requirement for colorless mana to stand in for.
     */
    private fun substitutes(cost: ManaCost, context: SpellPaymentContext?): Boolean =
        spendingColors.isNotEmpty() || (context?.colorlessAsAnyColor == true && cost.symbols.any { it.isColoredRequirement() })

    private fun ManaSymbol.isColoredRequirement(): Boolean =
        this is ManaSymbol.Colored || this is ManaSymbol.Phyrexian || this is ManaSymbol.HybridPair ||
            this is ManaSymbol.MonocolorHybrid

    /**
     * Get amount of mana for a specific color.
     */
    fun get(color: Color): Int = when (color) {
        Color.WHITE -> white
        Color.BLUE -> blue
        Color.BLACK -> black
        Color.RED -> red
        Color.GREEN -> green
    }

    /**
     * Total mana in the pool.
     */
    /**
     * Total unrestricted mana in the pool (does not include restricted mana).
     */
    val total: Int get() = white + blue + black + red + green + colorless

    /**
     * Total mana including restricted mana eligible for a given spell context.
     */
    fun totalForSpell(context: SpellPaymentContext): Int = total + getTotalEligibleRestricted(context)

    /**
     * Check if the pool is empty (including restricted mana).
     */
    fun isEmpty(): Boolean = total == 0 && restrictedMana.isEmpty()

    /**
     * Add mana of a specific color.
     */
    fun add(color: Color, amount: Int = 1): ManaPool = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
    }

    /**
     * Add colorless mana.
     */
    fun addColorless(amount: Int = 1): ManaPool = copy(colorless = colorless + amount)

    /**
     * Add restricted mana to the pool.
     */
    fun addRestricted(
        color: Color?,
        amount: Int,
        restriction: ManaRestriction,
        riders: Set<ManaSpellRider> = emptySet()
    ): ManaPool {
        val entries = (1..amount).map { RestrictedManaEntry(color, restriction, riders) }
        return copy(restrictedMana = restrictedMana + entries)
    }

    /**
     * Spend one unit of restricted mana matching the given color and whose restriction is satisfied by the spell context.
     * Returns null if no matching restricted mana is available.
     */
    fun spendRestricted(color: Color?, context: SpellPaymentContext): ManaPool? {
        val index = preferredRestrictedIndex { it.color == color && it.restriction.isSatisfiedBy(context) }
        if (index < 0) return null
        return copy(
            restrictedMana = restrictedMana.toMutableList().apply { removeAt(index) },
            dischargedObligations = dischargedObligations + restrictedMana[index].obligationIds,
        )
    }

    /** Preserve ordinary entry order while preferring an activation not yet used by this payment. */
    private inline fun preferredRestrictedIndex(eligible: (RestrictedManaEntry) -> Boolean): Int {
        var first = -1
        for (index in restrictedMana.indices) {
            val entry = restrictedMana[index]
            if (!eligible(entry)) continue
            if (first < 0) first = index
            if (entry.obligationIds.any { it !in dischargedObligations }) return index
        }
        return first
    }

    /**
     * Count eligible restricted mana of a given color for a spell.
     */
    fun getEligibleRestrictedCount(color: Color?, context: SpellPaymentContext): Int =
        restrictedMana.count { it.color == color && it.restriction.isSatisfiedBy(context) }

    /**
     * Count all eligible restricted mana (any color) for a spell.
     */
    fun getTotalEligibleRestricted(context: SpellPaymentContext): Int =
        restrictedMana.count { it.restriction.isSatisfiedBy(context) }

    /**
     * Remove mana of a specific color — its non-snow units first, so a snow unit stays available
     * for a `{S}` pip whenever a plain unit could pay instead.
     */
    fun spend(color: Color, amount: Int = 1): ManaPool? {
        val current = get(color)
        if (current < amount) return null
        val spent = when (color) {
            Color.WHITE -> copy(white = white - amount)
            Color.BLUE -> copy(blue = blue - amount)
            Color.BLACK -> copy(black = black - amount)
            Color.RED -> copy(red = red - amount)
            Color.GREEN -> copy(green = green - amount)
        }
        val snow = snowMana[color] ?: 0
        return if (snow <= current - amount) spent else spent.copy(snowMana = snowMana.withCount(color, current - amount))
    }

    /**
     * Remove colorless mana — non-snow units first, as [spend] does.
     */
    fun spendColorless(amount: Int = 1): ManaPool? {
        if (colorless < amount) return null
        return copy(colorless = colorless - amount, snowColorless = minOf(snowColorless, colorless - amount))
    }

    /**
     * Float what one solver-tapped source produced — [coloredAmount] units of its color, or its
     * colorless amount — marked as snow when the source was snow, so a `{S}` pip paid from the
     * pool afterwards can find it.
     */
    fun addProduction(production: ManaProduction, coloredAmount: Int = production.amount): ManaPool {
        val color = production.color
        val amount = if (color != null) coloredAmount else production.colorless
        val added = if (color != null) add(color, amount) else addColorless(amount)
        return if (production.snow) added.markSnow(color, amount) else added
    }

    /** Total floating units that came from a snow source. */
    val snowTotal: Int get() = snowColorless + snowMana.values.sum()

    /**
     * Mark [amount] of the floating units of [color] (null = colorless) as produced by a snow
     * source. The mana itself must already be in the pool; the mark is capped at what floats.
     */
    fun markSnow(color: Color?, amount: Int): ManaPool {
        if (amount <= 0) return this
        return if (color == null) copy(snowColorless = minOf(colorless, snowColorless + amount))
        else copy(snowMana = snowMana.withCount(color, minOf(get(color), (snowMana[color] ?: 0) + amount)))
    }

    /**
     * Spend one snow unit to pay a `{S}` pip (CR 107.4h), returning the new pool and the kind
     * spent (null = colorless), or null if no snow mana floats. Colorless snow goes first — it is
     * the kind least likely to be wanted by anything else.
     */
    fun spendSnow(context: SpellPaymentContext? = null): Pair<ManaPool, Color?>? {
        if (context != null) {
            val index = preferredRestrictedIndex { it.source?.isSnow == true && it.restriction.isSatisfiedBy(context) }
            if (index >= 0) {
                val entry = restrictedMana[index]
                return copy(restrictedMana = restrictedMana.toMutableList().apply { removeAt(index) },
                    dischargedObligations = dischargedObligations + entry.obligationIds) to entry.color
            }
        }
        if (snowColorless > 0) {
            return copy(colorless = colorless - 1, snowColorless = snowColorless - 1) to null
        }
        val color = Color.entries.firstOrNull { (snowMana[it] ?: 0) > 0 } ?: return null
        val unmarked = copy(snowMana = snowMana.withCount(color, snowMana.getValue(color) - 1))
        return unmarked.spend(color)!! to color
    }

    private fun Map<Color, Int>.withCount(color: Color, count: Int): Map<Color, Int> =
        if (count > 0) this + (color to count) else this - color

    /**
     * The ordered units of *unrestricted* floating mana this pool would spend to cover up to
     * [xAmount] of an {X} cost: colorless first (only when X is not color-restricted), then the
     * allowed colors (all colors when [xManaRestriction] is empty). Each element is the color of
     * one unit, or `null` for a colorless unit; the list length is how much of X this pool covers.
     * Restricted/rider mana is not considered.
     *
     * Shared by `CastPaymentProcessor.autoPay` (spends each unit and tallies per-color X spend)
     * and `ActivationAutoTapper.autoTapForManaCost` (uses only the count, to reduce how much X
     * it must tap sources for) so both apply the exact same coverage rule.
     */
    fun xCoveragePlan(xAmount: Int, xManaRestriction: Set<Color>): List<Color?> {
        if (xAmount <= 0) return emptyList()
        val allowed = if (xManaRestriction.isEmpty()) Color.entries.toSet() else xManaRestriction
        val plan = ArrayList<Color?>(xAmount)
        var remaining = xAmount
        // Colorless pays generic X, but never a color-restricted X.
        if (xManaRestriction.isEmpty()) {
            val take = minOf(remaining, colorless)
            repeat(take) { plan.add(null) }
            remaining -= take
        }
        for (color in Color.entries) {
            if (remaining <= 0) break
            if (color !in allowed) continue
            val take = minOf(remaining, get(color))
            repeat(take) { plan.add(color) }
            remaining -= take
        }
        return plan
    }

    /**
     * How much of an {X} amount of [xAmount] this pool can cover: eligible restricted mana first
     * (entries whose restriction is satisfied by [spellContext] and — for a color-restricted X —
     * whose color is allowed), then unrestricted mana per [xCoveragePlan]. Mirrors the spending
     * order of `CastPaymentProcessor.autoPay`, so validation counting with this method never
     * accepts an X the payment path can't actually cover from the pool.
     */
    fun xCoverage(xAmount: Int, xManaRestriction: Set<Color>, spellContext: SpellPaymentContext?): Int {
        if (xAmount <= 0) return 0
        val eligibleRestricted = if (spellContext == null) 0 else restrictedMana.count { entry ->
            entry.restriction.isSatisfiedBy(spellContext) &&
                // A color-restricted X can't be paid with off-color or colorless restricted mana.
                (xManaRestriction.isEmpty() || (entry.color != null && entry.color in xManaRestriction))
        }
        val fromRestricted = minOf(xAmount, eligibleRestricted)
        val fromUnrestricted = xCoveragePlan(xAmount - fromRestricted, xManaRestriction).size
        return fromRestricted + fromUnrestricted
    }

    /**
     * Check if this pool can pay a mana cost.
     * When [spellContext] is provided, eligible restricted mana is considered (spent first).
     */
    fun canPay(cost: ManaCost, spellContext: SpellPaymentContext? = null): Boolean {
        if (restrictedMana.any { it.obligationIds.isNotEmpty() }) return allocateFloating(cost, spellContext) != null
        if (substitutes(cost, spellContext)) {
            return payPartialWithSpending(cost, spellContext, allowMonoHybridGeneric = true).remainingCost.symbols.all { it is ManaSymbol.X }
        }
        var remaining = this

        // First, pay colored costs — try restricted mana first, then unrestricted
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext) ?: return false
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.trySpendColorless(spellContext) ?: return false
                }
                is ManaSymbol.Generic -> {
                    // Will handle in second pass
                }
                is ManaSymbol.X -> {
                    // X is 0 unless specified otherwise
                }
                is ManaSymbol.Hybrid, is ManaSymbol.HybridPhyrexian -> {
                    val spent = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)
                        ?: return false
                    remaining = spent
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext) ?: return false
                }
                is ManaSymbol.MonocolorHybrid -> {
                    // Resolved after strict pips below so a strict pip of the same color claims
                    // its mana first.
                }
                ManaSymbol.Snow -> {
                    // Paid after every colored pip, before generic, so it takes only snow units
                    // nothing stricter wanted.
                }
            }
        }

        // Monocolored hybrids ({2/B}): prefer one mana of the color (cheaper, fewer mana),
        // otherwise treat as the generic amount. Strict pips are already paid above, so spending
        // the color here can't rob them, and any other hybrid can still fall back to generic.
        var monoHybridGeneric = 0
        for (symbol in cost.symbols.filterIsInstance<ManaSymbol.MonocolorHybrid>()) {
            val spent = remaining.trySpendColored(symbol.color, spellContext)
            if (spent != null) remaining = spent else monoHybridGeneric += symbol.generic
        }

        repeat(cost.snowCount) { remaining = remaining.spendSnow(spellContext)?.first ?: return false }

        // Then, pay generic costs with any remaining mana (restricted first, then unrestricted)
        val genericAmount = cost.genericAmount + monoHybridGeneric
        val availableForGeneric = if (spellContext != null) {
            remaining.total + remaining.getTotalEligibleRestricted(spellContext)
        } else {
            remaining.total
        }
        if (availableForGeneric < genericAmount) return false

        return true
    }

    /**
     * Try to spend one colored mana, preferring eligible restricted mana first.
     */
    private fun trySpendColored(color: Color, spellContext: SpellPaymentContext?): ManaPool? {
        if (spellContext != null) {
            val fromRestricted = spendRestricted(color, spellContext)
            if (fromRestricted != null) return fromRestricted
        }
        return spend(color)
    }

    /**
     * Try to spend one colorless mana, preferring eligible restricted mana first.
     */
    private fun trySpendColorless(spellContext: SpellPaymentContext?): ManaPool? {
        if (spellContext != null) {
            val fromRestricted = spendRestricted(null, spellContext)
            if (fromRestricted != null) return fromRestricted
        }
        return spendColorless()
    }

    /**
     * Pay a mana cost, returning the new pool or null if can't pay.
     * When [spellContext] is provided, eligible restricted mana is spent first.
     */
    fun pay(cost: ManaCost, spellContext: SpellPaymentContext? = null): ManaPool? {
        if (restrictedMana.any { it.obligationIds.isNotEmpty() }) return allocateFloating(cost, spellContext)?.pool
        if (substitutes(cost, spellContext)) {
            val partial = payPartialWithSpending(cost, spellContext, allowMonoHybridGeneric = true)
            return partial.newPool.takeIf { partial.remainingCost.symbols.all { it is ManaSymbol.X } }
        }
        if (!canPay(cost, spellContext)) return null

        var remaining = this

        // Pay colored costs — restricted first, then unrestricted
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext)!!
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.trySpendColorless(spellContext)!!
                }
                is ManaSymbol.Generic -> {
                    // Will handle separately
                }
                is ManaSymbol.X -> {
                    // Handled by caller
                }
                is ManaSymbol.Hybrid, is ManaSymbol.HybridPhyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)!!
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext)!!
                }
                is ManaSymbol.MonocolorHybrid -> {
                    // Resolved after strict pips below (mirrors canPay).
                }
                ManaSymbol.Snow -> {
                    // Paid after the colored pips (mirrors canPay).
                }
            }
        }

        // Monocolored hybrids ({2/B}): prefer one mana of the color, else fall back to generic.
        // Mirrors canPay so a cost canPay accepts is always payable here.
        var monoHybridGeneric = 0
        for (symbol in cost.symbols.filterIsInstance<ManaSymbol.MonocolorHybrid>()) {
            val spent = remaining.trySpendColored(symbol.color, spellContext)
            if (spent != null) remaining = spent else monoHybridGeneric += symbol.generic
        }

        repeat(cost.snowCount) { remaining = remaining.spendSnow(spellContext)!!.first }

        // Pay generic costs - spend eligible restricted first, then colorless, then colored
        var genericRemaining = cost.genericAmount + monoHybridGeneric

        // Spend eligible restricted mana for generic costs (any color)
        if (spellContext != null) {
            for (entry in remaining.restrictedMana.toList()) {
                if (genericRemaining <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    remaining = remaining.spendRestricted(entry.color, spellContext)!!
                    genericRemaining--
                }
            }
        }

        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            genericRemaining--
        }

        for (color in Color.entries) {
            while (genericRemaining > 0 && remaining.get(color) > 0) {
                remaining = remaining.spend(color)!!
                genericRemaining--
            }
        }

        return remaining
    }

    /**
     * Result of a partial mana payment.
     */
    data class PartialPaymentResult(
        val newPool: ManaPool,
        val remainingCost: ManaCost,
        val manaSpent: ManaPool
    )

    /**
     * Pay as much of a mana cost as possible from this pool.
     * Returns the new pool, the remaining unpaid cost, and the mana that was spent.
     * This is used for AutoPay to use floating mana before tapping lands.
     * When [spellContext] is provided, eligible restricted mana is spent first.
     */
    fun payPartial(cost: ManaCost, spellContext: SpellPaymentContext? = null): PartialPaymentResult {
        if (restrictedMana.any { it.obligationIds.isNotEmpty() }) {
            allocateFloating(cost, spellContext)?.let {
                return PartialPaymentResult(it.pool, ManaCost(cost.symbols.filterIsInstance<ManaSymbol.X>()), it.spent)
            }
        }
        if (substitutes(cost, spellContext)) {
            if (cost.symbols.any { it is ManaSymbol.MonocolorHybrid }) {
                val full = payPartialWithSpending(cost, spellContext, allowMonoHybridGeneric = true)
                if (full.remainingCost.symbols.all { it is ManaSymbol.X }) return full
            }
            return payPartialWithSpending(cost, spellContext)
        }
        var remaining = this
        val unpaidSymbols = mutableListOf<ManaSymbol>()

        // Track mana spent
        var whiteSpent = 0
        var blueSpent = 0
        var blackSpent = 0
        var redSpent = 0
        var greenSpent = 0
        var colorlessSpent = 0

        fun trackColorSpent(color: Color) {
            when (color) {
                Color.WHITE -> whiteSpent++
                Color.BLUE -> blueSpent++
                Color.BLACK -> blackSpent++
                Color.RED -> redSpent++
                Color.GREEN -> greenSpent++
            }
        }

        // Try to pay colored costs first — restricted mana preferred
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Colorless -> {
                    val spent = remaining.trySpendColorless(spellContext)
                    if (spent != null) {
                        remaining = spent
                        colorlessSpent++
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Hybrid, is ManaSymbol.HybridPhyrexian -> {
                    val beforeRemaining = remaining
                    val spent = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)
                    if (spent != null) {
                        // Determine which color was used by checking what changed
                        val color1Before = beforeRemaining.get(symbol.color1) +
                            beforeRemaining.restrictedMana.count { it.color == symbol.color1 }
                        val color1After = spent.get(symbol.color1) +
                            spent.restrictedMana.count { it.color == symbol.color1 }
                        if (color1Before > color1After) {
                            trackColorSpent(symbol.color1)
                        } else {
                            trackColorSpent(symbol.color2)
                        }
                        remaining = spent
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Phyrexian -> {
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.MonocolorHybrid -> {
                    // Spend floating mana of the color if available; otherwise leave the hybrid
                    // unpaid (as-is) so the land solver keeps the color-vs-generic choice rather
                    // than locking it to the generic amount.
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Generic -> {
                    unpaidSymbols.add(symbol)
                }
                is ManaSymbol.X -> {
                    unpaidSymbols.add(symbol)
                }
                ManaSymbol.Snow -> {
                    // Paid below, once every colored pip has had its claim.
                }
            }
        }

        repeat(cost.snowCount) {
            val paid = remaining.spendSnow(spellContext)
            if (paid == null) {
                unpaidSymbols.add(ManaSymbol.Snow)
            } else {
                remaining = paid.first
                paid.second?.let(::trackColorSpent) ?: colorlessSpent++
            }
        }

        // Now pay generic costs with remaining mana
        var genericRemaining = unpaidSymbols.filterIsInstance<ManaSymbol.Generic>().sumOf { it.amount }
        unpaidSymbols.removeAll { it is ManaSymbol.Generic }

        // Spend eligible restricted mana for generic costs first
        if (spellContext != null) {
            for (entry in remaining.restrictedMana.toList()) {
                if (genericRemaining <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    val spent = remaining.spendRestricted(entry.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        if (entry.color != null) trackColorSpent(entry.color) else colorlessSpent++
                        genericRemaining--
                    }
                }
            }
        }

        // Spend colorless first for generic
        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            colorlessSpent++
            genericRemaining--
        }

        // Spend colored mana for remaining generic
        for (color in Color.entries) {
            while (genericRemaining > 0 && remaining.get(color) > 0) {
                remaining = remaining.spend(color)!!
                trackColorSpent(color)
                genericRemaining--
            }
        }

        // Add remaining generic back to unpaid
        if (genericRemaining > 0) {
            unpaidSymbols.add(ManaSymbol.Generic(genericRemaining))
        }

        return PartialPaymentResult(
            newPool = remaining,
            remainingCost = ManaCost(unpaidSymbols),
            manaSpent = ManaPool(
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )
    }

    /** Maximum matching of colored pips to actual mana colors; no subset enumeration.
     * Augmenting paths reserve inflexible pips even when flexible ones appear first in the cost.
     * Unpaid pips keep their ORIGINAL symbols, so a later land/payment pass retains every option.
     */
    private fun payPartialWithSpending(
        cost: ManaCost,
        context: SpellPaymentContext?,
        allowMonoHybridGeneric: Boolean = false
    ): PartialPaymentResult {
        val symbols = cost.symbols.filter { it !is ManaSymbol.Generic && it !is ManaSymbol.X && it !is ManaSymbol.Snow }
        fun options(symbol: ManaSymbol): List<Color?> {
            fun colors(color: Color): List<Color> = listOf(color) + spendingColors[color].orEmpty().filter { it != color }
            // Colorless spent as though it were any color comes last: native colors first.
            val colorless: List<Color?> = if (context?.colorlessAsAnyColor == true) listOf(null) else emptyList()
            return when (symbol) {
                is ManaSymbol.Colored -> colors(symbol.color) + colorless
                is ManaSymbol.Phyrexian -> colors(symbol.color) + colorless
                is ManaSymbol.Hybrid, is ManaSymbol.HybridPhyrexian -> (colors(symbol.color1) + colors(symbol.color2)).distinct() + colorless
                is ManaSymbol.MonocolorHybrid -> colors(symbol.color) + colorless
                is ManaSymbol.Colorless -> listOf(null)
                else -> emptyList()
            }
        }
        val choices = symbols.map(::options)
        val assigned = arrayOfNulls<Color>(symbols.size)
        val matched = BooleanArray(symbols.size)
        val capacities = (Color.entries.map { it as Color? } + null).associateWith { color ->
            val plain = if (color == null) colorless else get(color)
            plain + if (context == null) 0 else getEligibleRestrictedCount(color, context)
        }
        val occupants = capacities.keys.associateWith { mutableListOf<Int>() }
        fun augment(pip: Int, visited: MutableSet<Color?>): Boolean {
            for (color in choices[pip]) {
                if (!visited.add(color)) continue
                val used = occupants.getValue(color)
                if (used.size < capacities.getValue(color)) {
                    used.add(pip); assigned[pip] = color; matched[pip] = true
                    return true
                }
                for (other in used.toList()) {
                    if (augment(other, visited)) {
                        used.remove(other); used.add(pip); assigned[pip] = color; matched[pip] = true
                        return true
                    }
                }
            }
            return false
        }
        // A mono-hybrid can use generic instead; give strict pips first claim to colored mana.
        val strictPips = symbols.indices.filter { symbols[it] !is ManaSymbol.MonocolorHybrid }
            .sortedBy { choices[it].size }
        for (i in strictPips + symbols.indices.filter { symbols[it] is ManaSymbol.MonocolorHybrid }) augment(i, mutableSetOf())
        var pool = copy(spendingColors = emptyMap())
        var spent = EMPTY
        val unpaid = mutableListOf<ManaSymbol>()
        val generic = cost.genericAmount
        for (i in symbols.indices) {
            if (matched[i]) {
                val color = assigned[i]
                pool = if (color == null) pool.trySpendColorless(context)!! else pool.trySpendColored(color, context)!!
                spent = if (color == null) spent.addColorless() else spent.add(color)
            }
        }
        repeat(cost.snowCount) {
            val paid = pool.spendSnow(context)
            if (paid == null) {
                unpaid.add(ManaSymbol.Snow)
            } else {
                pool = paid.first
                spent = paid.second?.let { spent.add(it) } ?: spent.addColorless()
            }
        }
        val genericPartial = pool.payPartial(ManaCost(listOf(ManaSymbol.Generic(generic))), context)
        pool = genericPartial.newPool
        val genericSpent = genericPartial.manaSpent
        spent = Color.entries.fold(spent) { acc, color -> acc.add(color, genericSpent.get(color)) }
            .addColorless(genericSpent.colorless)
        unpaid.addAll(genericPartial.remainingCost.symbols)
        for (i in symbols.indices.filter { !matched[it] }) {
            val symbol = symbols[i]
            // Partial payment must preserve the colored alternative for the later source pass.
            // Full-pool payment can select generic once that entire alternative is available.
            if (allowMonoHybridGeneric && symbol is ManaSymbol.MonocolorHybrid) {
                val fallback = ManaCost(listOf(ManaSymbol.Generic(symbol.generic)))
                val paid = pool.payPartial(fallback, context)
                if (paid.remainingCost.isEmpty()) {
                    pool = paid.newPool
                    spent = Color.entries.fold(spent) { acc, color -> acc.add(color, paid.manaSpent.get(color)) }
                        .addColorless(paid.manaSpent.colorless)
                    continue
                }
            }
            unpaid.add(symbol)
        }
        unpaid.addAll(cost.symbols.filterIsInstance<ManaSymbol.X>())
        return PartialPaymentResult(pool.copy(spendingColors = spendingColors), ManaCost(unpaid), spent)
    }

    /**
     * Consume mana provenance tags proportional to [unrestrictedSpent] — the count of unrestricted
     * floating mana pulled from the pool by a payment. Each subtype / source / card-type counter is reduced by
     * `min(count, unrestrictedSpent)` (the same greedy, proportional rule the legacy Treasure
     * counter used), and the consumed amounts are returned as a [SpentManaProvenance] so the caller
     * can stamp the spell/event. Restricted units carry their own tag on each entry instead — see
     * [SpentManaProvenance.ofConsumedRestricted].
     */
    fun consumeProvenance(unrestrictedSpent: Int): Pair<ManaPool, SpentManaProvenance> {
        if (unrestrictedSpent <= 0 || (manaBySubtype.isEmpty() && manaBySource.isEmpty() && manaByCardType.isEmpty())) {
            return this to SpentManaProvenance()
        }
        val (newSubtype, consumedSubtypes) = consumeCounts(manaBySubtype, unrestrictedSpent)
        val (newSource, consumedSources) = consumeCounts(manaBySource, unrestrictedSpent)
        val (newCardType, consumedCardTypes) = consumeCounts(manaByCardType, unrestrictedSpent)
        return copy(manaBySubtype = newSubtype, manaBySource = newSource, manaByCardType = newCardType) to
            SpentManaProvenance(consumedSubtypes, consumedSources.keys, consumedCardTypes)
    }

    /** Reduce each counter by `min(count, spent)`; returns the remaining and the consumed counts. */
    private fun <K> consumeCounts(counts: Map<K, Int>, spent: Int): Pair<Map<K, Int>, Map<K, Int>> {
        if (counts.isEmpty()) return counts to emptyMap()
        val consumed = mutableMapOf<K, Int>()
        val remaining = mutableMapOf<K, Int>()
        for ((key, count) in counts) {
            val used = minOf(count, spent)
            if (used > 0) consumed[key] = used
            if (count - used > 0) remaining[key] = count - used
        }
        return remaining to consumed
    }

    /**
     * Empty the mana pool (at end of phases).
     */
    fun empty(): ManaPool = EMPTY

    companion object {
        val EMPTY = ManaPool()
    }
}
