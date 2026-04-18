package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
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

    companion object {
        /** "behold a [filter] and exile it" — Behold + ExileFromStorage composed as one cost. */
        fun BeholdAndExile(
            filter: GameObjectFilter,
            count: Int = 1,
            storeAs: String = "beheld"
        ): Composite = Composite(listOf(
            Behold(filter = filter, count = count, storeAs = storeAs),
            ExileFromStorage(from = storeAs, linkToSource = true)
        ))
    }

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

    /**
     * Forage: exile three cards from your graveyard or sacrifice a Food.
     * Used by Bloomburrow cards as an additional cost.
     */
    @SerialName("Forage")
    @Serializable
    data object Forage : AdditionalCost {
        override val description: String = "Forage (Exile three cards from your graveyard or sacrifice a Food)"
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost = this
    }

    /**
     * Blight N or pay additional mana: the caster must either put N -1/-1 counters on a creature
     * they control, or pay extra mana on top of the spell's base mana cost.
     * Used by Lorwyn Eclipsed cards (e.g., Wild Unraveling).
     *
     * The enumerator produces two legal actions: one for the blight path (base cost + creature selection)
     * and one for the pay path (base cost + [alternativeManaCost]).
     *
     * @property blightAmount Number of -1/-1 counters to place
     * @property alternativeManaCost Extra mana to pay instead of blighting (e.g., "{1}")
     */
    @SerialName("BlightOrPay")
    @Serializable
    data class BlightOrPay(
        val blightAmount: Int,
        val alternativeManaCost: String
    ) : AdditionalCost {
        override val description: String = "Blight $blightAmount or pay $alternativeManaCost"
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost = this
    }

    /**
     * Behold: choose a matching permanent you control or reveal a matching card from your hand.
     * Used by Lorwyn Eclipsed cards.
     *
     * Stores the chosen card IDs in [AdditionalCostPayment.beheldCards] and populates
     * pipeline storage under [storeAs] so downstream costs (e.g., [ExileFromStorage])
     * or effects can reference them.
     *
     * @property filter Which cards/permanents can be beheld (e.g., Elf, Kithkin, Goblin)
     * @property count Number of cards to behold
     * @property storeAs Pipeline storage key for the chosen cards
     */
    @SerialName("Behold")
    @Serializable
    data class Behold(
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1,
        val storeAs: String = "beheld"
    ) : AdditionalCost {
        override val description: String = buildString {
            append("Behold ")
            if (count == 1) {
                val filterDesc = filter.description
                val article = if (filterDesc.firstOrNull()?.lowercase() in listOf("a", "e", "i", "o", "u")) "an" else "a"
                append("$article ")
                append(filterDesc)
            } else {
                append("$count ")
                append(filter.description)
                append("s")
            }
        }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Exile cards from a named pipeline collection and optionally link them to the
     * source spell/permanent via LinkedExileComponent.
     *
     * General-purpose: consumes whatever a preceding cost stored under [from].
     * Example: Behold stores as "beheld", then ExileFromStorage("beheld") exiles those cards.
     *
     * @property from Pipeline storage key to read card IDs from
     * @property linkToSource Whether to add LinkedExileComponent (for LTB return patterns)
     */
    @SerialName("ExileFromStorage")
    @Serializable
    data class ExileFromStorage(
        val from: String,
        val linkToSource: Boolean = false
    ) : AdditionalCost {
        override val description: String = "Exile the chosen card"
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost = this
    }

    /**
     * A composite additional cost that groups multiple atomic costs into a single logical cost.
     * The engine processes the steps in order, with pipeline storage flowing between them.
     *
     * Example: "behold an Elf and exile it" is [Behold] + [ExileFromStorage] composed together.
     */
    @SerialName("Composite")
    @Serializable
    data class Composite(
        val steps: List<AdditionalCost>
    ) : AdditionalCost {
        override val description: String = steps.joinToString(", ") { it.description }

        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost {
            val newSteps = steps.map { it.applyTextReplacement(replacer) }
            return if (newSteps.zip(steps).any { (a, b) -> a !== b }) copy(steps = newSteps) else this
        }
    }

    /**
     * Remove a total of [totalCount] counters from among creatures you control.
     * The controller chooses how to distribute the removals across their creatures
     * (any counter types qualify — Dawnhand Dissident's cost is not restricted to +1/+1).
     *
     * The player must have at least [totalCount] counters total on their creatures
     * for this cost to be payable.
     *
     * Used for Dawnhand Dissident: "...by removing three counters from among creatures
     * you control in addition to paying their other costs."
     *
     * @property totalCount How many counters must be removed in total across creatures
     */
    @SerialName("RemoveCountersFromYourCreatures")
    @Serializable
    data class RemoveCountersFromYourCreatures(
        val totalCount: Int
    ) : AdditionalCost {
        override val description: String =
            "Remove $totalCount counters from among creatures you control"
        override fun applyTextReplacement(replacer: TextReplacer): AdditionalCost = this
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

    /** Cards chosen via Behold (from battlefield or hand) */
    val beheldCards: List<EntityId> = emptyList(),

    /** Permanents that were tapped */
    val tappedPermanents: List<EntityId> = emptyList(),

    /** Permanents that were returned to hand */
    val bouncedPermanents: List<EntityId> = emptyList(),

    /** Counter removals: entity ID -> number of +1/+1 counters to remove from that creature */
    val counterRemovals: Map<EntityId, Int> = emptyMap(),

    /** Creature that received -1/-1 counters via Blight */
    val blightTargets: List<EntityId> = emptyList(),

    /**
     * Distributed counter removals for costs like
     * [AdditionalCost.RemoveCountersFromYourCreatures] — each entry removes [count]
     * counters of [counterType] from [entityId]. The engine validates that the sum
     * matches the cost's totalCount and that each creature has enough of each type.
     */
    val distributedCounterRemovals: List<DistributedCounterRemoval> = emptyList()
) {
    /** Check if any costs were paid */
    val isEmpty: Boolean
        get() = sacrificedPermanents.isEmpty() &&
                discardedCards.isEmpty() &&
                lifePaid == 0 &&
                exiledCards.isEmpty() &&
                beheldCards.isEmpty() &&
                tappedPermanents.isEmpty() &&
                bouncedPermanents.isEmpty() &&
                counterRemovals.isEmpty() &&
                blightTargets.isEmpty() &&
                distributedCounterRemovals.isEmpty()

    companion object {
        val NONE = AdditionalCostPayment()
    }
}

/**
 * A single removal entry for distributed counter-removal costs.
 * Remove [count] counters of [counterType] from [entityId].
 */
@Serializable
data class DistributedCounterRemoval(
    val entityId: EntityId,
    val counterType: CounterType,
    val count: Int
)
