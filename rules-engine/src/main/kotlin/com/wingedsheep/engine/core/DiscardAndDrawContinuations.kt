package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
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
 * @property drawingPlayerId The player who was drawing (whose draw was replaced)
 * @property remainingDraws Number of draws left to process after the bounce pipeline
 * @property isDrawStep Whether this is from the draw step (vs spell/ability draws)
 */
@Serializable
data class DrawReplacementRemainingDrawsContinuation(
    override val decisionId: String = "remaining-draws",
    val drawingPlayerId: EntityId,
    val remainingDraws: Int,
    val isDrawStep: Boolean
) : ContinuationFrame

/**
 * Continuation for prompting the player to activate a "prompt on draw" ability
 * (e.g., Words of Wind) before a draw happens.
 *
 * After the player answers yes/no, the handler pays mana, creates a replacement
 * shield, and then proceeds with the draw.
 *
 * @property drawingPlayerId The player who is about to draw
 * @property sourceId The permanent with the promptOnDraw ability
 * @property sourceName Name of the source for display
 * @property abilityEffect The effect to execute on activation (creates a shield)
 * @property manaCost The mana cost string for the activation (e.g., "{1}")
 * @property drawCount Number of cards to draw after activation
 * @property isDrawStep Whether this is from the draw step (vs spell/ability draws)
 */
@Serializable
data class DrawReplacementActivationContinuation(
    override val decisionId: String,
    val drawingPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val abilityEffect: Effect,
    val manaCost: String,
    val drawCount: Int,
    val isDrawStep: Boolean,
    val drawnCardsSoFar: List<EntityId> = emptyList(),
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val declinedSourceIds: List<EntityId> = emptyList()
) : ContinuationFrame

/**
 * Resume after target selection for a "prompt on draw" ability that requires targeting
 * (e.g., Words of War). After the player paid mana and selected targets, we create
 * the replacement shield with the chosen targets, then proceed with draws.
 */
@Serializable
data class DrawReplacementTargetContinuation(
    override val decisionId: String,
    val drawingPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val abilityEffect: Effect,
    val drawCount: Int,
    val isDrawStep: Boolean,
    val drawnCardsSoFar: List<EntityId> = emptyList(),
    val targetRequirements: List<TargetRequirement> = emptyList()
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
    val drawnCardsSoFar: List<EntityId> = emptyList()
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
 * Resume the search step of a typecycling action after cycling triggers have resolved.
 *
 * Same issue as CycleDrawContinuation but for typecycling, which searches the library
 * instead of drawing.
 *
 * @property playerId The player who typecycled
 * @property cardId The card that was typecycled (source for search effect)
 * @property subtypeFilter The creature subtype to search for
 */
@Serializable
data class TypecycleSearchContinuation(
    override val decisionId: String = "typecycle-search",
    val playerId: EntityId,
    val cardId: EntityId,
    val subtypeFilter: String
) : ContinuationFrame

/**
 * Continuation for Read the Runes effect.
 * Tracks the iterative "for each card drawn, discard a card unless you sacrifice a permanent" choices.
 *
 * @property playerId The player making choices
 * @property sourceId The source spell
 * @property sourceName Name of the source spell
 * @property remainingChoices How many more discard-or-sacrifice choices remain
 * @property phase Whether we're awaiting a sacrifice selection or a discard selection
 */
@Serializable
data class ReadTheRunesContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val remainingChoices: Int,
    val phase: ReadTheRunesPhase
) : ContinuationFrame

/**
 * Phase discriminator for ReadTheRunesContinuation.
 */
@Serializable
enum class ReadTheRunesPhase {
    /** Player is choosing whether to sacrifice a permanent (select 0 to discard instead) */
    SACRIFICE_CHOICE,
    /** Player is choosing a card to discard */
    DISCARD_CHOICE
}
