package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after player chooses a mode for a modal spell/ability.
 *
 * When a modal effect (e.g., "Choose one —") is executed, the player is presented
 * with a list of modes. After they choose, we need to execute the chosen mode's
 * effect, potentially after target selection.
 *
 * For "Choose N" modal spells (Commands), the player iteratively picks modes one
 * at a time. [selectedModeIndices] accumulates the picks (in order) and
 * [availableIndices] narrows the options each step. Once
 * `selectedModeIndices.size == chooseCount`, the chosen modes are executed in
 * the order they were selected.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property modes The full list of modes (indexed by original position)
 * @property xValue The X value if applicable
 * @property opponentId The opponent player ID
 * @property chooseCount Total modes to pick (1 for classic modal, 2+ for Commands)
 * @property selectedModeIndices Original mode indices already picked, in order
 * @property availableIndices Original mode indices still offered; null = all
 */
@Serializable
data class ModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable Mode>,
    val xValue: Int? = null,
    val opponentId: EntityId? = null,
    val triggeringEntityId: EntityId? = null,
    val chooseCount: Int = 1,
    val selectedModeIndices: List<Int> = emptyList(),
    val availableIndices: List<Int>? = null
) : ContinuationFrame

/**
 * Resume after player selects targets for a chosen mode of a modal spell.
 *
 * After mode selection, if the chosen mode requires targets, this continuation
 * is pushed while the player selects targets.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property effect The chosen mode's effect to execute
 * @property xValue The X value if applicable
 * @property opponentId The opponent player ID
 */
@Serializable
data class ModalTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effect: Effect,
    val xValue: Int? = null,
    val opponentId: EntityId? = null,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    /** Original modes list for cancelling back to mode selection */
    val modes: List<@Serializable Mode>? = null,
    val triggeringEntityId: EntityId? = null,
    /** For "Choose N" modal spells: modes still queued to execute after this one. */
    val remainingChosenModes: List<@Serializable Mode> = emptyList()
) : ContinuationFrame

/**
 * Resume after player selects a creature to copy for Clone-style effects.
 *
 * When a permanent with EntersAsCopy resolves, the player is asked to choose
 * a creature on the battlefield. This continuation handles the response
 * and completes the permanent's entry to the battlefield.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property castFaceDown Whether the spell was cast face-down
 * @property optional Whether the copy is optional (Clone is optional)
 * @property additionalSubtypes Subtypes to add to the copy (e.g., "Bird" for Mockingbird)
 * @property additionalKeywords Keywords to grant to the copy (e.g., FLYING for Mockingbird)
 */
@Serializable
data class CloneEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val castFaceDown: Boolean,
    val additionalSubtypes: List<String> = emptyList(),
    val additionalKeywords: List<Keyword> = emptyList()
) : ContinuationFrame

/**
 * Resume after player makes an "as enters" choice for a spell being resolved.
 *
 * Handles all choice types (color, creature type, creature on battlefield) via
 * the [choiceType] discriminator. For creature type choices, [creatureTypes] holds
 * the options presented. After storing the chosen value, checks for chained choices
 * (e.g., Riptide Replicator needs both color AND creature type).
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property choiceType What kind of choice was presented
 * @property creatureTypes For CREATURE_TYPE choices, the list of options presented
 */
@Serializable
data class EntersWithChoiceSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val choiceType: com.wingedsheep.sdk.scripting.ChoiceType,
    val creatureTypes: List<String> = emptyList()
) : ContinuationFrame

/**
 * Resume after player makes an "as enters" choice for a land played directly to the battlefield.
 *
 * Unlike [EntersWithChoiceSpellContinuation] (used for spells), the land is already on
 * the battlefield when this continuation fires — it just needs the chosen value stored.
 * After storing, checks for chained choices (e.g., a land with both color and creature type).
 *
 * @property landId The land entity already on the battlefield
 * @property controllerId The player who played the land
 * @property choiceType What kind of choice was presented
 * @property creatureTypes For CREATURE_TYPE choices, the list of options presented
 */
