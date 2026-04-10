package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who gets offered the copy of the chain spell.
 */
@Serializable
enum class CopyRecipient {
    /** Controller of the targeted permanent (Destroy, Bounce, PreventDamage) */
    TARGET_CONTROLLER,
    /** The target player directly (Discard — target IS a player) */
    TARGET_PLAYER,
    /** The "affected player" — the target if it's a player, or its controller if a permanent (Damage) */
    AFFECTED_PLAYER
}

/**
 * Unified chain copy effect for all "Chain of X" cards from Onslaught.
 *
 * Executes a primary action on the target, then offers a specific player the option
 * to copy the spell (optionally paying a cost) and choose a new target.
 *
 * @property action The primary effect to execute (any generic Effect)
 * @property target The target of the primary action
 * @property targetFilter The filter for valid targets (used for permanent-targeting chains)
 * @property copyRecipient Who gets offered the copy
 * @property copyCost Cost required to create the copy (null = free)
 * @property copyTargetRequirement Target requirement for the copy's new target
 * @property spellName The name of the spell (for display and copy descriptions)
 */
@SerialName("ChainCopy")
@Serializable
data class ChainCopyEffect(
    val action: Effect,
    val target: EffectTarget,
    val targetFilter: TargetFilter? = null,
    val copyRecipient: CopyRecipient,
    val copyCost: PayCost? = null,
    val copyTargetRequirement: TargetRequirement,
    val spellName: String
) : Effect {
    override val description: String = buildString {
        append(action.description)
        append(". Then ")
        when (copyRecipient) {
            CopyRecipient.TARGET_CONTROLLER -> append("that permanent's controller")
            CopyRecipient.TARGET_PLAYER -> append("that player")
            CopyRecipient.AFFECTED_PLAYER -> append("that player or that permanent's controller")
        }
        append(" may ")
        if (copyCost != null) {
            append("${copyCost.description}. If the player does, they may ")
        }
        append("copy this spell and may choose a new target for that copy")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAction = action.applyTextReplacement(replacer)
        val newTargetFilter = targetFilter?.applyTextReplacement(replacer)
        val newCopyTargetReq = copyTargetRequirement.applyTextReplacement(replacer)
        val newCopyCost = copyCost?.applyTextReplacement(replacer)
        return if (newAction !== action || newTargetFilter !== targetFilter ||
            newCopyTargetReq !== copyTargetRequirement || newCopyCost !== copyCost)
            copy(
                action = newAction,
                targetFilter = newTargetFilter,
                copyTargetRequirement = newCopyTargetReq,
                copyCost = newCopyCost
            ) else this
    }
}
