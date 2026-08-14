package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.citysblessing.CitysBlessingService
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.sdk.core.Keyword

/**
 * CR 702.131b — **ascend** on a permanent is a static ability: "any time you control ten or more
 * permanents and you don't have the city's blessing, you get the city's blessing for the rest of
 * the game."
 *
 * Modeled the same way as [StartYourEnginesCheck], and for the same reason: the trigger-shaped
 * alternative is wrong. An enters-the-battlefield trigger samples the permanent count exactly once,
 * at the moment the ascend permanent lands, and never looks again — which for a one-mana 1/1 like
 * Ocelot Pride means the threshold is checked on the turn you are least likely to meet it and the
 * card's city's-blessing half never comes online. Running here instead means the count is re-read
 * every time state-based actions are polled, so the blessing arrives the moment the tenth permanent
 * does, whichever permanent that is; a *granted* ascend counts, because the keyword is read from
 * projected state (Layer 6); and gaining control of an opponent's ascend permanent gives *you* the
 * blessing, with no change-of-control trigger to write.
 *
 * Writing the marker is what makes the designation permanent — [CitysBlessingService.grant] is
 * idempotent and the component is never removed, so dropping back below ten permanents (or losing
 * the ascend permanent entirely) keeps the blessing, per CR 702.131b/c.
 *
 * **Reads don't wait for this check.** CR 704.3 has state-based actions checked whenever a player
 * would receive priority; this engine polls them more narrowly — after a spell or ability resolves,
 * after the draw step, after combat damage, on the end-the-turn path, and after a decision resolves.
 * Never inside a resolution, and not after a land drop. Either gap would be visible here: Ocelot
 * Pride's "create a Cat, *then* if you have the city's blessing…" needs the count re-read
 * mid-resolution when that Cat is the tenth permanent, and playing your tenth land should hand you
 * the blessing before anything else happens. So every *read* goes through
 * [CitysBlessingService.has], which evaluates the ascend condition live. This check is the
 * persistence half — it writes the marker at the next poll so the designation outlives dropping
 * back below ten permanents — not the whole rule.
 *
 * Ordering: next to the other player-designation check, before the loss checks, so a player ends an
 * SBA pass with a consistent designation. The position is otherwise immaterial — gaining the city's
 * blessing can't cause or prevent any other state-based action.
 */
class AscendCitysBlessingCheck : StateBasedActionCheck {
    override val name = "702.131b Ascend / City's Blessing"
    override val order = SbaOrder.ASCEND_CITYS_BLESSING

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            // Already marked: skip before the battlefield scan. The blessing is never removed, so
            // for most of a game this is the branch every poll takes.
            if (newState.getEntity(playerId)?.has<PlayerCitysBlessingComponent>() == true) continue
            if (!CitysBlessingService.qualifiesViaAscend(newState, playerId)) continue
            val (updated, grantEvents) = CitysBlessingService.grant(
                state = newState,
                playerId = playerId,
                sourceName = Keyword.ASCEND.displayName
            )
            newState = updated
            events.addAll(grantEvents)
        }

        return ExecutionResult.success(newState, events)
    }
}
