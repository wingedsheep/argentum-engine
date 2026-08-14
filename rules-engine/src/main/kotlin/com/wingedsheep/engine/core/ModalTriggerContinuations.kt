package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Put-on-stack mode selection for a modal **triggered** ability (CR 603.3c).
 *
 * A triggered ability's controller chooses its modes *and* its targets as the ability is put onto
 * the stack — the same moment a modal spell's caster does (CR 601.2b–c). Deferring that choice to
 * resolution would mean the chosen targets never "become the target" of anything on the stack, so
 * ward, "whenever this becomes the target of a spell or ability", and shroud/hexproof checks would
 * all be bypassed.
 *
 * The trigger-time twin of [CastModalModeSelectionContinuation]: each resume captures one mode
 * pick and re-pushes until `chooseCount` modes are picked (or "Done" once `minChooseCount` is
 * reached), then hands off to [TriggerModalTargetSelectionContinuation]. Unlike the cast-time
 * flow, there is nothing to cancel back to — a trigger is already on its way to the stack.
 *
 * @property ability The fully built stack component, held until modes/targets are known.
 * @property outerTargets Targets already chosen for the ability's own (non-modal) requirements.
 * @property chooseCount How many modes to pick. Already resolved: a
 *   [com.wingedsheep.sdk.scripting.effects.ModalEffect.dynamicChooseCount] cap is evaluated once,
 *   before the first pick, so the number can't drift between picks (CR 601.2c via 603.3d).
 */
@Serializable
data class TriggerModalModeSelectionContinuation(
    override val decisionId: String,
    val ability: TriggeredAbilityOnStackComponent,
    val outerTargets: List<ChosenTarget>,
    val outerTargetRequirements: List<@Serializable TargetRequirement>,
    val modes: List<@Serializable Mode>,
    val chooseCount: Int,
    val minChooseCount: Int,
    val allowRepeat: Boolean,
    val offeredIndices: List<Int>,
    val availableIndices: List<Int>? = null,
    val selectedModeIndices: List<Int> = emptyList(),
    val doneOptionOffered: Boolean = false,
    val causedByAttack: Boolean = false,
    /** See [TriggerModalTargetSelectionContinuation.recordChosenModesOnSource]. */
    val recordChosenModesOnSource: Boolean = false,
    /** See [TriggerModalTargetSelectionContinuation.recordChosenModesThisTurn]. */
    val recordChosenModesThisTurn: Boolean = false
) : ContinuationFrame

/**
 * Put-on-stack per-mode target selection for a modal triggered ability (CR 603.3c).
 *
 * Pushed once mode selection completes and some chosen mode has target requirements. One resume
 * per targeting mode; [resolvedModeTargets] stays aligned 1:1 with [chosenModeIndices] (modes with
 * no requirements get an empty slot). When the last mode is resolved the ability finally goes on
 * the stack with its modal fields populated, emitting the `BecomesTargetEvent`s that ward keys on.
 */
@Serializable
data class TriggerModalTargetSelectionContinuation(
    override val decisionId: String,
    val ability: TriggeredAbilityOnStackComponent,
    val outerTargets: List<ChosenTarget>,
    val outerTargetRequirements: List<@Serializable TargetRequirement>,
    val modes: List<@Serializable Mode>,
    val chosenModeIndices: List<Int>,
    val resolvedModeTargets: List<List<ChosenTarget>>,
    val currentOrdinal: Int,
    val causedByAttack: Boolean = false,
    /**
     * "Choose one that hasn't been chosen" (Gandalf the Grey): record every chosen mode in the
     * source's [com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent] once
     * the ability is on the stack, so later triggers of the same object never offer it again.
     */
    val recordChosenModesOnSource: Boolean = false,
    /**
     * "…that hasn't been chosen this turn" (Breeches, Eager Pillager): the turn-scoped sibling of
     * [recordChosenModesOnSource], written to
     * [com.wingedsheep.engine.state.components.battlefield.ChosenModesThisTurnComponent].
     */
    val recordChosenModesThisTurn: Boolean = false
) : ContinuationFrame
