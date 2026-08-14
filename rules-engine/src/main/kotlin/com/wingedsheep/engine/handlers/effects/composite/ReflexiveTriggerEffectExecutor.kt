package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ReflexiveTriggerEffect.
 * Handles "You may [action]. When you do, [reflexiveEffect]." abilities.
 *
 * CR 603.12: "When you do" is a genuinely separate reflexive triggered ability — a real second
 * stack object, with its own target chosen as it's placed on the stack and its own priority round
 * before it resolves. This executor only ever runs the *action* half; once the action succeeds, it
 * emits a [ReflexiveAbilityTriggeredEvent] instead of resolving [ReflexiveTriggerEffect.reflexiveEffect]
 * inline. [com.wingedsheep.engine.event.TriggerDetector]'s `detectReflexiveTriggers` turns that
 * event into a real [com.wingedsheep.engine.event.PendingTrigger], which flows through the ordinary
 * [com.wingedsheep.engine.event.TriggerProcessor] target-selection/stack-placement pipeline used by
 * every other triggered ability — giving opponents a genuine response window and CR 608.2b
 * illegal-target fizzle for free, neither of which an inline resolution could offer.
 *
 * When optional=true:
 *   Present yes/no. If yes, re-enter as optional=false.
 * When optional=false:
 *   Run the action (pre-pushing a continuation so a mid-action decision doesn't lose the reflexive
 *   payoff), then emit the triggered event once it completes.
 *
 * @param effectExecutor Function to execute sub-effects (provided by registry)
 * @param targetFinder Finder for legal targets (needed for the action's own "may sacrifice a..."
 * style feasibility check — the reflexive effect's own targets are found later, generically, by
 * `TriggerProcessor`)
 * @param decisionHandler Handler for creating the "may [action]?" yes/no decision
 */
class ReflexiveTriggerEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val targetFinder: TargetFinder,
    private val decisionHandler: DecisionHandler,
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ReflexiveTriggerEffect> {

    override val effectType: KClass<ReflexiveTriggerEffect> = ReflexiveTriggerEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        // An action that can't be performed never happens, so CR 603.12's "when you do" never
        // triggers. Gating here rather than inside the optional branch covers both templates:
        // the "you may [action]" prompt is meaningless (saying yes would no-op the action while
        // still firing the payoff), and a mandatory [action] would otherwise resolve vacuously —
        // a discard pipeline on an empty hand auto-selects nothing and reports success, which
        // `executeActionThenEmit`'s `result.isSuccess` check can't distinguish from a real discard.
        if (!isActionFeasible(state, effect.action, context)) {
            return EffectResult.success(state)
        }
        if (effect.optional) {
            return presentOptionalChoice(state, effect, context)
        }
        return executeActionThenEmit(state, effect, context)
    }

    private fun presentOptionalChoice(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = effect.description,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Yes",
            noText = "No",
            hint = effect.hint
        )

        val continuation = MayAbilityContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            effectIfYes = effect.copy(optional = false),
            effectIfNo = null,
            effectContext = context
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Check whether the action half of a "[action]. When you do, [reflexive]" trigger can actually
     * be performed. When false the whole effect is skipped: for the optional template presenting a
     * yes/no is meaningless (saying yes would silently no-op the action while still firing the
     * reflexive payoff), and for the mandatory one the action would resolve vacuously to the same
     * end.
     *
     * Walks the action effect tree looking for gating sub-effects:
     *  - [SelectTargetEffect] with no legal targets → infeasible
     *  - [SacrificeEffect] with fewer controlled matches than its count → infeasible
     *    (e.g. Shire Shirriff's "you may sacrifice a token" when you control no token)
     *  - [ChooseActionEffect] with no feasible choice → infeasible
     *  - [SelectFromCollectionEffect] carrying a minimum, drawing from a collection a preceding
     *    [GatherCardsEffect] will leave empty → infeasible. This is the Gather → Select → Move
     *    discard pipeline ([com.wingedsheep.sdk.dsl.Effects.Discard]): "you may discard a card"
     *    with an empty hand is impossible, so Inti, Seneschal of the Sun must not hand out its
     *    +1/+1 counter for a discard that never happens.
     *  - [CompositeEffect] → feasible iff every step is feasible, walked in order so the gathered
     *    collection sizes are known by the time a select step is scored (top-level sequencing)
     *  - any other effect → assumed feasible (don't gate on shapes we don't recognize)
     *
     * @param gathered Sizes of the pipeline collections produced by preceding [GatherCardsEffect]
     * steps of the enclosing composite. A collection that isn't in the map is unknown, and any
     * selection from it fails open; `null` means the bookkeeping has stopped altogether, so every
     * selection fails open (see [isCompositeFeasible]).
     */
    private fun isActionFeasible(
        state: GameState,
        action: Effect,
        context: EffectContext,
        gathered: Map<String, Int>? = emptyMap()
    ): Boolean = when (action) {
        is SelectTargetEffect -> targetFinder.findLegalTargets(
            state = state,
            requirement = action.requirement,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            // Carry granterId so the "may" feasibility check honors a granter-relative exclusion —
            // e.g. Dire Blunderbuss must NOT offer the sacrifice when the only artifact is the
            // granting Equipment itself. Minimal context (granterId only) matches the actual
            // selection path in SelectTargetPipelineExecutor, so feasibility and execution agree.
            pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = context.controllerId,
                granterId = context.granterId
            )
        ).isNotEmpty()
        is SacrificeEffect -> {
            // You can only sacrifice permanents you control that match the filter (mirrors
            // SacrificeExecutor.findValidPermanents). Fewer than `count` → can't pay → infeasible.
            val excludeId = if (action.excludeSource) context.sourceId else null
            BattlefieldFilterUtils.findMatchingOnBattlefield(
                state, action.filter.youControl(), context, excludeSelfId = excludeId
            ).size >= action.count
        }
        is ChooseActionEffect -> action.choices.any { choice ->
            checkFeasibility(state, context.controllerId, choice.feasibilityCheck)
        }
        is SelectFromCollectionEffect -> {
            val available = gathered?.get(action.from)
            val minimum = minimumSelection(state, action.selection, context)
            // "Non-empty", not "at least `minimum`": SelectFromCollectionExecutor clamps a
            // ChooseExactly/Random count down to the collection size, so "discard two cards" with
            // one card in hand discards that one and succeeds. Only an empty collection makes a
            // minimum-carrying selection impossible.
            available == null || minimum == null || minimum == 0 || available > 0
        }
        is CompositeEffect -> isCompositeFeasible(state, action, context, gathered)
        // "You may pay {E}{E}{E}" (Guide of Souls) — an all-or-nothing player-counter payment
        // is only feasible if the payer already has at least that many. Mirrors the SacrificeEffect
        // case: without this, the "may pay" prompt would be offered even at 0 energy, and
        // PayFixedCountersExecutor would then fail every time instead of the option never appearing.
        is com.wingedsheep.sdk.scripting.effects.PayFixedCountersEffect -> {
            val playerId = com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                .resolvePlayerRef(action.player, context, state)
            val current = playerId
                ?.let { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>() }
                ?.getCount(com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType(action.counterType))
                ?: 0
            current >= action.amount
        }
        // "You may remove a counter from ~" (Leatherhead, Swamp Stalker) — with no counters left
        // there is nothing to remove, so the may-clause must be absent. Both removal executors
        // no-op on an empty permanent and report *success*, which would otherwise arm the "when you
        // do" payoff for free: Leatherhead would keep destroying an artifact each combat long after
        // her last counter was spent. A `minTotal` floor raises the bar to the whole floor — an
        // action that can't pay it in full can't be performed at all.
        is com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect ->
            countersOn(state, context, action.target)
                ?.let { it >= action.minTotal.coerceAtLeast(1) } ?: true
        is com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect ->
            countersOn(state, context, action.target, kind = action.counterType)
                ?.let { it >= action.count } ?: true
        // "You may collect evidence 3" (Sample Collector) — CR 701.59b is explicit that a player
        // unable to exile cards totalling N *can't choose to collect evidence*, so the option must
        // be absent rather than offered and refused. Without this branch the `else -> true` below
        // would fail open and prompt a player whose graveyard can't pay.
        is com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect -> {
            val playerId = com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                .resolvePlayerRef(action.player, context, state)
            playerId != null && com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
                .canCollect(state, playerId, action.amount)
        }
        else -> true
    }

    /**
     * How many counters the permanent [target] resolves to currently carries — of every kind, or
     * of [kind] alone when one is named. Zero for an entity that is gone or tracks no counters,
     * which is the fail-*closed* answer the callers want: nothing to remove means no may-clause.
     *
     * `null` means the target shape couldn't be resolved at all, which is a different question and
     * gets the opposite answer. Feasibility runs before the action does, so a `PipelineTarget`
     * filled in by an earlier step of the same composite isn't stored yet, and `state` is consulted
     * for the relational shapes ([EffectTarget.EnchantedCreature], `EquippedCreature`,
     * `ChosenCreature`, …) that the stateless overload can't reach. Treating "don't know" as zero
     * would silently delete the whole ability, so callers fail open on it — the same policy as this
     * method's `else -> true`.
     */
    private fun countersOn(
        state: GameState,
        context: EffectContext,
        target: com.wingedsheep.sdk.scripting.targets.EffectTarget,
        kind: String? = null
    ): Int? {
        val targetId = context.resolveTarget(target, state) ?: return null
        val counters = state.getEntity(targetId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
            ?: return 0
        return if (kind == null) {
            counters.counters.values.sum()
        } else {
            counters.getCount(
                com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType(kind)
            )
        }
    }

    /**
     * Walk a composite action's steps in order, threading the sizes of the collections its
     * [GatherCardsEffect] steps produce so a later [SelectFromCollectionEffect] is scored against
     * real numbers — the Gather → Select → Move shape the counted discards compile to. (The
     * uncounted `Patterns.Hand.discardHand` is a bare Gather → Move with no selection step, so it
     * has nothing to score and is never gated.)
     *
     * Gather sizes are read off the *pre-action* state, so they're only recorded while every step
     * so far has been a gather or a select. The first step of any other kind stops the bookkeeping
     * by collapsing the map to `null`: from there on every selection fails open rather than being
     * judged against a stale count, so "you may draw a card, then discard a card" is still offered
     * on an empty hand. The stopped state is threaded *into* nested composites too — `Effect.then`
     * only flattens when its receiver is already a composite, so that draw-then-discard shape is
     * `Composite[Draw, Composite[Gather, Select, Move]]`, and a per-call flag would let the inner
     * walk start scoring again against the pre-draw hand.
     */
    private fun isCompositeFeasible(
        state: GameState,
        action: CompositeEffect,
        context: EffectContext,
        gathered: Map<String, Int>?
    ): Boolean {
        var known = gathered
        for (step in action.effects) {
            if (!isActionFeasible(state, step, context, known)) return false
            known = when (step) {
                is GatherCardsEffect -> known?.let { sizes ->
                    gatherableCount(state, step, context)?.let { sizes + (step.storeAs to it) }
                }
                is SelectFromCollectionEffect -> known
                else -> null
            }
        }
        return true
    }

    /**
     * How many cards a selection mode *requires*, or null when it has no minimum
     * ([SelectionMode.ChooseUpTo], [SelectionMode.All], [SelectionMode.ChooseAnyNumber] — each is
     * satisfied by selecting nothing, so an empty collection doesn't make them impossible; cf.
     * Miasma Demon, whose "you may discard any number of cards" (`Patterns.Hand.discardAnyNumber`,
     * a [SelectionMode.ChooseAnyNumber]) is still a legal action on an empty hand — discarding zero
     * performs it, and the reflexive payoff then targets up to zero creatures).
     *
     * Only the [SelectionMode] is scored; [SelectFromCollectionEffect.restrictions] are ignored.
     * That's safe in the fail-open direction as long as no restriction *raises* a minimum —
     * [com.wingedsheep.sdk.scripting.effects.SelectionRestriction.ReducedMinimumIfMatches] only
     * lowers it, and the rest narrow the maximum.
     */
    private fun minimumSelection(
        state: GameState,
        selection: SelectionMode,
        context: EffectContext
    ): Int? = when (selection) {
        is SelectionMode.ChooseExactly -> amountEvaluator.evaluate(state, selection.count, context)
        is SelectionMode.Random -> amountEvaluator.evaluate(state, selection.count, context)
        else -> null
    }

    /**
     * How many cards a [GatherCardsEffect] would collect right now, or null when the source isn't a
     * plain single-player zone read (target-driven and multi-player sources are left unscored, so
     * the enclosing feasibility check fails open).
     */
    private fun gatherableCount(
        state: GameState,
        gather: GatherCardsEffect,
        context: EffectContext
    ): Int? {
        val source = gather.source as? CardSource.FromZone ?: return null
        val playerId = TargetResolutionUtils.resolvePlayerRef(source.player, context, state) ?: return null
        val cards = state.getZone(ZoneKey(playerId, source.zone))
        if (source.filter == GameObjectFilter.Any) return cards.size
        val predicateContext = PredicateContext.fromEffectContext(context)
        return cards.count { cardId ->
            predicateEvaluator.matches(state, state.projectedState, cardId, source.filter, predicateContext)
        }
    }

    /**
     * Execute the action half; once it completes (possibly after its own nested decisions), emit
     * the [ReflexiveAbilityTriggeredEvent] that turns "When you do, ..." into a real CR 603.12
     * reflexive triggered ability, instead of resolving it inline.
     *
     * Uses the pre-push pattern: push [ReflexiveTriggerTargetContinuation] before executing the
     * action. If the action pauses, the continuation sits underneath and is auto-resumed after the
     * action's own decision(s) resolve ([com.wingedsheep.engine.handlers.continuations.CoreAutoResumerModule]).
     */
    private fun executeActionThenEmit(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        val continuation = ReflexiveTriggerTargetContinuation(
            decisionId = "pending",
            reflexiveEffect = effect.reflexiveEffect,
            reflexiveTargetRequirements = effect.reflexiveTargetRequirements,
            effectContext = context,
            descriptionOverride = effect.descriptionOverride
        )
        val stateWithCont = state.pushContinuation(continuation)

        // Execute the action
        val result = effectExecutor(stateWithCont, effect.action, context)

        if (result.isPaused) {
            // Action paused for a decision — our continuation sits underneath
            return result
        }

        // Pop our continuation now that the action has finished (success or failure)
        val (_, stateWithoutCont) = result.state.popContinuation()

        if (!result.isSuccess) {
            // Action failed — skip the reflexive trigger entirely
            return EffectResult.success(stateWithoutCont, result.events.toList())
        }

        // Action succeeded synchronously — merge whatever it stashed in the pipeline (e.g.
        // `EntityReference.AmassedArmy`, Foray of Orcs) into the context before emitting, mirroring
        // CompositeEffectExecutor's sibling-to-sibling propagation.
        val mergedContext = if (
            result.updatedCollections.isNotEmpty() || result.updatedSubtypeGroups.isNotEmpty() ||
            result.updatedStoredNumbers.isNotEmpty() || result.updatedChosenValues.isNotEmpty()
        ) {
            context.copy(
                pipeline = context.pipeline.copy(
                    storedCollections = context.pipeline.storedCollections + result.updatedCollections,
                    storedSubtypeGroups = context.pipeline.storedSubtypeGroups + result.updatedSubtypeGroups,
                    storedNumbers = context.pipeline.storedNumbers + result.updatedStoredNumbers,
                    chosenValues = context.pipeline.chosenValues + result.updatedChosenValues
                )
            )
        } else {
            context
        }

        val event = buildReflexiveTriggeredEvent(
            stateWithoutCont, effect.reflexiveEffect, effect.reflexiveTargetRequirements,
            effect.descriptionOverride, mergedContext
        )
        return EffectResult.success(stateWithoutCont, result.events.toList() + event)
    }

    companion object {
        /**
         * Build the [ReflexiveAbilityTriggeredEvent] for a completed action, carrying the reflexive
         * effect, its target requirements, and whatever pipeline state the action produced. Shared
         * by the synchronous path above and
         * [com.wingedsheep.engine.handlers.continuations.CoreAutoResumerModule]'s
         * [ReflexiveTriggerTargetContinuation] auto-resumer (the action paused for its own decision
         * and has now completed — `continuation.effectContext` already carries whatever the
         * propagation seam ([com.wingedsheep.engine.handlers.continuations.exposeCollectionsToNextFrame])
         * merged in while it sat on the continuation stack).
         */
        fun buildReflexiveTriggeredEvent(
            state: GameState,
            reflexiveEffect: Effect,
            reflexiveTargetRequirements: List<TargetRequirement>,
            descriptionOverride: String?,
            effectContext: EffectContext
        ): ReflexiveAbilityTriggeredEvent {
            val sourceId = effectContext.sourceId ?: EntityId("unknown")
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "ability"
            return ReflexiveAbilityTriggeredEvent(
                sourceId = sourceId,
                sourceName = sourceName,
                controllerId = effectContext.controllerId,
                granterId = effectContext.granterId,
                reflexiveEffect = reflexiveEffect,
                reflexiveTargetRequirements = reflexiveTargetRequirements,
                descriptionOverride = descriptionOverride,
                carriedPipeline = effectContext.pipeline,
                carriedTriggerContext = com.wingedsheep.engine.event.TriggerContext(
                    triggeringEntityId = effectContext.triggeringEntityId,
                    triggeringPlayerId = effectContext.triggeringPlayerId,
                    damageAmount = effectContext.triggerDamageAmount,
                    xValue = effectContext.xValue,
                    counterCount = effectContext.triggerCounterCount,
                    totalCounterCount = effectContext.triggerTotalCounterCount,
                    minusOneMinusOneCounterCount = effectContext.triggerMinusOneMinusOneCounterCount,
                    targetingSourceEntityId = effectContext.targetingSourceEntityId,
                    lastKnownPower = effectContext.triggerLastKnownPower,
                    lastKnownToughness = effectContext.triggerLastKnownToughness,
                    diedBatchTotalPower = effectContext.triggerDiedBatchTotalPower,
                    lastKnownSubtypes = effectContext.triggerLastKnownSubtypes,
                    lastKnownCounters = effectContext.triggerLastKnownCounters,
                    lastKnownDamageDealtByPlayers = effectContext.triggerLastKnownDamageDealtByPlayers,
                    lastKnownBlockingOrBlockedByIds = effectContext.triggerLastKnownBlockingOrBlockedByIds,
                    modesChosenCount = effectContext.triggerModesChosenCount,
                    manaSpentOnTriggeringSpell = effectContext.triggerManaSpentOnTriggeringSpell,
                    colorsSpentOnTriggeringSpell = effectContext.triggerColorsSpentOnTriggeringSpell,
                    manaValueOfTriggeringSpell = effectContext.triggerManaValueOfTriggeringSpell,
                    xValueOfTriggeringSpell = effectContext.triggerXValueOfTriggeringSpell,
                    enchantedCreatureLastKnownPower = effectContext.enchantedCreatureLastKnownPower,
                    scryCount = effectContext.triggerScryCount,
                    discardedCardCount = effectContext.triggerDiscardCount,
                    discoverValue = effectContext.triggerDiscoverValue,
                    excessDamageAmount = effectContext.triggerExcessDamageAmount,
                    recipientToughnessAtDamage = effectContext.triggerRecipientToughness
                )
            )
        }
    }
}
