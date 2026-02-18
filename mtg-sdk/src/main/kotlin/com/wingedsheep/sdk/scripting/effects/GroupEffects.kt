package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Group Iteration Effects
// =============================================================================

/**
 * Apply an effect to every entity matching a group filter.
 *
 * The inner [effect] is executed once per matched entity.
 * Within the inner effect, [EffectTarget.Self] resolves to the
 * current iteration entity rather than the source permanent.
 *
 * @property filter Which entities are affected
 * @property effect The effect to apply to each matched entity
 * @property noRegenerate If true, affected entities cannot be regenerated
 * @property simultaneous If true (default), the group is snapshotted before effects apply
 */
@SerialName("ForEachInGroup")
@Serializable
data class ForEachInGroupEffect(
    val filter: GroupFilter,
    val effect: Effect,
    val noRegenerate: Boolean = false,
    val simultaneous: Boolean = true
) : Effect {
    override val description: String = buildString {
        append(effect.description.replaceFirstChar { it.uppercase() })
        append(" ")
        append(filter.description.replaceFirstChar { it.lowercase() })
        if (noRegenerate) append(". They can't be regenerated")
    }
}