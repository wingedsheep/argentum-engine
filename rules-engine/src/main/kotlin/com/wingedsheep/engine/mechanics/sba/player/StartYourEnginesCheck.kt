package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.model.EntityId

/**
 * CR 704.5z — if a player controls a permanent with **start your engines!** and that player has no
 * speed, that player's speed becomes 1 (CR 702.179a).
 *
 * Being a state-based action rather than a triggered ability is load-bearing, and it's why this is a
 * dozen lines instead of a trigger plumbed through several layers:
 *
 * - Gaining control of an opponent's permanent with the keyword starts *your* speed, with no
 *   change-of-control trigger to write.
 * - A permanent that is *granted* the keyword mid-game starts its controller's speed, because the
 *   keyword is read from projected state (Layer 6), not from the printed card.
 * - It is idempotent and runs to fixpoint inside the SBA loop, so several permanents with the keyword
 *   under one controller still start exactly one speed.
 *
 * **When it actually fires.** CR 704.3 has state-based actions checked whenever a player would
 * receive priority; this engine polls them more narrowly — after a spell or ability resolves
 * (`PassPriorityHandler`), after the draw step and on the end-the-turn path (`TurnManager`), and after
 * a decision resolves. For the normal case that is indistinguishable: a permanent with the keyword is
 * *cast*, so the SBA runs the moment it resolves and speed starts before anyone gets priority. The
 * gap shows only for a permanent that reaches the battlefield without anything resolving afterwards —
 * a land drop (Amonkhet Raceway), or a scenario-injected board — where speed starts at the next poll
 * instead. Nothing can read speed in that window except a max-speed gate, which needs 4 anyway. This
 * is the engine's SBA cadence, shared by every check here, not something specific to speed.
 *
 * Ordering: this runs before the loss checks so that a player who is about to lose still ends up with
 * a consistent speed value in the same SBA pass — the position is otherwise immaterial, since
 * starting a speed can't cause or prevent any other state-based action.
 */
class StartYourEnginesCheck : StateBasedActionCheck {
    override val name = "704.5z Start Your Engines"
    override val order = SbaOrder.START_YOUR_ENGINES

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in playersControllingStartYourEngines(state)) {
            if (newState.hasSpeed(playerId)) continue
            val (updated, speedEvents) = SpeedService.set(
                state = newState,
                playerId = playerId,
                newSpeed = Speed.STARTING,
                sourceName = Keyword.START_YOUR_ENGINES.displayName
            )
            newState = updated
            events.addAll(speedEvents)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Controllers of battlefield permanents with the keyword, read from projected state so a granted
     * "Start your engines!" counts and so control-changing effects are honoured.
     *
     * A set, not a list: several permanents with the keyword under one controller start one speed.
     */
    private fun playersControllingStartYourEngines(state: GameState): Set<EntityId> {
        val projected = state.projectedState
        val controllers = mutableSetOf<EntityId>()
        for (entityId in state.getBattlefield()) {
            if (!projected.hasKeyword(entityId, Keyword.START_YOUR_ENGINES)) continue
            projected.getController(entityId)?.let { controllers.add(it) }
        }
        return controllers
    }
}
