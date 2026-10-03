package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Move input authority for [target] only while the captured stack object resolves.
 * Omitting the object controls the rest of the current resolution; a pipeline target schedules a later one.
 * Resource ownership and spell/permanent control remain with the affected player.
 */
@Serializable
@SerialName("ControlPlayerDuringResolution")
data class ControlPlayerDuringResolutionEffect(
    val target: EffectTarget,
    val resolvingObject: EffectTarget? = null,
) : Effect {
    override val description: String = "Control ${target.description} while ${resolvingObject?.description ?: "the current object"} resolves"
}
