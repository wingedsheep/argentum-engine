package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SecretChoiceEffect
import kotlinx.serialization.Serializable

/** Secret answers stay only in the engine's saved resolution, until the last participant answers. */
@Serializable
data class SecretChoiceContinuation(
    val effect: SecretChoiceEffect,
    val effectContext: EffectContext,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val numbers: Map<EntityId, Int> = emptyMap(),
    val entities: Map<EntityId, EntityId?> = emptyMap(),
    val decisionIds: Map<EntityId, String> = emptyMap(),
    val sourceName: String? = null,
    val objectReferences: ObjectReferenceEnvironment = effectContext.objectReferences,
) : AnswerContinuation
