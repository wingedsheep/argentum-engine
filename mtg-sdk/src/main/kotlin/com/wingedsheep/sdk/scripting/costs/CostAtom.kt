package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.util.quantify
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One shared vocabulary of *atomic* payable things — the things you can be asked to pay
 * regardless of which cost *context* asks for them.
 *
 * MTG has three parallel cost contexts: an activated-ability cost ([com.wingedsheep.sdk.scripting.AbilityCost]),
 * an additional cost paid while casting a spell ([com.wingedsheep.sdk.scripting.AdditionalCost]), and a
 * "payable cost" used by morph / "unless you …" / player-choice mechanics
 * ([PayCost]). Before [CostAtom] existed, each context redefined the *same* payable things — "sacrifice
 * a creature", "discard a card", "pay N life", "exile from a zone", … — so a new payable thing had to be
 * implemented once per context and could land in one but not the others. This is the **§3.2 "one cost
 * language"** vocabulary: each payable concept is declared *once*, here, and each context carries it via
 * its own `Atom` wrapper.
 *
 * **What lives here:** payable things whose meaning is identical across contexts — the *what* is paid,
 * not the *when* or *why*. Most counts are plain [Int]s because most shared costs have a fixed count.
 *
 * **The line is context-dependence, not variability.** A cost belongs on a wrapper when its *meaning*
 * changes with the context that asks for it — Echo's timing, kicker's linkage, Forage's mode choice
 * wired into cast-time action enumeration. Being *variable* is not by itself disqualifying: an atom
 * whose count is a player choice is still the same payable thing everywhere, and [VariablePermanents]
 * ("exile/sacrifice one or more … with total mana value X") and [CollectEvidence] (CR 701.59 — "exile
 * any number of cards from your graveyard with total mana value N or greater") both live here for that
 * reason. What stays off them is the context-specific *rider*: [CollectEvidence] carries the payment
 * but not the "if evidence was collected" linkage, which belongs to the optional-additional-cost
 * wrapper. Costs that are variable *and* context-bound — pay X life, blight X, kicker linkage — remain
 * subtypes on the wrapper that owns that behavior.
 *
 * Each atom's [description] is a canonical, lower-case-leading phrase ("sacrifice a Goblin"); the wrapper
 * adapts casing for its context (mid-sentence "unless you sacrifice a Goblin" vs. leading "Sacrifice a
 * Goblin").
 */
@Serializable
sealed interface CostAtom : TextReplaceable<CostAtom> {
    /** Canonical, lower-case-leading human phrase for this atom (e.g. "sacrifice a Goblin"). */
    val description: String

    /**
     * Number of distinct entities the payer must select for this atom, or 0 for atoms that take no
     * selection (mana, life, random discard). Used by every context to drive selection prompts and
     * affordability checks against the same number.
     */
    val selectionCount: Int
        get() = 0

    /** Pay a mana cost. */
    @SerialName("AtomMana")
    @Serializable
    data class Mana(val cost: ManaCost) : CostAtom {
        override val description: String get() = cost.toString()
    }

    /** Pay [amount] life (CR 119.4 — payable only while life total ≥ amount). */
    @SerialName("AtomPayLife")
    @Serializable
    data class PayLife(val amount: Int) : CostAtom {
        override val description: String get() = "pay $amount life"
    }

    /**
     * Mill [count] cards — put that many cards from the top of your library into your graveyard
     * (CR 701.17a).
     *
     * Takes no selection: the cards milled are the top [count], not a player choice. Per CR 701.17b
     * a player *can't pay a cost that includes milling more cards than their library holds*, so
     * affordability is a plain library-size check — unlike the mill *effect*, which mills as many as
     * possible. A `ModifyMillAmount` replacement (Bruvac) still applies to the announced number when
     * the cost is actually paid, and the resulting library→graveyard zone changes fire mill triggers
     * exactly as an effect's mill does.
     */
    @SerialName("AtomMill")
    @Serializable
    data class Mill(val count: Int = 1) : CostAtom {
        override val description: String get() =
            if (count == 1) "mill a card" else "mill $count cards"
    }

    /**
     * Exile the top [count] cards of your library — Arc-Slogger's "{R}, Exile the top ten cards of
     * your library".
     *
     * The exile twin of [Mill], and takes no selection for the same reason: the cards are the top
     * of the library, not a player choice. Affordability is a plain library-size check (CR 118.3 —
     * a player can't pay a cost without the resources to pay it fully), so a nine-card library
     * can't pay a ten-card exile at all rather than exiling as many as possible. That is the split
     * from the exile *effect*, which takes what it finds.
     *
     * Distinct from [ExileFrom], which is a *chosen-cards* cost ("exile two cards from your
     * graveyard") and so means "choose N" rather than "the top N". The resulting library→exile zone
     * changes are ordinary ones, and no mill replacement applies: exiling from the top is not
     * milling (CR 701.17a), so nothing enlarges the announced count.
     */
    @SerialName("AtomExileTopOfLibrary")
    @Serializable
    data class ExileTopOfLibrary(val count: Int) : CostAtom {
        override val description: String get() =
            if (count == 1) "exile the top card of your library"
            else "exile the top $count cards of your library"
    }

    /**
     * Sacrifice [count] permanents matching [filter].
     *
     * @property excludeSelf when true the cost's source permanent is excluded from the candidate
     *   pool — "sacrifice another [filter]" (an activated-ability shape; spell additional costs
     *   have no single source permanent to exclude, so they leave this false).
     * @property distinctNames when true the [count] sacrificed permanents must all have different
     *   names — "sacrifice three artifact tokens with different names" (Transmutation Font). The
     *   cost is only payable when at least [count] candidates with distinct names exist, and the
     *   payment is rejected unless the chosen permanents are pairwise distinctly named.
     */
    @SerialName("AtomSacrifice")
    @Serializable
    data class Sacrifice(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1,
        val excludeSelf: Boolean = false,
        val distinctNames: Boolean = false
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() = buildString {
            append("sacrifice ")
            if (excludeSelf && count == 1) append("another ${filter.description}")
            else append(quantify(count, filter.description))
            if (distinctNames) append(" with different names")
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Put one or more permanents matching [filter] you control into another zone — a
     * *variable-count* cost: the payer chooses how many (at least [minCount]). Unlike the
     * fixed-count [Sacrifice] / [ExileFrom] atoms, the number is a player choice made as the ability
     * is activated (CR 601.2b — the value of a variable defined by a cost choice is announced at
     * activation), and the resolving ability reads it as its X value
     * ([com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue]).
     *
     * Three orthogonal axes cover the printed shapes:
     *
     *  - [action] — what happens to the chosen permanents. `EXILE` for "exile one or more …"
     *    (Fabrication Foundry), `SACRIFICE` for "sacrifice one or more …" (Radiant Lotus), `TAP`
     *    for "tap any number of …" (Teamwork N, CR 702.194a). A sacrifice fires "whenever you
     *    sacrifice" triggers; an exile and a tap do not.
     *  - [xMeasure] — how the choice is measured. `TOTAL_MANA_VALUE` for "… with total mana value X"
     *    (bounds a "mana value X or less" target); `COUNT` for "… for each permanent chosen this
     *    way", where X is simply how many were chosen; `TOTAL_POWER` for "… with total power N or
     *    more" (Teamwork). The measure doubles as the ability's X when it resolves.
     *  - [minMeasure] — a *floor on the measure* rather than on the count: "with total power N or
     *    more". 0 means unbounded, in which case only [minCount] constrains the choice.
     *
     * @property filter which permanents you control may be chosen.
     * @property minCount minimum number to choose (default 1 — "one or more"). Set 0 alongside a
     *   [minMeasure] for "any number … with total power N or more", where the count itself is free.
     * @property excludeSelf when true the cost's source permanent is excluded — "exile one or more
     *   *other* [filter] you control" (Fabrication Foundry). Leave false when the source may pay for
     *   itself (Radiant Lotus is an artifact and may sacrifice itself to its own cost), and for
     *   spell additional costs, which have no source permanent on the battlefield to exclude.
     * @property action what the cost does with the chosen permanents.
     * @property xMeasure how the chosen set is measured.
     * @property minMeasure minimum total the chosen set's [xMeasure] must reach (0 = no floor).
     */
    @SerialName("AtomVariablePermanents")
    @Serializable
    data class VariablePermanents(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val minCount: Int = 1,
        val excludeSelf: Boolean = true,
        val action: PermanentCostAction = PermanentCostAction.EXILE,
        val xMeasure: VariableCostMeasure = VariableCostMeasure.TOTAL_MANA_VALUE,
        val minMeasure: Int = 0
    ) : CostAtom {
        // Variable count — the floor the payer must at least select. The picker's max is the number
        // of eligible permanents, resolved by the engine at activation time.
        override val selectionCount: Int get() = minCount
        override val description: String get() = buildString {
            append(when (action) {
                PermanentCostAction.EXILE -> "exile "
                PermanentCostAction.SACRIFICE -> "sacrifice "
                PermanentCostAction.TAP -> "tap "
            })
            append(when {
                minCount <= 0 -> "any number of "
                minCount == 1 -> "one or more "
                else -> "$minCount or more "
            })
            if (excludeSelf) append("other ")
            append("${filter.description}s you control")
            if (minMeasure > 0) when (xMeasure) {
                VariableCostMeasure.TOTAL_POWER -> append(" with total power $minMeasure or more")
                VariableCostMeasure.TOTAL_MANA_VALUE -> append(" with total mana value $minMeasure or more")
                VariableCostMeasure.COUNT -> {}
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Discard [count] cards matching [filter].
     *
     * @property random when true the discard is at random (no player selection — e.g. Pillaging Horde).
     */
    @SerialName("AtomDiscard")
    @Serializable
    data class Discard(
        val count: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val random: Boolean = false
    ) : CostAtom {
        // A random discard is paid without the player choosing which cards, so it takes no selection.
        override val selectionCount: Int get() = if (random) 0 else count
        override val description: String get() = buildString {
            append("discard ")
            append(quantify(count, filter.description))
            if (random) append(" at random")
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Exile [count] cards matching [filter] from [zone]. */
    @SerialName("AtomExileFrom")
    @Serializable
    data class ExileFrom(
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() =
            "exile ${quantify(count, filter.description)} from your ${zone.name.lowercase()}"

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Tap [count] untapped permanents matching [filter] you control.
     *
     * @property excludeSelf when true the cost's source permanent is excluded from the candidate
     *   pool — "tap another untapped [filter] you control".
     */
    @SerialName("AtomTapPermanents")
    @Serializable
    data class TapPermanents(
        val count: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val excludeSelf: Boolean = false
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() = buildString {
            append("tap ")
            if (count == 1) append(if (excludeSelf) "another untapped ${filter.description}" else "an untapped ${filter.description}")
            else append("$count untapped ${filter.description}s")
            append(" you control")
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Return [count] permanents matching [filter] you control to their owner's hand. */
    @SerialName("AtomReturnToHand")
    @Serializable
    data class ReturnToHand(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() = buildString {
            append("return ")
            append(quantify(count, filter.description))
            append(" you control to its owner's hand")
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Remove [count] counter(s) from among permanents matching [filter] you control,
     * or from this permanent when [self] is true.
     *
     * When [counterType] is non-null, only counters of that type are removed
     * (e.g. "Remove two +1/+1 counters from among artifacts you control").
     * When null, counters of any type may be removed in any combination
     * (e.g. Tayam's "Remove three counters from among creatures you control").
     *
     * [count] accepts [DynamicAmount] — use [DynamicAmount.Fixed] for a fixed number
     * or [DynamicAmount.XValue] for a player-chosen X.
     *
     * The player distributes the removal across eligible permanents, choosing
     * which counter types to remove where.
     */
    @SerialName("AtomRemoveCounters")
    @Serializable
    data class RemoveCounters(
        val counterType: String? = null,
        val count: DynamicAmount = DynamicAmount.Fixed(1),
        val filter: GameObjectFilter = GameObjectFilter.Permanent,
        val self: Boolean = false
    ) : CostAtom {
        override val selectionCount: Int get() = when (val c = count) {
            is DynamicAmount.Fixed -> if (self) 0 else c.amount
            else -> 0
        }
        override val description: String get() = buildString {
            append("remove ")
            val counterTypeString = if (counterType != null) "$counterType counter" else "counter"
            val isSingle = count is DynamicAmount.Fixed && count.amount == 1
            when (count) {
                is DynamicAmount.XValue -> append("X ${counterTypeString}s")
                is DynamicAmount.Fixed -> append(quantify(count.amount, counterTypeString))
                else -> throw IllegalArgumentException("Unsupported DynamicAmount type: ${count::class.simpleName}")
            }
            if (self) append(" from this permanent")
            else if (isSingle) {
                append(" from ${filter.indefiniteArticle} ${filter.description} you control")
            } else {
                append(" from among ")
                append(filter.description)
                append("s you control")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Put [count] counters of [counterType] on the permanent the cost belongs to — "Put a page
     * counter on this artifact" (Mazemind Tome), the *accruing* mirror of [RemoveCounters] with
     * `self = true`.
     *
     * Self-scoped by design: every printed counter-adding cost puts the counters on the very
     * permanent whose ability is being activated, so there is nothing to select and nothing to
     * choose ([selectionCount] stays 0). It is also **always payable** — unlike every other atom
     * this one takes nothing away, which is exactly what makes the printed cards work (Mazemind
     * Tome stays activatable right up to the counter that exiles it).
     */
    @SerialName("AtomPutCountersOnSelf")
    @Serializable
    data class PutCountersOnSelf(
        val counterType: String,
        val count: Int = 1,
    ) : CostAtom {
        override val description: String get() = buildString {
            append("put ")
            append(quantify(count, "$counterType counter"))
            append(" on this permanent")
        }
    }

    /**
     * Collect evidence [amount] — exile any number of cards from your graveyard with total mana
     * value [amount] or greater (CR 701.59a, Murders at Karlov Manor).
     *
     * An atom rather than a per-wrapper subtype because the *payable thing* means exactly the same
     * in all three cost contexts, and every one of them has printed cards: an activated-ability
     * cost ([com.wingedsheep.sdk.scripting.AbilityCost.Atom] — Cryptex, Polygraph Orb, Forensic
     * Researcher, Hedge Whisperer, Tenth District Hero, Kylox's Voltstrider), a cast-time
     * additional cost ([com.wingedsheep.sdk.scripting.AdditionalCost.Atom] — Extract a Confession,
     * Vitu-Ghazi Inspector, …), and a payable cost ([PayCost.Atom] — Axebane Ferox's
     * "Ward—Collect evidence 4"). Splitting it per wrapper would have re-created the exact
     * duplication this vocabulary exists to prevent.
     *
     * **What is deliberately *not* here:** the CR 701.59c *linkage* — "if evidence was collected".
     * That is a property of the optional-additional-cost wrapper, not of the payable thing, and
     * rides the existing rail
     * ([com.wingedsheep.sdk.scripting.KeywordAbility.OptionalAdditionalCost] stamping
     * [com.wingedsheep.sdk.scripting.ChoiceSlot.EVIDENCE_COLLECTED], read back through
     * `Conditions.WasEvidenceCollected`) exactly as bargain's linkage does. Keeping the linkage
     * off the atom is what lets the same atom serve the six *unlinked* contexts above.
     *
     * **Variable by nature, like [VariablePermanents].** The payer chooses *how many* cards; the
     * constraint is a floor on their **total mana value**, not on their count, so [selectionCount]
     * is only the floor of 1 card and the real gate is [amount]. Exiling more than [amount] worth
     * is legal, and land cards (mana value 0) are legal selections that contribute nothing.
     *
     * Per CR 701.59b a player who cannot reach [amount] **can't choose to collect evidence** — the
     * option must be *hidden*, not offered and refused. Every affordability check therefore fails
     * closed on "sum of available mana values < [amount]".
     *
     * @property amount The mana-value floor N — the total the exiled cards must meet or exceed.
     * @property linkToSource When true, the cards exiled to pay this cost join the *source
     *   permanent's* linked-exile pile
     *   ([com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent]), so a later
     *   ability on that same permanent can say "cards exiled **with it**"
     *   ([com.wingedsheep.sdk.scripting.effects.CardSource.FromLinkedExile]). Kylox's Voltstrider's
     *   "Collect evidence 6" feeds the pile its attack trigger casts from. Off by default: an
     *   ordinary collect evidence exiles the cards and forgets them, and an unread pile is state
     *   the UI would otherwise tether to the permanent for no reason.
     */
    @SerialName("AtomCollectEvidence")
    @Serializable
    data class CollectEvidence(
        val amount: Int,
        val linkToSource: Boolean = false,
    ) : CostAtom {
        // Variable count: at least one card must be exiled, but the binding constraint is the
        // total mana value, carried separately to the picker.
        override val selectionCount: Int get() = 1
        override val description: String get() = "collect evidence $amount"
    }

    /**
     * Exile **any number** of cards matching [filter] from your graveyard whose combined
     * [measure] is [minTotal] or more — the unnamed, filtered generalization of the shape
     * [CollectEvidence] names: a variable-size graveyard exile gated on a *sum* rather than a
     * count.
     *
     * Baron Helmut Zemo: "Exile any number of black cards from your graveyard with fifteen or more
     * black mana symbols among their mana costs" is
     * `ExileFromGraveyardForTotal(Filters.blackCard, CardMeasure.ColoredManaSymbols([BLACK]), 15)`.
     *
     * Two axes distinguish it from [CollectEvidence], which is otherwise the identical mechanic:
     *  - [filter] — collect evidence spends *any* graveyard card (CR 701.59a); this cost restricts
     *    which cards may be chosen, and non-matching cards are not offered at all;
     *  - [measure] — collect evidence's threshold is always total mana value; this one names the
     *    per-card quantity that is summed, so the same cost shape serves a pip total.
     *
     * **The threshold is a floor on the measure, not on the count** ([selectionCount] is only the
     * floor of one card). Over-paying is legal, and a matching card whose measure is 0 is a legal
     * selection contributing nothing — so "enough cards" never implies "enough total".
     *
     * **Fails closed**, the way CR 701.59b makes collect evidence fail closed: a player whose
     * matching graveyard cards cannot reach [minTotal] can't choose to pay, so the ability is not
     * offered at all rather than offered and refused.
     *
     * @property filter which graveyard cards may be chosen.
     * @property measure the per-card quantity that is summed toward [minTotal].
     * @property minTotal the floor the chosen cards' summed [measure] must meet or exceed.
     */
    @SerialName("AtomExileFromGraveyardForTotal")
    @Serializable
    data class ExileFromGraveyardForTotal(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val measure: CardMeasure,
        val minTotal: Int,
    ) : CostAtom {
        init {
            require(minTotal >= 1) {
                "ExileFromGraveyardForTotal needs minTotal >= 1, got $minTotal (a floor of 0 is " +
                    "satisfied by exiling nothing, which is not a cost)"
            }
        }

        // Variable count, like CollectEvidence: at least one card, with the real gate carried to
        // the picker as a running total of [measure].
        override val selectionCount: Int get() = 1
        override val description: String get() = buildString {
            append("exile any number of ")
            // `filter.description` is a noun phrase without the head noun ("black", "artifact"),
            // and reads "card" for the unfiltered case — so append "cards" only when it isn't
            // already the head noun itself.
            if (filter != GameObjectFilter.Any) append("${filter.description} ")
            append("cards from your graveyard with ")
            append(measure.thresholdPhrase(minTotal))
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /** Reveal [count] cards matching [filter] from your hand (the cards stay in hand). */
    @SerialName("AtomRevealFromHand")
    @Serializable
    data class RevealFromHand(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() = buildString {
            append("reveal ")
            append(quantify(count, filter.description))
            append(" in your hand")
        }

        override fun applyTextReplacement(replacer: TextReplacer): CostAtom {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }
}


/**
 * What a [CostAtom.VariablePermanents] cost does with the permanents the payer chose.
 *
 * The distinction is not cosmetic: a sacrifice puts the permanents into their owners' graveyards
 * and fires "whenever you sacrifice a permanent" triggers (CR 701.17), while an exile moves them
 * to exile and fires none of those.
 */
@Serializable
enum class PermanentCostAction {
    /** "Exile one or more artifacts you control …" (Fabrication Foundry). */
    EXILE,

    /** "Sacrifice one or more artifacts …" (Radiant Lotus). */
    SACRIFICE,

    /**
     * "Tap any number of creatures you control …" (Teamwork N, CR 702.194a). Only untapped
     * permanents may be chosen (CR 701.26a); this is a cost, not the `{T}` symbol, so summoning
     * sickness (CR 302.6) does not apply — the same rule crew and saddle already follow.
     */
    TAP
}

/**
 * How a [CostAtom.VariablePermanents] choice is measured — both as the ability's X (CR 601.2b) and
 * as the quantity a [CostAtom.VariablePermanents.minMeasure] floor is compared against.
 */
@Serializable
enum class VariableCostMeasure {
    /**
     * X is the sum of the chosen permanents' mana values — "… with total mana value X"
     * (Fabrication Foundry, whose reanimation target is then bounded "mana value X or less").
     */
    TOTAL_MANA_VALUE,

    /**
     * X is simply how many permanents were chosen — the "… for each permanent sacrificed this way"
     * shape (Radiant Lotus adds three mana per artifact sacrificed).
     */
    COUNT,

    /**
     * The measure is the sum of the chosen permanents' **projected** power — "… with total power N
     * or more" (Teamwork N, CR 702.194a; the same quantity crew and saddle sum). Read from
     * projected state so a lord bonus or a +1/+1 counter counts toward the threshold.
     */
    TOTAL_POWER
}

/**
 * A per-card quantity that a variable-size *card* selection can be summed against —
 * the graveyard-side counterpart of [VariableCostMeasure] (which measures battlefield permanents).
 *
 * Kept separate rather than folded into [VariableCostMeasure] because the two answer different
 * questions about different objects: [VariableCostMeasure] reads *projected* battlefield state
 * (a lord bonus counts toward total power), while a card in a graveyard has only its printed,
 * intrinsic characteristics (CR 202.3) — there is no projection to read, and TOTAL_POWER has no
 * meaning for a card that isn't a permanent. Sharing one enum would offer every variant to both
 * sites and make half of them nonsense.
 *
 * Used by [CostAtom.ExileFromGraveyardForTotal] and — through the shared resolver — by
 * [CostAtom.CollectEvidence].
 */
@Serializable
sealed interface CardMeasure {
    /**
     * How the "N or more" threshold reads in this measure's own words, appended after
     * "… from your graveyard with ".
     */
    fun thresholdPhrase(minTotal: Int): String

    /**
     * What one unit of this measure is called, for a running total the player watches while
     * picking ("3 / 6 mana value"). The bare noun phrase only — the numbers around it belong to
     * whoever renders the tally, and [thresholdPhrase] is the sentence form.
     */
    val unitLabel: String

    /**
     * The card's mana value (CR 202.3) — the measure collect evidence N uses
     * ("with total mana value N or greater", CR 701.59a).
     */
    @SerialName("MeasureManaValue")
    @Serializable
    data object ManaValue : CardMeasure {
        override fun thresholdPhrase(minTotal: Int): String = "total mana value $minTotal or greater"
        override val unitLabel: String get() = "mana value"
    }

    /**
     * How many mana symbols of [colors] appear in the card's **printed** mana cost — Baron Helmut
     * Zemo's "fifteen or more black mana symbols among their mana costs".
     *
     * Counted by [com.wingedsheep.sdk.core.ManaCost.coloredSymbolCount], the single counting rule
     * shared with [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ColoredManaSymbolsAtLeast]
     * (the per-object filter) and
     * [com.wingedsheep.sdk.scripting.values.EntityNumericProperty.ColoredManaSymbolCount] (the
     * per-object amount), so the group total and the per-card reads can never disagree: hybrid and
     * Phyrexian pips count for their colour(s) (CR 107.4e/f); generic, `{C}` and `{X}` count for
     * none; a pip that is two of the requested colours counts once.
     *
     * **Not the same as counting *black cards*** — colour is a characteristic, this is the pips
     * printed on the card. Pair it with a colour filter when the printed cost restricts both, as
     * Zemo's does.
     */
    @SerialName("MeasureColoredManaSymbols")
    @Serializable
    data class ColoredManaSymbols(val colors: List<Color>) : CardMeasure {
        init {
            require(colors.isNotEmpty()) { "ColoredManaSymbols needs at least one color" }
        }

        override fun thresholdPhrase(minTotal: Int): String =
            "$minTotal or more ${colors.joinToString(" or ") { it.displayName.lowercase() }} " +
                "mana symbols among their mana costs"

        override val unitLabel: String
            get() = "${colors.joinToString(" or ") { it.displayName.lowercase() }} mana symbols"
    }
}
