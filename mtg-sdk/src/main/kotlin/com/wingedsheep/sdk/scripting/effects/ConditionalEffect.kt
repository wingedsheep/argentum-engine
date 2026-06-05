package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.conditions.Condition

// =============================================================================
// Conditional Effect
// =============================================================================

/**
 * "If [condition], [effect]. Otherwise, [elseEffect]." — an effect that runs one branch or the
 * other based on a state test evaluated at resolution time.
 *
 * Backwards-compatible facade preserved for the cards (and the DSL `spell { onlyIf(...) }` path)
 * that authored against the former `ConditionalEffect` data class. It now lowers to a
 * [GatedEffect] with a [Gate.WhenCondition] gate — one frame, one executor, one resumer — so there
 * is no bespoke conditional executor. The condition is a synchronous state test (no decision, no
 * pause): success runs [effect], failure runs [elseEffect]. Card source is unchanged; only the
 * compiled/serialized representation moved to `Gated`.
 */
@Suppress("FunctionName")
fun ConditionalEffect(
    condition: Condition,
    effect: Effect,
    elseEffect: Effect? = null
): GatedEffect = GatedEffect(
    gate = Gate.WhenCondition(condition),
    then = effect,
    otherwise = elseEffect
)
