package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** Activation identities, rather than producing-source identities, survive nested payments. */
@Serializable
data class ManaSpendingObligationsContinuation(
    val playerId: EntityId,
    val context: EffectContext,
    val scopeId: String,
    val pendingIds: Set<String> = emptySet(),
) : AutomaticContinuation
