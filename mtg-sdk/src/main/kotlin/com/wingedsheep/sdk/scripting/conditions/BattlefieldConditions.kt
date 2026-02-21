package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Battlefield Conditions (non-generic — require special evaluation logic)
// =============================================================================

/**
 * Condition: "If a player controls more [subtype] creatures than each other player"
 * Used by Thoughtbound Primoc and similar Onslaught "tribal war" cards.
 * Returns true only if exactly one player has strictly more than all others.
 */
@SerialName("APlayerControlsMostOfSubtype")
@Serializable
data class APlayerControlsMostOfSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if a player controls more ${subtype.value}s than each other player"
}

/**
 * Condition: "If target creature's power is less than or equal to [amount]"
 * Used by Unified Strike and similar cards that compare a target's power to a dynamic count.
 * Checks the first target (ContextTarget(0)) by default.
 */
@SerialName("TargetPowerAtMost")
@Serializable
data class TargetPowerAtMost(val amount: DynamicAmount, val targetIndex: Int = 0) : Condition {
    override val description: String = "if its power is less than or equal to ${amount.description}"
}

/**
 * Condition: "If enchanted creature is a [subtype]"
 * Used by auras like Lavamancer's Skill that have different effects based on
 * the creature type of the enchanted creature.
 */
@SerialName("EnchantedCreatureHasSubtype")
@Serializable
data class EnchantedCreatureHasSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if enchanted creature is a ${subtype.value}"
}
