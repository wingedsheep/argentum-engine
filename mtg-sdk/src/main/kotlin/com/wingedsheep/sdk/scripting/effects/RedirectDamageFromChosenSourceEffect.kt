package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Choose a damage source on resolution and redirect its next damage instance to another recipient. */
@Serializable
@SerialName("RedirectDamageFromChosenSource")
data class RedirectDamageFromChosenSourceEffect(
    val protectedTarget: EffectTarget,
    val redirectTo: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "The next time a source of your choice would deal damage to ${protectedTarget.description} " +
            "${duration.description}, that source deals that damage to ${redirectTo.description} instead"
}
