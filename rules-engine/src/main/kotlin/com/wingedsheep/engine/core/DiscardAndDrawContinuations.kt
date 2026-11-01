package com.wingedsheep.engine.core

import com.wingedsheep.engine.replacement.ReplacementEffectIdentity
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * Resume after player selects cards to discard for hand size (cleanup step).
 *
 * This is separate from DiscardContinuation to distinguish hand size
 * discards from spell/ability-caused discards.
 *
 * @property playerId The player who is discarding
 */
@Serializable
data class HandSizeDiscardContinuation(
    val playerId: EntityId
) : AnswerContinuation

/**
 * Resume after a player selects a card to discard for "each player discards or lose life" effects.
 *
 * Used for Strongarm Tactics: "Each player discards a card. Then each player who didn't
 * discard a creature card this way loses 4 life."
 *
 * Tracks which players have already discarded and whether they discarded a creature,
 * then applies life loss to those who didn't.
 *
 * @property sourceId The spell/ability causing the effect
 * @property sourceName Name for display
 * @property controllerId The controller of the effect
 * @property currentPlayerId The player whose selection we are waiting for
 * @property remainingPlayers Players who still need to make their selection after current (APNAP order)
 * @property discardedCreature Map of player ID to whether they discarded a creature card
 * @property lifeLoss Life lost by each player who didn't discard a creature card
 */
