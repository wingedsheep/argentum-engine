package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Applies "[filter] enter the battlefield tapped" replacement effects sourced from *other*
 * battlefield permanents (e.g. Zhao, the Moon Slayer's "Nonbasic lands enter tapped").
 *
 * The global counterpart of the self-only [com.wingedsheep.sdk.scripting.EntersTapped]: a
 * [PermanentsEnterTapped] is stamped into the source's [ReplacementEffectSourceComponent]
 * (see `StaticAbilityHandler.isRuntimeReplacementEffect`) and consulted from the battlefield
 * whenever some *other* permanent enters. The entry paths (PlayLandHandler,
 * ZoneTransitionService) ask [entersTapped] and mark the permanent tapped when it returns true.
 *
 * Symmetric to [EnterUntappedReplacements]; per CR 614 an applicable [EnterUntappedReplacements]
 * wins, so callers consult that first and only apply this tap when the entering permanent is not
 * already made untapped by a replacement.
 */
object EnterTappedReplacements {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * True if any battlefield permanent grants a [PermanentsEnterTapped] replacement whose
     * `appliesTo` filter matches [enteringEntityId] (controlled by [enteringControllerId]). The
     * entering entity must already carry its [ControllerComponent] /
     * [com.wingedsheep.engine.state.components.identity.CardComponent] so the filter
     * (type/subtype/"you control") resolves correctly.
     */
    fun entersTapped(
        state: GameState,
        enteringEntityId: EntityId,
        enteringControllerId: EntityId,
    ): Boolean {
        for (sourceId in state.getBattlefield()) {
            if (sourceId == enteringEntityId) continue
            val container = state.getEntity(sourceId) ?: continue
            val replacementComponent = container.get<ReplacementEffectSourceComponent>() ?: continue
            val sourceControllerId = container.get<ControllerComponent>()?.playerId ?: continue
            for (effect in replacementComponent.replacementEffects) {
                if (effect !is PermanentsEnterTapped) continue
                if (matchesEnterFilter(effect.appliesTo, enteringEntityId, sourceControllerId, state)) {
                    return true
                }
            }
        }
        return false
    }

    private fun matchesEnterFilter(
        event: EventPattern,
        enteringEntityId: EntityId,
        sourceControllerId: EntityId,
        state: GameState,
    ): Boolean {
        if (event !is EventPattern.ZoneChangeEvent) return false
        if (event.to != Zone.BATTLEFIELD) return false
        val predicateContext = PredicateContext(
            sourceId = enteringEntityId,
            controllerId = sourceControllerId,
        )
        return predicateEvaluator.matches(
            state, state.projectedState, enteringEntityId, event.filter, predicateContext
        )
    }
}
