package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
    val trigger: GameEvent,
    val binding: TriggerBinding = TriggerBinding.SELF,
    val effect: Effect,
    val optional: Boolean = false,
    val targetRequirement: TargetRequirement? = null,
    /** Additional target requirements for multi-target triggered abilities (e.g., exchange control). */
    val additionalTargetRequirements: List<TargetRequirement> = emptyList(),
    val elseEffect: Effect? = null,
    val activeZone: Zone = Zone.BATTLEFIELD,
    /** Intervening-if condition (Rule 603.4): checked when trigger would fire AND at resolution. */
    val triggerCondition: Condition? = null,
    /** When true, the triggered ability is controlled by the triggering entity's controller
     * instead of the source permanent's controller. Used for cards like Death Match. */
    val controlledByTriggeringEntityController: Boolean = false,
    /** When true, this triggered ability triggers at most once each turn.
     * Used for cards like Scavenger's Talent: "This ability triggers only once each turn." */
    val oncePerTurn: Boolean = false
) : TextReplaceable<TriggeredAbility> {
    /** All target requirements for this ability (primary + additional). */
    val allTargetRequirements: List<TargetRequirement>
        get() = listOfNotNull(targetRequirement) + additionalTargetRequirements

    val description: String
        get() = buildString {
            append(trigger.description)
            if (triggerCondition != null) {
                append(", ")
                append(triggerCondition.description)
            }
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

    override fun applyTextReplacement(replacer: TextReplacer): TriggeredAbility {
        val newTrigger = trigger.applyTextReplacement(replacer)
        val newEffect = effect.applyTextReplacement(replacer)
        val newTargetReq = targetRequirement?.applyTextReplacement(replacer)
        var addlChanged = false
        val newAddlTargetReqs = additionalTargetRequirements.map {
            val n = it.applyTextReplacement(replacer)
            if (n !== it) addlChanged = true
            n
        }
        val newElseEffect = elseEffect?.applyTextReplacement(replacer)
        val newTriggerCondition = triggerCondition?.applyTextReplacement(replacer)
        return if (newTrigger !== trigger || newEffect !== effect ||
                   newTargetReq !== targetRequirement || addlChanged ||
                   newElseEffect !== elseEffect || newTriggerCondition !== triggerCondition)
            copy(trigger = newTrigger, effect = newEffect,
                 targetRequirement = newTargetReq,
                 additionalTargetRequirements = newAddlTargetReqs,
                 elseEffect = newElseEffect,
                 triggerCondition = newTriggerCondition) else this
    }

    companion object {
        fun create(
            trigger: GameEvent,
            binding: TriggerBinding = TriggerBinding.SELF,
            effect: Effect,
            optional: Boolean = false,
            targetRequirement: TargetRequirement? = null,
            additionalTargetRequirements: List<TargetRequirement> = emptyList(),
            elseEffect: Effect? = null,
            activeZone: Zone = Zone.BATTLEFIELD,
            triggerCondition: Condition? = null,
            controlledByTriggeringEntityController: Boolean = false,
            oncePerTurn: Boolean = false
        ): TriggeredAbility =
            TriggeredAbility(
                id = AbilityId.generate(),
                trigger = trigger,
                binding = binding,
                effect = effect,
                optional = optional,
                targetRequirement = targetRequirement,
                additionalTargetRequirements = additionalTargetRequirements,
                elseEffect = elseEffect,
                activeZone = activeZone,
                triggerCondition = triggerCondition,
                controlledByTriggeringEntityController = controlledByTriggeringEntityController,
                oncePerTurn = oncePerTurn
            )
    }
}
