package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Damage Effects
// =============================================================================

/**
 * Deal damage to a target.
 * Supports both fixed amounts and dynamic amounts (e.g., X value, creature count).
 *
 * Examples:
 * - Lightning Bolt: DealDamageEffect(3, target)
 * - Blaze: DealDamageEffect(DynamicAmount.XValue, target)
 * - Final Strike: DealDamageEffect(DynamicAmount.SacrificedPermanentPower, target)
 */
@SerialName("DealDamage")
@Serializable
data class DealDamageEffect(
    val amount: DynamicAmount,
    val target: EffectTarget,
    val cantBePrevented: Boolean = false,
    val damageSource: EffectTarget? = null
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget, cantBePrevented: Boolean = false, damageSource: EffectTarget? = null)
        : this(DynamicAmount.Fixed(amount), target, cantBePrevented, damageSource)

    override val description: String = buildString {
        if (damageSource != null) {
            append("${damageSource.description} deals ${amount.description} damage to ${target.description}")
        } else {
            append("Deal ${amount.description} damage to ${target.description}")
        }
        if (cantBePrevented) append(". This damage can't be prevented")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String = buildString {
        val resolved = resolver(amount)
        if (damageSource != null) {
            append("${damageSource.description} deals $resolved damage to ${target.description}")
        } else {
            append("Deal $resolved damage to ${target.description}")
        }
        if (cantBePrevented) append(". This damage can't be prevented")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Deal damage to multiple targets, dividing the total as you choose.
 * Used for cards like Forked Lightning ("4 damage divided among 1-3 targets").
 */
@SerialName("DividedDamage")
@Serializable
data class DividedDamageEffect(
    val totalDamage: Int,
    val minTargets: Int = 1,
    val maxTargets: Int = 3
) : Effect {
    override val description: String = "Deal $totalDamage damage divided as you choose among $minTargets to $maxTargets target creatures"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Two creatures fight — each deals damage equal to its power to the other.
 * Used for fight abilities like Contested Cliffs and the fight keyword action.
 *
 * @property target1 First creature (e.g., Beast you control)
 * @property target2 Second creature (e.g., creature opponent controls)
 */
@SerialName("Fight")
@Serializable
data class FightEffect(
    val target1: EffectTarget,
    val target2: EffectTarget
) : Effect {
    override val description: String = "${target1.description} fights ${target2.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
