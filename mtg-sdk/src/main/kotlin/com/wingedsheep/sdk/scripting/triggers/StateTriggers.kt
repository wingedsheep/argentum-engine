package com.wingedsheep.sdk.scripting.triggers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Damage Triggers
// =============================================================================

/**
 * Triggers when this creature is dealt damage.
 * "Whenever [this creature] is dealt damage..."
 */
@SerialName("DamageReceived")
@Serializable
data class OnDamageReceived(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature is dealt damage"
    } else {
        "Whenever a creature is dealt damage"
    }
}

// =============================================================================
// Tap/Untap Triggers
// =============================================================================

/**
 * Triggers when a permanent becomes tapped.
 * "Whenever this creature becomes tapped..."
 */
@SerialName("BecomesTapped")
@Serializable
data class OnBecomesTapped(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature becomes tapped"
    } else {
        "Whenever a creature becomes tapped"
    }
}

/**
 * Triggers when a permanent becomes untapped.
 * "Whenever this creature becomes untapped..."
 */
@SerialName("BecomesUntapped")
@Serializable
data class OnBecomesUntapped(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "Whenever this creature becomes untapped"
    } else {
        "Whenever a creature becomes untapped"
    }
}

// =============================================================================
// Transform Triggers
// =============================================================================

/**
 * Triggers when a permanent transforms.
 * "When this creature transforms..."
 *
 * Can be filtered to only trigger when transforming into a specific face.
 */
@SerialName("TransformTrigger")
@Serializable
data class OnTransform(
    val selfOnly: Boolean = true,
    val intoBackFace: Boolean? = null  // null = any transform, true = only when transforming to back, false = only when transforming to front
) : Trigger {
    override val description: String = buildString {
        append(if (selfOnly) "When this creature transforms" else "Whenever a permanent transforms")
        when (intoBackFace) {
            true -> append(" into its back face")
            false -> append(" into its front face")
            null -> { /* any transform */ }
        }
    }
}

// =============================================================================
// Morph Triggers
// =============================================================================

/**
 * Triggers when a face-down creature is turned face up.
 * "When [this creature] is turned face up..."
 *
 * Used for morph abilities that trigger upon revealing the creature.
 */
@SerialName("TurnFaceUpTrigger")
@Serializable
data class OnTurnFaceUp(
    val selfOnly: Boolean = true
) : Trigger {
    override val description: String = if (selfOnly) {
        "When this creature is turned face up"
    } else {
        "Whenever a creature is turned face up"
    }
}
