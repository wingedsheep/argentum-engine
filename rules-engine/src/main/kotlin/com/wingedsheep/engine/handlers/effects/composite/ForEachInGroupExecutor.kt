package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for ForEachInGroupEffect.
 *
 * Iterates over all entities matching a group filter and executes the inner effect
 * for each one. Within the inner effect, [com.wingedsheep.sdk.scripting.targets.EffectTarget.Self]
 * resolves to the current iteration entity via [EffectContext.iterationTarget].
 *
 * The group is snapshotted before any effects apply (simultaneous semantics),
 * so entities destroyed during iteration don't affect the list.
 */
class ForEachInGroupExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ForEachInGroupEffect> {

    override val effectType: KClass<ForEachInGroupEffect> = ForEachInGroupEffect::class

    override fun execute(
        state: GameState,
        effect: ForEachInGroupEffect,
        context: EffectContext
    ): EffectResult {
        // 1. Snapshot: resolve filter against current game state
        val matchedEntities = resolveGroup(state, effect, context)

        if (matchedEntities.isEmpty()) {
            return EffectResult.success(state)
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
            val innerContext = context.copy(pipeline = context.pipeline.copy(iterationTarget = entityId))
            val result = effectExecutor(currentState, effect.effect, innerContext)

            if (result.isPaused) {
                // Inner effect needs a decision - propagate pause
                return EffectResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = result.newState
            allEvents.addAll(result.events)
        }

        return EffectResult.success(currentState, allEvents)
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

        // If the filter references a chosen subtype, resolve it from context
        val chosenSubtype = filter.chosenSubtypeKey?.let { key ->
            context.pipeline.chosenValues[key]
        }
        // If a chosen subtype key is specified but no value was chosen, return empty
        if (filter.chosenSubtypeKey != null && chosenSubtype == null) {
            return emptyList()
        }

        val excludeSelfId = if (filter.excludeSelf) context.sourceId else null
        val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(state, filter.baseFilter, context, excludeSelfId)

        // Additionally filter by chosen subtype if specified
        return if (chosenSubtype != null) {
            val projected = state.projectedState
            matched.filter { projected.hasSubtype(it, chosenSubtype) }
        } else {
            matched
        }
    }

    /**
     * Add a CantBeRegenerated floating effect for the given entity.
     */
    private fun addCantBeRegenerated(
        state: GameState,
        entityId: EntityId,
        context: EffectContext
    ): GameState {
        return state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.CantBeRegenerated,
            affectedEntities = setOf(entityId),
            duration = Duration.EndOfTurn,
            context = context
        )
    }
}
