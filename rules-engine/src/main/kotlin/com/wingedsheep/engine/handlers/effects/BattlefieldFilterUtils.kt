package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Utility functions for finding entities on the battlefield matching filters.
 *
 * Always uses projected state to account for type-changing, color-changing,
 * and control-changing continuous effects.
 */
object BattlefieldFilterUtils {

    /**
     * Find all entities on the battlefield matching the given filter using projected state.
     *
     * This is the canonical way to filter battlefield permanents — it ensures projected state
     * is always used (accounting for type-changing, color-changing, and control-changing effects).
     *
     * @param excludeSelfId If non-null, excludes this entity from results (for GroupFilter.excludeSelf).
     */
    fun findMatchingOnBattlefield(
        state: GameState,
        filter: GameObjectFilter,
        context: PredicateContext,
        excludeSelfId: EntityId? = null,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val projected = state.projectedState
        return state.getBattlefield().filter { entityId ->
            if (excludeSelfId != null && entityId == excludeSelfId) return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, context)
        }
    }

    /**
     * Convenience overload that builds a [PredicateContext] from an [EffectContext].
     */
    fun findMatchingOnBattlefield(
        state: GameState,
        filter: GameObjectFilter,
        context: EffectContext,
        excludeSelfId: EntityId? = null,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        return findMatchingOnBattlefield(state, filter, PredicateContext.fromEffectContext(context), excludeSelfId, predicateEvaluator = predicateEvaluator)
    }

    /**
     * Resolve a group filter to the battlefield entities it matches, honoring a
     * chosen-subtype key and the filter's excludeSelf / excludeTarget / excludeTriggeringEntity flags.
     * Shared by `ForEach` group loops and effects whose source is an `EffectTarget.GroupRef`.
     */
    fun resolveGroup(
        state: GameState,
        filter: GroupFilter,
        context: EffectContext,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        // If the filter references a chosen subtype, resolve it from context
        val chosenSubtype = filter.chosenSubtypeKey?.let { key ->
            context.pipeline.chosenValues[key]
        }
        // If a chosen subtype key is specified but no value was chosen, return empty
        if (filter.chosenSubtypeKey != null && chosenSubtype == null) {
            return emptyList()
        }

        val excludeSelfId = if (filter.excludeSelf) context.sourceId else null
        // excludeTarget removes the spell/ability's first chosen target — used for "each other X
        // with the same controller" relative to a targeted permanent (Fear, Fire, Foes!).
        val excludeTargetId = if (filter.excludeTarget) {
            when (val first = context.targets.firstOrNull()) {
                is ChosenTarget.Permanent -> first.entityId
                is ChosenTarget.Card -> first.cardId
                else -> null
            }
        } else null
        val matched = findMatchingOnBattlefield(state, filter.baseFilter, context, excludeSelfId, predicateEvaluator = predicateEvaluator)
            .filter { excludeTargetId == null || it != excludeTargetId }
            .filter { !filter.excludeTriggeringEntity || it != context.triggeringEntityId }

        // Additionally filter by chosen subtype if specified
        return if (chosenSubtype != null) {
            val projected = state.projectedState
            matched.filter { projected.hasSubtype(it, chosenSubtype) }
        } else {
            matched
        }
    }
}
