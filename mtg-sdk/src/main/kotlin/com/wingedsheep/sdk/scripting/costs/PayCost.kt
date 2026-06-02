package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a cost that can be paid in various contexts:
 * - Morph face-up costs (e.g., "Morph {2}{U}" or "Morph—Pay 5 life")
 * - "Unless" mechanics via PayOrSufferEffect (e.g., "unless you sacrifice a creature")
 * - Any future mechanic that requires a payable cost
 */
@Serializable
sealed interface PayCost : TextReplaceable<PayCost> {
    val description: String

    /**
     * Pay a mana cost.
     * "Pay {2}{U}" or "Morph {3}{G}{G}"
     *
     * @property cost The mana cost to pay
     */
    @SerialName("PayMana")
    @Serializable
    data class Mana(val cost: ManaCost) : PayCost {
        override val description: String = cost.toString()
    }

    /**
     * Pay the mana cost of the permanent the cost applies to (its own mana cost).
     *
     * Resolved at payment time by reading the source permanent's `CardComponent.manaCost`,
     * so it works for granted abilities like Essence Leak ("...sacrifice this permanent
     * unless you pay its mana cost"), where the affected permanent — not a fixed cost — owns
     * the mana cost. The engine converts this into a concrete [Mana] cost against that
     * permanent before prompting.
     */
    @SerialName("OwnManaCost")
    @Serializable
    data object OwnManaCost : PayCost {
        override val description: String = "its mana cost"
    }

    /**
     * Discard one or more cards matching a filter.
     * "...unless you discard a land card"
     *
     * @property filter Which cards can be discarded
     * @property count How many cards must be discarded (default 1)
     * @property random If true, the discard is random (e.g., Pillaging Horde)
     */
    @SerialName("Discard")
    @Serializable
    data class Discard(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val count: Int = 1,
        val random: Boolean = false
    ) : PayCost {
        override val description: String = buildString {
            append("discard ")
            if (count == 1) {
                append("a ${filter.description}")
            } else {
                append("$count ${filter.description}s")
            }
            if (random) append(" at random")
        }

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Sacrifice one or more permanents matching a filter to avoid the consequence.
     * "...unless you sacrifice three Forests"
     *
     * @property filter Which permanents can be sacrificed
     * @property count How many permanents must be sacrificed (default 1)
     */
    @SerialName("Sacrifice")
    @Serializable
    data class Sacrifice(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val count: Int = 1
    ) : PayCost {
        override val description: String = buildString {
            append("sacrifice ")
            val filterDesc = filter.description
            if (count == 1) {
                append(if (filterDesc.first().lowercaseChar() in "aeiou") "an" else "a")
                append(" $filterDesc")
            } else {
                append(numberToWord(count))
                append(" ${filterDesc}s")
            }
        }

        private fun numberToWord(n: Int): String = when (n) {
            1 -> "one"
            2 -> "two"
            3 -> "three"
            4 -> "four"
            5 -> "five"
            else -> n.toString()
        }

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Pay life to avoid the consequence.
     * "...unless you pay 3 life"
     *
     * @property amount How much life to pay
     */
    @SerialName("PayLife")
    @Serializable
    data class PayLife(
        val amount: Int
    ) : PayCost {
        override val description: String = "pay $amount life"
    }

    /**
     * Exile one or more cards matching a filter from a specific zone.
     * "...unless you exile a blue card from your hand"
     * "Morph—Exile two cards from your graveyard."
     *
     * @property filter Which cards can be exiled
     * @property zone The zone to exile from (HAND, GRAVEYARD, etc.)
     * @property count How many cards must be exiled (default 1)
     */
    @SerialName("ExileFromZone")
    @Serializable
    data class Exile(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val zone: Zone = Zone.HAND,
        val count: Int = 1
    ) : PayCost {
        override val description: String = buildString {
            append("exile ")
            val filterDesc = filter.description
            if (count == 1) {
                append(if (filterDesc.first().lowercaseChar() in "aeiou") "an" else "a")
                append(" $filterDesc")
            } else {
                append("$count ${filterDesc}s")
            }
            append(" from your ${zone.name.lowercase()}")
        }

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Return one or more permanents you control matching a filter to their owner's hand.
     * "Morph—Return a Bird you control to its owner's hand."
     *
     * @property filter Which permanents can be returned
     * @property count How many permanents must be returned (default 1)
     */
    /**
     * Reveal a card from hand matching a filter.
     * "Morph—Reveal a white card in your hand."
     *
     * The card stays in hand; it is merely shown publicly.
     *
     * @property filter Which cards can be revealed
     * @property count How many cards must be revealed (default 1)
     */
    @SerialName("RevealCard")
    @Serializable
    data class RevealCard(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val count: Int = 1
    ) : PayCost {
        override val description: String = buildString {
            append("Reveal ")
            val filterDesc = filter.description
            if (count == 1) {
                append(if (filterDesc.first().lowercaseChar() in "aeiou") "an" else "a")
                append(" $filterDesc")
            } else {
                append("$count ${filterDesc}s")
            }
            append(" in your hand")
        }

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Choose one of several costs to pay.
     * "...unless they sacrifice a nonland permanent or discard a card"
     *
     * The player chooses which cost to pay (or accepts the consequence).
     *
     * @property options The available cost options
     */
    @SerialName("Choice")
    @Serializable
    data class Choice(
        val options: List<PayCost>
    ) : PayCost {
        override val description: String = options.joinToString(" or ") { it.description }
        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newOptions = options.map { it.applyTextReplacement(replacer) }
            return if (newOptions.zip(options).any { (a, b) -> a !== b }) copy(options = newOptions) else this
        }
    }

    @SerialName("ReturnToHand")
    @Serializable
    data class ReturnToHand(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val count: Int = 1
    ) : PayCost {
        override val description: String = buildString {
            append("Return ")
            val filterDesc = filter.description
            if (count == 1) {
                append(if (filterDesc.first().lowercaseChar() in "aeiou") "an" else "a")
                append(" $filterDesc")
            } else {
                append("$count ${filterDesc}s")
            }
            append(" you control to its owner's hand")
        }

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Tap one or more untapped permanents you control to avoid the consequence.
     * "...unless you tap an untapped permanent you control"
     *
     * The executor restricts candidates to permanents the paying player controls that
     * are untapped, and excludes the source itself. Tapping the selected permanent(s)
     * emits a `TappedEvent` for each one, so "becomes tapped" triggers fire normally.
     *
     * Used by Command Bridge.
     *
     * @property filter Additional filter on the permanents that can be tapped
     *                  (`Any` by default — any of your untapped permanents qualify).
     * @property count How many untapped permanents must be tapped (default 1).
     */
    @SerialName("Tap")
    @Serializable
    data class Tap(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val count: Int = 1
    ) : PayCost {
        override val description: String = buildString {
            append("tap ")
            val filterDesc = filter.description
            if (count == 1) {
                // The article always precedes "untapped", so it is always "an".
                append("an untapped $filterDesc you control")
            } else {
                append("$count untapped ${filterDesc}s you control")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): PayCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }
}
