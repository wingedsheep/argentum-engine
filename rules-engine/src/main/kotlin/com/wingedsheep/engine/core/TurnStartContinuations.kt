package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

@Serializable
data class TurnStartFollowUp(val effect: Effect, val context: EffectContext)

/** One turn occurrence, with the alternatives that were legal before it began. */
@Serializable
data class TurnStartReplacementContinuation(
    val nextPlayerId: EntityId,
    val options: List<TurnStartFollowUp>,
    val followUps: List<TurnStartFollowUp>,
    /** Whether this occurrence is an extra turn the bypasses inserted, so beginning it is counted. */
    val extraTurn: Boolean = false,
) : AnswerContinuation

/** Remaining first actions in the next actual turn, followed by that turn's untap step. */
@Serializable
data class FinishTurnStartContinuation(
    val activePlayerId: EntityId,
    val followUps: List<TurnStartFollowUp>,
) : AutomaticContinuation
