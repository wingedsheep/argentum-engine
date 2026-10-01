package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.MutableProjectedValues
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.buildIntermediateProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Tells [ConditionEvaluator] which evaluation mode it's running in:
 *
 * - [Resolution]: a spell, triggered ability, or activated ability is resolving — the full
 *   [EffectContext] is available (targets, cast-from zone, kicker state, etc.) and the
 *   projected state is the supplied snapshot or the canonical [GameState.projectedState].
 *
 * - [Projection]: a `ConditionalStaticAbility` is being evaluated *during* state projection
 *   (Rule 613 layer application). The only known facts are the source entity and the
 *   in-flight per-entity projected values built up so far. Conditions that depend on
 *   resolution-time context (targets, triggering entity, cast-from zone, kicker, etc.)
 *   evaluate to `false` in this mode — by construction, a static ability is never "in the
 *   middle of casting a spell."
 */
internal sealed interface ConditionEvaluationContext {
    val sourceId: EntityId?

    /**
     * The controller of the source, as observed in the current mode:
     * - [Resolution]: pulled from [EffectContext.controllerId]
     * - [Projection]: pulled from the source's in-flight projected values (so Annex-style
     *   control changes resolved in earlier layers are honored)
     */
    val controllerId: EntityId?

    /**
     * Projected state to use for filter/predicate evaluation:
     * - [Resolution]: the supplied snapshot, falling back to [GameState.projectedState]
     * - [Projection]: a frozen snapshot of the in-flight projected values (see
     *   [buildIntermediateProjectedState]). Computed lazily and cached.
     */
    fun projectedStateFor(state: GameState): ProjectedState

    data class Resolution(
        val effectContext: EffectContext,
        val projected: ProjectedState? = null,
    ) : ConditionEvaluationContext {
        override val sourceId: EntityId? get() = effectContext.sourceId
        override val controllerId: EntityId get() = effectContext.controllerId
        override fun projectedStateFor(state: GameState): ProjectedState = projected ?: state.projectedState
    }

    class Projection(
        override val sourceId: EntityId,
        internal val sourceValues: MutableProjectedValues?,
        internal val projectedValues: Map<EntityId, MutableProjectedValues>
    ) : ConditionEvaluationContext {
        override val controllerId: EntityId? get() = sourceValues?.controllerId

        private var cachedProjected: ProjectedState? = null
        override fun projectedStateFor(state: GameState): ProjectedState =
            cachedProjected ?: buildIntermediateProjectedState(state, projectedValues)
                .also { cachedProjected = it }
    }
}
