package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Phase/Step Triggers
// =============================================================================

/**
 * Triggers at the beginning of upkeep.
 * "At the beginning of your upkeep..." or "At the beginning of each upkeep..."
 */
@SerialName("Upkeep")
@Serializable
data class OnUpkeep(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of your upkeep"
    } else {
        "At the beginning of each upkeep"
    }
}

/**
 * Triggers at the beginning of the end step.
 * "At the beginning of your end step..."
 */
@SerialName("EndStep")
@Serializable
data class OnEndStep(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of your end step"
    } else {
        "At the beginning of each end step"
    }
}

/**
 * Triggers at the beginning of combat.
 * "At the beginning of combat on your turn..."
 */
@SerialName("BeginCombat")
@Serializable
data class OnBeginCombat(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of combat on your turn"
    } else {
        "At the beginning of each combat"
    }
}

/**
 * Triggers at the beginning of the first main phase.
 * "At the beginning of your first main phase..."
 *
 * This is distinct from generic main phase triggers - it only triggers
 * on the pre-combat main phase, not the post-combat main phase.
 */
@SerialName("FirstMainPhase")
@Serializable
data class OnFirstMainPhase(
    val controllerOnly: Boolean = true
) : Trigger {
    override val description: String = if (controllerOnly) {
        "At the beginning of your first main phase"
    } else {
        "At the beginning of each player's first main phase"
    }
}

/**
 * Triggers at the beginning of the enchanted creature's controller's upkeep.
 * Used for auras that grant abilities to the enchanted creature.
 * "At the beginning of your upkeep" (where "your" refers to the enchanted creature's controller).
 */
@Serializable
data object OnEnchantedCreatureControllerUpkeep : Trigger {
    override val description: String = "At the beginning of enchanted creature's controller's upkeep"
}

/**
 * Triggers when you gain control of this permanent from another player.
 * Used by Risky Move: "When you gain control of Risky Move from another player, ..."
 *
 * Detected from ControlChangedEvent where the permanent is this source entity
 * and the old and new controllers are different.
 */
@Serializable
data object OnGainControlOfSelf : Trigger {
    override val description: String = "When you gain control of this permanent from another player"
}
