package com.wingedsheep.engine.mechanics.enduringstory

import com.wingedsheep.engine.core.EnduringStoryGainedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerEnduringStoryComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * The one place the **enduring story** designation (CR 702.195b) is read and granted.
 *
 * The mechanic is the Ixalan city's blessing with a different threshold, and it is deliberately
 * modeled the same way — see [com.wingedsheep.engine.mechanics.citysblessing.CitysBlessingService]
 * for the long-form reasoning, which applies verbatim here:
 *
 * - **[has] evaluates live, it is not a component read.** CR 702.195a makes storied a *static*
 *   ability — "**any time** you control three or more permanents that are artifacts, Sagas, and/or
 *   legendary … you have an enduring story" — so the designation is yours the instant the third
 *   qualifying permanent arrives, with nothing on the stack and no priority in between. This engine
 *   polls state-based actions only between resolutions, so a resolution that creates your third
 *   qualifying permanent and *then* reads "if you have an enduring story" has to see the live
 *   answer, not a marker that no poll has written yet.
 * - **[StoriedEnduringStoryCheck][com.wingedsheep.engine.mechanics.sba.player.StoriedEnduringStoryCheck]
 *   writes the marker** at the next poll, which is what makes the designation survive dropping back
 *   below three qualifying permanents (CR 702.195a, "for the rest of the game") and what the client
 *   badge renders.
 *
 * There is no effect-shaped grant path: unlike ascend, which also exists as a spell ability on
 * instants and sorceries (CR 702.131a), storied is only ever a permanent's static ability, so the
 * state-based action is the sole writer.
 */
object EnduringStoryService {

    /** CR 702.195a — how many qualifying permanents storied looks for. */
    const val STORIED_THRESHOLD = 3

    /**
     * Does [playerId] have an enduring story right now?
     *
     * True once it has been granted (the marker is never removed — CR 702.195a/b), and true *live*
     * while a storied permanent they control sees three or more qualifying permanents, before the
     * state-based action has had a chance to write the marker.
     */
    fun has(
        state: GameState,
        playerId: EntityId,
        projected: ProjectedState = state.projectedState
    ): Boolean =
        state.getEntity(playerId)?.has<PlayerEnduringStoryComponent>() == true ||
            qualifiesViaStoried(state, playerId, projected)

    /**
     * CR 702.195a — does [playerId] control a permanent with storied *and* three or more permanents
     * that are artifacts, Sagas, and/or legendary?
     *
     * Every read goes through [projected], not the printed card: a *granted* storied counts, an
     * animated-into-an-artifact permanent counts toward the threshold, and a permanent stolen with a
     * Layer 2 control change counts for its new controller.
     *
     * The three categories are a union over permanents, not a sum over categories — a legendary
     * artifact Saga is one qualifying permanent, not three — which is why this counts permanents
     * matching *any* of the three rather than adding three tallies. A storied permanent that is
     * itself legendary (all nine printed ones are) counts toward its own threshold.
     *
     * [projected] is a parameter rather than a `state.projectedState` read for the reason spelled out
     * on `CitysBlessingService.qualifiesViaAscend`: this is reachable *from inside* projection —
     * every storied payoff is a `ConditionalStaticAbility` whose gate the layer system evaluates
     * while the projection is still being computed — and touching the lazy `GameState.projectedState`
     * there re-enters its initializer and recurses until the stack overflows. Mid-projection callers
     * pass the in-flight snapshot instead (`ConditionEvaluationContext.projectedStateFor`).
     */
    fun qualifiesViaStoried(
        state: GameState,
        playerId: EntityId,
        projected: ProjectedState = state.projectedState
    ): Boolean {
        val controlled = projected.getBattlefieldControlledBy(playerId)
        if (controlled.size < STORIED_THRESHOLD) return false
        if (controlled.none { projected.hasKeyword(it, Keyword.STORIED) }) return false
        return controlled.count { qualifies(projected, it) } >= STORIED_THRESHOLD
    }

    /** CR 702.195a — an artifact, a Saga, or a legendary permanent. Counted once either way. */
    private fun qualifies(projected: ProjectedState, entityId: EntityId): Boolean =
        projected.hasType(entityId, "ARTIFACT") ||
            projected.isLegendary(entityId) ||
            projected.hasSubtype(entityId, "Saga")

    /**
     * Persist the designation for [playerId], attributing the grant to [sourceName].
     *
     * Idempotent: a player who already has the marker is left alone and no event fires, which is what
     * lets the state-based action run every poll for the rest of the game without spamming
     * [EnduringStoryGainedEvent].
     */
    fun grant(
        state: GameState,
        playerId: EntityId,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(playerId) ?: return state to emptyList()
        if (container.has<PlayerEnduringStoryComponent>()) return state to emptyList()

        val playerName = container.get<PlayerComponent>()?.name ?: "Player"
        val newState = state.updateEntity(playerId) { it.with(PlayerEnduringStoryComponent) }
        return newState to listOf(EnduringStoryGainedEvent(playerId, playerName, sourceName))
    }
}
