package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import java.util.UUID

/**
 * Handles core effect and trigger resumption:
 * - EffectContinuation (composite effect pipelines)
 * - TriggeredAbilityContinuation (target selection for triggered abilities)
 * - ResolveSpellContinuation (no-op marker)
 * - MayAbilityContinuation (yes/no for may effects)
 * - MayTriggerContinuation (yes/no for may triggers with targets)
 */
class EffectAndTriggerContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(EffectContinuation::class, ::resumeEffect),
        resumer(TriggeredAbilityContinuation::class, ::resumeTriggeredAbility),
        resumer(TriggerDamageDistributionContinuation::class, ::resumeTriggerDamageDistribution),
        resumer(ResolveSpellContinuation::class) { state, _, _, _ ->
            ExecutionResult.success(state)
        },
        resumer(MayAbilityContinuation::class, ::resumeMayAbility),
        resumer(MayTriggerContinuation::class, ::resumeMayTrigger),
        resumer(ReflexiveTriggerResolveContinuation::class, ::resumeReflexiveTriggerResolve)
    )

    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val effectResult = effectRunner.executeRemainingEffects(state, continuation.remainingEffects, continuation.effectContext)
        if (effectResult.isPaused) return effectResult.toExecutionResult()
        return checkForMore(effectResult.state, effectResult.events.toList())
    }

    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for triggered ability")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        if (selectedTargets.isEmpty()) {
            if (continuation.elseEffect != null) {
                val elseComponent = TriggeredAbilityOnStackComponent(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    controllerId = continuation.controllerId,
                    effect = continuation.elseEffect,
                    description = continuation.description,
                    triggerDamageAmount = continuation.triggerDamageAmount,
                    triggeringEntityId = continuation.triggeringEntityId,
                    triggeringPlayerId = continuation.triggeringPlayerId,
                    triggerCounterCount = continuation.triggerCounterCount
                )
                val stackResult = services.stackResolver.putTriggeredAbility(state, elseComponent, emptyList())
                if (!stackResult.isSuccess) return stackResult
                return checkForMore(stackResult.newState, stackResult.events.toList())
            }
            return checkForMore(state, emptyList())
        }

        // Check if this is a DividedDamageEffect with multiple targets — need distribution
        val effect = continuation.effect
        if (effect is DividedDamageEffect && selectedTargets.size > 1) {
            return createTriggerDamageDistributionDecision(
                state, continuation, selectedTargets, effect.totalDamage, checkForMore
            )
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId,
            triggerCounterCount = continuation.triggerCounterCount
        )

        val stackResult = services.stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    /**
     * After targets are selected for a triggered ability with DividedDamageEffect,
     * pause to ask how to distribute damage among the chosen targets.
     */
    private fun createTriggerDamageDistributionDecision(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        selectedTargets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget>,
        totalDamage: Int,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val sourceName = continuation.sourceId.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: continuation.sourceName

        val targetEntityIds = selectedTargets.map { target ->
            when (target) {
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Player -> target.playerId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent -> target.entityId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Card -> target.cardId
                is com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell -> target.spellEntityId
            }
        }
        val decisionId = UUID.randomUUID().toString()
        val decision = DistributeDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Divide $totalDamage damage among ${selectedTargets.size} targets",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            totalAmount = totalDamage,
            targets = targetEntityIds,
            minPerTarget = 1
        )

        val distributionContinuation = TriggerDamageDistributionContinuation(
            decisionId = decisionId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId,
            triggerCounterCount = continuation.triggerCounterCount,
            selectedTargets = selectedTargets,
            targetRequirements = continuation.targetRequirements,
            totalDamage = totalDamage
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(distributionContinuation)

        val events = listOf(
            DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = continuation.controllerId,
                decisionType = "DISTRIBUTE",
                prompt = decision.prompt
            )
        )

        return ExecutionResult.paused(newState, decision, events)
    }

    /**
     * Resume after player distributes damage for a triggered ability's DividedDamageEffect.
     * Put the ability on the stack with the distribution locked in.
     */
    private fun resumeTriggerDamageDistribution(
        state: GameState,
        continuation: TriggerDamageDistributionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for triggered ability damage")
        }

        val abilityComponent = TriggeredAbilityOnStackComponent(
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            effect = continuation.effect,
            description = continuation.description,
            triggerDamageAmount = continuation.triggerDamageAmount,
            triggeringEntityId = continuation.triggeringEntityId,
            triggeringPlayerId = continuation.triggeringPlayerId,
            triggerCounterCount = continuation.triggerCounterCount,
            damageDistribution = response.distribution
        )

        val stackResult = services.stackResolver.putTriggeredAbility(
            state, abilityComponent, continuation.selectedTargets, continuation.targetRequirements
        )

        if (!stackResult.isSuccess) {
            return stackResult
        }

        return checkForMore(stackResult.newState, stackResult.events.toList())
    }

    private fun resumeMayTrigger(
        state: GameState,
        continuation: MayTriggerContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may trigger")
        }

        if (!response.choice) {
            return checkForMore(state, emptyList())
        }

        val trigger = continuation.trigger
        val mayEffect = trigger.ability.effect as MayEffect
        val innerEffect = mayEffect.effect

        val unwrappedAbility = trigger.ability.copy(effect = innerEffect)
        val unwrappedTrigger = trigger.copy(ability = unwrappedAbility)

        val result = services.triggerProcessor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

        if (result.isPaused) {
            return result
        }

        if (!result.isSuccess) {
            return result
        }

        return checkForMore(result.newState, result.events.toList())
    }

    private fun resumeMayAbility(
        state: GameState,
        continuation: MayAbilityContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may ability")
        }

        val context = continuation.effectContext
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            return checkForMore(state, emptyList())
        }

        val result = services.effectExecutorRegistry.execute(state, effectToExecute, context).toExecutionResult()

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }

    private fun resumeReflexiveTriggerResolve(
        state: GameState,
        continuation: ReflexiveTriggerResolveContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected target selection response for reflexive trigger")
        }

        val selectedTargets = response.selectedTargets.flatMap { (_, targetIds) ->
            targetIds.map { entityId -> entityIdToChosenTarget(state, entityId) }
        }

        if (selectedTargets.isEmpty()) {
            // Player declined targets (optional) or no valid targets selected
            return checkForMore(state, emptyList())
        }

        // Execute the reflexive effect with the chosen targets
        val context = continuation.effectContext.copy(
            targets = selectedTargets,
            pipeline = continuation.effectContext.pipeline.copy(
                namedTargets = com.wingedsheep.engine.handlers.EffectContext.buildNamedTargets(
                    continuation.reflexiveTargetRequirements, selectedTargets
                )
            )
        )

        val result = services.effectExecutorRegistry.execute(state, continuation.reflexiveEffect, context).toExecutionResult()

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }
}
