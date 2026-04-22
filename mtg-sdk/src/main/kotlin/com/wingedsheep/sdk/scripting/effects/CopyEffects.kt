package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Each permanent matching [filter] becomes a copy of the targeted permanent.
 *
 * Used by Mirrorform and similar group-copy effects. At resolution the executor
 * replaces each matching permanent's `CardComponent` with a copy of the target's
 * (per Rule 707 — copiable values only; counters, tapped state, attached auras/
 * equipment, and non-copy effects on the target are ignored). A `CopyOfComponent`
 * is attached to track the original identity.
 *
 * @property target The permanent whose characteristics are copied (usually `ContextTarget(0)`).
 * @property filter Which permanents become copies (defaults to all nonland permanents you control).
 */
@SerialName("EachPermanentBecomesCopyOfTarget")
@Serializable
data class EachPermanentBecomesCopyOfTargetEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val filter: GroupFilter = GroupFilter(
        com.wingedsheep.sdk.scripting.GameObjectFilter.NonlandPermanent.youControl()
    )
) : Effect {
    override val description: String =
        "Each ${filter.baseFilter.description} becomes a copy of ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
