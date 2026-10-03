package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * The rest of a step in which no player receives priority, parked beneath a choice that one of the
 * step's turn-based actions asked: a "may not untap" choice in the untap step (CR 502.3) or the
 * discard to maximum hand size in the cleanup step (CR 514.1). Once the choice is answered, the
 * auto-resumer advances the game out of that step, exactly as the unpaused path does.
 */
@Serializable
data object AdvanceStepContinuation : AutomaticContinuation

/**
 * The rest of starting [activePlayerId]'s turn, parked beneath a choice asked by its untap step.
 * Once the untap step is over, "until your next turn" effects, goad designations, and effects
 * that last until that player's next untap step end, then the game advances to the upkeep step.
 * See `TurnManager.finishUntapStep`.
 */
@Serializable
data class FinishUntapStepContinuation(
    val activePlayerId: EntityId,
    val skippedUntapStep: Set<EntityId>,
    val pendingSkipsToConsume: Set<EntityId>,
) : AutomaticContinuation

/** Select the replacement for a team's untap: at most one pending skip is spent. */
@Serializable
data class UntapStepSkipChoiceContinuation(val pendingPlayers: List<EntityId>) : AnswerContinuation
