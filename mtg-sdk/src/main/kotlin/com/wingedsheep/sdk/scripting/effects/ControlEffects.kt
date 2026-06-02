package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Control Change Effects
// =============================================================================

/**
 * Gain control of target permanent.
 * "Gain control of target permanent."
 *
 * Used by Blatant Thievery and similar control-stealing effects.
 */
@SerialName("GainControl")
@Serializable
data class GainControlEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("gain control of ${target.description}")
        if (duration != Duration.Permanent && duration.description.isNotEmpty()) {
            append(" ${duration.description}")
        }
    }
}

/**
 * Gain control of target permanent for the active player (whoever's turn it is).
 * Unlike GainControlEffect which gives control to the ability's controller,
 * this gives control to the current active player.
 *
 * Used by Risky Move: "At the beginning of each player's upkeep, that player
 * gains control of Risky Move."
 */
@SerialName("GainControlByActivePlayer")
@Serializable
data class GainControlByActivePlayerEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "that player gains control of ${target.description}"
}

/**
 * A per-player quantity used to rank players for "the player with the most X gains
 * control" effects. The player with strictly more than every other player wins; a tie
 * for the highest value means no control change. Add a variant here when a new card
 * ranks players by a different quantity (cards in hand, poison counters, etc.).
 */
@Serializable
sealed interface PlayerRankMetric {
    /** Each player's life total. (Ghazbán Ogre.) */
    @SerialName("LifeTotal")
    @Serializable
    data object LifeTotal : PlayerRankMetric

    /** How many creatures of [subtype] each player controls. (Thoughtbound Primoc.) */
    @SerialName("CreaturesOfSubtype")
    @Serializable
    data class CreaturesOfSubtype(val subtype: Subtype) : PlayerRankMetric
}

/**
 * Gain control of a permanent based on which player has the most of a [PlayerRankMetric].
 * The player with strictly more than every other player gains control of the target;
 * on a tie for the highest value, nothing happens.
 *
 * Used by Thoughtbound Primoc (most Wizards): "At the beginning of your upkeep, if a
 * player controls more Wizards than each other player, that player gains control of
 * Thoughtbound Primoc." and Ghazbán Ogre (most life): "At the beginning of your upkeep,
 * if a player has more life than each other player, the player with the most life gains
 * control of this creature."
 */
@SerialName("GainControlByMost")
@Serializable
data class GainControlByMostEffect(
    val metric: PlayerRankMetric,
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = buildString {
        append(
            when (metric) {
                is PlayerRankMetric.LifeTotal -> "the player with the most life"
                is PlayerRankMetric.CreaturesOfSubtype -> "the player who controls the most ${metric.subtype.value}s"
            }
        )
        append(" gains control of ${target.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val metric = metric
        if (metric !is PlayerRankMetric.CreaturesOfSubtype) return this
        val new = replacer.replaceSubtype(metric.subtype)
        return if (new == metric.subtype) this else copy(metric = PlayerRankMetric.CreaturesOfSubtype(new))
    }
}

/**
 * Give control of a permanent to a targeted player.
 * Unlike GainControlEffect (which always gives control to the ability's controller),
 * this effect gives control to a player resolved from a target.
 *
 * Used by Custody Battle: "target opponent gains control of this creature unless you sacrifice a land."
 *
 * @property permanent Which permanent changes control (default: enchanted creature)
 * @property newController Which player gains control (default: first target, expected to be a player)
 */
@SerialName("GiveControlToTargetPlayer")
@Serializable
data class GiveControlToTargetPlayerEffect(
    val permanent: EffectTarget = EffectTarget.EnchantedCreature,
    val newController: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = "target opponent gains control of ${permanent.description}"
}

/**
 * Exchange control of two target creatures.
 * "You may exchange control of target creature you control and target creature an opponent controls."
 *
 * Creates two floating effects at Layer.CONTROL:
 * 1. Target A (yours) → opponent gains control
 * 2. Target B (opponent's) → you gain control
 *
 * @property target1 The creature you control (becomes opponent's)
 * @property target2 The creature an opponent controls (becomes yours)
 */
@SerialName("ExchangeControl")
@Serializable
data class ExchangeControlEffect(
    val target1: EffectTarget = EffectTarget.ContextTarget(0),
    val target2: EffectTarget = EffectTarget.ContextTarget(1)
) : Effect {
    override val description: String =
        "Exchange control of ${target1.description} and ${target2.description}"
}