@Serializable
data class EntersWithChoiceLandContinuation(
    override val decisionId: String,
    val landId: EntityId,
    val controllerId: EntityId,
    val choiceType: com.wingedsheep.sdk.scripting.ChoiceType,
    val creatureTypes: List<String> = emptyList()
) : ContinuationFrame

/**
 * Resume after player answers yes/no to "pay life or enter tapped" for a land played directly.
 *
 * Used for shock lands like Steam Vents. The land is already on the battlefield when this
 * continuation fires — if the player pays life, the land stays untapped; if not, it gets tapped.
 *
 * @property landId The land entity already on the battlefield
 * @property controllerId The player who played the land
 * @property lifeCost The amount of life to pay if they choose yes
 * @property fromZone The zone the land was played from (for the ZoneChangeEvent)
 */
@Serializable
data class PayLifeOrEnterTappedLandContinuation(
    override val decisionId: String,
    val landId: EntityId,
    val controllerId: EntityId,
    val lifeCost: Int,
    val fromZone: Zone
) : ContinuationFrame

/**
 * Resume after player answers yes/no to "pay life or enter tapped" for a spell resolving.
 *
 * Used for shock lands entering via effects (e.g., fetched onto the battlefield).
 * The spell is still being resolved — if the player pays life, the permanent enters untapped;
 * if not, it gets tapped.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who controls the spell
 * @property ownerId The owner of the card
 * @property lifeCost The amount of life to pay if they choose yes
 */
@Serializable
data class PayLifeOrEnterTappedSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val lifeCost: Int
) : ContinuationFrame

/**
 * Resume after player reveals cards for enters-with-reveal-counters (Amplify mechanic).
 *
 * When a creature with this replacement effect enters, the controller may reveal
 * cards matching a filter. For each revealed card, N counters are placed on the
 * creature as it enters.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The card's owner
 * @property counterType Counter type description (e.g., "+1/+1")
 * @property countersPerReveal Number of counters per revealed card
 */
@Serializable
data class RevealCountersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val counterType: String,
    val countersPerReveal: Int
) : ContinuationFrame

/**
 * Resume after player chooses a budget modal combination (e.g., Season cycle pawprint modes).
 *
 * The executor pre-computes all valid combinations of modes that fit within the budget
 * Iterative mode selection: each step the player picks one mode (or "Done"),
 * deducting from the remaining budget. Modes execute in the order chosen,
 * giving the player control over sequencing.
 *
 * @property controllerId The player who controls the spell
 * @property sourceId The spell entity
 * @property sourceName Name of the source for event messages
 * @property modes The budget modes (cost + effect)
 * @property remainingBudget How many pawprints are left to spend
 * @property selectedModeIndices Mode indices selected so far, in order of selection
 * @property opponentId The opponent player ID
 */
@Serializable
data class BudgetModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable BudgetMode>,
    val remainingBudget: Int,
    val selectedModeIndices: List<Int> = emptyList(),
    val opponentId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after player chooses a permanent to create a token copy of.
 *
 * Used by CreateTokenCopyOfChosenPermanentEffect — the player selects one
 * artifact/creature/permanent they control, and a token copy is created.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability source
 * @property sourceName Name of the source for display
 */
@Serializable
data class CreateTokenCopyOfChosenContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after a player chooses an action from a list of labeled options.
 *
 * Used by [ChooseActionEffect] — the player is presented with feasible options
 * and picks one. This continuation stores the choices and original context
 * so the chosen effect can be executed with the correct targets.
 *
 * @property choosingPlayerId The player who is making the choice
 * @property controllerId The ability controller (for effect context)
 * @property sourceId The spell/ability source
 * @property sourceName Name of the source for display
 * @property choices The feasible choices presented to the player (indices match the decision options)
 * @property targets Original targets from the effect context (preserved for ContextTarget resolution)
 * @property namedTargets Named targets from the pipeline state
 * @property opponentId The opponent player ID
 * @property triggeringEntityId The entity that triggered the ability
 */
@Serializable
data class ChooseActionContinuation(
    override val decisionId: String,
    val choosingPlayerId: EntityId,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val choices: List<@Serializable EffectChoice>,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    val opponentId: EntityId? = null,
    val triggeringEntityId: EntityId? = null
) : ContinuationFrame
