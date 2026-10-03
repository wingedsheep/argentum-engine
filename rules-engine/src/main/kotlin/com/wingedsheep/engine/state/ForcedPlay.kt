package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.FinishForcedPlayContinuation
import com.wingedsheep.sdk.model.EntityId

/** An instruction waives type-based timing only for its captured card and affected player. */
fun GameState.forcedPlayFor(playerId: EntityId, cardId: EntityId? = null): FinishForcedPlayContinuation? =
    continuationStack.lastOrNull {
        it is FinishForcedPlayContinuation && it.playerId == playerId &&
            (cardId == null || it.card.entityId == cardId) && isCurrentObject(it.card)
    } as? FinishForcedPlayContinuation
