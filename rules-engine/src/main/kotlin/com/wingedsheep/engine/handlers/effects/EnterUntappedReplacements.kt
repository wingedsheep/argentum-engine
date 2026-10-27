package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.replacement.ActiveReplacements
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersUntapped
import com.wingedsheep.sdk.scripting.EventPattern

/**
 * Applies "[filter] enter the battlefield untapped" replacement effects sourced from *other*
 * battlefield permanents (e.g. The Wandering Minstrel's "Lands you control enter untapped").
 *
 * Unlike [com.wingedsheep.sdk.scripting.EntersTapped] — a self-replacement consumed once as the
 * source itself enters — [EntersUntapped] is a runtime replacement stamped into the source's
 * [com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent] (see `StaticAbilityHandler.isRuntimeReplacementEffect`) and
 * consulted from the battlefield whenever some *other* permanent would enter tapped. The entry
 * paths that mark a permanent tapped on entry (PlayLandHandler, ZoneTransitionService) ask
 * [entersUntapped] first and skip the tap when it returns true.
 *
 * Per CR 614 replacement-effect ordering, this collapses both "would enter tapped via another
 * replacement" (controller chooses untapped) and "simply put onto the battlefield tapped"
 * (no replacement → enters untapped) to the same outcome: the permanent enters untapped.
 */
object EnterUntappedReplacements {

    /**
     * True if any [EntersUntapped] replacement in force — printed on a battlefield permanent, held
     * by an emblem, or granted for a duration ([ActiveReplacements]) — has an `appliesTo` filter
     * that matches [enteringEntityId] (controlled by [enteringControllerId]). The entering
     * entity must already carry its [ControllerComponent] / [com.wingedsheep.engine.state.components.identity.CardComponent]
     * so the filter (type/subtype/"you control") resolves correctly.
     */
    fun entersUntapped(
        state: GameState,
        enteringEntityId: EntityId,
        enteringControllerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        for (active in ActiveReplacements.all(state) { it is EntersUntapped }) {
            val effect = active.effect as? EntersUntapped ?: continue
            if (!active.granted && active.sourceId == enteringEntityId) continue
            if (matchesEnterFilter(effect.appliesTo, enteringEntityId, active.sourceId, active.controllerId, state, predicateEvaluator = predicateEvaluator)) {
                return true
            }
        }
        return false
    }

    /**
     * [replacementSourceId] is the permanent that *grants* the replacement, not the one entering —
     * the entering permanent is what the filter is evaluated against and is passed separately. The
     * distinction only shows up for a source-relative predicate: "creatures enchanted player
     * controls" has to read the granting Aura's own attachment, and would otherwise be asked about
     * the entering creature's.
     */
    private fun matchesEnterFilter(
        event: EventPattern,
        enteringEntityId: EntityId,
        replacementSourceId: EntityId,
        sourceControllerId: EntityId,
        state: GameState,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        if (event !is EventPattern.ZoneChangeEvent) return false
        if (event.to != Zone.BATTLEFIELD) return false
        val predicateContext = PredicateContext(
            sourceId = replacementSourceId,
            controllerId = sourceControllerId,
        )
        return predicateEvaluator.matches(
            state, state.projectedState, enteringEntityId, event.filter, predicateContext
        )
    }
}
