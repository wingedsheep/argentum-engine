package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

@Serializable
data class ForcedPlayContinuation(val card: ObjectRef, val playerId: EntityId) : AnswerContinuation

/** Keeps the instruction alive across casting choices and publishes only a completed play. */
@Serializable
data class FinishForcedPlayContinuation(
    val card: ObjectRef,
    val playerId: EntityId,
    val from: String,
    val storePlayedTo: String?,
    val effectContext: EffectContext,
) : AutomaticContinuation
