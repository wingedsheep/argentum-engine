package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
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
 * The spell's name in prompts is read off the resolving card, so the effect never repeats it.
 *
 * @property action The primary effect to execute (any generic Effect)
 * @property target The target of the primary action
 * @property copyRecipient Who gets offered the copy
 * @property copyCost Cost required to create the copy (null = free): a sacrifice, a discard, or a
 *   mana cost (Chain Lightning's {R}{R}, paid through a mana-payment window)
 * @property copyTargetRequirement Target requirement for the copy's new target
 */
@SerialName("ChainCopy")
@Serializable
data class ChainCopyEffect(
    val action: Effect,
    val target: EffectTarget,
    val copyRecipient: CopyRecipient,
    val copyCost: PayCost? = null,
    val copyTargetRequirement: TargetRequirement
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
        if (copyCostText != null) {
            append("$copyCostText. If the player does, they may ")
        }
        append("copy this spell and may choose a new target for that copy")
    }

    /** The copy cost as an imperative ("sacrifice a land", "pay {R}{R}"), or null when the copy is free. */
    val copyCostText: String?
        get() = copyCost?.let { cost ->
            if ((cost as? PayCost.Atom)?.atom is CostAtom.Mana) "pay ${cost.description}" else cost.description
        }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAction = action.applyTextReplacement(replacer)
        val newCopyTargetReq = copyTargetRequirement.applyTextReplacement(replacer)
        val newCopyCost = copyCost?.applyTextReplacement(replacer)
        return if (newAction !== action || newCopyTargetReq !== copyTargetRequirement || newCopyCost !== copyCost)
            copy(
                action = newAction,
                copyTargetRequirement = newCopyTargetReq,
                copyCost = newCopyCost
            ) else this
    }
}
