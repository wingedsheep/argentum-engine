package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Protection Effects
// =============================================================================

/**
 * Choose a color, then grant protection from that color to a group of creatures until end of turn.
 * "Choose a color. Creatures you control gain protection from the chosen color until end of turn."
 *
 * This effect requires a player decision (color choice) before applying the protection.
 *
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@SerialName("ChooseColorAndGrantProtectionToGroup")
@Serializable
data class ChooseColorAndGrantProtectionToGroupEffect(
    val filter: GroupFilter = GroupFilter(GameObjectFilter.Creature.youControl()),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Choose a color. ${filter.description} gain protection from the chosen color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Choose a color, then grant protection from that color to a single target until end of turn.
 * "{W}: This creature gains protection from the color of your choice until end of turn."
 *
 * This effect requires a player decision (color choice) before applying the protection.
 *
 * @property target Which permanent gains protection
 * @property duration How long the effect lasts
 */
@SerialName("ChooseColorAndGrantProtectionToTarget")
@Serializable
data class ChooseColorAndGrantProtectionToTargetEffect(
    val target: EffectTarget = EffectTarget.Self,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains protection from the color of your choice")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}
