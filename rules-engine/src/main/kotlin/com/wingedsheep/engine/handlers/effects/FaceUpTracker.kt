package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PermanentsTurnedFaceUpThisTurnComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Records that a player turned a permanent face up for per-player, per-turn "you turned a
 * permanent face up this turn" tracking (Oblivious Bookworm).
 *
 * Called at every point that emits a [com.wingedsheep.engine.core.TurnFaceUpEvent] — the
 * turn-face-up special action (CR 116.2b) in `TurnFaceUpHandler`, and the `TurnFaceUpEffect`
 * executor (`TurnFaceUpExecutor`), which also backs the non-mana morph/disguise cost path via
 * the cost-payment `onPaid` flip. Merely revealing a face-down permanent as it leaves the
 * battlefield (CR 708.9) does not emit that event and so does not count.
 *
 * Cleared at end of turn by [com.wingedsheep.engine.core.CleanupPhaseManager].
 */
object FaceUpTracker {

    /** Record that [controllerId] just turned one of their permanents face up. */
    fun record(state: GameState, controllerId: EntityId): GameState =
        state.updateEntity(controllerId) { container ->
            val existing = container.get<PermanentsTurnedFaceUpThisTurnComponent>()
                ?: PermanentsTurnedFaceUpThisTurnComponent()
            container.with(PermanentsTurnedFaceUpThisTurnComponent(existing.count + 1))
        }
}
