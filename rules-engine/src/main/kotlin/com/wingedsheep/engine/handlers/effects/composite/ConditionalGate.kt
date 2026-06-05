package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect

/**
 * The "if <condition>, <then>. Otherwise, <otherwise>." shape — the lowered form of the former
 * `ConditionalEffect` wrapper: a [GatedEffect] whose gate is a [Gate.WhenCondition].
 *
 * Engine paths that used to special-case `is ConditionalEffect` (stack-time branch resolution for
 * opponent views, repeat-activation stacking analysis, limited card rating) key off this matcher
 * instead, so they still recognize the conditional branch after the type was lowered to the frame —
 * and they ignore the decision-driven [Gate.MayPay] / [Gate.MayDecide] gates, which never matched
 * the old `is ConditionalEffect` check either.
 *
 * @property condition The state test the gate evaluates.
 * @property then Branch that runs iff [condition] holds.
 * @property otherwise Branch that runs iff it does not (null = nothing happens).
 */
data class ConditionalBranch(val condition: Condition, val then: Effect, val otherwise: Effect?)

/** See [ConditionalBranch]. Returns the branch iff [this] is a [Gate.WhenCondition] gated effect. */
fun Effect.asConditional(): ConditionalBranch? {
    val gated = this as? GatedEffect ?: return null
    val gate = gated.gate as? Gate.WhenCondition ?: return null
    return ConditionalBranch(gate.condition, gated.then, gated.otherwise)
}
