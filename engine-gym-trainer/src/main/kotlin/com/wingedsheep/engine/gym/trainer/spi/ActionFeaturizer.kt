package com.wingedsheep.engine.gym.trainer.spi

import com.wingedsheep.engine.core.GameAction

/**
 * Maps concrete engine actions onto stable categorical slots in the user's
 * policy network.
 *
 * Two design decisions baked in:
 *
 * 1. **Multi-head is first-class.** [heads] declares every head the
 *    network exposes; [slot] tells the trainer which `(head, index)` pair
 *    a given action belongs to. An AlphaZero-for-chess setup passes a
 *    single head; an MTG setup that splits priority / target / binary
 *    decisions across separate heads uses several.
 *
 * 2. **Slot assignment is per-state, not global.** Callers may return
 *    different `SlotEncoding`s for the same action in different
 *    [TrainerContext]s — that's how a `PassPriority` can be slot 0 in the
 *    "priority" head when it's an action and unused entirely in the
 *    "target" head when that's the active decision.
 */
interface ActionFeaturizer {

    /** All heads the network exposes; names must be unique. */
    val heads: List<PolicyHead>

    /** Return the `(head, slot)` this action occupies for the given context. */
    fun slot(action: GameAction, ctx: TrainerContext): SlotEncoding
}
