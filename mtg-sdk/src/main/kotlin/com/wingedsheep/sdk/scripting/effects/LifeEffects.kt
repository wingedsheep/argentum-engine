package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Life Effects
// =============================================================================

/**
 * Gain life effect.
 * "You gain X life" or "Target player gains X life"
 */
@SerialName("GainLife")
@Serializable
data class GainLifeEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget = EffectTarget.Controller) : this(DynamicAmount.Fixed(amount), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> "You gain ${amount.description} life"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} gains ${amount.description} life"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Lose life effect.
 * "You lose X life" or "Target player loses X life"
 */
@SerialName("LoseLife")
@Serializable
data class LoseLifeEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent))
        : this(DynamicAmount.Fixed(amount), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose ${amount.description} life"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} loses ${amount.description} life"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Pay life cost effect.
 * Used as a cost in OptionalCostEffect.
 */
@SerialName("PayLife")
@Serializable
data class PayLifeEffect(
    val amount: Int
) : Effect {
    override val description: String = "pay $amount life"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Target player's owner gains life equal to a fixed amount.
 * Used for effects like "Its owner gains 4 life" (Path of Peace).
 * This targets the owner of the previously targeted permanent.
 */
@SerialName("OwnerGainsLife")
@Serializable
data class OwnerGainsLifeEffect(
    val amount: Int
) : Effect {
    override val description: String = "Its owner gains $amount life"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Set a player's life total to a specific amount.
 * Used for Form of the Dragon: "At the beginning of each end step, your life total becomes 5."
 *
 * Per MTG Rule 118.5, if an effect sets a player's life total to a specific number,
 * the player gains or loses the necessary amount of life.
 *
 * @property amount The amount to set the life total to
 * @property target The player whose life total is set (defaults to Controller)
 */
@SerialName("SetLifeTotal")
@Serializable
data class SetLifeTotalEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(amount: Int, target: EffectTarget = EffectTarget.Controller) : this(DynamicAmount.Fixed(amount), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> "Your life total becomes ${amount.description}"
        else -> "${target.description.replaceFirstChar { it.uppercase() }}'s life total becomes ${amount.description}"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
}

/**
 * Exchange a player's life total with a creature's power.
 * "{4}: Exchange your life total with Evra's power."
 *
 * Per MTG Rule 701.12g, the exchange is simultaneous — the creature's power becomes the
 * player's former life total, and the player's life total becomes the creature's former power.
 * The life change follows Rule 119.3 (gain or lose the necessary amount of life).
 * The power change creates a floating effect at Layer 7b (SET_VALUES).
 *
 * @property target The creature whose power is being exchanged (defaults to Self)
 */
@SerialName("ExchangeLifeAndPower")
@Serializable
data class ExchangeLifeAndPowerEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "Exchange your life total with ${target.description}'s power"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
