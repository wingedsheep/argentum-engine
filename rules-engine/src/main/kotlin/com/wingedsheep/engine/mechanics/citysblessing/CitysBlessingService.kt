package com.wingedsheep.engine.mechanics.citysblessing

import com.wingedsheep.engine.core.CitysBlessingGainedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * The one place the **city's blessing** designation (CR 702.131c) is read and granted.
 *
 * Two callers exist and they must not drift apart:
 *
 * - [com.wingedsheep.engine.mechanics.sba.player.AscendCitysBlessingCheck] persists the designation
 *   for a player whose ascend permanent currently sees ten permanents.
 * - [com.wingedsheep.engine.handlers.effects.player.GainCitysBlessingExecutor] persists it for the
 *   spell-ability form (CR 702.131a — ascend on an instant or sorcery) and for any card that simply
 *   says "you get the city's blessing".
 *
 * **Why [has] is more than a component read.** CR 702.131b makes ascend on a permanent a *static*
 * ability — "**any time** you control ten or more permanents … you get the city's blessing" — so the
 * designation is yours the instant the tenth permanent is there, with nothing on the stack and no
 * priority in between. The engine polls state-based actions only between resolutions, which is one
 * poll too late for the case the card's own rulings call out: Ocelot Pride's last ability creates a
 * Cat, and if that Cat is your tenth permanent you have the blessing *by the time the same
 * resolution reaches* "Then if you have the city's blessing…". Reading the ascend condition live
 * here, rather than waiting for the marker to be written, is what makes that mid-resolution read
 * come out right. [AscendCitysBlessingCheck] then writes the marker at the next poll, which is what
 * makes the designation survive dropping back below ten permanents (CR 702.131b, "for the rest of
 * the game") and what the client badge renders.
 */
object CitysBlessingService {

    /** CR 702.131a/b — the permanent count ascend looks for. */
    const val ASCEND_THRESHOLD = 10

    /**
     * Does [playerId] have the city's blessing right now?
     *
     * True once it has been granted (the marker is never removed — CR 702.131b/c), and true *live*
     * while an ascend permanent they control sees ten or more permanents, before the state-based
     * action has had a chance to write the marker.
     */
    fun has(
        state: GameState,
        playerId: EntityId,
        projected: ProjectedState = state.projectedState
    ): Boolean =
        state.getEntity(playerId)?.has<PlayerCitysBlessingComponent>() == true ||
            qualifiesViaAscend(state, playerId, projected)

    /**
     * CR 702.131b — does [playerId] control a permanent with ascend *and* ten or more permanents?
     *
     * Both halves read [projected], not the printed card: a *granted* ascend counts, and a permanent
     * stolen with a Layer 2 control change counts for its new controller.
     *
     * [projected] is a parameter rather than a `state.projectedState` read because this is reachable
     * *from inside* projection — Tendershoot Dryad's "Saprolings you control get +2/+2 as long as you
     * have the city's blessing" is a `ConditionalStaticAbility` whose gate the layer system evaluates
     * while the projection is still being computed. Touching the lazy `GameState.projectedState`
     * there re-enters its initializer and recurses until the stack overflows, so mid-projection
     * callers pass the in-flight snapshot instead (`ConditionEvaluationContext.projectedStateFor`).
     */
    fun qualifiesViaAscend(
        state: GameState,
        playerId: EntityId,
        projected: ProjectedState = state.projectedState
    ): Boolean {
        val controlled = projected.getBattlefieldControlledBy(playerId)
        if (controlled.size < ASCEND_THRESHOLD) return false
        return controlled.any { projected.hasKeyword(it, Keyword.ASCEND) }
    }

    /**
     * Persist the designation for [playerId], attributing the grant to [sourceName].
     *
     * Idempotent: a player who already has the marker is left alone and no event fires, which is
     * what lets the state-based action run every poll for the rest of the game without spamming
     * [CitysBlessingGainedEvent].
     */
    fun grant(
        state: GameState,
        playerId: EntityId,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(playerId) ?: return state to emptyList()
        if (container.has<PlayerCitysBlessingComponent>()) return state to emptyList()

        val playerName = container.get<PlayerComponent>()?.name ?: "Player"
        val newState = state.updateEntity(playerId) { it.with(PlayerCitysBlessingComponent) }
        return newState to listOf(CitysBlessingGainedEvent(playerId, playerName, sourceName))
    }
}
