package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.SELF_NOUN_TOKEN
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reminder text under a yes/no prompt whose number is only known at resolution.
 *
 * A printed "you may … *that much* damage / *that many* cards" renders the same sentence on every
 * instance, so when several instances of one ability are on the stack at once the prompts are
 * indistinguishable and the player is choosing blind. [template]'s `{n}` placeholder is replaced
 * with [amount] evaluated against the resolving context, turning "you may have it deal that much
 * damage" into a prompt that says *which* number this instance carries.
 *
 * The oracle text in the prompt itself is left alone — this is a line underneath it.
 *
 * ```
 * Effects.May(
 *     Effects.DealDamage(DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT), victim),
 *     dynamicHint = DynamicHint(
 *         "This instance would deal {n} damage.",
 *         DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
 *     ),
 * )
 * ```
 *
 * @property template Hint text containing `{n}` where the number belongs. Text without `{n}` is
 *   shown unchanged, which is simply a static hint.
 * @property amount Evaluated at resolution against the same context the gated effect resolves in,
 *   so it sees the triggering event's values.
 */
@Serializable
data class DynamicHint(
    val template: String,
    val amount: DynamicAmount
) {
    companion object {
        /** The placeholder [template] substitutes. */
        const val PLACEHOLDER: String = "{n}"
    }
}

/**
 * Presents a [Gate.MayDecide] whose `otherwise` is a real alternative — not "do it / don't" but
 * "this outcome / that outcome" — as two peer options rather than a Yes/No pair.
 *
 * Endure is the case that motivated it: "put N +1/+1 counters on it, or create an N/N Spirit" is a
 * consent gate in the rules (CR 701.63a), but to the player it is a pick between two results, and a
 * green "Yes" beside a grey "No" under a two-sentence prompt hides which button makes the Spirit.
 *
 * Every string is a template rendered at resolution: `{n}` becomes [amount] evaluated against the
 * resolving context, and `{s}` becomes `"s"` unless that number is 1 — so "{n} +1/+1 counter{s}"
 * reads "1 +1/+1 counter" and "3 +1/+1 counters". Without an [amount] both placeholders stay as
 * written.
 *
 * @property prompt The decision's title ("Endure {n}"), shown in place of the effect's oracle text.
 * @property yes Label for the option that runs [GatedEffect.then].
 * @property no Label for the option that runs [GatedEffect.otherwise].
 * @property yesDetail Optional second line under [yes] ("on this creature").
 * @property noDetail Optional second line under [no].
 * @property amount The number the templates substitute.
 */
