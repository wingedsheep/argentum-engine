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

/** The effect move to replay once an entrant's entry question is answered. */
@Serializable
data class EffectReplay(val effect: Effect, val context: EffectContext)

/**
 * The entrant [entityId]'s controller choosing which of several granted entry copies
 * ([com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy]) it applies last (CR 616.1) —
 * two Mystic Reflections bound to different creatures. The answer picks one of [options].
 * [replay] is the effect move to replay with the answer as the entrant's entry copy; null for a
 * resolving permanent spell, which then finishes its entry.
 */
@Serializable
data class GrantedEntryCopyOrderContinuation(
    val entityId: EntityId,
    val options: List<com.wingedsheep.engine.handlers.effects.copy.EntryCopyChoice>,
    val replay: EffectReplay?,
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
