package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

@Serializable
data class EffectCopyAuraEntryContinuation(
    val effect: Effect,
    val context: EffectContext,
    val entityId: EntityId,
) : AnswerContinuation

@Serializable
data class CloneAuraEntryContinuation(
    val clone: CloneEntersContinuation,
    val copiedEntityId: EntityId,
) : AnswerContinuation
