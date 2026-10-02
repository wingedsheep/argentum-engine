package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

@Serializable
data class EffectDiscardDestinationContinuation(
    val effect: Effect,
    val context: EffectContext,
    val cardId: EntityId,
    val destinations: List<CardDestination.ToZone>,
) : AnswerContinuation

@Serializable
data class EffectDiscardOrderContinuation(
    val effect: Effect,
    val context: EffectContext,
) : AnswerContinuation
