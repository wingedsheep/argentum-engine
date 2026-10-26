package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EffectCopyEntryContinuation(
    val effect: Effect,
    val context: EffectContext,
    val entityId: EntityId,
    val replacement: EntersAsCopy,
) : AnswerContinuation

/**
 * The work to replay once an entrant's controller has ordered its granted entry copies — work that
 * put the entrant onto the battlefield before it could ask
 * ([com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy.askIfUnasked]).
 */
@Serializable
sealed interface EntryCopyReplay

/** An effect: an effect move, a token, a return from linked exile. */
@Serializable
@SerialName("Effect")
data class EffectReplay(val effect: Effect, val context: EffectContext) : EntryCopyReplay

/** A resumed answer ([response] to [suspension]) whose resumer put the entrant onto the battlefield. */
@Serializable
@SerialName("Answer")
data class AnswerReplay(val suspension: Suspension, val response: DecisionResponse) : EntryCopyReplay

/** A land play (a land creature, Dryad Arbor). */
@Serializable
@SerialName("PlayLand")
data class PlayLandReplay(val action: PlayLand) : EntryCopyReplay

/**
 * The entrant [entityId]'s controller choosing which of several granted entry copies
 * ([com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy]) it applies last (CR 616.1) —
 * two Mystic Reflections bound to different creatures. The answer picks one of [options].
 * [replay] is the work to replay with the answer waiting for the entrant; null for a resolving
 * permanent spell, which then finishes its entry.
 */
@Serializable
data class GrantedEntryCopyOrderContinuation(
    val entityId: EntityId,
    val options: List<com.wingedsheep.engine.handlers.effects.copy.EntryCopyChoice>,
    val replay: EntryCopyReplay?,
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
