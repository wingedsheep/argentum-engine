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
    val destinations: List<DiscardDestinationChoice>,
) : AnswerContinuation

/** One alternate destination offered for an effect discard, and whether the card is revealed on the way. */
@Serializable
data class DiscardDestinationChoice(
    val destination: CardDestination.ToZone,
    val reveal: Boolean = false,
)

@Serializable
data class EffectDiscardOrderContinuation(
    val effect: Effect,
    val context: EffectContext,
) : AnswerContinuation
