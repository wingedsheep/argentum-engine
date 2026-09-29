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

/**
 * One "as this permanent enters, choose …" question ([com.wingedsheep.sdk.scripting.EntersWithChoice])
 * asked for [entityId] before an effect puts it onto the battlefield. [question] is the answer frame
 * the choice prompt built — it knows how to read the response; its own entry fields are unused here.
 * The resumer records the answer on [context] and replays [effect].
 */
@Serializable
data class EffectEntryChoiceContinuation(
    val effect: Effect,
    val context: EffectContext,
    val entityId: EntityId,
    val question: EntersWithChoiceOnBattlefieldContinuation,
) : AnswerContinuation
