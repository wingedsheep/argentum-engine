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

/**
 * Resume after the controller chose what the next creature token put onto the battlefield
 * attacking is attacking (CR 508.4) — see
 * [com.wingedsheep.engine.handlers.effects.token.EnteringAttackDefenderChooser].
 *
 * Nothing has been created yet: the answer is appended to [EffectContext.entryAttackDefenders] and
 * [effect] is executed again from the top, asking for the next token or, once every token has its
 * defender, creating them all at once. [context] is the full resolution context, so targets and
 * pipeline collections survive the pause.
 */
@Serializable
data class EnteringAttackDefenderContinuation(
    val effect: Effect,
    val context: EffectContext,
) : AnswerContinuation
