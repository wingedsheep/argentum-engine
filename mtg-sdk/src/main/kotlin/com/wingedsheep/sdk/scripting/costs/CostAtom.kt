package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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

    /** Sacrifice [count] permanents matching [filter]. */
    @SerialName("AtomSacrifice")
    @Serializable
    data class Sacrifice(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() = "sacrifice ${quantify(count, filter.description)}"

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

    /** Tap [count] untapped permanents matching [filter] you control. */
    @SerialName("AtomTapPermanents")
    @Serializable
    data class TapPermanents(
        val count: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : CostAtom {
        override val selectionCount: Int get() = count
        override val description: String get() = buildString {
            append("tap ")
            if (count == 1) append("an untapped ${filter.description}")
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
