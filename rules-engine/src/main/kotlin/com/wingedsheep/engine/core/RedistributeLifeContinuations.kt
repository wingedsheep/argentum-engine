package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resume after the chooser picked the life total one player receives in a
 * [com.wingedsheep.sdk.scripting.effects.RedistributeLifeTotalsEffect] (Reverse the Sands).
 *
 * [players] are the life-total owners in prompt order and [originalTotals] their totals when the
 * effect began, index-aligned. [assigned] holds the totals already handed to `players[0 until
 * assigned.size]`; the pending question is for `players[assigned.size]`, and [optionValues] are the
 * totals it offered, index-aligned to the decision's options. [canGain] / [canLose] are the
 * CR 119.7–8 permissions, captured once so every step checks against the same snapshot.
 */
@Serializable
@SerialName("RedistributeLifeTotalsContinuation")
data class RedistributeLifeTotalsContinuation(
    val chooserId: EntityId,
    val players: List<EntityId>,
    val originalTotals: List<Int>,
    val canGain: List<Boolean>,
    val canLose: List<Boolean>,
    val assigned: List<Int>,
    val optionValues: List<Int>,
    val effectContext: EffectContext,
) : AnswerContinuation
