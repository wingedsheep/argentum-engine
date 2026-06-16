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
        is Gate.DoAction -> buildString {
            append(g.action.description.replaceFirstChar { it.uppercase() })
            append(". If you do, ")
            append(then.description.replaceFirstChar { it.lowercase() })
            if (otherwise != null) {
                append(". If you don't, ${otherwise.description.replaceFirstChar { it.lowercase() }}")
            }
        }
        is Gate.MayPayX -> buildString {
            append("You may pay {X}. If you do, ${then.description.replaceFirstChar { it.lowercase() }}")
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
 * the single [GatedEffect] frame and its executor (decision-driven gates also share its
 * resumer; the action-drain and number-chooser gates keep their own bespoke continuations).
 * The set covers the single-decision-maker wrappers: yes/no ([MayDecide]), fixed cost
 * ([MayPay]), state test ([WhenCondition]), action-outcome ([DoAction]), and variable-mana
 * pay ([MayPayX]). The multi-player `AnyPlayerMayPayEffect` (APNAP ordering) stays a
 * standalone effect — a single [GatedEffect.decisionMaker] can't express its turn-order loop.
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
     * @property feasibility If set and unmet at resolution, the may-action is impossible — so the
     *   player "doesn't": the yes/no prompt is skipped and [GatedEffect.otherwise] runs directly
     *   (the no-target analogue of a targeted "may" with no legal targets falling to its else
     *   branch). Lets a "you may sacrifice an artifact. If you don't, …" trigger apply its else
     *   automatically when the controller has no artifact, with no pointless "sacrifice?" prompt.
     */
    @SerialName("Gate.MayDecide")
    @Serializable
    data class MayDecide(
        val prompt: String? = null,
        val hint: String? = null,
        val sourceRequiredZone: Zone? = null,
        val inlineOnTrigger: Boolean = false,
        val feasibility: FeasibilityCheck? = null
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

    /**
     * Not a decision — an *action-outcome* gate. [action] is performed (it may itself pause for
     * its own sub-decisions); once it has fully resolved, [successCriterion] scores it against a
     * pre-action snapshot to decide whether the action actually "happened". On success →
     * [GatedEffect.then]; on failure → [GatedEffect.otherwise].
     *
     * Models MTG's "[action]. If you do, [then]" templating, where the payoff is conditional on
     * the instruction being carried out — not on a yes/no decision. The classic case: "You may
     * discard a card. If you do, draw a card" — when the player declines or the hand is empty no
     * discard happens, so the draw ([then]) doesn't either. Distinct from [MayDecide] (gates on
     * the *decision*, so a "yes" with nothing to discard still passes) and [MayPay] (gates on
     * paying a recognized cost primitive). Replaces the `IfYouDoEffect` wrapper (see its facade).
     *
     * @property action The action whose outcome gates the branch.
     * @property successCriterion How to decide "did it happen" — see [SuccessCriterion]. Defaults
     *   to [SuccessCriterion.Auto] (infer from the action's terminal zone-move shape).
     */
    @SerialName("Gate.DoAction")
    @Serializable
    data class DoAction(
        val action: Effect,
        val successCriterion: SuccessCriterion = SuccessCriterion.Auto
    ) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate {
            val newAction = action.applyTextReplacement(replacer)
            return if (newAction !== action) copy(action = newAction) else this
        }
    }

    /**
     * Optionally pay a *variable* amount of generic mana — "You may pay {X}. If you do, [then]."
     * Unlike [MayPay] (a fixed cost resolved by a yes/no), here the *amount* is the decision: the
     * decision-maker is prompted for a number from 0 to the most generic mana they can currently
     * produce. Paying X > 0 succeeds → [GatedEffect.then] runs with the chosen X bound into the
     * resolution context (read via `DynamicAmount.XValue`); declining (X = 0) is failure →
     * [GatedEffect.otherwise] (none for the current cards). An unaffordable gate (no mana available)
     * is skipped silently. Carries no fields — the {X} cost is implicit and the amount is chosen at
     * resolution. Replaces the `MayPayXForEffect` wrapper (see its facade).
     */
    @SerialName("Gate.MayPayX")
    @Serializable
    data object MayPayX : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate = this
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
