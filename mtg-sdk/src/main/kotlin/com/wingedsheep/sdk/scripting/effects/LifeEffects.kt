package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Life Effects
// =============================================================================

/**
 * Gain life effect.
 * "You gain X life" or "Target player gains X life"
 */
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
@Serializable
data class PayLifeEffect(
    val amount: Int
) : Effect {
    override val description: String = "pay $amount life"
}

/**
 * Lose half your life, rounded up.
 * Used for cards like Cruel Bargain.
 */
@Serializable
data class LoseHalfLifeEffect(
    val roundUp: Boolean = true,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose half your life${if (roundUp) ", rounded up" else ", rounded down"}"
        else -> "${target.description} loses half their life${if (roundUp) ", rounded up" else ", rounded down"}"
    }
}

/**
 * Target player's owner gains life equal to a fixed amount.
 * Used for effects like "Its owner gains 4 life" (Path of Peace).
 * This targets the owner of the previously targeted permanent.
 */
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
@Serializable
data class GainLifeForEachLandOnBattlefieldEffect(
    val landType: String,
    val lifePerLand: Int = 1
) : Effect {
    override val description: String = "You gain $lifePerLand life for each $landType on the battlefield"
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
@Serializable
data class SetLifeTotalForEachPlayerEffect(
    val perPlayerAmount: DynamicAmount
) : Effect {
    override val description: String = "Each player's life total becomes ${perPlayerAmount.description}"
}
