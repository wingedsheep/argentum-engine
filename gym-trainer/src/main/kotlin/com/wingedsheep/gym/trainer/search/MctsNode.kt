package com.wingedsheep.gym.trainer.search

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.gym.trainer.spi.SlotEncoding
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * A node in the MCTS tree.
 *
 * Because [GameState] is immutable we simply hold a reference — no deep
 * copy, no fork cost. The environment is re-hydrated from this state only
 * when we need to step forward during expansion.
 *
 * A node is a *leaf* (`children` empty) until the first time it is visited
 * by [AlphaZeroSearch]; on that visit the children are materialised and the
 * evaluator is called to seed their priors.
 */
class MctsNode(
    val state: GameState,
    val playerIds: List<EntityId>,
    /** The player who is about to act at this state; null at terminal states. */
    val agentToAct: EntityId?,
    val pendingDecision: PendingDecision?,
    val terminal: Boolean,
    /** Reserved for pre-computed terminal values; AlphaZeroSearch computes on the fly from [winnerId]. */
    val terminalValue: Float = 0f,
    /** `null` at non-terminal nodes and at draws; an [EntityId] at a decided terminal. */
    val winnerId: EntityId? = null
) {
    /** Parallel list of outgoing edges. Materialised on first expansion. */
    var edges: List<MctsEdge> = emptyList()

    /** Total visits through this node. */
    var visits: Int = 0

    /** Sum of backed-up values, from the *root* acting player's perspective. */
    var valueSum: Double = 0.0

    val meanValue: Double get() = if (visits == 0) 0.0 else valueSum / visits
    val isLeaf: Boolean get() = edges.isEmpty()
}

/**
 * An edge from a node to a child.
 *
 * Wraps the [GameAction] that would be stepped, the `(head, slot)` the
 * user's policy network assigns it, and the prior pulled from the
 * evaluator's output. The child node is created lazily on first traversal.
 */
class MctsEdge(
    val action: GameAction,
    /** The [LegalAction] behind this edge when the parent is a priority state, else null. */
    val legalAction: LegalAction?,
    val slot: SlotEncoding,
    /** Normalized prior in [0, 1]; summed across edges at the parent = 1. */
    var prior: Float = 0f,
    /** Visits on this edge. Equal to `child.visits` but faster to read in PUCT loop. */
    var visits: Int = 0,
    /** Running mean action-value Q from root-player perspective. */
    var meanValue: Double = 0.0,
    var child: MctsNode? = null
)
