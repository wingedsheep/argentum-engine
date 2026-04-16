package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Mode
import kotlinx.serialization.Serializable

/**
 * Cast-time mode selection for a choose-N modal spell (rule 700.2).
 *
 * Pushed by [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler.execute] when
 * the player submits a `CastSpell` action for a modal spell with `chooseCount > 1` and no
 * `chosenModes` yet. Each resume captures one mode pick; the continuation is re-pushed
 * until the player has chosen `chooseCount` modes (or picked "Done" once
 * `selectedModeIndices.size >= minChooseCount`).
 *
 * Once mode selection completes the resumer transitions to
 * [CastModalTargetSelectionContinuation] (if any chosen mode requires targets) or
 * re-enters `CastSpellHandler.execute` with the finalized action.
 *
 * @property baseCastAction Original `CastSpell` the player submitted. Preserved so the
 *   finalized cast carries the same paymentStrategy, xValue, castFaceDown, etc.
 * @property modes Full declared mode list (indexed by original printed position).
 * @property offeredIndices Original mode indices presented in the current decision's
 *   options, in the order they were shown. `OptionChosenResponse.optionIndex` is an
 *   index into this list (plus one trailing "Done" slot when [doneOptionOffered] is true).
 * @property availableIndices Remaining original mode indices still eligible after
 *   prior picks (respects 700.2a target-legality filtering). `null` when
 *   [allowRepeat] is true since no narrowing is needed.
 * @property selectedModeIndices Original mode indices already picked, in selection order.
 * @property doneOptionOffered True when the current decision exposes a "Done" option,
 *   permitted once `selectedModeIndices.size >= minChooseCount`.
 */
@Serializable
data class CastModalModeSelectionContinuation(
    override val decisionId: String,
    val cardId: EntityId,
    val casterId: EntityId,
    val baseCastAction: CastSpell,
    val modes: List<@Serializable Mode>,
    val chooseCount: Int,
    val minChooseCount: Int,
    val allowRepeat: Boolean,
    val offeredIndices: List<Int>,
    val availableIndices: List<Int>? = null,
    val selectedModeIndices: List<Int> = emptyList(),
    val doneOptionOffered: Boolean = false
) : ContinuationFrame

/**
 * Cast-time per-mode target selection for a choose-N modal spell (rule 601.2c).
 *
 * Pushed after mode selection completes when any chosen mode has non-empty
 * `targetRequirements`. The resumer captures one mode's targets per resume, then
 * advances [currentOrdinal] until every chosen mode with targets has been resolved.
 *
 * Modes without target requirements are skipped when advancing; their slot in
 * [resolvedModeTargets] is `emptyList()`.
 *
 * @property chosenModeIndices The original mode indices, in the order they were picked.
 * @property resolvedModeTargets Targets collected so far, aligned 1:1 with
 *   [chosenModeIndices]. Entries beyond [currentOrdinal] are filled in by the resumer.
 * @property currentOrdinal Index into [chosenModeIndices] of the mode whose targets
 *   the current decision is collecting.
 */
@Serializable
data class CastModalTargetSelectionContinuation(
    override val decisionId: String,
    val cardId: EntityId,
    val casterId: EntityId,
    val baseCastAction: CastSpell,
    val modes: List<@Serializable Mode>,
    val chosenModeIndices: List<Int>,
    val resolvedModeTargets: List<List<ChosenTarget>>,
    val currentOrdinal: Int
) : ContinuationFrame
