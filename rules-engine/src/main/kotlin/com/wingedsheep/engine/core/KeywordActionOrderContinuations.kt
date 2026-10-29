package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/*
 * Answers to "in which order do your mill / scry / surveil replacements apply?" (CR 616.1). When two
 * or more `ModifyKeywordActionAmount` replacements apply to one action and their order changes the
 * count (a doubler next to a "+4"), the acting player picks among the distinct resulting [counts];
 * each frame below resumes one announcement site with that count. Commuting stacks never get here.
 */

/**
 * Resume a top-of-library gather (mill, scry or surveil) after [playerId] picks one of [counts]. The
 * resumer records it in [EffectContext.keywordActionCounts] and re-runs [effect] — only this gather,
 * so the next player whose order matters is asked in turn and the gather finally runs once with
 * every answer.
 */
@Serializable
data class KeywordActionOrderContinuation(
    val effect: Effect,
    val context: EffectContext,
    val playerId: EntityId,
    val counts: List<Int>,
) : AnswerContinuation

/**
 * Resume an activated ability whose cost mills (Millikin, Deranged Assistant) after its controller
 * picks one of [counts]. Nothing has been paid yet; the resumer re-enters the handler with the
 * chosen count in `costPayment.millCount`.
 */
@Serializable
data class ActivateAbilityMillOrderContinuation(
    val action: ActivateAbility,
    val counts: List<Int>,
) : AnswerContinuation

/**
 * Resume a [CostPaymentContinuation] mill payment the payer already agreed to, once they pick one of
 * [counts]; the payment then mills that many.
 */
@Serializable
data class CostPaymentMillOrderContinuation(
    val inner: CostPaymentContinuation,
    val counts: List<Int>,
) : AnswerContinuation

/**
 * Resume a pay-or-suffer mill (Deep Spawn) the payer already agreed to, once they pick one of
 * [counts]; the payment then mills that many.
 */
@Serializable
data class PayOrSufferMillOrderContinuation(
    val inner: PayOrSufferContinuation,
    val counts: List<Int>,
) : AnswerContinuation
