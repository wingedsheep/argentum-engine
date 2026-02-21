package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a cost that can be paid in various contexts:
 * - Morph face-up costs (e.g., "Morph {2}{U}" or "Morph—Pay 5 life")
 * - "Unless" mechanics via PayOrSufferEffect (e.g., "unless you sacrifice a creature")
 * - Any future mechanic that requires a payable cost
 */
@Serializable
sealed interface PayCost {
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
     * Return one or more permanents you control matching a filter to their owner's hand.
     * "Morph—Return a Bird you control to its owner's hand."
     *
     * @property filter Which permanents can be returned
     * @property count How many permanents must be returned (default 1)
     */
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
    }
}
