package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.targeting.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * An activated ability is an ability that a player can activate by paying a cost.
 * Format: "[Cost]: [Effect]"
 */
@Serializable
data class ActivatedAbility(
    val id: AbilityId,
    val cost: AbilityCost,
    val effect: Effect,
    val targetRequirement: TargetRequirement? = null,
    val timing: TimingRule = TimingRule.InstantSpeed,
    val restrictions: List<ActivationRestriction> = emptyList(),
    val isManaAbility: Boolean = false,
    val isPlaneswalkerAbility: Boolean = false,
    val activateFromZone: ZoneType = ZoneType.BATTLEFIELD
) {
    val description: String
        get() = "${cost.description}: ${effect.description}"
}

/**
 * Costs for activated abilities.
 */
@Serializable
sealed interface AbilityCost {
    val description: String

    /** Tap the permanent ({T}) */
    @Serializable
    data object Tap : AbilityCost {
        override val description: String = "{T}"
    }

    /** Untap the permanent ({Q}) */
    @Serializable
    data object Untap : AbilityCost {
        override val description: String = "{Q}"
    }

    /** Pay mana */
    @Serializable
    data class Mana(val cost: ManaCost) : AbilityCost {
        override val description: String = cost.toString()
    }

    /** Pay life */
    @Serializable
    data class PayLife(val amount: Int) : AbilityCost {
        override val description: String = "Pay $amount life"
    }

    /**
     * Sacrifice a permanent
     *
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class Sacrifice(
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val unifiedFilter: GameObjectFilter? = null
    ) : AbilityCost {
        override val description: String = "Sacrifice a ${unifiedFilter?.description ?: filter.description}"

        companion object {
            /** Create with unified filter */
            operator fun invoke(unifiedFilter: GameObjectFilter) =
                Sacrifice(filter = CardFilter.AnyCard, unifiedFilter = unifiedFilter)
        }
    }

    /**
     * Discard a card
     *
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class Discard(
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val unifiedFilter: GameObjectFilter? = null
    ) : AbilityCost {
        override val description: String = "Discard a ${unifiedFilter?.description ?: filter.description}"

        companion object {
            /** Create with unified filter */
            operator fun invoke(unifiedFilter: GameObjectFilter) =
                Discard(filter = CardFilter.AnyCard, unifiedFilter = unifiedFilter)
        }
    }

    /**
     * Exile cards from graveyard
     *
     * @property count Number of cards to exile
     * @property filter Legacy filter (deprecated, use unifiedFilter)
     * @property unifiedFilter Unified filter (preferred)
     */
    @Serializable
    data class ExileFromGraveyard(
        val count: Int,
        @Deprecated("Use unifiedFilter instead")
        val filter: CardFilter = CardFilter.AnyCard,
        val unifiedFilter: GameObjectFilter? = null
    ) : AbilityCost {
        override val description: String = buildString {
            val filterDesc = unifiedFilter?.description ?: filter.description
            append("Exile $count $filterDesc")
            if (count > 1) append("s")
            append(" from your graveyard")
        }

        companion object {
            /** Create with unified filter */
            operator fun invoke(count: Int, unifiedFilter: GameObjectFilter) =
                ExileFromGraveyard(count = count, filter = CardFilter.AnyCard, unifiedFilter = unifiedFilter)
        }
    }

    /** Discard self (the card with this ability) - used for cycling */
    @Serializable
    data object DiscardSelf : AbilityCost {
        override val description: String = "Discard this card"
    }

    /** Multiple costs combined */
    @Serializable
    data class Composite(val costs: List<AbilityCost>) : AbilityCost {
        override val description: String = costs.joinToString(", ") { it.description }
    }

    /** Loyalty cost for planeswalker abilities */
    @Serializable
    data class Loyalty(val change: Int) : AbilityCost {
        override val description: String = if (change >= 0) "+$change" else "$change"
    }
}
