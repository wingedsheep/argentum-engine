package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The "you may [then]." shape — the lowered form of the former `Effects.May` wrapper: a
 * [GatedEffect] whose gate is a [Gate.MayDecide] with no `otherwise` branch.
 *
 * Recognizes the inner effect in a saved MayTriggerContinuation. New targeted triggers keep
 * the gate intact on the stack and use the generic gated executor at resolution.
 *
 * @property then Inner effect that runs iff the player says yes.
 * @property sourceRequiredZone Skip silently if the source has left this zone by resolution.
 * @property inlineOnTrigger Render the yes/no inline on the triggering permanent.
 * @property decisionMaker Who answers the yes/no; null means the ability's controller.
 */
data class MayDecideGate(
    val then: Effect,
    val sourceRequiredZone: Zone?,
    val inlineOnTrigger: Boolean,
    val decisionMaker: EffectTarget? = null
)

/** See [MayDecideGate]. Returns the shape iff [this] is a bare (no-`otherwise`) [Gate.MayDecide]. */
fun Effect.asMayDecide(): MayDecideGate? {
    val gated = this as? GatedEffect ?: return null
    if (gated.otherwise != null) return null
    val gate = gated.gate as? Gate.MayDecide ?: return null
    return MayDecideGate(gated.then, gate.sourceRequiredZone, gate.inlineOnTrigger, gated.decisionMaker)
}
