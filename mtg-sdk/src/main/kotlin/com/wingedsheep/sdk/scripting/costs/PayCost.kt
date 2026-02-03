package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Represents a cost that can be paid to avoid a consequence.
 * Used by PayOrSufferEffect to unify "unless" mechanics.
 */
@Serializable
sealed interface PayCost {
    val description: String

    /**
     * Discard one or more cards matching a filter to avoid the consequence.
     * "...unless you discard a land card"
     *
     * @property filter Which cards can be discarded
     * @property count How many cards must be discarded (default 1)
     * @property random If true, the discard is random (e.g., Pillaging Horde)
     */
    @Serializable
    data class Discard(
        val filter: GameObjectFilter = GameObjectFilter.Any,
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
    @Serializable
    data class Sacrifice(
        val filter: GameObjectFilter = GameObjectFilter.Any,
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
    @Serializable
    data class PayLife(
        val amount: Int
    ) : PayCost {
        override val description: String = "pay $amount life"
    }
}
