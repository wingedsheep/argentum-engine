package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect

/**
 * The "you may pay <mana>. If you do, <then>." shape — the lowered form of the former
 * `Effects.MayPay` wrapper: a [GatedEffect] whose gate is a [Gate.MayPay] over a flat
 * [PayManaCostEffect], with no `otherwise` branch and the default (controller) decision-maker.
 *
 * The engine recognizes this exact shape to preserve the bespoke optional-mana-payment UX that the
 * wrapper used to own, rather than treating it like a generic [Gate.MayPay]: manual mana-source
 * selection at resolution (a `SelectManaSourcesDecision`), so the player chooses *which* sources to
 * tap — sources that sacrifice (Treasures) or carry a tap sub-cost (Springleaf Drum) can't be
 * auto-tapped silently.
 *
 * A triggered ability of this shape that also targets (Lightning Rift, Jubilant Mascot) is placed
 * like any other targeted trigger: targets on the stack (CR 603.3d), the pay question as it resolves,
 * against those locked targets. "You may pay … **When you do**, … target …" is this same shape with a
 * `ReflexiveTriggerEffect` as `then`: the ability itself has no target, the payment keeps manual source
 * selection, and the reflexive half picks its targets after paying (CR 603.12).
 *
 * Composite-cost, life-gated, or `otherwise`-bearing MayPay gates deliberately do **not** match —
 * those resolve through the generic [GatedEffectExecutor] yes/no + cost-composite path (auto-tap),
 * exactly as they did before this wrapper migrated onto the frame.
 */
data class OptionalManaPayment(val cost: ManaCost, val then: Effect)

/** See [OptionalManaPayment]. Returns the cost/inner pair iff [this] is that exact shape. */
fun Effect.asOptionalManaPayment(): OptionalManaPayment? {
    val gated = this as? GatedEffect ?: return null
    if (gated.otherwise != null || gated.decisionMaker != null) return null
    val gate = gated.gate as? Gate.MayPay ?: return null
    val pay = gate.cost as? PayManaCostEffect ?: return null
    return OptionalManaPayment(pay.cost, gated.then)
}
