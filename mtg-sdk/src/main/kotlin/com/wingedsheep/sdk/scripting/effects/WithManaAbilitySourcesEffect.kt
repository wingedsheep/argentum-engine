package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
}
