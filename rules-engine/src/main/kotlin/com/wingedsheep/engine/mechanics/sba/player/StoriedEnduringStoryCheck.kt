package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PlayerEnduringStoryComponent
import com.wingedsheep.sdk.core.Keyword

/**
 * CR 702.195a — **storied** is a static ability: "any time you control three or more permanents that
 * are artifacts, Sagas, and/or legendary and you don't have an enduring story, you have an enduring
 * story for the rest of the game."
 *
 * Modeled exactly like [AscendCitysBlessingCheck], whose doc carries the full argument for why the
 * trigger-shaped alternative is wrong. It bites harder here than it does for ascend: the printed
 * storied cards are one- and two-drops (Dáin at {1}{W}, Thorin Oakenshield at {R}{W}), so an
 * enters-the-battlefield trigger would sample the count on the turn you are least likely to control
 * three qualifying permanents and never look again — the payoff half of every storied card would
 * simply never come online. Running here re-reads the count every SBA poll, so the designation
 * arrives with whichever permanent is the third, from whatever direction.
 *
 * Writing the marker is what makes the designation permanent — [EnduringStoryService.grant] is
 * idempotent and the component is never removed, so dropping back below three qualifying permanents
 * (or losing the storied permanent entirely) keeps the enduring story, per CR 702.195a/b.
 *
 * **Reads don't wait for this check.** State-based actions are polled between resolutions, never
 * inside one, so every *read* goes through [EnduringStoryService.has], which evaluates the storied
 * condition live. This check is the persistence half, not the whole rule.
 *
 * Ordering: next to the other player-designation checks, before the loss checks, so a player ends an
 * SBA pass with a consistent designation. The position is otherwise immaterial — gaining an enduring
 * story can't cause or prevent any other state-based action.
 */
class StoriedEnduringStoryCheck : StateBasedActionCheck {
    override val name = "702.195a Storied / Enduring Story"
    override val order = SbaOrder.STORIED_ENDURING_STORY

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            // Already marked: skip before the battlefield scan. The designation is never removed, so
            // for most of a game this is the branch every poll takes.
            if (newState.getEntity(playerId)?.has<PlayerEnduringStoryComponent>() == true) continue
            if (!EnduringStoryService.qualifiesViaStoried(newState, playerId)) continue
            val (updated, grantEvents) = EnduringStoryService.grant(
                state = newState,
                playerId = playerId,
                sourceName = Keyword.STORIED.displayName
            )
            newState = updated
            events.addAll(grantEvents)
        }

        return ExecutionResult.success(newState, events)
    }
}
