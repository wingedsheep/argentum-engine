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
    override val decisionId: String,
    val playerId: EntityId
) : ContinuationFrame

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
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val discardedCreature: Map<EntityId, Boolean>,
    val lifeLoss: Int
) : ContinuationFrame

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
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val maxCards: Int,
    val originalMaxCards: Int = 0,
    val storeNotDrawnAs: String? = null
) : ContinuationFrame

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
    override val decisionId: String = "remaining-draws",
    val drawingPlayerId: EntityId,
    val remainingDraws: Int,
    val isDrawStep: Boolean,
    val announcementApplied: Boolean = false
) : ContinuationFrame

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
    override val decisionId: String,
    val drawingPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val replacementEffect: Effect,
    val drawCount: Int,
    val isDrawStep: Boolean,
    val drawnCardsSoFar: List<EntityId> = emptyList(),
    val declinedIdentity: ReplacementEffectIdentity? = null
) : ContinuationFrame

/**
 * Resume the draw step of a cycling action after cycling triggers have resolved.
 *
 * When cycling triggers (e.g., Choking Tethers' "you may tap target creature") pause
 * for player input, the CycleCardHandler returns early before reaching the draw step.
 * This continuation ensures the draw happens after triggers resolve.
 *
 * @property playerId The player who cycled and needs to draw
 */
@Serializable
data class CycleDrawContinuation(
    override val decisionId: String = "cycle-draw",
    val playerId: EntityId
) : ContinuationFrame

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
    override val decisionId: String,
    val action: CycleCard
) : ContinuationFrame

/**
 * Resume the search step of a typecycling action after cycling triggers have resolved.
 *
 * Same issue as CycleDrawContinuation but for typecycling, which searches the library
 * instead of drawing.
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
    override val decisionId: String = "typecycle-search",
    val playerId: EntityId,
    val cardId: EntityId,
    val searchFilter: GameObjectFilter,
    val abilityDescription: String
) : ContinuationFrame

