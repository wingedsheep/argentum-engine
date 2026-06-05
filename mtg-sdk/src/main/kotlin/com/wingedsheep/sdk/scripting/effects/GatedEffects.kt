package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Gated effects — one resolution frame for the optional / gated-effect cluster
// =============================================================================

/**
 * One resolution frame for the optional / gated-effect cluster.
 *
 * A [GatedEffect] pairs a [Gate] — the thing that must succeed first — with a [then]
 * effect that runs iff the gate succeeds and an optional [otherwise] that runs iff it
 * fails. A single executor and a single continuation resumer own the canonical unwind
 * order for *every* gate kind, so the "decide / pay vs. lock-targets" timing that used to
 * be re-encoded (and re-bugged) per wrapper is correct by construction:
 *
 *  1. Targets for [then] / [otherwise] are locked when the ability is put on the stack
 *     (trigger time, CR 603.3d) — independent of the gate, before the gate is resolved.
 *  2. The gate is resolved at resolution time (CR 117.3a) via [decisionMaker].
 *  3. On success → [then]; on failure → [otherwise].
 *
 * This is the "composition over enumeration" replacement for the wrapper-per-concern
 * cluster (MayEffect / IfYouDoEffect / OptionalCostEffect / …). It models the decision-driven
 * gates ([Gate.MayDecide], [Gate.MayPay]) and the synchronous state-test gate
 * ([Gate.WhenCondition]); the action-outcome gate (IfYouDo) and the any-player-pays gate are
 * folded in as their wrappers migrate.
 *
 * @property gate What must succeed before [then] runs.
 * @property then Effect that runs iff the gate succeeds.
 * @property otherwise Effect that runs iff the gate fails ("if you don't" / "otherwise").
 * @property decisionMaker Who resolves the gate. Defaults to the ability's controller.
 *   Only the prompt is delegated; [then] still resolves under the original controller.
 * @property descriptionOverride Hand-written text to use instead of the gate-derived one.
 * @property hint Optional reminder text shown under the yes/no prompt.
 */
@SerialName("Gated")
@Serializable
data class GatedEffect(
    val gate: Gate,
    val then: Effect,
    val otherwise: Effect? = null,
    val decisionMaker: EffectTarget? = null,
    val descriptionOverride: String? = null,
    val hint: String? = null
) : Effect {
    override val description: String = descriptionOverride ?: when (val g = gate) {
        is Gate.MayDecide -> g.prompt ?: buildString {
            append("You may ${then.description.replaceFirstChar { it.lowercase() }}")
            if (otherwise != null) {
                append(". Otherwise, ${otherwise.description.replaceFirstChar { it.lowercase() }}")
            }
        }
        is Gate.MayPay -> buildString {
            append("You may ${g.cost.description.replaceFirstChar { it.lowercase() }}. ")
            append("If you do, ${then.description.replaceFirstChar { it.lowercase() }}")
            if (otherwise != null) {
                append(". Otherwise, ${otherwise.description.replaceFirstChar { it.lowercase() }}")
            }
        }
        is Gate.WhenCondition -> buildString {
            append(g.condition.description.replaceFirstChar { it.uppercase() })
            append(", ")
            append(then.description.replaceFirstChar { it.lowercase() })
            if (otherwise != null) {
                append(". Otherwise, ${otherwise.description.replaceFirstChar { it.lowercase() }}")
            }
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newGate = gate.applyTextReplacement(replacer)
        val newThen = then.applyTextReplacement(replacer)
        val newOtherwise = otherwise?.applyTextReplacement(replacer)
        return if (newGate !== gate || newThen !== then || newOtherwise !== otherwise)
            copy(gate = newGate, then = newThen, otherwise = newOtherwise) else this
    }
}

/**
 * The condition a [GatedEffect] must clear before its [GatedEffect.then] runs.
 *
 * Each variant captures one *gate kind* from the former wrapper cluster; they all share
 * the single [GatedEffect] frame, its executor, and its resumer. New gate kinds are added
 * here as the remaining wrappers migrate in (IfYouDoEffect → a `DoAction` gate that scores
 * an action against a `SuccessCriterion`; AnyPlayerMayPayEffect → an APNAP `AnyPlayerMayPay`
 * gate).
 */
@Serializable
sealed interface Gate {
    /** Rewrite any effects this gate carries (text-replacement for variabilized cards). */
    fun applyTextReplacement(replacer: TextReplacer): Gate

    /**
     * Pure yes/no — "You may [then]." The decision-maker chooses whether
     * [GatedEffect.then] happens at all. Replaces the `MayEffect` wrapper (see the
     * [MayEffect] facade).
     *
     * @property prompt Optional override for the yes/no prompt text.
     * @property hint Optional reminder text shown under the prompt.
     * @property sourceRequiredZone If set, the gate is skipped silently (no prompt, nothing
     *   happens) when the source has left this zone by resolution — e.g. a "when this dies, you
     *   may ..." ability whose source is no longer where the may-action needs it.
     * @property inlineOnTrigger When true, the yes/no is rendered inline on the triggering
     *   permanent rather than as a centered modal (the Dragon Shadow / "may" trigger UX); flows
     *   into `DecisionContext.inlineOnTrigger`.
     */
    @SerialName("Gate.MayDecide")
    @Serializable
    data class MayDecide(
        val prompt: String? = null,
        val hint: String? = null,
        val sourceRequiredZone: Zone? = null,
        val inlineOnTrigger: Boolean = false
    ) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate = this
    }

    /**
     * Optionally pay a cost — "You may [cost]. If you do, [then]." The gate succeeds iff
     * the [cost] effect is paid in full. Affordability is checked before prompting, so an
     * unpayable cost skips straight to [GatedEffect.otherwise] rather than offering an
     * impossible "yes". Replaces OptionalCostEffect.
     *
     * @property cost The cost effect the decision-maker may pay — e.g. `PayManaCostEffect`,
     *   `PayLifeEffect`, `SacrificeEffect`, or a `CompositeEffect` composing them.
     */
    @SerialName("Gate.MayPay")
    @Serializable
    data class MayPay(
        val cost: Effect
    ) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate {
            val newCost = cost.applyTextReplacement(replacer)
            return if (newCost !== cost) copy(cost = newCost) else this
        }
    }

    /**
     * Not a decision — a state test. The gate succeeds iff [condition] holds at resolution time;
     * there is no yes/no prompt and no pause. [GatedEffect.then] runs when the condition is met,
     * [GatedEffect.otherwise] when it is not. The condition is evaluated through the single
     * `ConditionEvaluationContext`, so it reads identically at resolution and under projection
     * (never a separate `*ProjectionCondition`). Replaces ConditionalEffect.
     *
     * @property condition The condition that must hold for [GatedEffect.then] to run.
     */
    @SerialName("Gate.WhenCondition")
    @Serializable
    data class WhenCondition(
        val condition: Condition
    ) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate {
            val newCondition = condition.applyTextReplacement(replacer)
            return if (newCondition !== condition) copy(condition = newCondition) else this
        }
    }
}

