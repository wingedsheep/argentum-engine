package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Restrict one player's activated mana-ability sources while a nested instruction executes. */
@Serializable
@SerialName("WithManaAbilitySources")
data class WithManaAbilitySourcesEffect(
    val effect: Effect,
    val sources: GameObjectFilter,
    val player: EffectTarget = EffectTarget.Controller,
) : Effect {
    override val description: String = effect.description

    override fun runtimeDescription(resolver: (com.wingedsheep.sdk.scripting.values.DynamicAmount) -> Int?): String =
        effect.runtimeDescription(resolver)

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        val newSources = sources.applyTextReplacement(replacer)
        return if (newEffect !== effect || newSources !== sources) copy(effect = newEffect, sources = newSources) else this
    }
}
