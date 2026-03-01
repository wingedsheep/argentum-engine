package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Executor for StormCopyEffect.
 * Creates copies of a spell on the stack for the Storm mechanic.
 *
 * If the spell has no targets, all copies are created immediately.
 * If the spell has targets, pauses for target selection for the first copy,
 * then uses StormCopyTargetContinuation for remaining copies.
 */
class StormCopyEffectExecutor(
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<StormCopyEffect> {

    override val effectType: KClass<StormCopyEffect> = StormCopyEffect::class

    override fun execute(
        state: GameState,
        effect: StormCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        if (effect.copyCount <= 0) {
            return ExecutionResult.success(state)
        }

        val stackResolver = StackResolver()

        // If spell has no targets, create all copies immediately
        if (effect.spellTargetRequirements.isEmpty()) {
            return createAllCopiesNoTargets(state, effect, context, stackResolver)
        }

        // Spell has targets — need to ask for target selection for each copy
        return promptForNextCopyTarget(state, effect, context, effect.copyCount)
    }

    private fun createAllCopiesNoTargets(
        state: GameState,
        effect: StormCopyEffect,
        context: EffectContext,
        stackResolver: StackResolver
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for (i in 1..effect.copyCount) {
            val copyAbility = TriggeredAbilityOnStackComponent(
                sourceId = context.sourceId ?: EntityId.generate(),
                sourceName = effect.spellName,
                controllerId = context.controllerId,
                effect = effect.spellEffect,
                description = "Storm copy of ${effect.spellName} ($i/${effect.copyCount})"
            )
            val result = stackResolver.putTriggeredAbility(currentState, copyAbility)
            if (!result.isSuccess) return result
            currentState = result.newState
            allEvents.addAll(result.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    private fun promptForNextCopyTarget(
        state: GameState,
        effect: StormCopyEffect,
        context: EffectContext,
        remainingCopies: Int
    ): ExecutionResult {
        val decisionId = "storm-copy-target-${System.nanoTime()}"

        // Find legal targets for the copy
        val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
        for ((index, requirement) in effect.spellTargetRequirements.withIndex()) {
            val legalTargets = targetFinder.findLegalTargets(
                state, requirement, context.controllerId, context.sourceId
            )
            legalTargetsMap[index] = legalTargets
        }

        // If no legal targets for any requirement, skip this copy and remaining copies
        val hasNoLegalTargets = legalTargetsMap.any { (_, targets) -> targets.isEmpty() }
        if (hasNoLegalTargets) {
            return ExecutionResult.success(state)
        }

        // Push continuation for resuming after target selection
        val continuation = StormCopyTargetContinuation(
            decisionId = decisionId,
            remainingCopies = remainingCopies,
            spellEffect = effect.spellEffect,
            spellTargetRequirements = effect.spellTargetRequirements,
            spellName = effect.spellName,
            controllerId = context.controllerId,
            sourceId = context.sourceId ?: EntityId.generate()
        )
        val targetReqInfos = effect.spellTargetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(
                index = index,
                description = req.description
            )
        }

        val copyNumber = effect.copyCount - remainingCopies + 1
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose targets for Storm copy $copyNumber of ${effect.spellName}",
            context = DecisionContext(
                phase = DecisionPhase.CASTING,
                sourceName = effect.spellName
            ),
            targetRequirements = targetReqInfos,
            legalTargets = legalTargetsMap
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(stateWithContinuation, decision)
    }
}
