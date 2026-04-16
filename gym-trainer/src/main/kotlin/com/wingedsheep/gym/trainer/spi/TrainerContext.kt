package com.wingedsheep.gym.trainer.spi

import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Describes *why* the agent is being asked to decide — passed to every SPI
 * call so featurizers / action encoders / evaluators can branch on whether
 * this is a normal priority point, a yes/no decision, a target selection,
 * etc.
 *
 * Named `TrainerContext` rather than `DecisionContext` to avoid colliding
 * with the engine's internal `com.wingedsheep.engine.core.DecisionContext`.
 *
 * [pendingDecision] is non-null when the engine is paused on a
 * [PendingDecision]; its concrete subtype (`YesNoDecision`,
 * `ChooseTargetsDecision`, `DistributeDecision`, …) carries the fine-grained
 * kind. A `null` pending decision means the agent has priority and should
 * pick a `LegalAction`.
 */
data class TrainerContext(
    val state: GameState,
    val playerId: EntityId,
    val pendingDecision: PendingDecision?
) {
    val isPriority: Boolean get() = pendingDecision == null
}
