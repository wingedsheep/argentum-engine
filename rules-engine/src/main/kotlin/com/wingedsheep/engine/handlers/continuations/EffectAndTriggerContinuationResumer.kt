package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Handles core effect and trigger resumption:
 * - EffectContinuation (composite effect pipelines)
 * - TriggeredAbilityContinuation (target selection for triggered abilities)
 * - ResolveSpellContinuation (no-op marker)
 * - MayAbilityContinuation (yes/no for may effects)
 * - MayTriggerContinuation (yes/no for may triggers with targets)
 */
class EffectAndTriggerContinuationResumer(
    private val ctx: ContinuationContext,
    private val effectRunner: EffectContinuationRunner
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(EffectContinuation::class, ::resumeEffect),
        resumer(TriggeredAbilityContinuation::class, ::resumeTriggeredAbility),
        resumer(ResolveSpellContinuation::class) { state, _, _, _ ->
            ExecutionResult.success(state)
        },
        resumer(MayAbilityContinuation::class, ::resumeMayAbility),
        resumer(MayTriggerContinuation::class, ::resumeMayTrigger)
    )

    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val result = effectRunner.executeRemainingEffects(state, continuation.remainingEffects, continuation.effectContext)
        if (result.isPaused) return result
        return checkForMore(result.state, result.events.toList())
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
                val stackResult = ctx.stackResolver.putTriggeredAbility(state, elseComponent, emptyList())
                if (!stackResult.isSuccess) return stackResult
                return checkForMore(stackResult.newState, stackResult.events.toList())
            }
            return checkForMore(state, emptyList())
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

        val stackResult = ctx.stackResolver.putTriggeredAbility(
            state, abilityComponent, selectedTargets, continuation.targetRequirements
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

        val processor = ctx.triggerProcessor
            ?: return ExecutionResult.error(state, "TriggerProcessor not available for may trigger continuation")

        val result = processor.processTargetedTrigger(state, unwrappedTrigger, continuation.targetRequirement)

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

        val result = ctx.effectExecutorRegistry.execute(state, effectToExecute, context)

        if (result.isPaused) {
            return result
        }

        return checkForMore(result.state, result.events.toList())
    }
}
