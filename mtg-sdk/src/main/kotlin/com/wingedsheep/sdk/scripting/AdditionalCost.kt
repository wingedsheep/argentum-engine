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
     */
    @Serializable
    data class SacrificePermanent(
        val filter: CardFilter,
        val count: Int = 1
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Sacrifice ")
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filter.description)
            if (count != 1) append("s")
        }
    }

    /**
     * Discard cards from hand.
     * Example: "Discard a card" or "Discard 2 cards"
     */
    @Serializable
    data class DiscardCards(
        val count: Int = 1,
        val filter: CardFilter = CardFilter.AnyCard
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Discard ")
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filter.description)
            if (count != 1) append("s")
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
     */
    @Serializable
    data class ExileCards(
        val count: Int = 1,
        val filter: CardFilter = CardFilter.AnyCard,
        val fromZone: CostZone = CostZone.GRAVEYARD
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Exile ")
            if (count == 1) {
                append("a ")
            } else {
                append("$count ")
            }
            append(filter.description)
            if (count != 1) append("s")
            append(" from your ${fromZone.description}")
        }
    }

    /**
     * Tap permanents you control.
     * Example: "Tap an untapped creature you control"
     */
    @Serializable
    data class TapPermanents(
        val count: Int = 1,
        val filter: CardFilter = CardFilter.CreatureCard
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Tap ")
            if (count == 1) {
                append("an untapped ")
            } else {
                append("$count untapped ")
            }
            append(filter.description)
            if (count != 1) append("s")
            append(" you control")
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
