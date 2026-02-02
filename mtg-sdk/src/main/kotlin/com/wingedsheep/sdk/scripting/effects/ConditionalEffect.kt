package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Conditional Effect
// =============================================================================

/**
 * An effect that only happens if a condition is met.
 */
@Serializable
data class ConditionalEffect(
    val condition: Condition,
    val effect: Effect,
    val elseEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append(condition.description.replaceFirstChar { it.uppercase() })
        append(", ")
        append(effect.description.replaceFirstChar { it.lowercase() })
        if (elseEffect != null) {
            append(". Otherwise, ")
            append(elseEffect.description.replaceFirstChar { it.lowercase() })
        }
    }
}
