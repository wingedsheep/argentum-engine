package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Whether [entityId] currently carries the suspected designation (CR 701.60a).
 *
 * The designation is a Layer-ability floating effect rather than a component, so the normal
 * read is `ProjectedState.isSuspected`. This raw-state form exists for the callers that have no
 * projection in hand — trigger gating, and `SetSuspectedExecutor`'s CR 701.60d "can't become
 * suspected again" check, which has to answer the question *while* building the next state.
 *
 * Both readings agree: the projection's `isSuspected` is set from exactly these floating
 * effects, and suspect has no duration to expire out from under it.
 */
fun isSuspected(state: GameState, entityId: EntityId): Boolean =
    state.floatingEffects.any { fx ->
        fx.effect.modification is SerializableModification.SetSuspected &&
            entityId in fx.effect.affectedEntities
    }
