package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.targeting.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * A triggered ability is an ability that fires when a specific condition is met.
 * It combines a trigger condition with an effect.
 *
 * Triggered abilities can optionally require targets. When a triggered ability
 * has a targetRequirement, the player must choose valid targets when the ability
 * goes on the stack. If no legal targets exist, the ability is removed from
 * the stack without resolving.
 */
@Serializable
data class TriggeredAbility(
    val id: AbilityId,
    val trigger: Trigger,
    val effect: Effect,
    val optional: Boolean = false,
    val targetRequirement: TargetRequirement? = null,
    val elseEffect: Effect? = null
) {
    val description: String
        get() = buildString {
            append(trigger.description)
            if (optional) append(", you may")
            append(", ")
            if (targetRequirement != null) {
                append(targetRequirement.description)
                append(" ")
            }
            append(effect.description.replaceFirstChar { it.lowercase() })
            if (elseEffect != null) {
                append(". If you don't, ")
                append(elseEffect.description.replaceFirstChar { it.lowercase() })
            }
            append(".")
        }

    /** Whether this triggered ability requires targets */
    val requiresTargets: Boolean
        get() = targetRequirement != null

    companion object {
        fun create(
            trigger: Trigger,
            effect: Effect,
            optional: Boolean = false,
            targetRequirement: TargetRequirement? = null,
            elseEffect: Effect? = null
        ): TriggeredAbility =
            TriggeredAbility(
                id = AbilityId.generate(),
                trigger = trigger,
                effect = effect,
                optional = optional,
                targetRequirement = targetRequirement,
                elseEffect = elseEffect
            )
    }
}
