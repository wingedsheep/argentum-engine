package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
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
sealed interface AdditionalCost : TextReplaceable<AdditionalCost> {
    /** Human-readable description of the cost */
    val description: String

    /**
     * Sacrifice a permanent matching the given filter.
     * Example: "Sacrifice a green creature" for Natural Order
     *
     * @property filter Which permanents can be sacrificed
     * @property count Number of permanents to sacrifice
     */
    @SerialName("SacrificePermanent")
    @Serializable
    data class SacrificePermanent(
        val filter: GameObjectFilter = GameObjectFilter.Any,
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

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Discard cards from hand.
     * Example: "Discard a card" or "Discard 2 cards"
     *
     * @property count Number of cards to discard
     * @property filter Which cards can be discarded
     */
    @SerialName("DiscardCards")
    @Serializable
    data class DiscardCards(
        val count: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any
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

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Pay life as an additional cost.
     * Example: "Pay 1 life" for Phyrexian mana
     */
    @SerialName("PayLife")
    @Serializable
    data class PayLife(
        val amount: Int
    ) : AdditionalCost {
        override val description: String = "Pay $amount life"
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost = this
    }

    /**
     * Exile cards from a zone (usually graveyard or hand).
     * Example: "Exile a creature card from your graveyard"
     *
     * @property count Number of cards to exile
     * @property filter Which cards can be exiled
     * @property fromZone Zone to exile from
     */
    @SerialName("ExileCards")
    @Serializable
    data class ExileCards(
        val count: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any,
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

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Exile a variable number of cards from a zone as an additional cost.
     * The player chooses how many matching cards to exile (at least [minCount]).
     * Example: "Exile X creature cards from your graveyard" for Chill Haunting
     *
     * @property minCount Minimum number of cards to exile (default 1)
     * @property filter Which cards can be exiled
     * @property fromZone Zone to exile from
     */
    @SerialName("ExileVariableCards")
    @Serializable
    data class ExileVariableCards(
        val minCount: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val fromZone: CostZone = CostZone.GRAVEYARD
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Exile X ")
            append(filter.description)
            append("s from your ${fromZone.description}")
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Tap permanents you control.
     * Example: "Tap an untapped creature you control"
     *
     * @property count Number of permanents to tap
     * @property filter Which permanents can be tapped
     */
    /**
     * Sacrifice any number of permanents matching the given filter as an additional cost.
     * Each sacrifice reduces the spell's generic mana cost by [costReductionPerCreature].
     * Example: "You may sacrifice any number of creatures. This spell costs {2} less for each creature sacrificed."
     *
     * @property filter Which permanents can be sacrificed
     * @property costReductionPerCreature Generic mana reduction per sacrificed creature
     */
    @SerialName("SacrificeCreaturesForCostReduction")
    @Serializable
    data class SacrificeCreaturesForCostReduction(
        val filter: GameObjectFilter = GameObjectFilter.Creature,
        val costReductionPerCreature: Int = 2
    ) : AdditionalCost {
        override val description: String = "You may sacrifice any number of creatures. This spell costs {$costReductionPerCreature} less to cast for each creature sacrificed this way."
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    @SerialName("TapPermanents")
    @Serializable
    data class TapPermanents(
        val count: Int = 1,
        val filter: GameObjectFilter = GameObjectFilter.Creature
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

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
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
    val tappedPermanents: List<EntityId> = emptyList(),

    /** Permanents that were returned to hand */
    val bouncedPermanents: List<EntityId> = emptyList(),

    /** Counter removals: entity ID -> number of +1/+1 counters to remove from that creature */
    val counterRemovals: Map<EntityId, Int> = emptyMap()
) {
    /** Check if any costs were paid */
    val isEmpty: Boolean
        get() = sacrificedPermanents.isEmpty() &&
                discardedCards.isEmpty() &&
                lifePaid == 0 &&
                exiledCards.isEmpty() &&
                tappedPermanents.isEmpty() &&
                bouncedPermanents.isEmpty() &&
                counterRemovals.isEmpty()

    companion object {
        val NONE = AdditionalCostPayment()
    }
}
