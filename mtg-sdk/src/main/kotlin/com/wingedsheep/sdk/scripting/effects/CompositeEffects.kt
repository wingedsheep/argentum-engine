package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Composite Effects
// =============================================================================

/**
 * Multiple effects that happen together.
 *
 * By default, [description] and [runtimeDescription] concatenate each sub-effect's
 * own text with ". ". For cards whose pipeline produces a verbose or implementation-leaking
 * join (e.g., intermediate `StoreNumber` steps), supply [descriptionOverride] to render
 * a single hand-written sentence instead. Use `{0}`, `{1}`, ... placeholders paired with
 * [descriptionAmounts] to interpolate evaluated dynamic values at runtime — the static
 * [description] leaves placeholders literal.
 */
@SerialName("Composite")
@Serializable
data class CompositeEffect(
    val effects: List<Effect>,
    val stopOnError: Boolean = false,
    val descriptionOverride: String? = null,
    val descriptionAmounts: List<DynamicAmount> = emptyList()
) : Effect {
    override val description: String =
        descriptionOverride ?: effects.joinToString(". ") { it.description }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String {
        val template = descriptionOverride
            ?: return effects.joinToString(". ") { it.runtimeDescription(resolver) }
        var rendered = template
        descriptionAmounts.forEachIndexed { index, amount ->
            rendered = rendered.replace("{$index}", resolver(amount).toString())
        }
        return rendered
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newEffects = effects.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        val newAmounts = descriptionAmounts.map { val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n }
        return if (changed) copy(effects = newEffects, descriptionAmounts = newAmounts) else this
    }
}

/**
 * Represents a single mode in a modal spell.
 *
 * Each mode can have its own targeting requirements, allowing cards like
 * Cryptic Command where different modes need different targets.
 *
 * Modes can also carry per-mode costs for cards like Feed the Cycle
 * ("forage or pay {B}") where different modes have different costs.
 *
 * @property effect The effect when this mode is chosen
 * @property targetRequirements Targets required for this specific mode
 * @property description Human-readable description of the mode
 * @property additionalManaCost Extra mana added to the spell's cost when this mode is chosen (e.g., "{B}")
 * @property additionalCosts Per-mode additional costs (e.g., Forage). When non-null, overrides card-level additional costs.
 */
@Serializable
data class Mode(
    val effect: Effect,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val description: String = effect.description,
    val additionalManaCost: String? = null,
    val additionalCosts: List<AdditionalCost>? = null
) {
    companion object {
        /**
         * Create a mode with no targeting.
         */
        fun noTarget(effect: Effect, description: String = effect.description): Mode =
            Mode(effect, emptyList(), description)

        /**
         * Create a mode with a single target.
         */
        fun withTarget(effect: Effect, target: TargetRequirement, description: String = effect.description): Mode =
            Mode(effect, listOf(target), description)
    }
}

/**
 * Modal spell effect - choose one or more of several modes.
 * "Choose one — [Mode A] or [Mode B]"
 * "Choose two — [Mode A], [Mode B], [Mode C], or [Mode D]"
 * "Choose one or both — [Mode A], [Mode B]"
 * "Choose one or more —" (Escalate / Spree style, combined with [allowRepeat] on Spree)
 *
 * Each mode can have its own targeting requirements, which are combined
 * based on which modes are chosen when the spell is cast.
 *
 * Example (Cryptic Command):
 * ```kotlin
 * ModalEffect(
 *     modes = listOf(
 *         Mode.withTarget(CounterSpellEffect, TargetSpell(), "Counter target spell"),
 *         Mode.withTarget(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand), TargetPermanent(), "Return target permanent to its owner's hand"),
 *         Mode.noTarget(TapAllCreaturesEffect(CreatureGroupFilter.OpponentsControl), "Tap all creatures your opponents control"),
 *         Mode.noTarget(DrawCardsEffect(1), "Draw a card")
 *     ),
 *     chooseCount = 2
 * )
 * ```
 *
 * @property modes List of possible modes to choose from
 * @property chooseCount Maximum number of modes to choose (default 1)
 * @property minChooseCount Minimum number of modes to choose. Defaults to [chooseCount]
 *           (i.e. "choose exactly N"). Set lower for "choose one or both" / "choose one or more" (rules 700.2).
 * @property allowRepeat If true, the same mode index may be chosen more than once
 *           (rules 700.2d — Escalate/Spree-style).
 */
