package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Instruct a player to play the first gathered card if able, paying its costs. */
@Serializable
@SerialName("ForcePlay")
data class ForcePlayEffect(
    val from: String,
    val player: EffectTarget = EffectTarget.Controller,
    val storePlayedTo: String? = null,
) : Effect {
    override val description: String = "${player.description} plays that card if able"
}
