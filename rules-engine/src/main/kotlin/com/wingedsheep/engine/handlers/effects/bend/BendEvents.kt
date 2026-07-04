package com.wingedsheep.engine.handlers.effects.bend

import com.wingedsheep.engine.core.BendPerformedEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.BendsThisTurnComponent
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.model.EntityId

/**
 * Single point that records a bend: folds [bendType] into the player's per-turn distinct-bend set
 * (`BendsThisTurnComponent`) and produces the matching [BendPerformedEvent].
 *
 * Shared by every bend emit site so tracking and eventing stay consistent regardless of how the
 * bend happened (effect resolution for earthbend/airbend/firebend via [EmitBendEventExecutor], or
 * cost payment for waterbend in the cast/activate handlers — CR 701.67c).
 */
object BendEvents {
    /**
     * Record that [playerId] performed a [bendType] bend. Returns the updated state (with the
     * player's `BendsThisTurnComponent` extended) and the event to emit so "whenever you … bend"
     * triggers fire.
     */
    fun record(state: GameState, playerId: EntityId, bendType: BendType): Pair<GameState, BendPerformedEvent> {
        val existing = state.getEntity(playerId)?.get<BendsThisTurnComponent>()?.types ?: emptySet()
        val newState = state.updateEntity(playerId) { container ->
            container.with(BendsThisTurnComponent(existing + bendType))
        }
        return newState to BendPerformedEvent(playerId, bendType)
    }
}
