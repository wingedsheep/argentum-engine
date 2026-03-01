package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An activated ability is an ability that a player can activate by paying a cost.
 * Format: "[Cost]: [com.wingedsheep.sdk.scripting.effects.Effect]"
 */
@Serializable
data class ActivatedAbility(
    val id: AbilityId = AbilityId.generate(),
    val cost: AbilityCost,
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val timing: TimingRule = TimingRule.InstantSpeed,
    val restrictions: List<ActivationRestriction> = emptyList(),
    val isManaAbility: Boolean = false,
    val isPlaneswalkerAbility: Boolean = false,
    val activateFromZone: Zone = Zone.BATTLEFIELD,
    val promptOnDraw: Boolean = false,
    val descriptionOverride: String? = null
) {
    /** Backward-compatible secondary constructor for single-target abilities. */
    constructor(
        id: AbilityId,
        cost: AbilityCost,
        effect: Effect,
        targetRequirement: TargetRequirement?,
        timing: TimingRule = TimingRule.InstantSpeed,
        restrictions: List<ActivationRestriction> = emptyList(),
        isManaAbility: Boolean = false,
        isPlaneswalkerAbility: Boolean = false,
        activateFromZone: Zone = Zone.BATTLEFIELD,
        promptOnDraw: Boolean = false,
        descriptionOverride: String? = null
    ) : this(
        id = id,
        cost = cost,
        effect = effect,
        targetRequirements = listOfNotNull(targetRequirement),
        timing = timing,
        restrictions = restrictions,
        isManaAbility = isManaAbility,
        isPlaneswalkerAbility = isPlaneswalkerAbility,
        activateFromZone = activateFromZone,
        promptOnDraw = promptOnDraw,
        descriptionOverride = descriptionOverride
    )

    /** Convenience accessor for single-target abilities. */
    val targetRequirement: TargetRequirement?
        get() = targetRequirements.firstOrNull()

    val description: String
        get() = descriptionOverride ?: "${cost.description}: ${effect.description}"
}

/**
 * Costs for activated abilities.
 */
@Serializable
sealed interface AbilityCost {
    val description: String

    /** No cost ({0}) — the ability is free to activate */
    @SerialName("CostFree")
    @Serializable
    data object Free : AbilityCost {
        override val description: String = "{0}"
    }

    /** Tap the permanent ({T}) */
    @SerialName("CostTap")
    @Serializable
    data object Tap : AbilityCost {
        override val description: String = "{T}"
    }

    /** Untap the permanent ({Q}) */
    @SerialName("CostUntap")
    @Serializable
    data object Untap : AbilityCost {
        override val description: String = "{Q}"
    }

    /** Pay mana */
    @SerialName("CostMana")
    @Serializable
    data class Mana(val cost: ManaCost) : AbilityCost {
        override val description: String = cost.toString()
    }

    /** Pay life */
    @SerialName("CostPayLife")
    @Serializable
    data class PayLife(val amount: Int) : AbilityCost {
        override val description: String = "Pay $amount life"
    }

    /**
     * Sacrifice a permanent
     *
     * @property filter Which permanents can be sacrificed
     */
    @SerialName("CostSacrifice")
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
    @SerialName("CostDiscard")
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
    @SerialName("CostExileFromGraveyard")
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

    /** Discard your entire hand */
    @SerialName("CostDiscardHand")
    @Serializable
    data object DiscardHand : AbilityCost {
        override val description: String = "Discard your hand"
    }

    /** Discard self (the card with this ability) - used for cycling */
    @SerialName("CostDiscardSelf")
    @Serializable
    data object DiscardSelf : AbilityCost {
        override val description: String = "Discard this card"
    }

    /** Sacrifice self (the permanent with this ability) */
    @SerialName("CostSacrificeSelf")
    @Serializable
    data object SacrificeSelf : AbilityCost {
        override val description: String = "Sacrifice this permanent"
    }

    /**
     * Sacrifice a creature of the type chosen when this permanent entered the battlefield.
     * Used by cards like Doom Cannon that choose a creature type on entry and reference it in costs.
     */
    @SerialName("CostSacrificeChosenCreatureType")
    @Serializable
    data object SacrificeChosenCreatureType : AbilityCost {
        override val description: String = "Sacrifice a creature of the chosen type"
    }

    /**
     * Tap permanents you control.
     * Example: "Tap five untapped Clerics you control"
     *
     * @property count Number of permanents to tap
     * @property filter Which permanents can be tapped
     */
    @SerialName("CostTapPermanents")
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
    @SerialName("CostComposite")
    @Serializable
    data class Composite(val costs: List<AbilityCost>) : AbilityCost {
        override val description: String = costs.joinToString(", ") { it.description }
    }

    /** Tap the creature this aura is attached to ({T} enchanted creature) */
    @SerialName("CostTapAttachedCreature")
    @Serializable
    data object TapAttachedCreature : AbilityCost {
        override val description: String = "{T} enchanted creature"
    }

    /**
     * Return a permanent you control to its owner's hand.
     * Example: "Return an Elf you control to its owner's hand"
     *
     * @property filter Which permanents can be returned
     */
    @SerialName("CostReturnToHand")
    @Serializable
    data class ReturnToHand(
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : AbilityCost {
        override val description: String = "Return a ${filter.description} you control to its owner's hand"
    }

    /** Loyalty cost for planeswalker abilities */
    @SerialName("CostLoyalty")
    @Serializable
    data class Loyalty(val change: Int) : AbilityCost {
        override val description: String = if (change >= 0) "+$change" else "$change"
    }
}
