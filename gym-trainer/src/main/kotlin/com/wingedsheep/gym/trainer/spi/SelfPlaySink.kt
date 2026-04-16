package com.wingedsheep.gym.trainer.spi

import com.wingedsheep.sdk.model.EntityId

/**
 * Writes self-play training rows.
 *
 * One `beginGame` + N `recordStep` + one `endGame` sequence represents a
 * single game trajectory. The sink may buffer rows and only flush on
 * `endGame` so the final value label from the game outcome can be
 * back-patched onto every row — a `JsonlSelfPlaySink` does this; other
 * implementations may stream.
 *
 * Thread-safety is the implementation's concern; the built-in trainer runs
 * games sequentially within a single [SelfPlayLoop][com.wingedsheep.gym.trainer.selfplay.SelfPlayLoop]
 * so no sync is required for that path.
 */
interface SelfPlaySink<T> : AutoCloseable {

    fun beginGame(gameId: String, players: List<EntityId>)

    /**
     * Record a single (state, policy, value) triple.
     *
     * @param features      [StateFeaturizer] output for this state
     * @param ctx           the decision context at which this step was made
     * @param actingPlayer  the player who made the move (== `ctx.playerId`)
     * @param headUsed      the policy head active for this decision
     * @param legalSlots    slot encodings of every legal action at this state
     * @param visits        MCTS visit counts, parallel to `legalSlots`
     * @param mctsValue     MCTS-estimated value in `[-1, +1]` from
     *                      `actingPlayer`'s perspective; the terminal outcome
     *                      overrides this in the training target if the sink
     *                      uses an outcome-label rather than a blended label
     */
    fun recordStep(
        features: T,
        ctx: TrainerContext,
        actingPlayer: EntityId,
        headUsed: String,
        legalSlots: List<SlotEncoding>,
        visits: IntArray,
        mctsValue: Float
    )

    /** Called exactly once per game after the final state; `winner == null` = draw. */
    fun endGame(winner: EntityId?)

    override fun close() {}
}
