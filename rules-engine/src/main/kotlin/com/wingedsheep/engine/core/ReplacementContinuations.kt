package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.GatheredReplacement
import com.wingedsheep.engine.replacement.PendingGameEvent
import com.wingedsheep.engine.replacement.ReplacementEffectIdentity
import kotlinx.serialization.Serializable

/**
 * Continuation frame for when the player must choose between multiple
 * competing replacement effects that would all apply to the same event
 * (CR 616.1).
 *
 * When multiple replacement effects match the same [PendingGameEvent] and
 * all fall into the same priority group (CR 616.1a-d), the affected player
 * chooses which one to apply first. This frame captures everything needed
 * to resume after the choice.
 *
 * @property pendingEvent The event being replaced
 * @property options The competing replacement effects to choose from
 * @property alreadyApplied Effects already applied in this chain (CR 614.5)
 * @property context The execution context
 */
@Serializable
data class ReplacementChoiceContinuation(
    val pendingEvent: PendingGameEvent,
    val options: List<GatheredReplacement>,
    val alreadyApplied: Set<ReplacementEffectIdentity>,
    val context: EffectContext? = null
) : AnswerContinuation

/**
 * Continuation frame for resuming the original execution context after a
 * replacement chain has fully resolved.
 *
 * When a replacement effect replaces an event with a new effect to execute
 * ([ReplacementOutcome.Replaced]), the new effect is pushed on the execution
 * stack. After it completes, this frame auto-resumes to carry the original
 * context forward so the caller can continue.
 *
 * Its stack position supplies the resumption relationship; it has no question ID.
 */
@Serializable
data object ReplacementResolveContinuation : AutomaticContinuation

/** Apply a modified gain after its replacement-order choice completes. */
@Serializable
data class PerformLifeGainContinuation(val playerId: com.wingedsheep.sdk.model.EntityId, val amount: Int) : AutomaticContinuation

/** Keep a parent replacement chain in force across a nested effect's decisions. */
@Serializable
data class RestoreReplacementChainContinuation(
    val previous: Set<ReplacementEffectIdentity>?,
    val applied: Set<ReplacementEffectIdentity> = emptySet()
) : AutomaticContinuation

/** The later replacement results wait until the current result, including its decisions, finishes. */
@Serializable
data class ReplacementRidersContinuation(val riders: List<com.wingedsheep.engine.replacement.PendingReplacementRider>) : AutomaticContinuation