@SerialName("Modal")
@Serializable
data class ModalEffect(
    val modes: List<Mode>,
    val chooseCount: Int = 1,
    val minChooseCount: Int = chooseCount,
    val allowRepeat: Boolean = false,
    /**
     * If true, when this spell's `AdditionalCost.BlightOrPay` cost was paid via the
     * blight path, the effective number of modes the player must choose becomes
     * `modes.size` (i.e. all modes). When the blight path was not paid, the regular
     * [chooseCount]/[minChooseCount] apply. Models the "Choose one. If this spell's
     * additional cost was paid, choose both instead." pattern (e.g., Pyrrhic Strike).
     */
    val chooseAllIfBlightPaid: Boolean = false,
    /**
     * Optional runtime-evaluated upper bound on the number of modes that may be chosen.
     * When non-null, the engine's modal executor evaluates this against the current
     * resolution context and uses the result as the effective maximum, clamped to
     * `modes.size`. Used for "choose up to X" triggered abilities where X depends on
     * resolution-time data (Riku of Many Paths — X is the number of modes the cast
     * modal spell chose). [minChooseCount] is treated as `0` when this is set
     * (i.e. always "choose up to"); [chooseCount] is ignored.
     */
    val dynamicChooseCount: com.wingedsheep.sdk.scripting.values.DynamicAmount? = null,
    /**
     * Whether choosing among [modes] makes this a *modal spell* in MTG terms (rules
     * 700.2). `true` for printed "Choose one — • X • Y" wording; `false` when this
     * data class is used as an implementation shortcut for a non-modal mechanic —
     * notably Gift ("you may promise an opponent a gift as you cast this spell"),
     * which models a yes/no cost decision as two modes but is *not* a modal spell.
     * `SpellCastEvent.chosenModesCount` is reported as 0 when this is `false`, so
     * `SpellCastPredicate.IsModal` and `ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL`
     * see only true modal spells.
     */
    val countsAsModalSpell: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        when {
            dynamicChooseCount != null -> {
                append("up to ")
                append(dynamicChooseCount.description)
            }
            minChooseCount < chooseCount && chooseCount == 2 && modes.size == 2 -> append("one or both")
            minChooseCount < chooseCount -> append("one or more")
            else -> when (chooseCount) {
                1 -> append("one")
                2 -> append("two")
                3 -> append("three")
                else -> append(chooseCount)
            }
        }
        append(" —\n")
        modes.forEachIndexed { index, mode ->
            append("• ")
            append(mode.description)
            if (index < modes.lastIndex) append("\n")
        }
        if (allowRepeat) {
            append("\nYou may choose the same mode more than once.")
        }
        if (chooseAllIfBlightPaid) {
            append("\nIf this spell's additional cost was paid, choose all instead.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var anyChanged = false
        val newModes = modes.map { mode ->
            val newEffect = mode.effect.applyTextReplacement(replacer)
            var modeChanged = newEffect !== mode.effect
            val newTargetReqs = mode.targetRequirements.map { tr ->
                val n = tr.applyTextReplacement(replacer); if (n !== tr) modeChanged = true; n
            }
            if (modeChanged) anyChanged = true
            if (modeChanged) mode.copy(effect = newEffect, targetRequirements = newTargetReqs) else mode
        }
        return if (anyChanged) copy(modes = newModes) else this
    }

    companion object {
        /**
         * Create a simple modal effect with effects that have no targeting.
         * Backwards compatible with the old List<Effect> pattern.
         */
        fun simple(effects: List<Effect>, chooseCount: Int = 1): ModalEffect =
            ModalEffect(effects.map { Mode.noTarget(it) }, chooseCount)

        /**
         * Create a choose-one modal effect.
         *
         * @param countsAsModalSpell Pass `false` for non-modal mechanics (Gift) that
         *   reuse this data class to model a binary choice — see [ModalEffect.countsAsModalSpell].
         */
        fun chooseOne(vararg modes: Mode, countsAsModalSpell: Boolean = true): ModalEffect =
            ModalEffect(modes.toList(), 1, countsAsModalSpell = countsAsModalSpell)

        /**
         * Create a choose-two modal effect (Cryptic Command style).
         */
        fun chooseTwo(vararg modes: Mode): ModalEffect =
            ModalEffect(modes.toList(), 2)

        /**
         * Create a "choose up to X" modal effect where X is evaluated at resolution
         * time from a [com.wingedsheep.sdk.scripting.values.DynamicAmount]. The player
         * may decline (pick 0) and may pick at most `min(X, modes.size)` total modes.
         * Mode repetition is not allowed by default (per CR-compliant "choose up to N")
         * — pass `allowRepeat = true` for Spree-style behavior.
         *
         * Used by triggered abilities like Riku of Many Paths.
         */
        fun chooseUpToDynamic(
            dynamicMax: com.wingedsheep.sdk.scripting.values.DynamicAmount,
            vararg modes: Mode,
            allowRepeat: Boolean = false
        ): ModalEffect = ModalEffect(
            modes = modes.toList(),
            chooseCount = modes.size,
            minChooseCount = 0,
            allowRepeat = allowRepeat,
            dynamicChooseCount = dynamicMax
        )
    }
}

/**
 * "[action]. If you do, [ifYouDo]." — conditional execution gated on whether [action] actually
 * accomplished its work, not on a yes/no decision. The classic case: "You may discard a card. If
 * you do, draw a card" — when the player declines or the hand is empty, no discard happens, so no
 * draw happens.
 *
 * Backwards-compatible facade preserved for the cards (and the `Effects.IfYouDo` facade) that
 * authored against the former `IfYouDoEffect` data class. It now lowers to a [GatedEffect] with a
 * [Gate.DoAction] gate — one frame, one executor, one resumer — so there is no bespoke `IfYouDo`
 * executor or continuation type of its own. Card source is unchanged; only the compiled/serialized
 * representation moved to `Gated`.
 *
 * Differences from related gates:
 * - [MayEffect] / [Gate.MayDecide] gates on the *decision* (yes/no), not the *outcome*. A "yes"
 *   with nothing to discard still passes through. Wrap with `MayEffect` for "You may [action]. If
 *   you do, [effect]": `MayEffect(IfYouDoEffect(action, then))`.
 * - [OptionalCostEffect] / [Gate.MayPay] gates on *paying a recognized cost primitive* (mana /
 *   life) via a payability check before prompting; it does not handle discard / sacrifice / mill /
 *   etc. where success is data-driven.
 * - [CompositeEffect].`stopOnError` aborts on raised errors only — silent zero-progress actions
 *   (empty hand, no legal sacrifice) still let downstream effects run.
 *
 * @param action The action whose outcome gates [ifYouDo] (becomes [Gate.DoAction.action]).
 * @param ifYouDo Effect that runs only if [action] performed its work (becomes [GatedEffect.then]).
 * @param ifYouDont Optional effect that runs if [action] did nothing (becomes [GatedEffect.otherwise]).
 * @param successCriterion How to determine "did it happen". Defaults to [SuccessCriterion.Auto],
 *   which infers from the action shape (pipeline ending in a move → destination zone grew).
 */
@Suppress("FunctionName")
fun IfYouDoEffect(
    action: Effect,
    ifYouDo: Effect,
    ifYouDont: Effect? = null,
    successCriterion: SuccessCriterion = SuccessCriterion.Auto,
    descriptionOverride: String? = null
): GatedEffect = GatedEffect(
    gate = Gate.DoAction(action, successCriterion),
    then = ifYouDo,
    otherwise = ifYouDont,
    descriptionOverride = descriptionOverride
)

/**
 * How to determine whether an [IfYouDoEffect] action accomplished its work.
 */
@Serializable
sealed interface SuccessCriterion {
    /**
     * Infer success from the action's shape. The executor walks [IfYouDoEffect.action]
     * for a terminal zone move — either a pipeline [MoveCollectionEffect] or a
     * single-target `MoveToZoneEffect` whose target is the source itself; if found, the
     * destination zone is snapshot pre-execution and counted as "succeeded" iff it grew
     * by at least one entry.
     *
     * Auto is only legal on actions whose shape it can actually infer ([canInfer]) —
     * card-load validation ([com.wingedsheep.sdk.serialization.CardValidator]) rejects
     * everything else. An action whose outcome isn't a zone-size delta (deal damage,
     * gain/lose life, …) must state its criterion explicitly ([Always] when performing
     * the action can't fail, [CollectionNonEmpty] to gate on a pipeline result) instead
     * of silently inheriting a fail-open "it happened".
     */
    @SerialName("SuccessCriterion.Auto")
    @Serializable
    data object Auto : SuccessCriterion {

        /**
         * Whether [Auto] can infer "did it happen" for [action]. This is the single
         * source of truth for the shape probe — the engine's gated-action executor
         * snapshots exactly the shapes accepted here, and card-load validation rejects
         * an [Auto] criterion on any action this returns false for.
         */
        fun canInfer(action: Effect): Boolean {
            terminalCollectionMove(action)?.let { return it.destination is CardDestination.ToZone }
            terminalSingleMove(action)?.let { return it.target is EffectTarget.Self }
            return false
        }

        /**
         * The last [MoveCollectionEffect] in execution order, or null when the shape
         * isn't recognized. Checked before [terminalSingleMove] so a pipeline ending in
         * a collection move keeps its semantics.
         */
        fun terminalCollectionMove(action: Effect): MoveCollectionEffect? = when (action) {
            is MoveCollectionEffect -> action
            is CompositeEffect -> action.effects.asReversed().firstNotNullOfOrNull { terminalCollectionMove(it) }
            else -> null
        }

        /**
         * The last single-target [MoveToZoneEffect] in execution order, or null when the
         * shape isn't recognized. Only a [EffectTarget.Self] target is inferable — other
         * targets can't be resolved to a destination zone owner without full target
         * resolution.
         */
        fun terminalSingleMove(action: Effect): MoveToZoneEffect? = when (action) {
            is MoveToZoneEffect -> action
            is CompositeEffect -> action.effects.asReversed().firstNotNullOfOrNull { terminalSingleMove(it) }
            else -> null
        }
    }

    /**
     * Action succeeded iff `pipeline.storedCollections[name].size >= min` after the
     * action runs. Use when [Auto] inference is wrong (e.g. multi-phase pipelines
     * where you want to gate on a specific intermediate result rather than the
     * terminal move's destination).
     */
    @SerialName("SuccessCriterion.CollectionNonEmpty")
    @Serializable
    data class CollectionNonEmpty(val name: String, val min: Int = 1) : SuccessCriterion

    /**
     * Action always counts as having happened. Equivalent to dropping the gate
     * entirely; mostly useful as the default fallback for unrecognized action shapes.
     */
    @SerialName("SuccessCriterion.Always")
    @Serializable
    data object Always : SuccessCriterion
}

/**
 * "Behold a [filter]" as a resolution-time effect (CR: a player beholds a permanent by either
 * choosing a matching permanent they control or revealing a matching card from their hand).
 * Unlike the cast-time [AdditionalCost.Behold] / [AdditionalCost.BeholdOrPay], this is the
 * *effect-side* behold used inside abilities such as Sarkhan, Dragon Ascendant's ETB
 * ("you may behold a Dragon. If you do, create a Treasure token.").
 *
 * The behold is optional: the player may decline (or be unable to behold if they control no
 * matching permanent and hold no matching card), in which case [ifBeheld] does not run. When a
 * choice is made, a matching hand card is revealed (battlefield permanents are merely chosen,
 * not revealed) and [ifBeheld] runs. No information about the beheld object is otherwise stored —
 * per the "behold reveals are identity, not parameters" principle, this effect only models the
 * Dragon-storm behold-then-payoff shape.
 *
 * @property filter Which permanents/cards qualify to be beheld
 * @property ifBeheld Effect that runs only if the player successfully beholds
 */
@SerialName("Behold")
@Serializable
data class BeholdEffect(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val ifBeheld: Effect? = null
) : Effect {
    override val description: String = buildString {
        val filterDesc = filter.description
        val article = if (filterDesc.firstOrNull()?.lowercase() in listOf("a", "e", "i", "o", "u")) "an" else "a"
        append("You may behold $article $filterDesc")
        if (ifBeheld != null) append(". If you do, ${ifBeheld.description.replaceFirstChar { it.lowercase() }}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newIfBeheld = ifBeheld?.applyTextReplacement(replacer)
        return if (newFilter !== filter || newIfBeheld !== ifBeheld)
            copy(filter = newFilter, ifBeheld = newIfBeheld) else this
    }
}

/**
 * Reflexive trigger - "When you do, [effect]."
 *
 * Used for abilities that trigger from the resolution of another effect.
 * Example: Heart-Piercer Manticore - "You may sacrifice another creature.
 *          When you do, deal damage equal to that creature's power."
 *
 * @property action The optional action (sacrifice, discard, etc.)
 * @property optional Whether the action is optional
 * @property reflexiveEffect The effect that happens "when you do"
 * @property reflexiveTargetRequirements Target requirements for the reflexive effect,
 *           selected AFTER the action completes (not when the trigger goes on the stack).
 *           Use this for cards like Wick's Patrol where the target depends on what the action did.
 */
@SerialName("ReflexiveTrigger")
@Serializable
data class ReflexiveTriggerEffect(
    val action: Effect,
    val optional: Boolean = true,
    val reflexiveEffect: Effect,
    val reflexiveTargetRequirements: List<TargetRequirement> = emptyList(),
    /** Optional hint text shown on the yes/no decision (e.g., keyword reminder text) */
    val hint: String? = null,
    /**
     * Optional override for the auto-generated description. Useful when the action or
     * reflexive effect is a pipeline whose composed description reads poorly (e.g.
     * blight expanded to its Gather/Select/AddCounters internals).
     */
    val descriptionOverride: String? = null
) : Effect {
    override val description: String = descriptionOverride ?: buildString {
        if (optional) append("You may ")
        append(action.description.replaceFirstChar { it.lowercase() })
        append(". When you do, ")
        append(reflexiveEffect.description.replaceFirstChar { it.lowercase() })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAction = action.applyTextReplacement(replacer)
        val newReflexiveEffect = reflexiveEffect.applyTextReplacement(replacer)
        return if (newAction !== action || newReflexiveEffect !== reflexiveEffect)
            copy(action = newAction, reflexiveEffect = newReflexiveEffect) else this
    }
}

/**
 * Generic "unless" effect for punisher mechanics.
 * "Do [suffer], unless you [cost]."
 *
 * This is a unified effect that handles:
 * - "Sacrifice this unless you discard a land card" (Thundering Wurm)
 * - "Sacrifice this unless you sacrifice three Forests" (Primeval Force)
 * - Similar punisher-style effects
 *
 * @property cost The cost that can be paid to avoid the consequence
 * @property suffer The consequence if the cost is not paid
 * @property player Who must make the choice (defaults to controller)
 */
@SerialName("PayOrSuffer")
@Serializable
data class PayOrSufferEffect(
    val cost: PayCost,
    val suffer: Effect,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "${suffer.description} unless you ${cost.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCost = cost.applyTextReplacement(replacer)
        val newSuffer = suffer.applyTextReplacement(replacer)
        return if (newCost !== cost || newSuffer !== suffer)
            copy(cost = newCost, suffer = newSuffer) else this
    }
}

/**
 * The earliest turn a step-based [CreateDelayedTriggerEffect] may fire. A single
 * mutually-exclusive axis (replaces the former onControllerNextTurn / skipCurrentTurn
 * booleans, whose presence/absence encoded these three states).
 *
 * Orthogonal to [CreateDelayedTriggerEffect.fireOnPlayer], which gates *whose* turn the
 * trigger may fire on, not *which* turn is the earliest eligible one.
 */
@Serializable
enum class DelayedTriggerTiming {
    /**
     * No turn floor: the trigger fires at the next upcoming occurrence of its step,
     * which may be the current turn. Default — Astral Slide-style exile-until-end-step.
     */
    @SerialName("CurrentTurnOrLater")
    CURRENT_TURN_OR_LATER,

    /**
     * "At the beginning of your next end step" timing: the current turn's end step
     * still qualifies if it hasn't begun yet; only once it has started (END or CLEANUP
     * on the controller's turn) does the trigger defer to the controller's following
     * turn. Typically paired with `fireOnPlayer = PlayerRef(Player.You)`. (Dragonhawk,
     * Fate's Tempest.)
     */
    @SerialName("NextEndStep")
    NEXT_END_STEP,

    /**
     * "On your next turn" timing: the current turn never qualifies, regardless of step;
     * the trigger fires no earlier than the next turn. Pair with
     * `fireOnPlayer = PlayerRef(Player.You)` to land on the controller's upcoming own turn
     * rather than an intervening opponent turn. (Kav Landseeker.)
     */
    @SerialName("NextTurn")
    NEXT_TURN
}

/**
 * Create a delayed triggered ability that fires at a specific step.
 *
 * When executed, bakes any context-dependent target references (ContextTarget)
 * into concrete SpecificEntity references so the delayed trigger fires correctly
 * even after the original execution context is gone.
 *
 * Used for Astral Slide-style exile-until-end-step effects:
 * ```kotlin
 * CompositeEffect(listOf(
 *     MoveToZoneEffect(ContextTarget(0), Zone.EXILE),
 *     CreateDelayedTriggerEffect(
 *         step = Step.END,
 *         effect = MoveToZoneEffect(ContextTarget(0), Zone.BATTLEFIELD)
 *     )
 * ))
 * ```
 *
 * @param step The step at which the delayed trigger fires
 * @param effect The effect to execute when the trigger fires
 */
@SerialName("CreateDelayedTrigger")
@Serializable
data class CreateDelayedTriggerEffect(
    val step: Step? = null,
    val effect: Effect,
    /**
     * Event-based trigger. When non-null, the delayed trigger fires whenever
     * an event matching this TriggerSpec occurs (scoped to [watchedTarget] if set),
     * and remains resident until [expiry] removes it. When null, the delayed
     * trigger is step-based (fires at the beginning of [step]).
     */
    val trigger: TriggerSpec? = null,
    /**
     * For event-based delayed triggers: the entity that scopes the trigger.
     * The trigger only fires for events sourced from this entity. Context
     * references (e.g. ContextTarget(0)) are baked into a concrete entity id
     * at creation time by CreateDelayedTriggerExecutor.
     */
    val watchedTarget: EffectTarget? = null,
    /**
     * For event-based delayed triggers: scopes the trigger to events whose *recipient*
     * (the damaged / targeted entity) is this target. Whereas [watchedTarget] narrows by
     * the event's **source**, [watchedRecipient] narrows by the event's **recipient** — i.e.
     * "whenever a creature you control deals combat damage to *that player* this turn"
     * (Great Train Heist). Context references (e.g. ContextTarget(0) for the chosen opponent)
     * are baked into a concrete entity id at creation time by CreateDelayedTriggerExecutor.
     * Currently honored for [com.wingedsheep.sdk.scripting.EventPattern.DealsDamageEvent].
     */
    val watchedRecipient: EffectTarget? = null,
    /**
     * For event-based delayed triggers: when the ability is removed.
     */
    val expiry: DelayedTriggerExpiry = DelayedTriggerExpiry.EndOfTurn,
    /**
     * For event-based delayed triggers: when true the ability is a *one-shot* — it is
     * removed the first time it fires, then it's gone (e.g. "when you **next** attack this
     * turn, …", "when you next cast a spell this turn, …"). When false (default) the trigger
     * fires every matching event until [expiry] removes it (e.g. double-strike combat damage).
     * Ignored for step-based triggers, which are always consumed on fire.
     */
    val fireOnce: Boolean = false,
    /**
     * The earliest turn this step-based delayed trigger may fire. See
     * [DelayedTriggerTiming]. Orthogonal to [fireOnPlayer], which gates *whose* turn the
     * trigger fires on (not *which* turn is the earliest eligible one).
     */
    val timing: DelayedTriggerTiming = DelayedTriggerTiming.CURRENT_TURN_OR_LATER,
    /**
     * Target requirement chosen *each time* the delayed trigger fires. Used for delayed
     * triggers whose effect targets, e.g. "Whenever you cast a noncreature spell this turn,
     * **target** creature you control gains double strike" (Rediscover the Way). The chosen
     * target is exposed to [effect] as [com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget].
     * Null for non-targeting delayed triggers (the common case).
     */
    val targetRequirement: TargetRequirement? = null,
    /**
     * For step-based delayed triggers: restrict firing to a specific player's matching step,
     * rather than every player's. Resolved to a concrete player entity id at scheduling time
     * by [com.wingedsheep.engine.handlers.effects.composite.CreateDelayedTriggerExecutor] and
     * also exposed as `triggeringPlayerId` / `triggeringEntityId` to [effect] when it fires,
     * so `EffectTarget.PlayerRef(Player.TriggeringPlayer)` inside [effect] resolves back to
     * the same player. Defaults to `null` (no player gate — fires on the next matching step of
     * any turn).
     *
     * This is the single "whose turn" gate. Common shapes:
     *  - `PlayerRef(Player.You)` — only on the controller's turn ("at the beginning of *your*
     *    next end step"; Dragonhawk, Kav Landseeker, Meandering Towershell).
     *  - `PlayerRef(Player.TriggeringPlayer)` — on the triggering/damaged player's turn ("at
     *    the beginning of *their* next draw step"; Nafs Asp).
     */
    val fireOnPlayer: EffectTarget? = null
) : Effect {
    override val description: String = when {
        trigger != null -> "create a delayed trigger that fires on ${trigger.event::class.simpleName}"
        step != null -> "create a delayed trigger at the beginning of the next ${step.displayName}"
        else -> "create a delayed trigger"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Expiry rule for an event-based delayed triggered ability.
 */
@Serializable
sealed interface DelayedTriggerExpiry {
    /** Remove the delayed trigger at the end of the current turn. */
    @SerialName("DelayedTriggerExpiry.EndOfTurn")
    @Serializable
    data object EndOfTurn : DelayedTriggerExpiry
}

/**
 * "You may pay [cost]. If you do, [effect]."
 *
 * Optional mana payment offered to the controller. Backwards-compatible facade preserved for the
 * cards that authored against the former `MayPayManaEffect` data class. It now lowers to a
 * [GatedEffect] with a [Gate.MayPay] over a [PayManaCostEffect], so there is no bespoke MayPayMana
 * executor or continuation type — the gated frame owns the resolution order. Card source is
 * unchanged; only the compiled/serialized representation moved to `Gated`.
 *
 * The engine recognizes this exact shape — a flat mana [Gate.MayPay] with no `otherwise` and the
 * default decision-maker — to keep the optional-mana-payment UX the wrapper used to own: manual
 * mana-source selection at resolution, and, for a triggered ability that *also* requires a target
 * (the Onslaught "Words of ..." cycle, Lightning Rift), the deliberate pay-then-choose-target
 * order. Composite / life-gated / `otherwise`-bearing MayPay gates intentionally fall through to
 * the generic gated yes/no instead.
 *
 * Example: Lightning Rift — "you may pay {1}. If you do, Lightning Rift deals 2 damage to any target."
 */
@Suppress("FunctionName")
fun MayPayManaEffect(cost: ManaCost, effect: Effect): GatedEffect =
    GatedEffect(gate = Gate.MayPay(PayManaCostEffect(cost)), then = effect)

/**
 * "You may pay {X}. If you do, [effect]."
 *
 * Presents the player with a number chooser (0 to max affordable mana). If X > 0, pays X mana
 * (auto-tapping lands) and executes the inner effect with the chosen X value set in the effect
 * context (read via `DynamicAmount.XValue`).
 *
 * Backwards-compatible facade preserved for the cards that authored against the former
 * `MayPayXForEffect` data class. It now lowers to a [GatedEffect] with a [Gate.MayPayX] gate — one
 * frame, one executor — so there is no bespoke MayPayX executor. Card source is unchanged; only the
 * compiled/serialized representation moved to `Gated`.
 *
 * Example: Decree of Justice cycling trigger — "you may pay {X}. If you do, create X 1/1 white
 * Soldier creature tokens."
 *
 * @param effect The effect that happens if the player pays (uses `DynamicAmount.XValue`).
 */
@Suppress("FunctionName")
fun MayPayXForEffect(effect: Effect): GatedEffect =
    GatedEffect(gate = Gate.MayPayX, then = effect)

/**
 * "Any player may [cost]." with branching outcomes based on whether anyone paid.
 *
 * Each player in APNAP order gets the chance to pay the cost. As soon as
 * any player does, [consequence] is executed and no further players are asked.
 * If no player pays, [consequenceIfNonePaid] is executed instead.
 *
 * This single primitive covers both directions of the template:
 *  - "any player may [cost]; if a player does, [X]" → set [consequence] (Prowling Pangolin).
 *  - "[X] unless any player pays [cost]" → set [consequenceIfNonePaid] (Aether Rift). Use the
 *    [com.wingedsheep.sdk.dsl.Effects.UnlessAnyPlayerPays] facade for this reading.
 *
 * Either consequence may be null (no effect for that branch). The effect runs inside a
 * pipeline transparently: the surrounding pipeline's stored collections / chosen values are
 * carried into whichever consequence fires, so a consequence can reference a collection built
 * earlier in the same resolution (e.g. "return the discarded card this way").
 *
 * Supported costs: [PayCost.Sacrifice] (card selection) and [PayCost.PayLife] (yes/no).
 *
 * @property cost The cost any player may choose to pay
 * @property consequence The effect that happens if a player pays (null = nothing)
 * @property consequenceIfNonePaid The effect that happens if no player pays (null = nothing)
 */
@SerialName("AnyPlayerMayPay")
@Serializable
data class AnyPlayerMayPayEffect(
    val cost: PayCost,
    val consequence: Effect? = null,
    val consequenceIfNonePaid: Effect? = null
) : Effect {
    override val description: String = buildString {
        when {
            consequenceIfNonePaid != null && consequence == null ->
                append("${consequenceIfNonePaid.description} unless any player ${cost.description}")
            else -> {
                append("Any player may ${cost.description}.")
                if (consequence != null) append(" If a player does, ${consequence.description}.")
                if (consequenceIfNonePaid != null) append(" If no player does, ${consequenceIfNonePaid.description}.")
            }
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newCost = cost.applyTextReplacement(replacer)
        val newConsequence = consequence?.applyTextReplacement(replacer)
        val newConsequenceIfNonePaid = consequenceIfNonePaid?.applyTextReplacement(replacer)
        return if (newCost !== cost || newConsequence !== consequence || newConsequenceIfNonePaid !== consequenceIfNonePaid)
            copy(cost = newCost, consequence = newConsequence, consequenceIfNonePaid = newConsequenceIfNonePaid) else this
    }
}

// ForEachTargetEffect / ForEachPlayerEffect / ForEachInCollectionEffect (and the group
// siblings) live in ForEachEffects.kt — lowering facades over the unified ForEachEffect.

/**
 * Flip a coin. Execute one effect if you win, another if you lose.
 * "Flip a coin. If you win the flip, [wonEffect]. If you lose the flip, [lostEffect]."
 *
 * Used for cards like Skittish Valesk, Goblin Bomb, Mana Clash, etc.
 *
 * @property wonEffect The effect that happens if the player wins the flip (null = nothing)
 * @property lostEffect The effect that happens if the player loses the flip (null = nothing)
 */
@SerialName("FlipCoin")
@Serializable
data class FlipCoinEffect(
    val wonEffect: Effect? = null,
    val lostEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("Flip a coin.")
        if (wonEffect != null) {
            append(" If you win the flip, ${wonEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
        if (lostEffect != null) {
            append(" If you lose the flip, ${lostEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newWon = wonEffect?.applyTextReplacement(replacer)
        val newLost = lostEffect?.applyTextReplacement(replacer)
        return if (newWon !== wonEffect || newLost !== lostEffect)
            copy(wonEffect = newWon, lostEffect = newLost) else this
    }
}

/**
 * Flip two coins. Execute different effects based on the combined outcome.
 * "Flip two coins. If both are heads, [bothHeadsEffect]. If both are tails, [bothTailsEffect]."
 *
 * Used for cards like Two-Headed Giant where two coins determine the outcome.
 *
 * @property bothHeadsEffect Effect if both coins are heads (null = nothing)
 * @property bothTailsEffect Effect if both coins are tails (null = nothing)
 * @property mixedEffect Effect if one head and one tail (null = nothing, most common)
 */
@SerialName("FlipTwoCoins")
@Serializable
data class FlipTwoCoinsEffect(
    val bothHeadsEffect: Effect? = null,
    val bothTailsEffect: Effect? = null,
    val mixedEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("Flip two coins.")
        if (bothHeadsEffect != null) {
            append(" If both coins come up heads, ${bothHeadsEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
        if (bothTailsEffect != null) {
            append(" If both coins come up tails, ${bothTailsEffect.description.replaceFirstChar { it.lowercase() }}.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newHeads = bothHeadsEffect?.applyTextReplacement(replacer)
        val newTails = bothTailsEffect?.applyTextReplacement(replacer)
        val newMixed = mixedEffect?.applyTextReplacement(replacer)
        return if (newHeads !== bothHeadsEffect || newTails !== bothTailsEffect || newMixed !== mixedEffect)
            copy(bothHeadsEffect = newHeads, bothTailsEffect = newTails, mixedEffect = newMixed) else this
    }
}

// =============================================================================
// Budget Modal (Pawprint / Season Cycle)
// =============================================================================

/**
 * Represents a single mode in a budget modal spell.
 *
 * Each mode has a cost (in "pawprints" or other currency) and an effect.
 * Players can select modes multiple times as long as the total cost doesn't
 * exceed the budget.
 *
 * @property cost The cost of this mode (e.g., 1, 2, or 3 pawprints)
 * @property effect The effect when this mode is chosen
 * @property description Human-readable description of the mode
 */
@Serializable
data class BudgetMode(
    val cost: Int,
    val effect: Effect,
    val description: String = effect.description
) {
    fun applyTextReplacement(replacer: TextReplacer): BudgetMode {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Budget-based modal spell effect — "Choose up to N {P} worth of modes."
 *
 * Used by the Bloomburrow Season cycle (Season of Loss, Season of the Bold, etc.)
 * where each mode has a different cost and the player distributes a budget among them.
 * The same mode can be chosen multiple times. Modes execute in printed order.
 *
 * Example (Season of Loss):
 * ```kotlin
 * BudgetModalEffect(
 *     budget = 5,
 *     modes = listOf(
 *         BudgetMode(1, eachPlayerSacrificesCreature, "Each player sacrifices a creature"),
 *         BudgetMode(2, drawForDeadCreatures, "Draw a card for each creature that died this turn"),
 *         BudgetMode(3, opponentLosesLifeForGraveyard, "Each opponent loses X life")
 *     )
 * )
 * ```
 *
 * @property budget The total budget available (e.g., 5 pawprints)
 * @property modes The modes with their costs and effects
 */
@SerialName("BudgetModal")
@Serializable
data class BudgetModalEffect(
    val budget: Int,
    val modes: List<BudgetMode>
) : Effect {
    override val description: String = buildString {
        append("Choose up to $budget worth of modes:\n")
        modes.forEachIndexed { index, mode ->
            append("• (${mode.cost}) ${mode.description}")
            if (index < modes.lastIndex) append("\n")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var anyChanged = false
        val newModes = modes.map { mode ->
            val newMode = mode.applyTextReplacement(replacer)
            if (newMode !== mode) anyChanged = true
            newMode
        }
        return if (anyChanged) copy(modes = newModes) else this
    }
}

// =============================================================================
// Repeat Conditions
// =============================================================================

/**
 * Determines whether a [RepeatWhileEffect] should repeat after each body execution.
 *
 * Variants:
 * - [PlayerChooses]: Interactive — a player decides yes/no each iteration.
 * - [WhileCondition]: Synchronous — checks a game-state [Condition].
 */
@Serializable
sealed interface RepeatCondition {

    /**
     * A player decides each iteration whether to repeat.
     *
     * @property decider Which player makes the choice (resolved from EffectTarget)
     * @property prompt Text shown in the yes/no decision
     * @property yesText Label for the "repeat" button
     * @property noText Label for the "stop" button
     */
    @SerialName("PlayerChooses")
    @Serializable
    data class PlayerChooses(
        val decider: EffectTarget,
        val prompt: String,
        val yesText: String = "Repeat",
        val noText: String = "Stop"
    ) : RepeatCondition

    /**
     * Repeat while a game-state condition is true (checked after each body execution).
     *
     * @property condition The condition to evaluate
     */
    @SerialName("WhileCondition")
    @Serializable
    data class WhileCondition(
        val condition: Condition
    ) : RepeatCondition
}

/**
 * Repeat a body effect in a do-while loop controlled by a [RepeatCondition].
 *
 * The body executes at least once. After each iteration, the repeat condition
 * is evaluated:
 * - [RepeatCondition.PlayerChooses]: pauses for a yes/no decision
 * - [RepeatCondition.WhileCondition]: evaluates synchronously
 *
 * Example (Trade Secrets):
 * ```kotlin
 * RepeatWhileEffect(
 *     body = CompositeEffect(listOf(
 *         DrawCardsEffect(2, EffectTarget.ContextTarget(0)),
 *         DrawUpToEffect(4, EffectTarget.Controller)
 *     )),
 *     repeatCondition = RepeatCondition.PlayerChooses(
 *         decider = EffectTarget.ContextTarget(0),
 *         prompt = "Repeat the process? (You draw 2 cards, opponent draws up to 4)"
 *     )
 * )
 * ```
 *
 * @property body The effect to execute each iteration
 * @property repeatCondition Determines whether to repeat after each body execution
 */
@SerialName("RepeatWhile")
@Serializable
data class RepeatWhileEffect(
    val body: Effect,
    val repeatCondition: RepeatCondition
) : Effect {
    override val description: String = buildString {
        append(body.description)
        append(". Repeat this process")
        when (repeatCondition) {
            is RepeatCondition.PlayerChooses -> append(" as many times as ${repeatCondition.decider.description} chooses")
            is RepeatCondition.WhileCondition -> append(" while ${repeatCondition.condition.description}")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newBody = body.applyTextReplacement(replacer)
        return if (newBody !== body) copy(body = newBody) else this
    }
}

// =============================================================================
// Repeat Dynamic Times
// =============================================================================

/**
 * Repeat a body effect N times, where N is determined by a [DynamicAmount].
 *
 * The amount is evaluated once when the effect starts executing. Then the body
 * is executed that many times in sequence. If any iteration pauses for a decision,
 * remaining iterations resume via the standard [CompositeEffect] continuation.
 *
 * Example (Rottenmouth Viper - for each blight counter):
 * ```kotlin
 * RepeatDynamicTimesEffect(
 *     amount = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named("blight")),
 *     body = ForEachPlayerEffect(
 *         players = Player.EachOpponent,
 *         effects = listOf(chooseAction)
 *     )
 * )
 * ```
 *
 * @property amount How many times to repeat (evaluated once at execution start)
 * @property body The effect to execute each iteration
 */
@SerialName("RepeatDynamicTimes")
@Serializable
data class RepeatDynamicTimesEffect(
    val amount: DynamicAmount,
    val body: Effect
) : Effect {
    override val description: String = "Repeat ${body.description} a number of times"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newBody = body.applyTextReplacement(replacer)
        return if (newBody !== body) copy(body = newBody) else this
    }
}

// =============================================================================
// Choose Action (Player Chooses Between Effects)
// =============================================================================

/**
 * Check whether a choice is feasible for the choosing player.
 *
 * Used by [ChooseActionEffect] to filter out options the player cannot fulfill.
 * For example, "sacrifice a nonland permanent" is infeasible if the player controls none.
 */
@Serializable
sealed interface FeasibilityCheck {

    /**
     * The player controls at least [count] permanents matching [filter].
     */
    @SerialName("ControlsPermanentMatching")
    @Serializable
    data class ControlsPermanentMatching(
        val filter: GameObjectFilter,
        val count: Int = 1
    ) : FeasibilityCheck

    /**
     * The player has at least [count] cards in [zone] matching [filter].
     */
    @SerialName("HasCardsInZone")
    @Serializable
    data class HasCardsInZone(
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val count: Int = 1
    ) : FeasibilityCheck
}

/**
 * A labeled option in a [ChooseActionEffect].
 *
 * @property label Human-readable label shown in the UI (e.g., "Sacrifice a nonland permanent")
 * @property effect The effect executed when this option is chosen
 * @property feasibilityCheck Optional check — if non-null and the check fails, this option is hidden
 */
@Serializable
data class EffectChoice(
    val label: String,
    val effect: Effect,
    val feasibilityCheck: FeasibilityCheck? = null
) {
    fun applyTextReplacement(replacer: TextReplacer): EffectChoice {
        val newEffect = effect.applyTextReplacement(replacer)
        return if (newEffect !== effect) copy(effect = newEffect) else this
    }
}

/**
 * Present a player with labeled options and execute the chosen effect.
 *
 * This is a reusable "choose one action" pattern for punisher effects,
 * opponent choices, and any card where a player picks between distinct effects.
 *
 * Infeasible options (per [FeasibilityCheck]) are filtered out at execution time.
 * If only one option remains, it is auto-selected. If zero remain, nothing happens.
 *
 * Example (Thornplate Intimidator):
 * ```kotlin
 * ChooseActionEffect(
 *     choices = listOf(
 *         EffectChoice("Sacrifice a nonland permanent", ForceSacrificeEffect(...),
 *             FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.NonlandPermanent)),
 *         EffectChoice("Discard a card", discardPipeline,
 *             FeasibilityCheck.HasCardsInZone(Zone.HAND)),
 *         EffectChoice("Lose 3 life", LoseLifeEffect(3, target))
 *     ),
 *     player = EffectTarget.ContextTarget(0)  // opponent chooses
 * )
 * ```
 *
 * @property choices The labeled options to present
 * @property player Who makes the choice (defaults to controller)
 */
@SerialName("ChooseAction")
@Serializable
data class ChooseActionEffect(
    val choices: List<EffectChoice>,
    val player: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = buildString {
        append("Choose one —\n")
        choices.forEachIndexed { index, choice ->
            append("• ")
            append(choice.label)
            if (index < choices.lastIndex) append("\n")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var anyChanged = false
        val newChoices = choices.map { choice ->
            val newChoice = choice.applyTextReplacement(replacer)
            if (newChoice !== choice) anyChanged = true
            newChoice
        }
        return if (anyChanged) copy(choices = newChoices) else this
    }
}
