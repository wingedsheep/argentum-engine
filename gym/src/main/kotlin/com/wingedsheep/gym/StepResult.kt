package com.wingedsheep.gym

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * Result of a [GameEnvironment.step] call.
 *
 * Follows the Gymnasium convention: (observation, reward, terminated, truncated, info).
 * The "observation" here is the full [GameState] — external consumers can extract
 * features from it however they like.
 */
data class StepResult(
    /** The game state after the action was applied. */
    val state: GameState,

    /** Events produced by this step (zone changes, damage, triggers, etc.). */
    val events: List<GameEvent>,

    /**
     * Per-player reward signal.
     * - During the game: all zeros.
     * - At game end: +1.0 for winner, -1.0 for loser, 0.0 for draw.
     */
    val reward: Map<EntityId, Double>,

    /** True if the game has ended naturally (win/loss/draw). */
    val terminated: Boolean,

    /** True if the game was cut short (e.g., max steps exceeded). */
    val truncated: Boolean,

    /** The player who needs to act next, or null if the game is over. */
    val agentToAct: EntityId?,

    /** Non-null if the engine is paused waiting for a player decision. */
    val pendingDecision: PendingDecision?,

    /** Additional metadata about the current game state. */
    val info: StepInfo
)

/**
 * Metadata about the game state, included in every [StepResult].
 */
data class StepInfo(
    val turnNumber: Int,
    val stepCount: Int,
    val winnerId: EntityId?,
    val phase: Phase,
    val step: Step
)
