package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Require each activated mana ability during an instruction to contribute mana to its payments. */
@Serializable
@SerialName("WithManaSpendingObligations")
data class WithManaSpendingObligationsEffect(
    val effect: Effect,
    val player: EffectTarget = EffectTarget.Controller,
) : Effect {
    override val description: String = effect.description

    override fun runtimeDescription(resolver: (com.wingedsheep.sdk.scripting.values.DynamicAmount) -> Int?): String =
        effect.runtimeDescription(resolver)

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}
