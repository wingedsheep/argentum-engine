package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
}

/**
 * Gain life equal to the number of lands of a specific type on the battlefield.
 * Used for Fruition: "You gain 1 life for each Forest on the battlefield."
 *
 * @property landType The type of land to count (e.g., "Forest")
 * @property lifePerLand The amount of life gained per land
 */
@SerialName("GainLifeForEachLand")
@Serializable
data class GainLifeForEachLandOnBattlefieldEffect(
    val landType: String,
    val lifePerLand: Int = 1
) : Effect {
    override val description: String = "You gain $lifePerLand life for each $landType on the battlefield"
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
}

/**
 * Set each player's life total to a dynamic amount evaluated per-player.
 * Used for Biorhythm: "Each player's life total becomes the number of creatures they control."
 *
 * The [perPlayerAmount] is evaluated with each player as the "controller" in the context,
 * so Player.You in the DynamicAmount resolves to the player whose life is being set.
 *
 * Per MTG Rule 118.5, if an effect sets a player's life total to a specific number,
 * the player gains or loses the necessary amount of life.
 */
@SerialName("SetLifeTotalForEachPlayer")
@Serializable
data class SetLifeTotalForEachPlayerEffect(
    val perPlayerAmount: DynamicAmount
) : Effect {
    override val description: String = "Each player's life total becomes ${perPlayerAmount.description}"
}
