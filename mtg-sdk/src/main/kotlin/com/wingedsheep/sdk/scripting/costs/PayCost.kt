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
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property count How many cards must be discarded (default 1)
     * @property random If true, the discard is random (e.g., Pillaging Horde)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class Discard(
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val count: Int = 1,
        val random: Boolean = false,
        val unifiedFilter: GameObjectFilter? = null
    ) : PayCost {
        override val description: String = buildString {
            append("discard ")
            val filterDesc = unifiedFilter?.description
            if (filterDesc != null) {
                if (count == 1) {
                    append("a $filterDesc")
                } else {
                    append("$count ${filterDesc}s")
                }
            } else {
                @Suppress("DEPRECATION")
                if (count == 1) {
                    when (filter) {
                        CardFilter.AnyCard -> append("a card")
                        CardFilter.LandCard -> append("a land card")
                        CardFilter.CreatureCard -> append("a creature card")
                        else -> append("a ${filter.description}")
                    }
                } else {
                    append("$count ")
                    when (filter) {
                        CardFilter.AnyCard -> append("cards")
                        CardFilter.LandCard -> append("land cards")
                        CardFilter.CreatureCard -> append("creature cards")
                        else -> append("${filter.description}s")
                    }
                }
            }
            if (random) append(" at random")
        }

        companion object {
            /** Create with unified filter */
            operator fun invoke(unifiedFilter: GameObjectFilter, count: Int = 1, random: Boolean = false) =
                Discard(
                    filter = CardFilter.AnyCard,
                    count = count,
                    random = random,
                    unifiedFilter = unifiedFilter
                )
        }
    }

    /**
     * Sacrifice one or more permanents matching a filter to avoid the consequence.
     * "...unless you sacrifice three Forests"
     *
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property count How many permanents must be sacrificed (default 1)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class Sacrifice(
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val count: Int = 1,
        val unifiedFilter: GameObjectFilter? = null
    ) : PayCost {
        override val description: String = buildString {
            append("sacrifice ")
            val filterDesc = unifiedFilter?.description ?: filter.description
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

        companion object {
            /** Create with unified filter */
            operator fun invoke(unifiedFilter: GameObjectFilter, count: Int = 1) =
                Sacrifice(
                    filter = CardFilter.AnyCard,
                    count = count,
                    unifiedFilter = unifiedFilter
                )
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
