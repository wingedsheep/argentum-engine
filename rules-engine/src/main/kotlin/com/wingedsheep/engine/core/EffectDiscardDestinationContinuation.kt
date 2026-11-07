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

/**
 * One alternate destination offered for an effect discard, whether the card is revealed on the way,
 * and the name of the permanent whose replacement offers it (the reveal's source).
 */
@Serializable
data class DiscardDestinationChoice(
    val destination: CardDestination.ToZone,
    val reveal: Boolean = false,
    val sourceName: String? = null,
)

@Serializable
data class EffectDiscardOrderContinuation(
    val effect: Effect,
    val context: EffectContext,
) : AnswerContinuation
