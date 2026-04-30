package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
    val filter: GroupFilter = GroupFilter(GameObjectFilter.Companion.Creature.youControl()),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Choose a color. ${filter.description} gain protection from the chosen color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Choose a color, then grant Skrelv-style evasion/protection to a target creature.
 *
 * The target gains toxic N and hexproof from the chosen color until end of turn,
 * and can't be blocked by creatures of the chosen color this turn.
 */
@SerialName("ChooseColorGrantToxicHexproofAndCantBeBlockedByColor")
@Serializable
data class ChooseColorGrantToxicHexproofAndCantBeBlockedByColorEffect(
    val target: EffectTarget,
    val toxicAmount: Int,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Choose a color. ${target.description} gains toxic $toxicAmount and hexproof from that color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(". It can't be blocked by creatures of that color this turn")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
