package com.wingedsheep.sdk.scripting.triggers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Life Triggers
// =============================================================================

/**
 * Triggers when a player gains life.
 * "Whenever a player gains life..." or "Whenever you gain life..."
 */
@SerialName("LifeGain")
@Serializable
data class OnLifeGain(
    val controllerOnly: Boolean = false
) : Trigger {
    override val description: String = if (controllerOnly) {
        "Whenever you gain life"
    } else {
        "Whenever a player gains life"
    }
}
