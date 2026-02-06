package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
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
    val activateFromZone: Zone = Zone.BATTLEFIELD
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
     * @property filter Which permanents can be sacrificed
     */
    @Serializable
    data class Sacrifice(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : AbilityCost {
        override val description: String = "Sacrifice a ${filter.description}"
    }

    /**
     * Discard a card
     *
     * @property filter Which cards can be discarded
     */
    @Serializable
    data class Discard(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : AbilityCost {
        override val description: String = "Discard a ${filter.description}"
    }

    /**
     * Exile cards from graveyard
     *
     * @property count Number of cards to exile
     * @property filter Which cards can be exiled
     */
    @Serializable
    data class ExileFromGraveyard(
        val count: Int,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : AbilityCost {
        override val description: String = buildString {
            append("Exile $count ${filter.description}")
            if (count > 1) append("s")
            append(" from your graveyard")
        }
    }

    /** Discard self (the card with this ability) - used for cycling */
    @Serializable
    data object DiscardSelf : AbilityCost {
        override val description: String = "Discard this card"
    }

    /**
     * Tap permanents you control.
     * Example: "Tap five untapped Clerics you control"
     *
     * @property count Number of permanents to tap
     * @property filter Which permanents can be tapped
     */
    @Serializable
    data class TapPermanents(
        val count: Int,
        val filter: GameObjectFilter = GameObjectFilter.Creature
    ) : AbilityCost {
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