@Serializable
data class ChoiceLabels(
    val prompt: String,
    val yes: String,
    val no: String,
    val yesDetail: String? = null,
    val noDetail: String? = null,
    val amount: DynamicAmount? = null
)

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
 *  2. The gate is resolved at resolution time (CR 608.2c) via [decisionMaker].
 *  3. On success → [then]; on failure → [otherwise].
 *
 * This is the "composition over enumeration" replacement for the wrapper-per-concern
 * cluster (Effects.May / Effects.IfYouDo / Effects.MayPay / …). It models the decision-driven
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
) : Effect, SelfReferentialDescription {
    // Built twice from one renderer: once over each sub-effect's self-noun *template* (so the
    // type-aware render layer can say "tap this land" / name the card), once over the plain
    // descriptions. The two agree once the placeholder is default-resolved.
    override val descriptionTemplate: String
        get() = descriptionOverride ?: render { (it as? SelfReferentialDescription)?.descriptionTemplate ?: it.description }

    override val description: String = descriptionOverride ?: render { it.description }

    private fun render(textOf: (Effect) -> String): String {
        val thenText = textOf(then).trim().removeSuffix(".")
        val otherwiseText = otherwise?.let { textOf(it).trim().removeSuffix(".") }?.takeIf { it.isNotBlank() }
        return when (val g = gate) {
            is Gate.MayDecide -> g.prompt ?: buildString {
                append("You may ${mayClause(thenText)}")
                if (otherwiseText != null) append(". Otherwise, ${otherwiseText.lowercaseFirst()}")
            }
            is Gate.MayPay -> buildString {
                append("You may ${mayClause(textOf(g.cost).trim().removeSuffix("."))}")
                // "You may pay 2 life. If you don't, it enters tapped." — paying can be its own
                // reward (an empty `then`), and "If you do, ." is not a sentence.
                if (thenText.isNotBlank()) {
                    append(". If you do, ${thenText.lowercaseFirst()}")
                    if (otherwiseText != null) append(". Otherwise, ${otherwiseText.lowercaseFirst()}")
                } else if (otherwiseText != null) {
                    append(". If you don't, ${otherwiseText.lowercaseFirst()}")
                }
            }
            is Gate.WhenCondition -> buildString {
                // Most conditions read "if …"; a bare comparison ("you gained life this turn")
                // needs the "If" supplied.
                val condition = g.condition.description
                val clause = if (condition.startsWith("if ", ignoreCase = true) ||
                    condition.startsWith("unless ", ignoreCase = true)
                ) condition else "if $condition"
                append(clause.replaceFirstChar { it.uppercase() })
                append(", ")
                append(pronounForTestedTarget(g.condition, thenText).lowercaseFirst())
                if (otherwiseText != null) append(". Otherwise, ${otherwiseText.lowercaseFirst()}")
            }
            is Gate.DoAction -> buildString {
                append(textOf(g.action).trim().removeSuffix(".").replaceFirstChar { it.uppercase() })
                append(". If you do, ")
                append(thenText.lowercaseFirst())
                if (otherwiseText != null) append(". If you don't, ${otherwiseText.lowercaseFirst()}")
            }
            is Gate.MayPayX -> buildString {
                append("You may pay {X}${g.additionalCost}. If you do, ${thenText.lowercaseFirst()}")
                if (otherwiseText != null) append(". Otherwise, ${otherwiseText.lowercaseFirst()}")
            }
            is Gate.MayPayAnyAmountOfLife -> buildString {
                append("You may pay any amount of life. If you do, ${thenText.lowercaseFirst()}")
                if (otherwiseText != null) append(". Otherwise, ${otherwiseText.lowercaseFirst()}")
            }
            // The budget gate is invisible in prompt text — the "Do this only once each turn" rider is
            // rendered once, on the owning TriggeredAbility. Rendering it here too would double it up
            // inside the enclosing "You may …" prompt.
            is Gate.OnceEachTurn -> textOf(then)
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
     * [GatedEffect.then] happens at all. Replaces the `Effects.May` wrapper (see the
     * [Effects.May] facade).
     *
     * @property prompt Optional override for the yes/no prompt text.
     * @property hint Optional reminder text shown under the prompt.
     * @property dynamicHint Reminder text whose number is resolved at resolution ([DynamicHint]).
     *   Takes precedence over [hint] when set — use it when several instances of one ability can be
     *   on the stack at once carrying different numbers, so the prompts can be told apart.
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
     * @property choiceLabels When set, the prompt is shown as a pick between two peer outcomes with
     *   these labels instead of a Yes/No question ([ChoiceLabels]). Use it only when
     *   [GatedEffect.otherwise] is an outcome in its own right, never for a plain "you may".
     */
    @SerialName("Gate.MayDecide")
    @Serializable
    data class MayDecide(
        val prompt: String? = null,
        val hint: String? = null,
        val dynamicHint: DynamicHint? = null,
        val sourceRequiredZone: Zone? = null,
        val inlineOnTrigger: Boolean = false,
        val feasibility: FeasibilityCheck? = null,
        val choiceLabels: ChoiceLabels? = null
    ) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate = this
    }

    /**
     * Optionally pay a cost — "You may [cost]. If you do, [then]." The gate succeeds iff
     * the [cost] effect is paid in full. Affordability is checked before prompting, so an
     * unpayable cost skips straight to [GatedEffect.otherwise] rather than offering an
     * impossible "yes". Replaces Effects.MayPay.
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
     * (never a separate `*ProjectionCondition`). Replaces Effects.If.
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
     * paying a recognized cost primitive). Replaces the `Effects.IfYouDo` wrapper (see its facade).
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
     * Unlike [MayPay] (a fixed cost), the *amount* is also a decision: after "Pay {X}?" (no →
     * [GatedEffect.otherwise]) the decision-maker names a number from 0 to the largest X they can
     * currently afford, and [GatedEffect.then] runs with that X bound into the resolution context
     * (read via `DynamicAmount.XValue`). X = 0 is a real payment, not a decline — Isareth the
     * Awakener can return a mana value 0 creature card; when nothing beyond [additionalCost] is
     * affordable, "yes" pays X = 0 without the number prompt.
     *
     * @property additionalCost The fixed part paid alongside X in the same payment — the `{R}` of
     *   "you may pay {X}{R}" (Tilonalli's Summoner), the `{W}{U}{U}` of "you may pay {X}{W}{U}{U}"
     *   (Azor, the Lawbringer). Empty for a bare "{X}". When it can't be paid at resolution the
     *   "yes" is impossible, so the prompt is skipped and [GatedEffect.otherwise] runs.
     *   Replaces the `Effects.MayPayX` wrapper (see its facade).
     */
    @SerialName("Gate.MayPayX")
    @Serializable
    data class MayPayX(val additionalCost: ManaCost = ManaCost.ZERO) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate = this
    }

    /**
     * Optionally pay a *variable* amount of life — "You may pay any amount of life. If you do,
     * [then]." The life twin of [MayPayX]: the decision-maker names a number from 0 to the most
     * life they can pay (their life total, or 0 under a "can't lose life" lock — CR 119.4, 119.8).
     * Paying X > 0 succeeds → [GatedEffect.then] runs with X bound into the resolution context
     * (read via `DynamicAmount.XValue`, e.g. "draw that many cards"); choosing 0 is the decline and
     * runs [GatedEffect.otherwise] (if any). A player who can pay no life is not prompted and goes
     * straight to `otherwise`. Necrodominance.
     */
    @SerialName("Gate.MayPayAnyAmountOfLife")
    @Serializable
    data object MayPayAnyAmountOfLife : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate = this
    }

    /**
     * Not a decision — a **per-turn action budget**. Models the printed rider "*Do this only once
     * each turn*" (Jennifer Walters // The Sensational She-Hulk, Baron Strucker, HYDRA Overlord).
     *
     * The gate succeeds iff the source permanent's controller has not yet taken this ability's
     * indicated action this turn; succeeding *spends* the budget when [spend] is true, so at most
     * one resolution per turn runs the real payoff. Check and spend are one atomic step in the
     * executor — there is no window where two simultaneous instances both see an unspent budget.
     *
     * **This is not the trigger cap — the two printed templates are different and must not be
     * swapped.** Per CR 603.2h an ability carrying "Do this only once each turn" *triggers only if
     * its source's controller has not yet taken the indicated action that turn*: while the action
     * is untaken every matching event triggers a fresh instance, and once it is taken the ability
     * stops triggering for the rest of the turn and instances already on the stack do nothing as
     * they resolve. The other wording, "*This ability triggers only once each turn*", stops after
     * the *first trigger* whether or not the action happened, and is
     * [com.wingedsheep.sdk.scripting.TriggeredAbility.oncePerTurn] instead.
     *
     * Cards never author this gate directly: set
     * [com.wingedsheep.sdk.scripting.TriggeredAbility.effectOncePerTurn] and the engine lowers it
     * into this gate at trigger-processing time. For an optional ability the lowering emits *two*
     * of these: a [spend]`= false` check outside the consent gate, so an instance whose budget is
     * already gone resolves silently instead of raising a yes/no that cannot matter, and the
     * spending one inside it, so declining costs nothing.
     *
     * @property abilityId The ability whose budget this gate reads. Budgets are tracked per
     *   (source permanent, ability) — CR 603.2h scopes the rider to "*its source's controller*" for
     *   *that* ability, and the Nykthos Paragon ruling spells it out: two copies of the permanent
     *   each get their own use. The id is carried explicitly rather than read off `EffectContext`
     *   because `EffectContext.abilityIdentity` is definition-scoped and null for synthesized
     *   sources (spell copies), which would silently merge or lose budgets.
     * @property spend Whether succeeding also marks the action taken. `false` is a read-only
     *   pre-check used by the lowering above; exactly one gate per ability may spend.
     */
    @SerialName("Gate.OnceEachTurn")
    @Serializable
    data class OnceEachTurn(
        val abilityId: AbilityId,
        val spend: Boolean = true
    ) : Gate {
        override fun applyTextReplacement(replacer: TextReplacer): Gate = this
    }
}

