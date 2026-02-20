package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventDamageAndChainCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PreventDamageAndChainCopyEffect.
 *
 * Prevents all damage target creature would deal this turn by creating a floating effect,
 * then offers the creature's controller the option to sacrifice a land to copy the spell
 * and choose a new target.
 */
class PreventDamageAndChainCopyExecutor(
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<PreventDamageAndChainCopyEffect> {

    override val effectType: KClass<PreventDamageAndChainCopyEffect> = PreventDamageAndChainCopyEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: PreventDamageAndChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        // Capture the controller BEFORE applying the effect
        val targetControllerId = container.get<ControllerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
            ?: return ExecutionResult.success(state)

        // Create a floating effect that prevents all damage this creature would deal this turn
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,  // Layer doesn't matter for this effect
                modification = SerializableModification.PreventAllDamageDealtBy,
                affectedEntities = setOf(targetId)
            ),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = effect.spellName,
            controllerId = context.controllerId,
            timestamp = state.timestamp
        )

        var newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        // Check if the controller has any lands to sacrifice
        val controllerLands = findControllerLands(newState, targetControllerId)
        if (controllerLands.isEmpty()) {
            // No lands to sacrifice — chain ends
            return ExecutionResult.success(newState)
        }

        // Check if there are valid creature targets for the chain copy
        val requirement = TargetPermanent(filter = effect.targetFilter)
        val legalTargets = targetFinder.findLegalTargets(
            newState, requirement, targetControllerId, context.sourceId
        )

        if (legalTargets.isEmpty()) {
            // No valid targets for a copy — chain ends
            return ExecutionResult.success(newState)
        }

        // Offer the target creature's controller the option to sacrifice a land to copy
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: effect.spellName

        val decision = YesNoDecision(
            id = decisionId,
            playerId = targetControllerId,
            prompt = "Sacrifice a land to copy ${effect.spellName} and choose a new target?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Sacrifice a land",
            noText = "Decline"
        )

        val continuation = PreventDamageChainCopyDecisionContinuation(
            decisionId = decisionId,
            targetControllerId = targetControllerId,
            targetFilter = effect.targetFilter,
            spellName = effect.spellName,
            sourceId = context.sourceId
        )

        newState = newState.withPendingDecision(decision)
        newState = newState.pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = targetControllerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun findControllerLands(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = stateProjector.project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(controllerId)
        val context = PredicateContext(controllerId = controllerId)
        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matches(state, permanentId, GameObjectFilter.Land, context)
        }
    }
}
