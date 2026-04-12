package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ReflexiveTriggerEffect.
 * Handles "You may [action]. When you do, [reflexiveEffect]." abilities.
 *
 * When optional=true:
 *   Present yes/no. If yes, execute CompositeEffect(action, reflexiveEffect).
 *   Uses MayAbilityContinuation to delegate to the existing composite flow.
 *
 * When optional=false:
 *   Execute action, then reflexiveEffect sequentially using the same
 *   pre-push EffectContinuation pattern as CompositeEffectExecutor.
 *
 * When reflexiveTargetRequirements is non-empty:
 *   Targets for the reflexive effect are selected AFTER the action completes,
 *   not when the trigger goes on the stack. This is used for cards like
 *   Wick's Patrol where the target depends on what the action did (mill).
 *
 * @param effectExecutor Function to execute sub-effects (provided by registry)
 * @param targetFinder Finder for legal targets (needed for deferred targeting)
 * @param decisionHandler Handler for creating target decisions
 */
class ReflexiveTriggerEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val targetFinder: TargetFinder,
    private val decisionHandler: DecisionHandler,
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ReflexiveTriggerEffect> {

    override val effectType: KClass<ReflexiveTriggerEffect> = ReflexiveTriggerEffect::class

    override fun execute(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        if (effect.optional) {
            return presentOptionalChoice(state, effect, context)
        }
        if (effect.reflexiveTargetRequirements.isNotEmpty()) {
            return executeActionThenTarget(state, effect, context)
        }
        // No deferred targets: delegate to composite effect executor pattern
        return executeAsComposite(state, effect, context)
    }

    private fun presentOptionalChoice(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        // If the action is a ChooseActionEffect with no feasible choices, skip the decision
        if (effect.action is ChooseActionEffect) {
            val chooseEffect = effect.action as ChooseActionEffect
            val anyFeasible = chooseEffect.choices.any { choice ->
                checkFeasibility(state, context.controllerId, choice.feasibilityCheck)
            }
            if (!anyFeasible) return EffectResult.success(state)
        }

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

        // If yes and there are reflexive targets: execute just the action, then target the reflexive effect.
        // If yes and no reflexive targets: execute action + reflexive effect as a composite.
        val effectIfYes = if (effect.reflexiveTargetRequirements.isNotEmpty()) {
            // Make non-optional so it goes through executeActionThenTarget when resumed
            effect.copy(optional = false)
        } else {
            CompositeEffect(listOf(effect.action, effect.reflexiveEffect))
        }

        val continuation = MayAbilityContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            effectIfYes = effectIfYes,
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
     * Execute action, then pause for reflexive target selection.
     *
     * Uses the pre-push pattern: push ReflexiveTriggerTargetContinuation before
     * executing the action. If the action pauses, the continuation sits underneath
     * and is auto-resumed after the action's decision resolves.
     */
    private fun executeActionThenTarget(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        // Pre-push continuation for reflexive targeting
        val continuation = ReflexiveTriggerTargetContinuation(
            decisionId = "pending",
            reflexiveEffect = effect.reflexiveEffect,
            reflexiveTargetRequirements = effect.reflexiveTargetRequirements,
            effectContext = context
        )
        val stateWithCont = state.pushContinuation(continuation)

        // Execute the action
        val result = effectExecutor(stateWithCont, effect.action, context)

        if (result.isPaused) {
            // Action paused for a decision — our continuation sits underneath
            return result
        }

        if (!result.isSuccess) {
            // Action failed — pop our continuation, skip reflexive effect
            val (_, stateWithoutCont) = result.state.popContinuation()
            return EffectResult.success(stateWithoutCont, result.events.toList())
        }

        // Action succeeded — pop our continuation, present reflexive targets
        val (_, stateAfterPop) = result.state.popContinuation()
        return presentReflexiveTargets(stateAfterPop, effect.reflexiveEffect, effect.reflexiveTargetRequirements, context, result.events.toList())
    }

    /**
     * Find legal targets for the reflexive effect and present target selection to the player.
     * This is called both inline (when action succeeds synchronously) and from the auto-resumer.
     */
    internal fun presentReflexiveTargets(
        state: GameState,
        reflexiveEffect: Effect,
        targetRequirements: List<TargetRequirement>,
        context: EffectContext,
        priorEvents: List<GameEvent>
    ): EffectResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "ability"

        // Find legal targets for each requirement
        val allLegalTargets = mutableMapOf<Int, List<com.wingedsheep.sdk.model.EntityId>>()
        for ((index, req) in targetRequirements.withIndex()) {
            val legalTargets = targetFinder.findLegalTargets(
                state = state,
                requirement = req,
                controllerId = controllerId,
                sourceId = sourceId
            )
            allLegalTargets[index] = legalTargets
        }

        // If no legal targets exist for any required requirement, skip the reflexive effect
        for ((index, req) in targetRequirements.withIndex()) {
            val legalTargets = allLegalTargets[index] ?: emptyList()
            if (legalTargets.isEmpty() && req.effectiveMinCount > 0) {
                return EffectResult.success(
                    state,
                    priorEvents + AbilityFizzledEvent(
                        sourceId ?: com.wingedsheep.sdk.model.EntityId("unknown"),
                        reflexiveEffect.description,
                        "No legal targets available for reflexive trigger"
                    )
                )
            }
        }

        // Auto-select player targets when there's exactly one legal target
        if (targetRequirements.size == 1) {
            val req = targetRequirements[0]
            val isPlayerTarget = req is TargetPlayer || req is TargetOpponent
            val legalTargets = allLegalTargets[0] ?: emptyList()
            if (isPlayerTarget && legalTargets.size == 1 && req.effectiveMinCount == 1 && req.count == 1) {
                val autoSelectedTarget = legalTargets.first()
                val chosenTarget = com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget(state, autoSelectedTarget)
                val contextWithTargets = context.copy(
                    targets = listOf(chosenTarget),
                    pipeline = context.pipeline.copy(
                        namedTargets = EffectContext.buildNamedTargets(targetRequirements, listOf(chosenTarget))
                    )
                )
                val reflexiveResult = effectExecutor(state, reflexiveEffect, contextWithTargets)
                return if (reflexiveResult.isPaused) {
                    EffectResult.paused(reflexiveResult.state, reflexiveResult.pendingDecision!!, priorEvents + reflexiveResult.events)
                } else {
                    EffectResult.success(reflexiveResult.state, priorEvents + reflexiveResult.events)
                }
            }
        }

        // Create target requirement infos for the decision
        val requirementInfos = targetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(
                index = index,
                description = req.description,
                minTargets = req.effectiveMinCount,
                maxTargets = req.count
            )
        }

        // Resolve dynamic amounts so the player sees concrete values (e.g., "-6/-6 until end of turn")
        val effectHint = try {
            val resolver: (com.wingedsheep.sdk.scripting.values.DynamicAmount) -> Int = { amount ->
                amountEvaluator.evaluate(state, amount, context)
            }
            val resolved = reflexiveEffect.runtimeDescription(resolver)
            if (resolved != reflexiveEffect.description) resolved else null
        } catch (_: Exception) { null }

        // Create the target selection decision
        val decisionResult = decisionHandler.createTargetDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId ?: com.wingedsheep.sdk.model.EntityId("unknown"),
            sourceName = sourceName,
            requirements = requirementInfos,
            legalTargets = allLegalTargets,
            effectHint = effectHint
        )

        if (!decisionResult.isPaused || decisionResult.pendingDecision == null) {
            return EffectResult.error(state, "Failed to create target decision for reflexive trigger")
        }

        // Push continuation to execute reflexive effect after targets are chosen
        val resolveContinuation = ReflexiveTriggerResolveContinuation(
            decisionId = decisionResult.pendingDecision.id,
            reflexiveEffect = reflexiveEffect,
            reflexiveTargetRequirements = targetRequirements,
            effectContext = context
        )
        val stateWithContinuation = decisionResult.state.pushContinuation(resolveContinuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            priorEvents + decisionResult.events.toList()
        )
    }

    /**
     * Execute action + reflexiveEffect as a composite.
     * Uses pre-push EffectContinuation for the reflexive effect (same pattern as CompositeEffectExecutor).
     */
    private fun executeAsComposite(
        state: GameState,
        effect: ReflexiveTriggerEffect,
        context: EffectContext
    ): EffectResult {
        val compositeEffect = CompositeEffect(listOf(effect.action, effect.reflexiveEffect))
        return effectExecutor(state, compositeEffect, context)
    }
}