/**
 * Gates that are the printed **"you may"** — the controller's own consent to the effect, as opposed
 * to a state test ([Gate.WhenCondition]) or a bookkeeping cap ([Gate.OnceEachTurn]).
 *
 * One list, in the SDK, because three layers ask the same question about the same value: the DSL
 * refuses to lower `optional = true` onto an effect that already asks, the engine's
 * `effectOncePerTurn` lowering has to place its budget gate *inside* this gate, and the trigger
 * processor decides where a "you may" is answered by whether the effect owns one.
 */
val Gate.isConsentGate: Boolean
    get() = this is Gate.MayDecide || this is Gate.MayPay || this is Gate.MayPayX ||
        this is Gate.MayPayAnyAmountOfLife

/**
 * Does this effect already ask its controller for consent before doing anything?
 *
 * Looks through a [Gate.OnceEachTurn] because the `effectOncePerTurn` lowering sandwiches a capped
 * optional ability as `OnceEachTurn(spend = false) → May… → OnceEachTurn() → effect`; the consent
 * gate is still there, one level down.
 */
fun Effect.ownsConsentGate(): Boolean {
    val gated = this as? GatedEffect ?: return false
    return gated.gate.isConsentGate || (gated.gate is Gate.OnceEachTurn && gated.then.ownsConsentGate())
}

