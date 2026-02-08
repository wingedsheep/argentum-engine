package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import kotlinx.serialization.Serializable

// =============================================================================
// Battlefield Conditions
// =============================================================================

/**
 * Condition: "If you control a creature"
 */
@Serializable
data object ControlCreature : Condition {
    override val description: String = "if you control a creature"
}

/**
 * Condition: "If you control X or more creatures"
 */
@Serializable
data class ControlCreaturesAtLeast(val count: Int) : Condition {
    override val description: String = "if you control $count or more creatures"
}

/**
 * Condition: "If you control a creature with keyword X"
 */
@Serializable
data class ControlCreatureWithKeyword(val keyword: Keyword) : Condition {
    override val description: String = "if you control a creature with ${keyword.displayName.lowercase()}"
}

/**
 * Condition: "If you control a [subtype] creature" (e.g., "If you control a Dragon")
 */
@Serializable
data class ControlCreatureOfType(val subtype: Subtype) : Condition {
    override val description: String = "if you control a ${subtype.value}"
}

/**
 * Condition: "If you control an enchantment"
 */
@Serializable
data object ControlEnchantment : Condition {
    override val description: String = "if you control an enchantment"
}

/**
 * Condition: "If you control an artifact"
 */
@Serializable
data object ControlArtifact : Condition {
    override val description: String = "if you control an artifact"
}

/**
 * Condition: "If an opponent controls a creature"
 */
@Serializable
data object OpponentControlsCreature : Condition {
    override val description: String = "if an opponent controls a creature"
}

/**
 * Condition: "If an opponent controls more creatures than you"
 */
@Serializable
data object OpponentControlsMoreCreatures : Condition {
    override val description: String = "if an opponent controls more creatures than you"
}

/**
 * Condition: "If an opponent controls more lands than you"
 * Used by Gift of Estates and similar cards.
 */
@Serializable
data object OpponentControlsMoreLands : Condition {
    override val description: String = "if an opponent controls more lands than you"
}

/**
 * Condition: "If a player controls more [subtype] creatures than each other player"
 * Used by Thoughtbound Primoc and similar Onslaught "tribal war" cards.
 * Returns true only if exactly one player has strictly more than all others.
 */
@Serializable
data class APlayerControlsMostOfSubtype(val subtype: Subtype) : Condition {
    override val description: String = "if a player controls more ${subtype.value}s than each other player"
}

/**
 * Condition: "If target creature's power is less than or equal to [amount]"
 * Used by Unified Strike and similar cards that compare a target's power to a dynamic count.
 * Checks the first target (ContextTarget(0)) by default.
 */
@Serializable
data class TargetPowerAtMost(val amount: DynamicAmount, val targetIndex: Int = 0) : Condition {
    override val description: String = "if its power is less than or equal to ${amount.description}"
}
