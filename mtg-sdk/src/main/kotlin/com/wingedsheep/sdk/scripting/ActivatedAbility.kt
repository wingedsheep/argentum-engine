package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ZoneType
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

    /** Sacrifice a permanent */
    @Serializable
    data class Sacrifice(val filter: CardFilter) : AbilityCost {
        override val description: String = "Sacrifice a ${filter.description}"
    }

    /** Discard a card */
    @Serializable
    data class Discard(val filter: CardFilter = CardFilter.AnyCard) : AbilityCost {
        override val description: String = "Discard a ${filter.description}"
    }

    /** Exile cards from graveyard */
    @Serializable
    data class ExileFromGraveyard(val count: Int, val filter: CardFilter = CardFilter.AnyCard) : AbilityCost {
        override val description: String = "Exile $count ${filter.description}${if (count > 1) "s" else ""} from your graveyard"
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