/**
 * "If that creature is a Bear, put a +1/+1 counter on *it*" — once a condition has named the
 * target it tests, the branch it gates calls that target "it" instead of repeating the whole
 * target phrase, which would read as a second target.
 */
private fun pronounForTestedTarget(condition: Condition, then: String): String {
    val entity = (condition as? com.wingedsheep.sdk.scripting.conditions.EntityMatches)?.entity ?: return then
    if (entity !is EffectTarget.BoundVariable && entity !is EffectTarget.ContextTarget) return then
    val phrase = entity.description
    if (targetNoun(phrase) == null) return then
    return then.replace(phrase, "it")
}

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase() }

/**
 * Words that open a clause whose subject is something other than the decision-maker — "*target
 * creature* gets +2/+0", "*this creature* becomes …", "*each opponent* loses 1 life". After "you
 * may" such a clause needs "have" and its verb in the base form: "you may have target creature get
 * +2/+0".
 */
private val NON_YOU_SUBJECT_OPENERS = setOf(
    "target", "this", "that", "each", "it", "its", "enchanted", "equipped", "another", "all",
    "defending", "up", SELF_NOUN_TOKEN
)

/**
 * The third-person verbs effect descriptions are generated with, mapped to the base form a "you may
 * (have X) …" clause takes. Only these are rewritten — a relative clause's verb ("a creature that
 * player *controls*") is deliberately absent so it is never mistaken for the main verb.
 */
private val THIRD_PERSON_VERBS: Map<String, String> = listOf(
    "becomes", "gets", "gains", "loses", "fights", "deals", "discards", "draws", "sacrifices",
    "shuffles", "connives", "blocks", "untaps", "taps", "mills", "returns", "explores", "attacks",
    "creates", "reveals", "searches", "exiles", "phases", "transforms", "puts", "assigns", "copies",
    "chooses", "pays", "scries", "surveils", "investigates", "proliferates", "adapts", "has", "is",
    "does", "dies", "takes", "skips", "plays", "casts", "looks", "doubles", "switches"
).associateWith { verb ->
    when {
        verb == "has" -> "have"
        verb == "is" -> "be"
        verb == "does" -> "do"
        verb == "dies" -> "die"
        verb.endsWith("ies") -> verb.dropLast(3) + "y"
        verb.endsWith("ches") || verb.endsWith("shes") || verb.endsWith("sses") || verb.endsWith("xes") -> verb.dropLast(2)
        else -> verb.dropLast(1)
    }
}

/**
 * Rewrite a generated effect clause so it reads after "You may " — the decision-maker is the
 * implicit subject, so a clause that already names one must not repeat or contradict it:
 *
 *  - "You gain that many life" → "gain that many life" (not "You may you gain …")
 *  - "this creature becomes an Equipment …" → "have this creature become an Equipment …"
 *  - "Target creature gets +1/+1 until end of turn" → "have target creature get +1/+1 …"
 *  - "draw a card" → unchanged (already imperative)
 *
 * Only the first sentence's main verb (and any verb joined to it by "and") is put in the base
 * form; quoted ability text is never touched.
 */
internal fun mayClause(clause: String): String {
    val text = clause.trim()
    val firstWord = text.substringBefore(' ').lowercase()
    return when {
        firstWord == "you" -> baseFormVerbs(text.substringAfter(' ')).lowercaseFirst()
        firstWord in NON_YOU_SUBJECT_OPENERS -> "have " + baseFormVerbs(text).lowercaseFirst()
        else -> text.lowercaseFirst()
    }
}

/**
 * Put the main verb of [text]'s first sentence into its base form — the first known third-person
 * verb outside quotes, plus any later one directly after "and" ("becomes … and loses"). Text after
 * the first sentence, and inside quotes, is left as written.
 */
private fun baseFormVerbs(text: String): String {
    val sentenceEnd = text.indexOf(". ").let { if (it < 0) text.length else it }
    val head = text.substring(0, sentenceEnd)
    val tail = text.substring(sentenceEnd)
    val out = StringBuilder()
    var inQuote = false
    var foundMain = false
    var previousWord = ""
    val tokens = Regex("""[A-Za-z']+|[^A-Za-z']""").findAll(head).map { it.value }.toList()
    for (token in tokens) {
        if (token == "\"") inQuote = !inQuote
        val base = THIRD_PERSON_VERBS[token]
        if (!inQuote && base != null && (!foundMain || previousWord == "and")) {
            out.append(base)
            foundMain = true
        } else {
            out.append(token)
        }
        if (token.isNotBlank() && token.first().isLetter()) previousWord = token
    }
    return out.toString() + tail
}
