package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

@Serializable
data class EffectCopyEntryContinuation(
    val effect: Effect,
    val context: EffectContext,
    val entityId: EntityId,
    val replacement: EntersAsCopy,
) : AnswerContinuation
