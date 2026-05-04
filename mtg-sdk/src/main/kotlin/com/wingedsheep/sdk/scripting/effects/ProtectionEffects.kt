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
 * Choose a color, then run [then] with the chosen color exposed on the
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget] context. Atomic effects under
 * [then] (e.g., [GrantHexproofFromChosenColorEffect]) read the color from context
 * to apply per-color modifications.
 *
 * Use this combinator instead of inventing a new monolithic "choose color, then do
 * X+Y+Z" effect for every card. Compose with [CompositeEffect] when [then] is a
 * sequence of grants.
 */
@SerialName("ChooseColorThen")
@Serializable
data class ChooseColorThenEffect(
    val then: Effect,
    val prompt: String = "Choose a color"
) : Effect {
    override val description: String = "Choose a color. ${then.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newThen = then.applyTextReplacement(replacer)
        return if (newThen !== then) copy(then = newThen) else this
    }
}

/**
 * Grant "hexproof from the chosen color" to a target. Must run inside a
 * [ChooseColorThenEffect] — the executor reads the chosen color from the
 * effect context. Resolves to a `HEXPROOF_FROM_<COLOR>` keyword grant.
 */
@SerialName("GrantHexproofFromChosenColor")
@Serializable
data class GrantHexproofFromChosenColorEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains hexproof from the chosen color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
