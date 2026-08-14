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
 * Life drain composite: each player in [from] loses [amount] life, then [to] gains life
 * equal to the total life actually lost this way ("Each opponent loses X life. You gain
 * life equal to the life lost this way." — Exsanguinate, Gray Merchant of Asphodel).
 *
 * The gain is keyed to the life *actually* lost — each loss honors `ModifyLifeLoss`
 * replacements individually — and lands as a single life-gain event after all losses
 * (one lifegain trigger, not one per opponent).
 */
@SerialName("DrainLife")
@Serializable
data class DrainLifeEffect(
    val amount: DynamicAmount,
    val from: EffectTarget = EffectTarget.PlayerRef(Player.EachOpponent),
    val to: EffectTarget = EffectTarget.Controller
) : Effect {
    /** Convenience constructor for fixed amounts */
    constructor(
        amount: Int,
        from: EffectTarget = EffectTarget.PlayerRef(Player.EachOpponent),
        to: EffectTarget = EffectTarget.Controller
    ) : this(DynamicAmount.Fixed(amount), from, to)

    override val description: String =
        "${from.description.replaceFirstChar { it.uppercase() }} loses ${amount.description} life. " +
            "${to.description.replaceFirstChar { it.uppercase() }} gains life equal to the life lost this way"

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
}

/**
 * Pay life equal to a [DynamicAmount], evaluated at payment/resolution time (e.g. "pay life
 * equal to its power"). The dynamic, payer-parametric twin of [PayLifeEffect]; mirrors
 * [PayDynamicManaCostEffect]. A non-positive evaluated amount pays nothing and still succeeds,
 * so a gating [Gate.MayPay] proceeds to its `then`.
 */
@SerialName("PayDynamicLife")
@Serializable
data class PayDynamicLifeEffect(
    val amount: DynamicAmount,
    val payer: Player = Player.You
) : Effect {
    override val description: String = "pay ${amount.description} life"
    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
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

/** Which of a creature's two combat stats an effect reads and writes. */
@Serializable
enum class CreatureStat {
    POWER,
    TOUGHNESS;

    /** Lowercase name for description strings ("power" / "toughness"). */
    val displayName: String get() = name.lowercase()
}

/**
 * Exchange a player's life total with a creature's power or toughness (CR 701.12g).
 *
 * - Evra, Halcyon Witness: "{4}: Exchange your life total with Evra's power."
 * - Tree of Perdition: "{T}: Exchange target opponent's life total with this creature's toughness."
 *
 * The exchange is simultaneous — the creature's chosen base stat becomes the player's former life
 * total, and the player's life total becomes the creature's former *projected* stat. The life change
 * follows Rule 119.3 (gain or lose the necessary amount of life), so life-gain prevention and
 * gain/loss triggers apply. The stat change creates a floating effect at Layer 7b (SET_VALUES), so
 * counters, Auras, and Equipment apply *on top of* the newly set base value.
 *
 * If the creature isn't on the battlefield when the effect resolves, nothing happens.
 *
 * @property target The creature whose stat is being exchanged (defaults to Self)
 * @property stat Which of the creature's stats takes part in the exchange (defaults to power)
 * @property player The player whose life total is exchanged (defaults to the controller)
 */
@SerialName("ExchangeLifeAndStat")
@Serializable
data class ExchangeLifeAndStatEffect(
    val target: EffectTarget = EffectTarget.Self,
    val stat: CreatureStat = CreatureStat.POWER,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = run {
        val whose = if (player == EffectTarget.Controller) "your" else "${player.description}'s"
        "Exchange $whose life total with ${target.description}'s ${stat.displayName}"
    }
}

/**
 * Exchange the controller's life total with [target] player's (CR 701.12c): each player gains or
 * loses the life needed to reach the other's former total. Applied through the shared gain/lose-life
 * primitives, so life-gain prevention/replacements and life-loss modification apply, and both
 * players' `LifeChangedEvent`s fire for "whenever you gain/lose life" triggers.
 *
 * When [drawEqualToLifeLost] is true, the controller then draws a card for each point of life they
 * **lost** in the exchange (their former total minus their new total, when positive) — Mister
 * Negative's "If you lost life this way, draw that many cards." Modelled as one self-contained
 * effect because the draw amount is the controller's life-loss delta, which no `DynamicAmount`
 * otherwise exposes.
 */
@SerialName("ExchangeLifeTotals")
@Serializable
data class ExchangeLifeTotalsEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val drawEqualToLifeLost: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Exchange life totals with ${target.description}")
        if (drawEqualToLifeLost) append(". If you lost life this way, draw that many cards")
    }
}
