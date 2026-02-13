package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Battlefield Conditions
// =============================================================================

/**
 * Condition: "If you control a creature"
 */
@SerialName("ControlCreature")
@Serializable
data object ControlCreature : Condition {
    override val description: String = "if you control a creature"
}

/**
 * Condition: "If you control X or more creatures"
 */
@SerialName("ControlCreaturesAtLeast")
@Serializable
data class ControlCreaturesAtLeast(val count: Int) : Condition {
    override val description: String = "if you control $count or more creatures"
}

/**
 * Condition: "If you control a creature with keyword X"
 */
@SerialName("ControlCreatureWithKeyword")
@Serializable
data class ControlCreatureWithKeyword(val keyword: Keyword) : Condition {
    override val description: String = "if you control a creature with ${keyword.displayName.lowercase()}"
}

/**
 * Condition: "If you control a [subtype] creature" (e.g., "If you control a Dragon")
 */
@SerialName("ControlCreatureOfType")
@Serializable
data class ControlCreatureOfType(val subtype: Subtype) : Condition {
    override val description: String = "if you control a ${subtype.value}"
}

/**
 * Condition: "If you control an enchantment"
 */
@SerialName("ControlEnchantment")
@Serializable
data object ControlEnchantment : Condition {
    override val description: String = "if you control an enchantment"
}

/**
 * Condition: "If you control an artifact"
 */
@SerialName("ControlArtifact")
@Serializable
data object ControlArtifact : Condition {
    override val description: String = "if you control an artifact"
}

/**
 * Condition: "If an opponent controls a creature"
 */
@SerialName("OpponentControlsCreature")
@Serializable
data object OpponentControlsCreature : Condition {
    override val description: String = "if an opponent controls a creature"
}

/**
 * Condition: "If an opponent controls more creatures than you"
 */
@SerialName("OpponentControlsMoreCreatures")
@Serializable
data object OpponentControlsMoreCreatures : Condition {
    override val description: String = "if an opponent controls more creatures than you"
}

/**
 * Condition: "If an opponent controls more lands than you"
 * Used by Gift of Estates and similar cards.
 */
@SerialName("OpponentControlsMoreLands")
@Serializable
data object OpponentControlsMoreLands : Condition {
    override val description: String = "if an opponent controls more lands than you"
}

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
