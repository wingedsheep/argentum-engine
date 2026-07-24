package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
 * not the *when* or *why*. Counts are plain [Int]s because every current shared cost has a fixed count;
 * genuinely *variable* costs (exile X cards, pay X life, blight X) and context-specific oddities (Forage,
 * Behold, Echo timing, kicker linkage) are deliberately **not** atoms — they stay as subtypes on the
 * wrapper that owns their context-specific behavior.
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
     * Exile one or more permanents matching [filter] you control — a *variable-count* cost: the
     * payer chooses how many to exile (at least [minCount]). Unlike the fixed-count [Sacrifice] /
     * [ExileFrom] atoms, the number exiled is a player choice made as the ability is activated (CR
     * 601.2b — the value of a variable defined by a cost choice is announced at activation). The
     * resolving ability reads the **total mana value** of the exiled permanents as its X value
     * ([com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue]), so a target/effect can be
     * bounded "with mana value X or less".
     *
     * @property filter which permanents you control may be exiled.
     * @property minCount minimum number to exile (default 1 — "one or more").
     * @property excludeSelf when true the cost's source permanent is excluded — "exile one or more
     *   *other* [filter] you control" (Fabrication Foundry).
     */
    @SerialName("AtomExilePermanents")
    @Serializable
    data class ExilePermanents(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val minCount: Int = 1,
        val excludeSelf: Boolean = true
    ) : CostAtom {
        // Variable count — the floor the payer must at least select. The picker's max is the number
        // of eligible permanents, resolved by the engine at activation time.
        override val selectionCount: Int get() = minCount
        override val description: String get() = buildString {
            append("exile ")
            append(if (minCount <= 1) "one or more " else "$minCount or more ")
            if (excludeSelf) append("other ")
            append("${filter.description}s you control")
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
 * "a Goblin" / "three Goblins" — the article-or-count phrase shared by the selection atoms. Small
 * counts are spelled out (oracle convention); the article respects a vowel-leading filter description.
 */
private fun quantify(count: Int, filterDescription: String): String =
    if (count == 1) {
        val article = if (filterDescription.firstOrNull()?.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
        "$article $filterDescription"
    } else {
        "${numberToWord(count)} ${filterDescription}s"
    }

private fun numberToWord(n: Int): String = when (n) {
    1 -> "one"
    2 -> "two"
    3 -> "three"
    4 -> "four"
    5 -> "five"
    6 -> "six"
    7 -> "seven"
    8 -> "eight"
    9 -> "nine"
    10 -> "ten"
    else -> n.toString()
}
