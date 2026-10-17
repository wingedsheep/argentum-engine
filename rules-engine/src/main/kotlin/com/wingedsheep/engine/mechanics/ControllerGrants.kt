package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ControllerGrantMarker
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProjectedStaticAbility
import com.wingedsheep.sdk.scripting.conditions.Condition

/**
 * The single place a [ControllerGrantMarker]'s "as long as …" gate is evaluated.
 *
 * Player-level grants ("you have shroud", "you can't lose the game") sit outside the Rule 613
 * layer system: `StaticAbilityHandler` stamps a marker component once, as the permanent enters,
 * and readers scan the battlefield for it. A grant written behind a
 * [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] therefore can't be resolved at stamp
 * time — the gate flips later — so the condition rides on the marker and is re-evaluated here
 * against current state on every read.
 *
 * Reading a gated marker with a bare `container.has<M>()` leaves the grant permanently on. Every
 * reader goes through this object (or a mechanic-specific facade over it, like
 * [com.wingedsheep.engine.mechanics.targeting.ControllerHexproof]) so that can't happen.
 */
object ControllerGrants {

    /**
     * Whether a gate held by the permanent [entityId] is satisfied right now. A `null` [condition]
     * is an unconditional grant and is always active.
     *
     * The condition is evaluated with [entityId] as the source and its *projected* controller as
     * "you", so a control-change effect (Layer 2) correctly re-points a "you control …" gate at
     * the permanent's current controller.
     */
    fun isActive(state: GameState, entityId: EntityId, condition: Condition?, predicateEvaluator: PredicateEvaluator): Boolean {
        if (condition == null) return true
        val controllerId = state.projectedState.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
            ?: return false
        return predicateEvaluator.conditions.evaluate(
            state,
            condition,
            EffectContext(sourceId = entityId, controllerId = controllerId),
        )
    }

    /** Whether [marker], carried by [entityId], is granting right now. */
    fun isGrantingNow(state: GameState, entityId: EntityId, marker: ControllerGrantMarker, predicateEvaluator: PredicateEvaluator): Boolean =
        isActive(state, entityId, marker.condition, predicateEvaluator = predicateEvaluator)

    /**
     * Who [entityId] currently grants to — its projected controller, since control change is a
     * Layer 2 floating effect that never writes back to [ControllerComponent]. Reading the base
     * component instead would leave a stolen Sigarda protecting the player it was taken from, and
     * would disagree with [isActive], which resolves "you" the same way when evaluating the gate.
     */
    fun granterController(state: GameState, entityId: EntityId): EntityId? =
        state.projectedState.getController(entityId)
            ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

    /**
     * Whether any permanent on the battlefield currently grants [M] and has a controller
     * satisfying [controllerMatches].
     *
     * [controllerMatches] takes the controller rather than being a plain player id so callers can
     * widen the scope — the CR 810.8a team reach on "can't lose the game", for one, matches any
     * teammate rather than one player.
     */
    inline fun <reified M> anyGranting(
        state: GameState,
        predicateEvaluator: PredicateEvaluator,
        crossinline controllerMatches: (EntityId) -> Boolean
    ): Boolean where M : Component, M : ControllerGrantMarker =
        state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            val marker = container.get<M>() ?: return@any false
            val controllerId = granterController(state, entityId) ?: return@any false
            controllerMatches(controllerId) && isGrantingNow(state, entityId, marker, predicateEvaluator = predicateEvaluator)
        }

    /**
     * Whether any permanent on the battlefield holds an [A] among its *projected* static abilities
     * and has a controller satisfying [controllerMatches].
     *
     * The companion read to [anyGranting] for the controller grants that are also a
     * [ProjectedStaticAbility]: a printed instance is stamped as a marker at entry (that's what
     * carries an "as long as …" gate), while one granted to a permanent — by an Equipment or Aura's
     * [com.wingedsheep.sdk.scripting.GrantStaticAbility] (Cloudsteel Kirin's equipped creature) —
     * only exists in projection. Projection is the right owner for it: the grant ends when the
     * Equipment moves, and stops when the holder loses all its abilities. "You" is the holder's
     * projected controller, never the granting source's.
     */
    inline fun <reified A : ProjectedStaticAbility> anyHoldingProjected(
        state: GameState,
        crossinline controllerMatches: (EntityId) -> Boolean
    ): Boolean {
        val projected = state.projectedState
        return state.getBattlefield().any { entityId ->
            projected.getStaticAbilities(entityId).any { it is A } &&
                granterController(state, entityId)?.let { controllerMatches(it) } == true
        }
    }

    /** Whether [playerId] controls a permanent currently granting [M]. */
    inline fun <reified M> grantedTo(
        state: GameState,
        playerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean where M : Component, M : ControllerGrantMarker =
        anyGranting<M>(state, predicateEvaluator = predicateEvaluator) { it == playerId }

    /**
     * Whether the permanent [entityId] itself carries [M] and its gate holds right now — for the
     * self-scoped markers, where the effect lands on the permanent rather than on its controller
     * ([com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent]).
     */
    inline fun <reified M> isActiveOn(
        state: GameState,
        entityId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean where M : Component, M : ControllerGrantMarker {
        val marker = state.getEntity(entityId)?.get<M>() ?: return false
        return isGrantingNow(state, entityId, marker, predicateEvaluator = predicateEvaluator)
    }
}