/**
 * "You may [cost]. If you do, [ifPaid]. Otherwise, [ifNotPaid]."
 *
 * Backwards-compatible facade preserved for the cards (and the `mayPay` / `mayPayOrElse`
 * patterns) that authored against the former `OptionalCostEffect` data class. It now lowers
 * to a [GatedEffect] with a [Gate.MayPay] gate — one frame, one executor, one resumer — so
 * there is no bespoke optional-cost executor or continuation. Card source is unchanged; only
 * the compiled/serialized representation moved to `Gated`.
 */
@Suppress("FunctionName")
fun OptionalCostEffect(
    cost: Effect,
    ifPaid: Effect,
    ifNotPaid: Effect? = null,
    descriptionOverride: String? = null
): GatedEffect = GatedEffect(
    gate = Gate.MayPay(cost),
    then = ifPaid,
    otherwise = ifNotPaid,
    descriptionOverride = descriptionOverride
)

/**
 * "You may [effect]." — the player may choose to perform or skip [effect].
 *
 * Backwards-compatible facade preserved for the cards that authored against the former
 * `MayEffect` data class. It now lowers to a [GatedEffect] with a [Gate.MayDecide] gate —
 * one frame, one executor, one resumer — so there is no bespoke `MayEffect` executor.
 * Card source is unchanged; only the compiled/serialized representation moved to `Gated`.
 *
 * @param effect The optional inner effect (becomes [GatedEffect.then]).
 * @param descriptionOverride Hand-written prompt text instead of the gate-derived "You may …".
 * @param sourceRequiredZone Skip silently if the source has left this zone by resolution.
 * @param inlineOnTrigger Render the yes/no inline on the triggering permanent.
 * @param hint Optional reminder text shown under the prompt.
 * @param decisionMaker Who answers the yes/no. Defaults to the controller; only the prompt is
 *   delegated (e.g. [EffectTarget.TargetController] for "that creature's controller may …").
 */
@Suppress("FunctionName")
fun MayEffect(
    effect: Effect,
    descriptionOverride: String? = null,
    sourceRequiredZone: Zone? = null,
    inlineOnTrigger: Boolean = false,
    hint: String? = null,
    decisionMaker: EffectTarget? = null
): GatedEffect = GatedEffect(
    gate = Gate.MayDecide(
        hint = hint,
        sourceRequiredZone = sourceRequiredZone,
        inlineOnTrigger = inlineOnTrigger
    ),
    then = effect,
    decisionMaker = decisionMaker,
    descriptionOverride = descriptionOverride
)
