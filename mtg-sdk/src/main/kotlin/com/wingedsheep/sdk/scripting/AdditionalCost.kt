package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Represents an additional cost that must be paid when casting a spell.
 * This is separate from mana costs and includes things like:
 * - Sacrifice a creature (Natural Order)
 * - Discard a card (Force of Will)
 * - Pay life (Phyrexian mana)
 *
 * Additional costs are declared in the CardScript and validated/paid during casting.
 */
@Serializable
sealed interface AdditionalCost {
    /** Human-readable description of the cost */
    val description: String

    /**
     * Sacrifice a permanent matching the given filter.
     * Example: "Sacrifice a green creature" for Natural Order
     *
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property count Number of permanents to sacrifice
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class SacrificePermanent(
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val count: Int = 1,
        val unifiedFilter: GameObjectFilter? = null
    ) : AdditionalCost {
        override val description: String = buildString {
            val filterDesc = unifiedFilter?.description ?: filter.description
            append("Sacrifice ")
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filterDesc)
            if (count != 1) append("s")
        }

        companion object {
            /** Create with unified filter */
            operator fun invoke(unifiedFilter: GameObjectFilter, count: Int = 1) =
                SacrificePermanent(
                    filter = CardFilter.AnyCard,
                    count = count,
                    unifiedFilter = unifiedFilter
                )
        }
    }

    /**
     * Discard cards from hand.
     * Example: "Discard a card" or "Discard 2 cards"
     *
     * @property count Number of cards to discard
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class DiscardCards(
        val count: Int = 1,
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val unifiedFilter: GameObjectFilter? = null
    ) : AdditionalCost {
        override val description: String = buildString {
            val filterDesc = unifiedFilter?.description ?: filter.description
            append("Discard ")
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filterDesc)
            if (count != 1) append("s")
        }

        companion object {
            /** Create with unified filter */
            operator fun invoke(count: Int = 1, unifiedFilter: GameObjectFilter) =
                DiscardCards(
                    count = count,
                    filter = CardFilter.AnyCard,
                    unifiedFilter = unifiedFilter
                )
        }
    }

    /**
     * Pay life as an additional cost.
     * Example: "Pay 1 life" for Phyrexian mana
     */
    @Serializable
    data class PayLife(
        val amount: Int
    ) : AdditionalCost {
        override val description: String = "Pay $amount life"
    }

    /**
     * Exile cards from a zone (usually graveyard or hand).
     * Example: "Exile a creature card from your graveyard"
     *
     * @property count Number of cards to exile
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property fromZone Zone to exile from
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class ExileCards(
        val count: Int = 1,
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val fromZone: CostZone = CostZone.GRAVEYARD,
        val unifiedFilter: GameObjectFilter? = null
    ) : AdditionalCost {
        override val description: String = buildString {
            val filterDesc = unifiedFilter?.description ?: filter.description
            append("Exile ")
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filterDesc)
            if (count != 1) append("s")
            append(" from your ${fromZone.description}")
        }

        companion object {
            /** Create with unified filter */
            operator fun invoke(count: Int = 1, unifiedFilter: GameObjectFilter, fromZone: CostZone = CostZone.GRAVEYARD) =
                ExileCards(
                    count = count,
                    filter = CardFilter.AnyCard,
                    fromZone = fromZone,
                    unifiedFilter = unifiedFilter
                )
        }
    }

    /**
     * Tap permanents you control.
     * Example: "Tap an untapped creature you control"
     *
     * @property count Number of permanents to tap
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class TapPermanents(
        val count: Int = 1,
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.CreatureCard,
        val unifiedFilter: GameObjectFilter? = null
    ) : AdditionalCost {
        override val description: String = buildString {
            val filterDesc = unifiedFilter?.description ?: filter.description
            append("Tap ")
            if (count == 1) {
                append("an untapped ")
            } else {
                append("$count untapped ")
            }
            append(filterDesc)
            if (count != 1) append("s")
            append(" you control")
        }

        companion object {
            /** Create with unified filter */
            operator fun invoke(count: Int = 1, unifiedFilter: GameObjectFilter) =
                TapPermanents(
                    count = count,
                    filter = CardFilter.CreatureCard,
                    unifiedFilter = unifiedFilter
                )
        }
    }
}

/**
 * Zones that cards can be exiled from as an additional cost.
 */
@Serializable
enum class CostZone(val description: String) {
    HAND("hand"),
    GRAVEYARD("graveyard"),
    LIBRARY("library"),
    BATTLEFIELD("battlefield")
}

/**
 * Represents the payment of additional costs.
 * This is included in the cast spell action to record what was paid.
 */
@Serializable
data class AdditionalCostPayment(
    /** Permanents that were sacrificed */
    val sacrificedPermanents: List<EntityId> = emptyList(),

    /** Cards that were discarded */
    val discardedCards: List<EntityId> = emptyList(),

    /** Life that was paid */
    val lifePaid: Int = 0,

    /** Cards that were exiled */
    val exiledCards: List<EntityId> = emptyList(),

    /** Permanents that were tapped */
    val tappedPermanents: List<EntityId> = emptyList()
) {
    /** Check if any costs were paid */
    val isEmpty: Boolean
        get() = sacrificedPermanents.isEmpty() &&
                discardedCards.isEmpty() &&
                lifePaid == 0 &&
                exiledCards.isEmpty() &&
                tappedPermanents.isEmpty()

    companion object {
        val NONE = AdditionalCostPayment()
    }
}
