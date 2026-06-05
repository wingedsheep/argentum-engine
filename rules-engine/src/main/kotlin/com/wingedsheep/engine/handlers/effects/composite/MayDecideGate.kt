package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect

/**
 * The "you may [then]." shape — the lowered form of the former `MayEffect` wrapper: a
 * [GatedEffect] whose gate is a [Gate.MayDecide] with no `otherwise` branch.
 *
 * Engine paths that used to special-case `is MayEffect` (the may-then-target trigger reorder in
 * `TriggerProcessor`, its `resumeMayTrigger` unwrap) key off this matcher instead, so they still
 * recognize a bare "may" after the type was lowered to the frame. The `otherwise == null` guard
 * keeps it the *exact* `MayEffect` equivalent — the old wrapper had no else branch, so a
 * `Gate.MayDecide` that carries an `otherwise` ("you may X, otherwise Y") deliberately does not
 * match and resolves through the generic [GatedEffectExecutor] yes/no path instead.
 *
 * @property then Inner effect that runs iff the player says yes.
 * @property sourceRequiredZone Skip silently if the source has left this zone by resolution.
 * @property inlineOnTrigger Render the yes/no inline on the triggering permanent.
 */
data class MayDecideGate(
    val then: Effect,
    val sourceRequiredZone: Zone?,
    val inlineOnTrigger: Boolean
)

/** See [MayDecideGate]. Returns the shape iff [this] is a bare (no-`otherwise`) [Gate.MayDecide]. */
fun Effect.asMayDecide(): MayDecideGate? {
    val gated = this as? GatedEffect ?: return null
    if (gated.otherwise != null) return null
    val gate = gated.gate as? Gate.MayDecide ?: return null
    return MayDecideGate(gated.then, gate.sourceRequiredZone, gate.inlineOnTrigger)
}
