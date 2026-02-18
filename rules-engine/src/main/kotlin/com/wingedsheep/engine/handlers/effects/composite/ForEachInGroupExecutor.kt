package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for ForEachInGroupEffect.
 *
 * Iterates over all entities matching a group filter and executes the inner effect
 * for each one. Within the inner effect, [com.wingedsheep.sdk.scripting.EffectTarget.Self]
 * resolves to the current iteration entity via [EffectContext.iterationTarget].
 *
 * The group is snapshotted before any effects apply (simultaneous semantics),
 * so entities destroyed during iteration don't affect the list.
 */
class ForEachInGroupExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ForEachInGroupEffect> {

    override val effectType: KClass<ForEachInGroupEffect> = ForEachInGroupEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ForEachInGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        // 1. Snapshot: resolve filter against current game state
        val matchedEntities = resolveGroup(state, effect, context)

        if (matchedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        // 2. If noRegenerate, mark all matched entities before any effects apply
        if (effect.noRegenerate) {
            for (entityId in matchedEntities) {
                currentState = addCantBeRegenerated(currentState, entityId, context)
            }
        }

        // 3. Execute inner effect for each matched entity
        for (entityId in matchedEntities) {
            val innerContext = context.copy(iterationTarget = entityId)
            val result = effectExecutor(currentState, effect.effect, innerContext)

            if (result.isPaused) {
                // Inner effect needs a decision - propagate pause
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = result.newState
            allEvents.addAll(result.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Resolve the group filter to a list of entity IDs on the battlefield.
     */
    private fun resolveGroup(
        state: GameState,
        effect: ForEachInGroupEffect,
        context: EffectContext
    ): List<EntityId> {
        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)
        val result = mutableListOf<EntityId>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down permanents are always creatures (Rule 707.2)
            val isCreature = cardComponent.typeLine.isCreature || container.has<FaceDownComponent>()

            // For creature-only filters, skip non-creatures
            // The predicate evaluator handles this via the baseFilter
            if (filter.excludeSelf && entityId == context.sourceId) continue

            if (!predicateEvaluator.matches(state, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            result.add(entityId)
        }

        return result
    }

    /**
     * Add a CantBeRegenerated floating effect for the given entity.
     */
    private fun addCantBeRegenerated(
        state: GameState,
        entityId: EntityId,
        context: EffectContext
    ): GameState {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.CantBeRegenerated,
                affectedEntities = setOf(entityId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )
        return state.copy(floatingEffects = state.floatingEffects + floatingEffect)
    }
}
