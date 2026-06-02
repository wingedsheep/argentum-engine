package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mark [target] as suspended (CR 702.62) — the engine attaches the suspended marker
 * component so the card gains the countdown-and-cast triggered ability while in exile
 * (see [com.wingedsheep.sdk.scripting.Suspend.countdownAbility]).
 *
 * This is the marker step of the [com.wingedsheep.sdk.dsl.Effects.Suspend] chain; it does
 * not move the card or add time counters itself (the chain composes those from
 * [MoveToZoneEffect] and [AddCountersEffect]), keeping each step a reusable primitive.
 */
@SerialName("GrantSuspend")
@Serializable
data class GrantSuspendEffect(
    val target: EffectTarget = EffectTarget.Self,
) : Effect {
    override val description: String = "${target.description} gains suspend"
}
