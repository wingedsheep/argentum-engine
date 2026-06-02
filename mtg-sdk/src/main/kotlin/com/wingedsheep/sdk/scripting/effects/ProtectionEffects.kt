package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.EntityReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Protection Effects
// =============================================================================

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

/**
 * Grant "protection from the chosen color" to a target. Must run inside a
 * [ChooseColorThenEffect] — the executor reads the chosen color from the
 * effect context. Resolves to a `PROTECTION_FROM_<COLOR>` keyword grant.
 */
@SerialName("GrantProtectionFromChosenColor")
@Serializable
data class GrantProtectionFromChosenColorEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains protection from the chosen color")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Grant "protection from each of {source}'s colors" to every battlefield permanent matching
 * [filter], for [duration]. The colors are read from the referenced entity's projected state
 * at execution time, then one `PROTECTION_FROM_<COLOR>` floating effect is added per color
 * for the snapshot of matching entities. Colorless source → no grants (colorless is not a
 * color, CR 105.2).
 *
 * One-shot at resolution: the recipient set is captured when the effect resolves; permanents
 * entering [filter] later in the duration don't gain the grant (CR 611.1).
 *
 * Used by Éowyn, Fearless Knight: "Legendary creatures you control gain protection from each
 * of that creature's colors until end of turn." Compose with an exile/destroy step first so
 * the source's projected colors are read while it's still on the battlefield.
 *
 * TODO(SDK): if a second card needs "for each color of [entity], do X", factor the color
 * iteration into a `ForEachColorOf(source) { color -> ... }` combinator that exposes the
 * current color via the chosen-color context, so this monolith can decompose into
 * `ForEachColorOf(source) { GrantProtectionFromChosenColor(filter) }`.
 */
@SerialName("GrantProtectionFromColorsOfEntity")
@Serializable
data class GrantProtectionFromColorsOfEntityEffect(
    val filter: GroupFilter,
    val source: EntityReference,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} gain protection from each of ${source.description}'s colors")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
