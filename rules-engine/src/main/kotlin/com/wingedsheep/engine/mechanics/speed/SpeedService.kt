package com.wingedsheep.engine.mechanics.speed

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.SpeedChangedEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.player.PlayerSpeedComponent
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.model.EntityId

/**
 * The one place a player's **speed** changes (Aetherdrift, CR 702.179).
 *
 * Both writers go through here — the CR 704.5z state-based action that starts a speed at 1
 * ([com.wingedsheep.engine.mechanics.sba.player.StartYourEnginesCheck]) and the
 * [com.wingedsheep.sdk.scripting.effects.ChangeSpeedEffect] executor behind "your speed increases by
 * N" / "reduce that opponent's speed by 1" — so the rules that govern the value can't drift apart
 * between them:
 *
 * - **CR 702.179c** — a player with no speed who is told to increase it ends up at that amount.
 *   Falls out of plain addition from [Speed.NONE].
 * - **CR 702.179e** — max speed is a speed of *exactly* 4, so the result is clamped to [Speed.MAX].
 *   The clamp is what lets the max-speed gate be an equality test rather than a `>=`.
 * - A change never moves speed the wrong way, and a reduction never hands the speed designation to a
 *   player who has none. A change that wouldn't move the value emits no event.
 */
object SpeedService {

    /**
     * Apply a signed [amount] to [playerId]'s speed, clamped to [Speed.MAX] above and [minimum]
     * below.
     *
     * [minimum] is the *reducing effect's own* floor (Spikeshell Harrier's "can't reduce their speed
     * below 1"), not a rule of the game, which is why it's a parameter. It is applied only in the
     * reducing direction and never raises a value: a player with no speed stays at [Speed.NONE] no
     * matter how high the floor, and a reduction can never end above where it started.
     *
     * @param sourceName attribution for the emitted [SpeedChangedEvent].
     * @return the new state plus the event, or the state unchanged and no events when the speed would
     *   not actually move (already at max, already at the floor, or a zero [amount]).
     */
    fun change(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        minimum: Int = Speed.NONE,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        val current = state.speed(playerId)
        val target = when {
            amount > 0 -> (current + amount).coerceAtMost(Speed.MAX)
            amount < 0 -> (current + amount).coerceAtLeast(minimum).coerceAtMost(current)
            else -> current
        }
        return set(state, playerId, target, sourceName)
    }

    /**
     * Set [playerId]'s speed to exactly [newSpeed] (CR 702.179b, "a rule or effect sets their speed
     * to a specific value") — the low-level primitive, used by the CR 704.5z state-based action for
     * its "becomes 1" and by [change] once it has computed the target.
     *
     * Clamped into `0..`[Speed.MAX]. A [newSpeed] equal to the current speed is a no-op, which is what
     * makes the state-based action idempotent across the many times the SBA loop runs it.
     */
    fun set(
        state: GameState,
        playerId: EntityId,
        newSpeed: Int,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        val current = state.speed(playerId)
        val target = newSpeed.coerceIn(Speed.NONE, Speed.MAX)
        if (target == current) return state to emptyList()

        val playerName = state.getEntity(playerId)?.get<PlayerComponent>()?.name ?: "Player"
        val newState = state.updateEntity(playerId) { container ->
            container.with(PlayerSpeedComponent(target))
        }
        return newState to listOf(
            SpeedChangedEvent(
                playerId = playerId,
                playerName = playerName,
                oldSpeed = current,
                newSpeed = target,
                sourceName = sourceName
            )
        )
    }
}
