package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.IsNotYourTurn
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants flash to spells matching a filter.
 * Used for Quick Sliver: "Any player may cast Sliver spells as though they had flash."
 * Used for Raff Capashen: "You may cast historic spells as though they had flash."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when determining if a non-instant spell can be cast at instant speed.
 *
 * @property filter The filter that spells must match to gain flash
 * @property controllerOnly If true, only the permanent's controller benefits (default: false = any player)
 * @property nthOfTypePerTurn When set, only the Nth matching spell a player casts each turn gains
 *   flash (1-indexed; counts the spell being cast). `1` is "the **first** [type] spell you cast each
 *   turn … can be cast as though it had flash" (Radagast of Rhosgobel). The timing half of the same
 *   "first spell each turn" gate that [CostGating.NthOfTypePerTurn] applies to cost, and it counts
 *   the same way — off the caster's spells-cast-this-turn record, so a matching spell already cast
 *   this turn closes the window even if it was countered. `null` (default) grants flash to every
 *   matching spell, the Quick Sliver / Raff Capashen shape.
 */
@SerialName("GrantFlashToSpellType")
@Serializable
data class GrantFlashToSpellType(
    val filter: GameObjectFilter,
    val controllerOnly: Boolean = false,
    val nthOfTypePerTurn: Int? = null
) : StaticAbility {
    init {
        require(nthOfTypePerTurn == null || nthOfTypePerTurn >= 1) {
            "nthOfTypePerTurn must be at least 1, was $nthOfTypePerTurn"
        }
    }

    override val description: String = buildDescription()

    private fun buildDescription(): String {
        val subject = if (controllerOnly) "you cast" else "any player casts"
        return if (nthOfTypePerTurn != null) {
            // The gate names a single spell, so the whole clause goes singular.
            "The ${ordinal(nthOfTypePerTurn)} ${filter.description} spell $subject each turn " +
                "can be cast as though it had flash"
        } else if (controllerOnly) {
            "You may cast ${filter.description} spells as though they had flash"
        } else {
            "Any player may cast ${filter.description} spells as though they had flash"
        }
    }

    private fun ordinal(n: Int): String = when (n) {
        1 -> "first"
        2 -> "second"
        3 -> "third"
        4 -> "fourth"
        5 -> "fifth"
        else -> {
            val suffix = when {
                n % 100 in 11..13 -> "th"
                n % 10 == 1 -> "st"
                n % 10 == 2 -> "nd"
                n % 10 == 3 -> "rd"
                else -> "th"
            }
            "$n$suffix"
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Spells matching a filter can't be countered.
 * Used for Root Sliver: "Sliver spells can't be countered."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when a spell would be countered. If the spell matches the filter, the counter
 * attempt fails.
 *
 * @property filter The filter that spells must match to be uncounterable
 * @property includesAbilities When true, activated/triggered abilities matching [filter] also can't
 *   be countered (Spider-Punk: "Spells **and abilities** can't be countered"). Default false — a
 *   plain "spells can't be countered" grant leaves abilities counterable.
 */
@SerialName("GrantCantBeCountered")
@Serializable
data class GrantCantBeCountered(
    val filter: GameObjectFilter,
    val includesAbilities: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        append("${filter.description} spells")
        if (includesAbilities) append(" and abilities")
        append(" can't be countered")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants an alternative casting cost for spells cast by this permanent's controller.
 * Used for cards like Jodah, Archmage Eternal: "You may pay {W}{U}{B}{R}{G} rather than pay
 * the mana cost for spells you cast."
 *
 * When a player controls a permanent with this ability, they may choose to pay the
 * alternative cost instead of the spell's normal mana cost. Alternative costs cannot
 * be combined with other alternative costs (e.g., flashback).
 *
 * Per Rule 118.9a, additional costs, cost increases, and cost reductions still apply
 * to the alternative cost.
 *
 * The granted cost has the same two halves as a card's own [SelfAlternativeCost] — a mana half
 * and a list of non-mana [AdditionalCost]s — because "rather than pay the mana cost" says nothing
 * about the substituted cost being mana. Conspiracy Unraveler's "You may collect evidence 10
 * rather than pay the mana cost for spells you cast" is the whole cost in the non-mana half, with
 * `cost = "{0}"` standing in for the absent mana half (the same `{0}` idiom Fireblast and Force of
 * Vigor use for their own alternative costs).
 *
 * @property cost The alternative mana cost string (e.g., "{W}{U}{B}{R}{G}"), `"{0}"` when the
 *   substituted cost is entirely non-mana.
 * @property additionalCosts The non-mana half, paid alongside [cost] whenever this grant is the
 *   alternative cost the caster chose. Gated on `AlternativeCostType.GRANTED` at payment time so a
 *   different alternative cost never drags these in.
 */
@SerialName("GrantAlternativeCastingCost")
@Serializable
data class GrantAlternativeCastingCost(
    val cost: String,
    val additionalCosts: List<AdditionalCost> = emptyList()
) : StaticAbility {
    override val description: String =
        "You may ${describePayment()} rather than pay the mana cost for spells you cast"

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        if (additionalCosts.isEmpty()) return this
        val newCosts = additionalCosts.map { it.applyTextReplacement(replacer) }
        return if (newCosts == additionalCosts) this else copy(additionalCosts = newCosts)
    }

    /**
     * "pay {W}{U}{B}{R}{G}" / "collect evidence 10" / "pay {2} and sacrifice a creature".
     *
     * The mana half is dropped from the wording when it is `{0}`, so a purely non-mana grant reads
     * as its oracle text does rather than as "pay {0} and …". A grant with neither half is
     * degenerate; it falls back to naming the mana cost so the description is never blank.
     */
    private fun describePayment(): String {
        val parts = buildList {
            if (cost != "{0}" || additionalCosts.isEmpty()) add("pay $cost")
            additionalCosts.forEach { add(it.description.replaceFirstChar { c -> c.lowercaseChar() }) }
        }
        return parts.joinToString(" and ")
    }
}

/**
 * You may cast cards exiled with this permanent (linked via LinkedExileComponent).
 * The [filter] restricts which exiled cards can be cast (e.g., GameObjectFilter.Nonland
 * for "you may cast spells from among cards exiled with ~").
 *
 * When this permanent leaves the battlefield, the static ability naturally ceases to apply
 * and the exiled cards can no longer be cast. The cards remain in exile.
 *
 * Used by Rona, Disciple of Gix and similar cards. Dawnhand Dissident uses the
 * [duringYourTurnOnly] timing restriction and the [additionalCost] gate (a distributed
 * counter-removal) to gate its reanimation ability.
 *
 * @property filter Which exiled cards can be cast
 * @property duringYourTurnOnly If true, only legal during the controller's turn
 * @property additionalCost Optional additional cost that must be paid alongside the spell's mana cost
 */
@SerialName("GrantMayCastFromLinkedExile")
@Serializable
data class GrantMayCastFromLinkedExile(
    val filter: GameObjectFilter = GameObjectFilter.Companion.Nonland,
    val duringYourTurnOnly: Boolean = false,
    val additionalCost: AdditionalCost? = null,
    /**
     * When true, restricts the permission to cards owned by the granter's controller.
     * Dawnhand Dissident's "cards you own exiled with this creature" — exiling an
     * opponent's graveyard card is still permitted, but only the exiler can ever
     * cast it, not the granter, so those stripped cards sit in exile permanently.
     */
    val ownedByYou: Boolean = false,
    /**
     * When true, the spell may be cast without paying its mana cost
     * (Maralen, Fae Ascendant). The spell still pays any other mandatory
     * additional costs.
     */
    val withoutPayingManaCost: Boolean = false,
    /**
     * When true, this permission may be used at most once per turn. A successful
     * cast marks the granter with [com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent]
     * (cleared at end of turn).
     */
    val oncePerTurn: Boolean = false,
    /**
     * Optional cap on the cast spell's mana value. The amount is evaluated each
     * time legality is checked, so dynamic counts (e.g. "the number of Elves and
     * Faeries you control") update live as the battlefield changes.
     */
    val maxManaValue: DynamicAmount? = null,
    /**
     * When true, only cards exiled this turn are eligible (Maralen). Checked via
     * [com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent].
     */
    val exiledThisTurnOnly: Boolean = false,
    /**
     * Cast-this-way entry rider (CR 614-style): when set, a permanent cast from this linked-exile
     * pile *under this grant* enters the battlefield with one counter of that type on it. Mirrors
     * [MayCastFromGraveyard.entersWithCounter], but for the linked-exile cast path. Defaults to null
     * (no rider), so existing linked-exile cast cards are unaffected.
     *
     * Intrepid Paleontologist: `entersWithCounter = CounterType.FINALITY` — "If you cast a spell this
     * way, that creature enters with a finality counter on it."
     */
    val entersWithCounter: com.wingedsheep.sdk.core.CounterType? = null
) : StaticAbility {
    override val description: String = buildString {
        // "pay life equal to its mana value rather than pay its mana cost" — Valgavoth, Terror Eater
        // (the grant waives the mana cost via [withoutPayingManaCost] and substitutes the life cost).
        if (additionalCost is AdditionalCost.PayLifeEqualToManaValueOfSpell) {
            if (duringYourTurnOnly) append("During your turn, you may play ") else append("You may play ")
            append("cards exiled with this permanent")
            if (exiledThisTurnOnly) append(" this turn")
            append(". If you cast a spell this way, pay life equal to its mana value rather than pay its mana cost.")
            return@buildString
        }
        if (duringYourTurnOnly) append("During your turn, y") else append("Y")
        if (oncePerTurn) append("ou may cast a ${filter.description} ")
        else append("ou may cast ${filter.description} ")
        if (maxManaValue != null) append("with mana value less than or equal to ${maxManaValue.description} ")
        if (ownedByYou) append("cards you own ")
        else if (oncePerTurn) append("card ")
        else append("cards ")
        append("exiled with this permanent")
        if (exiledThisTurnOnly) append(" this turn")
        if (withoutPayingManaCost) append(" without paying its mana cost")
        if (additionalCost != null) append(" by ${additionalCost.description.lowercase()} in addition to paying their other costs")
        if (oncePerTurn) append(". This ability may be used only once each turn")
        append(".")
        if (entersWithCounter != null) {
            append(" If you cast a spell this way, that permanent enters with a ${entersWithCounter.name.lowercase()} counter on it.")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAdditional = additionalCost?.applyTextReplacement(replacer)
        val newMaxManaValue = maxManaValue?.applyTextReplacement(replacer)
        if (newFilter === filter && newAdditional === additionalCost && newMaxManaValue === maxManaValue) return this
        return copy(filter = newFilter, additionalCost = newAdditional, maxManaValue = newMaxManaValue)
    }
}

/**
 * Grants a keyword (typically a cost-modifying one like CONVOKE or DELVE) to spells cast by this
 * permanent's controller that match [spellFilter].
 *
 * Used for cards like Eirdu, Carrier of Dawn: "Creature spells you cast have convoke." The engine
 * consults battlefield permanents with this static ability when computing legal casts and alternative
 * payments, treating the granted keyword as if it were printed on the spell.
 *
 * Scoped to the controller of the source permanent — "you cast" semantics in the oracle text.
 *
 * @property keyword The keyword that matching spells gain while this permanent is in play.
 * @property spellFilter Which spells gain the keyword (defaults to creature spells).
 * @property keywordParameter Numeric parameter for parameterized keywords (e.g. Casualty N — the
 *   power threshold), or null for parameterless keywords like Convoke/Conspire. Read by the engine
 *   when the granted keyword needs a number (e.g. Silverquill: "instant and sorcery spell you cast
 *   has casualty 1" → `keyword = CASUALTY, keywordParameter = 1`).
 */
@SerialName("GrantKeywordToOwnSpells")
@Serializable
data class GrantKeywordToOwnSpells(
    val keyword: Keyword,
    val spellFilter: GameObjectFilter = GameObjectFilter.Creature,
    val keywordParameter: Int? = null
) : StaticAbility {
    override val description: String =
        "${spellFilter.description.replaceFirstChar { it.uppercase() }} spells you cast have " +
            "${keyword.displayName.lowercase()}${keywordParameter?.let { " $it" } ?: ""}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * Grants **web-slinging [cost]** (CR 702.188) to spells the controller casts that match
 * [spellFilter]. Web-slinging carries a [ManaCost], which the generic [GrantKeywordToOwnSpells]
 * (keyword + optional int) cannot express, so it gets its own static. Read by the web-slinging cast
 * enumerator / handler via `WebSlinging.effectiveWebSlinging`, alongside printed web-slinging.
 *
 * Amazing Spider-Man: "Each legendary spell you cast that's one or more colors has web-slinging
 * {G}{W}{U}" → `GrantWebSlingingToSpells({G}{W}{U}, GameObjectFilter.legendary + IsColored)`.
 *
 * @property cost The granted web-slinging cost.
 * @property spellFilter Which spells you cast gain web-slinging (matched against the spell's card).
 */
@SerialName("GrantWebSlingingToSpells")
@Serializable
data class GrantWebSlingingToSpells(
    val cost: ManaCost,
    val spellFilter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "${spellFilter.description.replaceFirstChar { it.uppercase() }} spells you cast have web-slinging $cost"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * Grants warp (CR 702.185) to cards in the granter's controller's hand that match [filter].
 * Models oracle text like "Artifact cards and red creature cards in your hand have warp {2}{R}."
 *
 * Read by the cast-from-hand enumerator and the cast handler the same way printed
 * [KeywordAbility.Warp] is read: the granted warp surfaces an alternative "Cast (Warp)" legal
 * action whose cost is [cost], the spell is marked `wasWarped` on resolution, and the
 * post-resolution permanent is exiled at the beginning of the next end step (CR 702.185b),
 * after which it can be cast again from exile for its regular mana cost (CR 702.185a — the
 * warp alternative cost is hand-only). The grant is controller-only — only the source
 * permanent's controller benefits.
 *
 * Hand-only by design: CR 702.185a restricts warp to the hand, and the granter's oracle text
 * scopes the grant to cards "in your hand". This static doesn't extend warp to other zones —
 * warp-exiled cards are recast from exile via the standard `MayPlayPermission` (regular mana
 * cost), independent of whether the original cast used printed or granted warp.
 *
 * @property filter Which cards in the controller's hand gain warp (e.g., artifact or red creature)
 * @property cost The warp mana cost the granted ability uses
 */
@SerialName("GrantWarpToCardsInHand")
@Serializable
data class GrantWarpToCardsInHand(
    val filter: GameObjectFilter,
    val cost: ManaCost
) : StaticAbility {
    override val description: String =
        "${filter.description.replaceFirstChar { it.uppercase() }} in your hand have warp $cost"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants miracle (CR 702.94) to cards in the granter's controller's hand that match [filter].
 * Models oracle text like "Each instant and sorcery card in your hand has miracle {2}."
 * (Lorehold, the Historian).
 *
 * Read by the draw flow the same way printed [KeywordAbility.Miracle] is read: when a matching card
 * is the first card its owner draws in a turn, the engine opens a one-turn miracle window on it and
 * the cast-from-hand enumerator surfaces a "Cast (Miracle)" alternative cost at [cost]. Hand-only by
 * design (CR 702.94a — the miracle ability functions only while the card is in hand).
 *
 * @property filter Which cards in the controller's hand gain miracle (e.g. instant or sorcery).
 * @property cost The miracle mana cost the granted ability uses.
 */
@SerialName("GrantMiracleToCardsInHand")
@Serializable
data class GrantMiracleToCardsInHand(
    val filter: GameObjectFilter,
    val cost: ManaCost
) : StaticAbility {
    override val description: String =
        "${filter.description.replaceFirstChar { it.uppercase() }} in your hand have miracle $cost"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants madness (CR 702.35) to cards the granter's controller **owns** that match [filter] and
 * aren't on the battlefield, with the madness cost equal to each card's own mana cost. Models
 * Falkenrath Gorger: "Each Vampire creature card you own that isn't on the battlefield has madness.
 * The madness cost is equal to its mana cost."
 *
 * Cost-free by construction: "equal to its mana cost" is the only shape printed on a card, so the
 * granted cost is derived per card rather than carried here. Should a grant with a fixed cost ever
 * print, that's a second field, not a second static.
 *
 * Read by the discard path exactly where printed madness is read — `MadnessGrants` is consulted by
 * the zone-change replacement that diverts a discard to exile, so a granted madness behaves
 * identically to a printed one: same exile redirect, same CR 702.35a cast offer, same fixed
 * alternative cost stamped on the exiled card. Because the exiled card carries the cost from that
 * moment on, the grant leaving the battlefield mid-trigger doesn't revoke the offer.
 *
 * The "isn't on the battlefield" clause is the card's own wording; madness only ever *functions*
 * from a hand (CR 702.35a replaces a discard), so in practice the grant is read for cards being
 * discarded from their owner's hand.
 *
 * @property filter Which cards the controller owns gain madness (e.g. Vampire creature card).
 */
@SerialName("GrantMadnessToOwnedCards")
@Serializable
data class GrantMadnessToOwnedCards(
    val filter: GameObjectFilter
) : StaticAbility {
    override val description: String =
        "Each ${filter.description} you own that isn't on the battlefield has madness. " +
            "The madness cost is equal to its mana cost."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * You may cast spells matching [filter] from your graveyard, optionally by paying [lifeCost]
 * life in addition to their other costs. Only during your turn if [duringYourTurnOnly] is true.
 *
 * Normal timing rules still apply — instants any time you have priority, everything else only
 * at sorcery speed. A spell cast this way uses its normal mana cost (plus the optional life cost).
 *
 * Used for:
 *  - Festival of Embers: `MayCastFromGraveyard(InstantOrSorcery, lifeCost = 1, duringYourTurnOnly = true)`
 *  - Yawgmoth's Agenda: `MayCastFromGraveyard(Nonland)` — free, any spell type, any turn.
 *  - The Tomb of Aclazotz (Tarrian's Journal back): `MayCastFromGraveyard(Creature,
 *    entersWithCounter = CounterType.FINALITY, addedSubtypeOnEntry = "Vampire")` — a permanent cast
 *    from the graveyard under this grant enters with a finality counter and gains the subtype.
 *  - Gisa and Geralf: `MayCastFromGraveyard(Creature.withSubtype("Zombie"),
 *    duringYourTurnOnly = true, oncePerTurn = true)` — "Once during each of your turns, you may
 *    cast a Zombie creature spell from your graveyard."
 *
 * [entersWithCounter] and [addedSubtypeOnEntry] are the **cast-this-way entry rider** (CR 614-style):
 * they apply only to a permanent cast from the graveyard *under this grant* — when it resolves onto
 * the battlefield it enters with one counter of that type and gains the subtype "in addition to its
 * other types" (a persistent characteristic, for as long as it remains on the battlefield). Both
 * default to null (no rider), so existing graveyard-cast cards are unaffected.
 *
 * [oncePerTurn] limits the grant to a single use per turn, tracked on the *granting permanent*
 * (`MayCastFromGraveyardUsedThisTurnComponent`, cleared each cleanup step) rather than on the
 * player — so a second copy of the granter brings a second use, and a use is only consumed when
 * no unlimited grant could have authorized the same cast. Pair with [duringYourTurnOnly] for
 * "once during each of your turns".
 *
 * [exileInsteadOfGraveyard] is the third member of the cast-this-way rider family, but it applies to
 * the *instant and sorcery* half rather than the permanent half: an instant or sorcery cast from the
 * graveyard under this grant is exiled instead of being put into its owner's graveyard (Bilbo, Thief
 * in the Night — "If an instant or sorcery spell cast this way would be put into your graveyard,
 * exile it instead"). Unlike the "exile it as it resolves" triggers (Lilah, Goliath Daydreamer), this
 * is a true replacement on the *would be put into your graveyard* event, so it also catches a spell
 * that is countered or fizzles — per Bilbo's Adventure ruling.
 *
 * @property filter The filter that spells must match (e.g., instant/sorcery, or any nonland card)
 * @property lifeCost The life cost to pay in addition to other costs (0 = free)
 * @property duringYourTurnOnly If true, only castable during your turn
 * @property entersWithCounter If set, a permanent cast this way enters with one such counter
 * @property addedSubtypeOnEntry If set, a permanent cast this way gains this subtype on entry
 * @property oncePerTurn If true, this grant authorizes at most one graveyard cast per turn
 * @property exileInsteadOfGraveyard If true, an instant or sorcery cast this way is exiled rather
 *   than put into its owner's graveyard — whether it resolves, is countered, or fizzles
 */
@SerialName("MayCastFromGraveyard")
@Serializable
data class MayCastFromGraveyard(
    val filter: GameObjectFilter,
    val lifeCost: Int = 0,
    val duringYourTurnOnly: Boolean = false,
    val entersWithCounter: com.wingedsheep.sdk.core.CounterType? = null,
    val addedSubtypeOnEntry: String? = null,
    val oncePerTurn: Boolean = false,
    val exileInsteadOfGraveyard: Boolean = false
) : StaticAbility {
    /** True when this grant carries a cast-this-way entry rider (finality counter / added subtype). */
    val hasEntryRider: Boolean get() = entersWithCounter != null || addedSubtypeOnEntry != null

    override val description: String = buildString {
        when {
            oncePerTurn && duringYourTurnOnly -> append("Once during each of your turns, y")
            oncePerTurn -> append("Once each turn, y")
            duringYourTurnOnly -> append("During your turn, y")
            else -> append("Y")
        }
        append("ou may cast ${filter.description} spells from your graveyard")
        if (lifeCost > 0) append(" by paying $lifeCost life in addition to their other costs")
        if (entersWithCounter != null || addedSubtypeOnEntry != null) {
            append(". If you do, it enters")
            if (entersWithCounter != null) append(" with a ${entersWithCounter.name.lowercase()} counter on it")
            if (entersWithCounter != null && addedSubtypeOnEntry != null) append(" and")
            if (addedSubtypeOnEntry != null) append(" is a $addedSubtypeOnEntry in addition to its other types")
        }
        if (exileInsteadOfGraveyard) {
            append(". If an instant or sorcery spell cast this way would be put into your graveyard, exile it instead")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * A continuous grant of flashback (CR 702.34) to *every* card in the controller's graveyard that
 * matches [filter] — a whole-graveyard group grant, not a single-card grant like
 * [com.wingedsheep.sdk.scripting.effects.GrantFlashbackEffect] (Archmage's Newt). While the
 * granting permanent is on the battlefield (and, if [duringYourTurnOnly], during its controller's
 * turn), each matching graveyard card may be cast for its flashback cost and is exiled on
 * resolution, exactly as if it had printed flashback.
 *
 * The flashback cost is [cost] when set, otherwise the card's own mana cost (`null` = "equal to
 * that card's mana cost").
 *
 * Used for Iroh, Grand Lotus:
 *  - `GraveyardCardsHaveFlashback(InstantOrSorcery.notSubtype(Lesson), duringYourTurnOnly = true)`
 *    — "During your turn, each non-Lesson instant and sorcery card in your graveyard has flashback.
 *    The flashback cost is equal to that card's mana cost."
 *  - `GraveyardCardsHaveFlashback(InstantOrSorcery.withSubtype(Lesson), cost = {1}, duringYourTurnOnly = true)`
 *    — "During your turn, each Lesson card in your graveyard has flashback {1}."
 *
 * Read by [com.wingedsheep.engine.mechanics.FlashbackGrants], the single source of truth every
 * flashback read site routes through (enumeration, cast cost, permission, and exile-on-resolution),
 * so a group-granted flashback behaves identically to a printed one.
 *
 * @property filter Which graveyard cards gain flashback (matched against the card's characteristics).
 * @property cost The granted flashback cost, or null for "equal to that card's mana cost".
 * @property duringYourTurnOnly If true, the grant is active only during the controller's turn.
 */
@SerialName("GraveyardCardsHaveFlashback")
@Serializable
data class GraveyardCardsHaveFlashback(
    val filter: GameObjectFilter,
    val cost: ManaCost? = null,
    val duringYourTurnOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        if (duringYourTurnOnly) append("During your turn, e") else append("E")
        append("ach ${filter.description} card in your graveyard has flashback")
        if (cost != null) append(" $cost")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * "Each [filter] card in your graveyard has mayhem [cost]." The Mayhem analogue of
 * [GraveyardCardsHaveFlashback] — a whole-graveyard group grant of the Mayhem keyword ability
 * (CR 702.187), read through `MayhemGrants.effectiveMayhem` alongside the discarded-this-turn gate.
 * Green Goblin's "Goblin Formula — Each nonland card in your graveyard has mayhem. The mayhem cost
 * is equal to its mana cost."
 *
 * @property filter Which graveyard cards gain mayhem (matched against the card's characteristics).
 * @property cost The granted mayhem cost, or null for "equal to that card's mana cost".
 * @property duringYourTurnOnly If true, the grant is active only during the controller's turn.
 */
@SerialName("GraveyardCardsHaveMayhem")
@Serializable
data class GraveyardCardsHaveMayhem(
    val filter: GameObjectFilter,
    val cost: ManaCost? = null,
    val duringYourTurnOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        if (duringYourTurnOnly) append("During your turn, e") else append("E")
        append("ach ${filter.description} card in your graveyard has mayhem")
        if (cost != null) append(" $cost")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * "Creature cards in your graveyard have sneak [cost]. You may cast creature spells from your
 * graveyard using their sneak abilities." (Ninja Teen level 3.)
 *
 * Read by `SneakCastEnumerator` (an additive graveyard loop) and the sneak branch of
 * `CastSpellHandler`: while the controller has this grant active, their graveyard creature cards
 * may be cast for [cost] (plus the normal sneak cost of returning an unblocked attacker you
 * control to hand) during the declare-blockers sneak window — just as if they had the printed
 * Sneak keyword, but cast-from-graveyard. A creature cast this way simply enters the battlefield
 * (no exile-on-resolution, unlike flashback).
 *
 * @property cost The granted sneak mana cost (Ninja Teen: {3}{B}).
 */
@SerialName("GraveyardCreaturesHaveSneak")
@Serializable
data class GraveyardCreaturesHaveSneak(
    val cost: com.wingedsheep.sdk.core.ManaCost
) : StaticAbility {
    override val description: String = "Creature cards in your graveyard have sneak ${cost}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * A per-turn cap on how many spells a player can cast, scoped along a single who-axis:
 *
 *  - [eachPlayer] = false (default) — the cap applies only to the permanent's **controller**
 *    ("You can't cast more than one spell each turn." — Yawgmoth's Agenda). Each player is
 *    limited only by the permanents they themselves control.
 *  - [eachPlayer] = true — the cap is a global continuous restriction applying to **every
 *    player** ("Each player can't cast more than one spell each turn." — High Noon). It is read
 *    off any permanent on the battlefield with this ability, regardless of who controls it.
 *
 * Spells already cast earlier in the turn count, even if cast before this permanent entered
 * (per the Yawgmoth's Agenda ruling). The engine enforces this by comparing a player's
 * spells-cast-this-turn tally against [maxPerTurn] during cast-legality checks. Copies of
 * spells and activated/triggered abilities are not "cast" and do not count. When several such
 * permanents are in play, the most restrictive (smallest [maxPerTurn]) applies.
 *
 * @property maxPerTurn The maximum number of spells a restricted player may cast each turn.
 * @property eachPlayer Whether the restriction binds every player (true) or only the controller (false).
 */
@SerialName("RestrictSpellsCastPerTurn")
@Serializable
data class RestrictSpellsCastPerTurn(
    val maxPerTurn: Int = 1,
    val eachPlayer: Boolean = false
) : StaticAbility {
    override val description: String
        get() {
            val plural = if (maxPerTurn == 1) "" else "s"
            return if (eachPlayer) {
                "Each player can't cast more than $maxPerTurn spell$plural each turn"
            } else {
                "You can't cast more than $maxPerTurn spell$plural each turn"
            }
        }
}

/**
 * Players can't cast a spell that shares a color with the spell most recently cast this turn.
 * Used by Mana Maze (Invasion).
 *
 * This is a global continuous restriction — it applies to every player, not just the source's
 * controller. The engine tracks the colors of the most recently cast spell on
 * [com.wingedsheep.engine.state.GameState.lastCastSpellColors] (cleared at the start of each turn)
 * and, while any permanent on the battlefield has this static ability, refuses to cast a spell
 * whose colors overlap that set. A colorless spell shares no color, so it is always castable and
 * casting one (an empty color set) lifts the restriction until the next colored spell is cast.
 */
@SerialName("CantCastSpellsSharingColorWithLastCast")
@Serializable
data object CantCastSpellsSharingColorWithLastCast : StaticAbility {
    override val description: String =
        "Players can't cast a spell that shares a color with the spell most recently cast this turn"
}

/**
 * Generic "may cast a spell without paying its mana cost" battlefield permission. The static is
 * read at cast-legality time on every permanent on the battlefield; when its gates are open it
 * surfaces a `CastWithoutPayingManaCost` [com.wingedsheep.engine.legalactions.LegalAction]
 * variant routing through `CastSpell.useWithoutPayingManaCost`.
 *
 * Per CR 118.9, "without paying its mana cost" is itself an alternative cost; mandatory
 * additional costs (kicker, blight, behold, etc.) still apply, and per CR 107.3b X is treated
 * as 0 for X-cost spells cast this way. Per CR 118.9a only one alternative cost can apply to
 * a given cast, so the variant is **mutually exclusive** with `useAlternativeCost`
 * (validated in `CastSpellHandler`).
 *
 * The variant is emitted **alongside** Jodah-style [GrantAlternativeCastingCost], flashback,
 * harmonize, warp, evoke, impending, and `selfAlternativeCost` variants so the player picks
 * one explicitly — CR 118.9a leaves the choice to the player, not handler priority.
 *
 * Composable gates:
 *  - [controllerOnly] — if true, only the source permanent's controller benefits ("you cast"
 *    wording). Default false = any player benefits ("each player casts" wording).
 *  - [firstSpellOfTurnOnly] — if true, the caster must be the active player and must have cast
 *    no spells yet this turn. Default false = no first-spell gate.
 *  - [spellFilter] — only spells whose card matches this filter may be cast for free
 *    (card predicates work in any zone). Default [GameObjectFilter.Any] = every spell.
 *    Dracogenesis is `MayCastWithoutPayingManaCost(controllerOnly = true,
 *    spellFilter = GameObjectFilter.Any.withSubtype("Dragon"))`.
 *
 * Composes for Weftwalking ({4}{U}{U}, EOE: "The first spell each player casts during each of
 * their turns may be cast without paying its mana cost.") via
 * `MayCastWithoutPayingManaCost(firstSpellOfTurnOnly = true)`. A future "you may cast the first
 * spell you cast each turn without paying its mana cost" composes via
 * `MayCastWithoutPayingManaCost(controllerOnly = true, firstSpellOfTurnOnly = true)`. An
 * always-on Omniscience-style static would set both gates false (though Omniscience itself is
 * modelled via `PlayWithoutPayingCostComponent` on the spell rather than this static).
 *
 * Modelled as a static ability on a permanent so the permission tracks the permanent's
 * presence on the battlefield — removing the source in response to the cast offer immediately
 * revokes it.
 */
@SerialName("MayCastWithoutPayingManaCost")
@Serializable
data class MayCastWithoutPayingManaCost(
    val controllerOnly: Boolean = false,
    val firstSpellOfTurnOnly: Boolean = false,
    val spellFilter: GameObjectFilter = GameObjectFilter.Any,
    /**
     * When true, the permission may be used at most once during each of the caster's own turns —
     * unlike [firstSpellOfTurnOnly] (which forces the free cast to be the turn's *first* spell),
     * the caster may cast other spells first and still use this free cast once. Like
     * [firstSpellOfTurnOnly] it requires the caster to be the active player ("each of your turns").
     * Each source tracks its own use via
     * `com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent`,
     * cleared at end of turn. Zaffai and the Tempests:
     * `MayCastWithoutPayingManaCost(controllerOnly = true, oncePerTurn = true,
     * spellFilter = GameObjectFilter.InstantOrSorcery)`.
     */
    val oncePerTurn: Boolean = false,
    /**
     * When true, the permission applies only to spells **cast from exile** — the free cast is
     * offered on the cast-from-exile path and withheld from hand / graveyard / other casts. Warped
     * Space (Charred Foyer // Warped Space): "Once each turn, you may pay {0} rather than pay the
     * mana cost for a spell you cast from exile" → `MayCastWithoutPayingManaCost(controllerOnly =
     * true, oncePerTurn = true, fromExileOnly = true)`. The engine gates this on the cast's source
     * zone in `CostCalculator.hasFreeCastPermission` / `oncePerTurnFreeCastSourceToConsume`.
     */
    val fromExileOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        val noun = if (spellFilter == GameObjectFilter.Any) "spells" else "${spellFilter.description} spells"
        val singular = if (spellFilter == GameObjectFilter.Any) "spell" else "${spellFilter.description} spell"
        val fromExile = if (fromExileOnly) " from exile" else ""
        if (oncePerTurn) {
            append("Once during each of your turns, you may cast a $singular$fromExile without paying its mana cost")
        } else if (firstSpellOfTurnOnly) {
            if (controllerOnly) append("The first spell you cast each turn may be cast without paying its mana cost")
            else append("The first spell each player casts during each of their turns may be cast without paying its mana cost")
        } else {
            if (controllerOnly) append("You may cast $noun without paying their mana costs")
            else append("Each player may cast $noun without paying their mana costs")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        return if (newFilter !== spellFilter) copy(spellFilter = newFilter) else this
    }
}

/**
 * A continuous prohibition on casting spells, scoped along three independent axes — each a
 * reused SDK primitive, so one type covers a whole family of "can't cast" cards:
 *
 *  - **who** — [affected], a [Player] reference interpreted *relative to this permanent's
 *    controller*: `EachOpponent` / `Opponent` (your opponents), `You` (the controller), `Each`
 *    (everyone).
 *  - **which** — [spellFilter], matched against the card being cast (card predicates work in any
 *    zone). Defaults to every spell.
 *  - **when** — [condition], evaluated in the controller's context, so `IsYourTurn` reads as
 *    "during your turn" and `IsNotYourTurn` as "during an opponent's turn". `null` = always.
 *
 * Read at cast-legality time (never on the stack), so it composes with every casting path —
 * hand, graveyard flashback/harmonize, exile, top of library. Control is read from projected
 * state, so a control-changing effect flips who is restricted.
 *
 *  - Voice of Victory: `PlayersCantCastSpells(Player.EachOpponent, condition = IsYourTurn)`
 *    ("Your opponents can't cast spells during your turn.")
 *  - Grand Abolisher's cast clause: `PlayersCantCastSpells(Player.EachOpponent)` (every turn).
 *  - Void Winnower: `PlayersCantCastSpells(Player.EachOpponent, spellFilter =
 *    GameObjectFilter(cardPredicates = listOf(CardPredicate.ManaValueIsEven)))`.
 *
 * @property affected Who is forbidden, relative to the source's controller.
 * @property spellFilter Which spells are forbidden (matched against the card being cast).
 * @property condition Optional timing/state gate, evaluated in the controller's context; null = always.
 */
/**
 * Players matching [affected] can't play lands (CR 305.1) — Worms of the Earth's "players can't
 * play lands", with [Player.Each] the printed form.
 *
 * The land-play sibling of [PlayersCantCastSpells], and deliberately separate from it: playing a
 * land is a special action, not casting a spell, so a card that stops one says nothing about the
 * other. Enforced both in `PlayLandHandler` (rejecting the action) and in `EnumerationContext`
 * (never advertising it), because a legal-action list that offers a land drop the handler will
 * refuse is worse than either alone.
 *
 * This stops the *play*. A land put onto the battlefield by an effect is a different event and
 * needs [LandsCantEnterTheBattlefield]; Worms of the Earth prints both lines for exactly that
 * reason.
 */
/**
 * Lands can't enter the battlefield — Worms of the Earth's second lock line.
 *
 * Separate from [PlayersCantPlayLands] because it catches a different event: that one stops the
 * *special action* of playing a land, this one stops a land arriving by any other route (a search
 * effect, a reanimation, a blink). Worms of the Earth prints both lines precisely because neither
 * subsumes the other, and a card that printed only this one would still let a land be played.
 *
 * The land simply does not enter (CR 614.12-style prohibition): the move is a no-op and the card
 * stays where it was.
 */
@SerialName("LandsCantEnterTheBattlefield")
@Serializable
data object LandsCantEnterTheBattlefield : StaticAbility {
    override val description: String = "Lands can't enter the battlefield"
}

@SerialName("PlayersCantPlayLands")
@Serializable
data class PlayersCantPlayLands(
    val affected: Player = Player.Each,
    val condition: Condition? = null
) : StaticAbility {
    override val description: String = buildString {
        when (affected) {
            is Player.You -> append("You can't play lands")
            is Player.EachOpponent -> append("Your opponents can't play lands")
            is Player.Each -> append("Players can't play lands")
            else -> append("${affected.description.replaceFirstChar { it.uppercase() }} can't play lands")
        }
        condition?.let { append(" ${it.description}") }
    }
}

@SerialName("PlayersCantCastSpells")
@Serializable
data class PlayersCantCastSpells(
    val affected: Player = Player.EachOpponent,
    val spellFilter: GameObjectFilter = GameObjectFilter.Any,
    val condition: Condition? = null
) : StaticAbility {
    override val description: String = buildString {
        when (affected) {
            is Player.You -> append("You can't cast ")
            is Player.EachOpponent -> append("Your opponents can't cast ")
            else -> append("${affected.description.replaceFirstChar { it.uppercase() }} can't cast ")
        }
        append(if (spellFilter == GameObjectFilter.Any) "spells" else "${spellFilter.description} spells")
        when (condition) {
            is IsYourTurn -> append(" during your turn")
            is IsNotYourTurn -> append(" during your opponents' turns")
            null -> {}
            else -> append(" ${condition.description}")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = spellFilter.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newFilter === spellFilter && newCondition === condition) this
        else copy(spellFilter = newFilter, condition = newCondition)
    }
}

/**
 * A continuous prohibition on activating activated abilities, scoped along the same three
 * independent axes as [PlayersCantCastSpells] — each a reused SDK primitive, so one type covers a
 * whole family of "can't activate" cards:
 *
 *  - **who** — [affected], a [Player] reference interpreted *relative to this permanent's
 *    controller*: `EachOpponent` / `Opponent` (your opponents), `You` (the controller), `Each`
 *    (everyone). The activating player is the controller of the permanent whose ability is being
 *    activated, so this gates by who controls the source permanent.
 *  - **which** — [permanentFilter], matched (in projected state) against the **permanent whose
 *    ability is being activated**, not the ability itself. Defaults to every permanent.
 *  - **when** — [condition], evaluated in the source's controller's context, so `IsYourTurn` reads
 *    as "during your turn". `null` = always.
 *
 * Read at ability-activation-legality time on every battlefield permanent (mirrors how
 * [PlayersCantCastSpells] is read at cast-legality time), so it composes with mana and non-mana
 * activated abilities alike. Like [PreventActivatedAbilities] this blocks the source's activated
 * abilities; unlike it, it additionally scopes by *who* is activating and *when*, which the
 * who/when-blind [PreventActivatedAbilities] (Cursed Totem) can't express.
 *
 *  - Grand Abolisher's activate clause: `PlayersCantActivateAbilities(Player.EachOpponent,
 *    permanentFilter = artifact-or-creature-or-enchantment, condition = IsYourTurn)`
 *    ("During your turn, your opponents can't activate abilities of artifacts, creatures, or
 *    enchantments.")
 *
 * @property affected Who is forbidden, relative to the source's controller.
 * @property permanentFilter Which permanents' abilities are forbidden (matched against the source).
 * @property condition Optional timing/state gate, evaluated in the controller's context; null = always.
 */
@SerialName("PlayersCantActivateAbilities")
@Serializable
data class PlayersCantActivateAbilities(
    val affected: Player = Player.EachOpponent,
    val permanentFilter: GameObjectFilter = GameObjectFilter.Any,
    val condition: Condition? = null
) : StaticAbility {
    override val description: String = buildString {
        when (condition) {
            is IsYourTurn -> append("During your turn, ")
            is IsNotYourTurn -> append("During your opponents' turns, ")
            else -> {}
        }
        when (affected) {
            is Player.You -> append("you can't activate ")
            is Player.EachOpponent -> append("your opponents can't activate ")
            else -> append("${affected.description} can't activate ")
        }
        append(
            if (permanentFilter == GameObjectFilter.Any) "activated abilities"
            else "abilities of ${permanentFilter.description}"
        )
        when (condition) {
            is IsYourTurn, is IsNotYourTurn, null -> {}
            else -> append(" ${condition.description}")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = permanentFilter.applyTextReplacement(replacer)
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newFilter === permanentFilter && newCondition === condition) this
        else copy(permanentFilter = newFilter, condition = newCondition)
    }
}

/**
 * Which family of keyword-prefixed "Activate this ability only once" abilities an
 * [ExtraOnceOnlyActivations] permission reaches.
 *
 * Both keywords install the same [com.wingedsheep.sdk.scripting.ActivationRestriction.Once] on the
 * ability they prefix, so the permission has to say *which* of them it lifts — a card that raises
 * the power-up limit must not also raise an exhaust limit, and neither ever touches a plain `Once`
 * an ordinary ability printed for itself.
 */
@Serializable
enum class OnceOnlyAbilityKind {
    /** Exhaust, CR 702.177. */
    EXHAUST,

    /** Power-up, CR 702.193. */
    POWER_UP;

    /** How the keyword reads inside a rules sentence. */
    val displayName: String get() = when (this) {
        EXHAUST -> "exhaust"
        POWER_UP -> "power-up"
    }
}

/**
 * The controller may activate [kind] abilities more times than the keyword's "Activate this ability
 * only once" allows — i.e. the [com.wingedsheep.sdk.scripting.ActivationRestriction.Once] memory
 * that `isExhaust`/`isPowerUp` installs is raised by [extraActivations], or waived outright when
 * that is `null`.
 *
 * Read at ability-activation-legality time on every battlefield permanent, scoped to the activating
 * player being this permanent's controller (you can only activate abilities of permanents you
 * control, so there is no separate "who" axis), and gated by [condition], evaluated in the
 * controller's context — `null` = always.
 *
 * It is the permission counterpart of [PlayersCantActivateAbilities] but not its exact mirror:
 * that type carries a `permanentFilter`, while this one hardcodes "every [kind] ability of
 * permanents you control". Both printed cards want that scope, so the axis is deliberately absent;
 * a future "each power-up ability of *creatures* you control" would have to add it.
 *
 * Two shapes exist in print, and the [extraActivations] axis is exactly what separates them:
 *  - **Waive** — Elvish Refueler: "During your turn, as long as you haven't activated an exhaust
 *    ability this turn, you may activate exhaust abilities as though they haven't been activated."
 *    = `ExtraOnceOnlyActivations(EXHAUST, extraActivations = null, condition = IsYourTurn AND
 *    YouHaventActivatedAnExhaustAbilityThisTurn)`. The condition makes the waiver evaporate the
 *    moment the turn's first exhaust ability is activated, which is what bounds it.
 *  - **Raise by N** — Wonder Man, Hollywood Hero: "Each power-up ability of permanents you control
 *    can be activated an additional time." = `ExtraOnceOnlyActivations(POWER_UP,
 *    extraActivations = 1)`. Two copies grant two extra activations: the allowance is summed across
 *    every permanent whose condition currently holds, and compared against how many times *that*
 *    ability of *that* object has been activated.
 *
 * Abilities of the other kind, and any ability carrying a plain `Once`/`OncePerTurn` restriction it
 * printed for itself, are untouched.
 *
 * @property kind Which keyword's once-only limit this lifts. Required — a default on a two-valued
 *   discriminator would let `ExtraOnceOnlyActivations(extraActivations = 1)` compile and silently
 *   mean the wrong keyword, which is the one mistake this axis exists to prevent.
 * @property extraActivations Extra activations granted per instance; `null` = no limit at all.
 *   Must otherwise be at least 1 — 0 grants nothing and a negative would lower the ceiling below
 *   the printed activation.
 * @property condition Optional timing/state gate, evaluated in the controller's context; null = always.
 */
@SerialName("ExtraOnceOnlyActivations")
@Serializable
data class ExtraOnceOnlyActivations(
    val kind: OnceOnlyAbilityKind,
    val extraActivations: Int? = null,
    val condition: Condition? = null
) : StaticAbility {
    init {
        require(extraActivations == null || extraActivations >= 1) {
            "extraActivations must be null (waive the limit) or at least 1, was $extraActivations"
        }
    }

    override val description: String = buildString {
        when (condition) {
            is IsYourTurn -> append("During your turn, ")
            is IsNotYourTurn -> append("During your opponents' turns, ")
            else -> {}
        }
        when (extraActivations) {
            null -> append("you may activate ${kind.displayName} abilities as though they haven't been activated")
            1 -> append(
                "each ${kind.displayName} ability of permanents you control can be activated an additional time"
            )
            else -> append(
                "each ${kind.displayName} ability of permanents you control can be activated " +
                    "$extraActivations additional times"
            )
        }
        when (condition) {
            is IsYourTurn, is IsNotYourTurn, null -> {}
            else -> append(" ${condition.description}")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newCondition = condition?.applyTextReplacement(replacer)
        return if (newCondition === condition) this else copy(condition = newCondition)
    }
}
