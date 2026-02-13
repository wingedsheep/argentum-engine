package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Transform Effects
// =============================================================================

/**
 * Transform a double-faced permanent.
 * Toggles between front and back face.
 * "Transform this creature"
 */
@SerialName("Transform")
@Serializable
data class TransformEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "Transform ${target.description}"
}
