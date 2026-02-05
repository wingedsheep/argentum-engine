package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Damage Effects
// =============================================================================

/**
 * Deal damage effect.
 * "Deal X damage to target creature/player"
 */
@Serializable
data class DealDamageEffect(
    val amount: Int,
    val target: EffectTarget,
    val cantBePrevented: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Deal $amount damage to ${target.description}")
        if (cantBePrevented) append(". This damage can't be prevented")
    }
}

/**
 * Deal dynamic damage to a target.
 * "Deal damage equal to X to target"
 * Used for effects like Final Strike where damage depends on a dynamic value.
 */
@Serializable
data class DealDynamicDamageEffect(
    val amount: DynamicAmount,
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal damage equal to ${amount.description} to ${target.description}"
}

/**
 * Deal X damage where X is determined by the spell's X value.
 * Used for cards like Blaze.
 */
@Serializable
data class DealXDamageEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Deal X damage to ${target.description}"
}

/**
 * Deal damage to a group of creatures matching a filter.
 * Use with .then(DealDamageToPlayersEffect) for effects that also damage players.
 *
 * Examples:
 * - Pyroclasm: DealDamageToGroupEffect(2, GroupFilter.AllCreatures)
 * - Needle Storm: DealDamageToGroupEffect(4, GroupFilter.AllCreatures.withKeyword(Keyword.FLYING))
 * - Earthquake: DealDamageToGroupEffect(DynamicAmount.XValue, GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING))
 *                  .then(DealDamageToPlayersEffect(DynamicAmount.XValue))
 *
 * @param amount The amount of damage to deal (can be fixed or dynamic like X)
 * @param filter Which creatures are damaged
 */
@Serializable
data class DealDamageToGroupEffect(
    val amount: DynamicAmount,
    val filter: GroupFilter = GroupFilter.AllCreatures
) : Effect {
    constructor(amount: Int, filter: GroupFilter = GroupFilter.AllCreatures) : this(DynamicAmount.Fixed(amount), filter)

    override val description: String = "Deal ${amount.description} damage to ${filter.description}"
}

/**
 * Deal damage to players.
 * Can target each player, controller only, opponent only, or each opponent.
 * Often composed with DealDamageToGroupEffect for effects like Earthquake.
 *
 * Examples:
 * - Earthquake: DealDamageToGroupEffect(...).then(DealDamageToPlayersEffect(DynamicAmount.XValue))
 * - Fire Tempest: DealDamageToGroupEffect(6).then(DealDamageToPlayersEffect(6))
 * - Flame Rift: DealDamageToPlayersEffect(4) (just players, no creatures)
 *
 * @param amount The amount of damage to deal (can be fixed or dynamic like X)
 * @param target Which players to damage (EachPlayer, Controller, Opponent, EachOpponent)
 */
@Serializable
data class DealDamageToPlayersEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.Each)
) : Effect {
    constructor(amount: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.Each)) : this(DynamicAmount.Fixed(amount), target)

    override val description: String = when (target) {
        EffectTarget.Controller -> "Deal ${amount.description} damage to you"
        else -> "Deal ${amount.description} damage to ${target.description}"
    }
}

/**
 * Deal damage to each attacking creature.
 * Used for Scorching Winds: "Deal 1 damage to each attacking creature."
 */
@Serializable
data class DealDamageToAttackingCreaturesEffect(
    val amount: Int
) : Effect {
    override val description: String = "Deal $amount damage to each attacking creature"
}

/**
 * Deal damage to multiple targets, dividing the total as you choose.
 * Used for cards like Forked Lightning ("4 damage divided among 1-3 targets").
 */
@Serializable
data class DividedDamageEffect(
    val totalDamage: Int,
    val minTargets: Int = 1,
    val maxTargets: Int = 3
) : Effect {
    override val description: String = "Deal $totalDamage damage divided as you choose among $minTargets to $maxTargets target creatures"
}

/**
 * Deal damage with a replacement effect: if the creature would die this turn, exile it instead.
 * Used for cards like Feed the Flames.
 *
 * This combines damage dealing with a death-replacement effect that lasts until end of turn.
 *
 * @property amount The amount of damage to deal
 * @property target The creature to target
 */
@Serializable
data class DealDamageExileOnDeathEffect(
    val amount: Int,
    val target: EffectTarget
) : Effect {
    override val description: String = buildString {
        append("Deal $amount damage to ${target.description}. ")
        append("If that creature would die this turn, exile it instead")
    }
}