@Serializable
data class EachPlayerDiscardsOrLoseLifeContinuation(
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val discardedCreature: Map<EntityId, Boolean>,
    val lifeLoss: Int,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume after a player chooses how many cards to draw for DrawUpToEffect.
 *
 * @property playerId The player who is drawing
 * @property sourceId The spell/ability that caused the effect
 * @property sourceName Name of the source for display
 * @property maxCards Maximum cards offered (capped by library size)
 */
@Serializable
data class DrawUpToContinuation(
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val maxCards: Int,
    val originalMaxCards: Int = 0,
    val storeNotDrawnAs: String? = null,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
    /** The label the creating effect ran under; the draw its resume runs keeps it. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val programOrigin: com.wingedsheep.engine.core.ProgramOrigin? = null,
) : AnswerContinuation

/**
 * Resume remaining card draws after a bounce pipeline completes.
 *
 * When a draw is replaced by a bounce (Words of Wind), the pipeline handles the
 * "each player returns a permanent" part. This continuation tracks remaining draws
 * so execution can resume drawing after the pipeline finishes.
 *
 * This is CR 614.11a in continuation form: "if an effect replaces a draw within a
 * sequence of card draws, all actions required by the replacement are completed, if
 * possible, before resuming the sequence" — the frame sits below the replacement's own
 * work on the stack, so the sequence resumes only once that work is done.
 *
 * @property drawingPlayerId The player who was drawing (whose draw was replaced)
 * @property remainingDraws Number of draws left to process after the bounce pipeline
 * @property isDrawStep Whether this is from the draw step (vs spell/ability draws)
 * @property announcementApplied Whether the draw instruction's announcement-level
 *     replacements (CR 121.2a — `ModifyDrawAmount`) have already been applied to this
 *     instruction. True whenever these draws are the tail of an instruction that was
 *     announced before it paused; re-announcing on resume would apply the same
 *     `ModifyDrawAmount` a second time.
 */
@Serializable
data class DrawReplacementRemainingDrawsContinuation(
    val drawingPlayerId: EntityId,
    val remainingDraws: Int,
    val isDrawStep: Boolean,
    val announcementApplied: Boolean = false,
    /** The card whose cycling ability caused these draws (`EventPattern.DrawEvent.cyclingOf`). */
    val cycledCardId: EntityId? = null
) : AutomaticContinuation

/**
 * Resume after the player answers yes/no for an optional static draw replacement effect
 * (e.g., Parallel Thoughts: "you may instead put the top card of the exiled pile into your hand").
 *
 * @property drawingPlayerId The player who is about to draw
 * @property sourceId The permanent with the replacement effect
 * @property sourceName Name of the source for display
 * @property replacementEffect The effect to execute if the player says yes
 * @property drawCount Number of draws remaining (including this one)
 * @property isDrawStep Whether this is from the draw step (vs spell/ability draws)
 * @property drawnCardsSoFar Cards already drawn before this replacement was offered
 * @property declinedIdentity The replacement effect identity to stamp on the chain
 *     when the player says NO, so this specific replacement won't re-prompt for
 *     the current draw but other optional replacements still can.
 */
@Serializable
data class StaticDrawReplacementContinuation(
    val drawingPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val replacementEffect: Effect,
    val drawCount: Int,
    val isDrawStep: Boolean,
    val drawnCardsSoFar: List<EntityId> = emptyList(),
    val declinedIdentity: ReplacementEffectIdentity? = null,
    /** Prior applications and declines for this individual draw, retained across pauses. */
    val alreadyApplied: Set<ReplacementEffectIdentity> = emptySet(),
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
    /** The card whose cycling ability caused this draw (`EventPattern.DrawEvent.cyclingOf`). */
    val cycledCardId: EntityId? = null,
) : AnswerContinuation

/**
 * The rest of a shared-turn team's draw step (CR 805.4b): [remainingTeammates]' draws, then
 * [activePlayer]'s. Pushed beneath each teammate's draw so that a pause in it — a miracle reveal, an
 * optional draw replacement — still leads to the rest of the team drawing; popped when the draw
 * completes without one.
 */
@Serializable
data class DrawStepTeamDrawsContinuation(
    val activePlayer: EntityId,
    val remainingTeammates: List<EntityId>
) : AutomaticContinuation

/**
 * The rest of the draw step's draws by players other than the active one — "You draw a card during
 * each opponent's draw step" ([com.wingedsheep.sdk.scripting.DrawDuringOtherDrawSteps]): one card
 * per entry of [remainingDrawers], then priority to [activePlayer]. Pushed beneath each draw-step
 * draw that precedes them so a pause in it still leads to these draws.
 */
@Serializable
data class DrawStepOtherDrawsContinuation(
    val activePlayer: EntityId,
    val remainingDrawers: List<EntityId>
) : AutomaticContinuation

/**
 * Resume after a player answers whether to reveal a card with miracle as they draw it (CR 702.94a:
 * "you may reveal this card from your hand as you draw it if it's the first card you've drawn this
 * turn").
 *
 * Asked between cards: the rest of the draw instruction waits beneath this question in a
 * [DrawReplacementRemainingDrawsContinuation]. A [PrivateQuestion] — it exists only because the
 * card has miracle, so its owner's opponents must not learn it was asked.
 *
 * @property playerId The player who drew the card and is asked
 * @property cardId The card with miracle, now in that player's hand
 * @property cost The miracle cost the card had when drawn, for the prompt and the trigger's text
 */
@Serializable
data class MiracleRevealContinuation(
    val playerId: EntityId,
    val cardId: EntityId,
    val cost: com.wingedsheep.sdk.core.ManaCost
) : AnswerContinuation, PrivateQuestion

/**
 * Resume after a player answers whether [sourceId]'s optional first-draw reveal shows the card they
 * just drew ("You may reveal the first card you draw each turn as you draw it", God-Eternal Kefnet —
 * [com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn.optional]).
 *
 * Asked between cards, like [MiracleRevealContinuation]: the rest of the draw instruction waits
 * beneath it in a [DrawReplacementRemainingDrawsContinuation]. Not a [PrivateQuestion] — the source
 * is on the battlefield, so the table already knows the question is asked on every first draw; the
 * drawn card is named only in the asked player's prompt, and the shared log names just the source.
 *
 * @property playerId The player who drew the card and is asked
 * @property cardId The card just drawn, now in that player's hand
 * @property sourceId The permanent whose reveal this question is about
 * @property laterSources Further optional reveal sources still to ask about, in order
 * @property miracleCost The drawn card's miracle cost when its miracle reveal is still to be asked
 *     after these, or null
 */
@Serializable
data class OptionalFirstDrawRevealContinuation(
    val playerId: EntityId,
    val cardId: EntityId,
    val sourceId: EntityId,
    val laterSources: List<EntityId> = emptyList(),
    val miracleCost: com.wingedsheep.sdk.core.ManaCost? = null
) : AnswerContinuation

/**
 * Resume the draw step of a cycling action after cycling triggers have resolved.
 *
 * Nothing pushes this frame any more: cycling triggers wait in the settle boundary's queue, so
 * the cycling draw happens inline. It stays registered so a saved game that paused with the frame
 * on its stack still resumes.
 *
 * @property playerId The player who cycled and needs to draw
 */
@Serializable
data class CycleDrawContinuation(
    val playerId: EntityId
) : AutomaticContinuation

/**
 * Resume a cycling action after the player announces X for an `{X}` cycling cost (CR 107.3a) —
 * Webstrike Elite's "Cycling {X}{G}{G}".
 *
 * The legal-actions submission path sends a bare [CycleCard] with `xValue == null`; the handler
 * raises a ChooseNumberDecision and stores this frame. On resume the handler is re-entered with
 * `xValue` bound, and the cost is paid for that amount. Mirrors
 * [ActivateAbilityChooseManaXContinuation] — paying the mana is automatic, so there is no follow-up
 * decision.
 *
 * @property action The original [CycleCard] (its `xValue` is still null on the stored copy).
 */
@Serializable
data class CycleCardChooseXContinuation(
    val action: CycleCard
) : AnswerContinuation

/**
 * Resume the search step of a typecycling action after cycling triggers have resolved.
 *
 * Like [CycleDrawContinuation], nothing pushes this frame any more; it stays registered so a
 * saved game that paused with it on its stack still resumes.
 *
 * @property playerId The player who typecycled
 * @property cardId The card that was typecycled (source for search effect)
 * @property searchFilter The filter describing which library cards are valid search targets
 *                       (e.g., cards with subtype "Forest", or any basic land card)
 * @property abilityDescription Human-readable name of the cycling variant (e.g., "Forestcycling",
 *                              "Basic landcycling") used in decision prompts and logs
 */
@Serializable
data class TypecycleSearchContinuation(
    val playerId: EntityId,
    val cardId: EntityId,
    val searchFilter: GameObjectFilter,
    val abilityDescription: String
) : AutomaticContinuation

