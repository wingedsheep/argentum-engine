package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after putting a card from hand onto the battlefield after card selection.
 *
 * @property playerId The player selecting a card from their hand
 * @property entersTapped Whether the card enters the battlefield tapped
 * @property sourceId The source entity that triggered this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class PutFromHandContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val entersTapped: Boolean,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after a player chooses a number for secret bidding effects (Menacing Ogre).
 *
 * Each player secretly chooses a number. After all players have chosen,
 * execute outcome effects per matching bidder (xValue = bid, controllerId = bidder).
 *
 * @property sourceId The creature that entered the battlefield
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the source creature
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenNumbers Numbers chosen so far by each player
 * @property highestBidderEffect Effect executed per highest bidder
 * @property lowestBidderEffect Effect executed per lowest non-zero bidder
 * @property tiedBidderEffect Effect executed per bidder when all non-zero bids are equal
 */
@Serializable
data class SecretBidContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val chosenNumbers: Map<EntityId, Int>,
    val highestBidderEffect: Effect?,
    val lowestBidderEffect: Effect?,
    val tiedBidderEffect: Effect?
) : ContinuationFrame

/**
 * Resume after player has distributed counters from a source creature to other creatures.
 *
 * Used for effects like Forgotten Ancient where the player moves counters from
 * the source creature onto other creatures. The response contains the counter distribution.
 *
 * @property sourceId The creature the counters are being moved from
 * @property controllerId The player who controls the effect
 * @property counterType The type of counter being moved (e.g., "+1/+1")
 */
@Serializable
data class DistributeCountersContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val controllerId: EntityId,
    val counterType: String
) : ContinuationFrame

/**
 * Resume after the controller picks the permanents and/or players that should
 * each receive another counter of each kind already on them (Proliferate).
 *
 * @property controllerId The player resolving the proliferate
 * @property eligibleEntities The permanents/players that had at least one counter
 *                            when the decision was offered. Used to discard stale
 *                            selections and to defend against the response naming
 *                            an entity that had no counters at decision time.
 */
@Serializable
data class ProliferateContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val eligibleEntities: List<EntityId>
) : ContinuationFrame

/**
 * Resume creating a copy of a triggered ability after the player selects new targets.
 *
 * Used by Kirol-style effects ("Copy target triggered ability you control. You may choose
 * new targets for the copy."). The controller re-chooses targets, then the copy is cloned
 * from the source ability's TriggeredAbilityOnStackComponent and pushed onto the stack.
 *
 * @property abilityEntityId The triggered ability being copied (on the stack)
 * @property controllerId The player controlling the copy (also the target-chooser)
 * @property targetRequirements Target requirements inherited from the source ability
 */
@Serializable
data class CopyTriggeredAbilityTargetContinuation(
    override val decisionId: String,
    val abilityEntityId: EntityId,
    val controllerId: EntityId,
    val targetRequirements: List<TargetRequirement>
) : ContinuationFrame

/**
 * Resume creating Storm copies after the player selects targets for a copy.
 *
 * When a Storm spell has targets, we pause to ask for target selection for each copy.
 * After the player chooses targets, we create the copy and (if more copies remain)
 * prompt for the next copy's targets.
 *
 * @property remainingCopies Number of copies still to create (including the one being targeted)
 * @property spellEffect The effect of the original spell to copy
 * @property spellTargetRequirements Target requirements for each copy
 * @property spellName Name of the original spell
 * @property controllerId The player who controls the copies
 * @property sourceId The source spell entity ID
 */
@Serializable
data class StormCopyTargetContinuation(
    override val decisionId: String,
    val remainingCopies: Int,
    val spellEffect: Effect,
    val spellTargetRequirements: List<TargetRequirement>,
    val spellName: String,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val totalCopies: Int = remainingCopies  // Original total copies (defaults to remainingCopies for backward compat)
) : ContinuationFrame

/**
 * Resume Storm modal target selection (rule 702.40a + 707.7c).
 *
 * Modal Storm spells keep their chosen modes on every copy (700.2g — modes are
 * decided once by the caster), but the copy controller may re-choose targets
 * for each mode per 702.40a. This continuation iterates through chosen modes
 * for a single copy, pausing once per mode that has target requirements and
 * advancing without prompt for no-target modes. When all modes are collected
 * for a copy, the copy is pushed on the stack and the loop restarts for the
 * next copy.
 *
 * @property remainingCopies Copies still to create (including the one being built)
 * @property totalCopies Original copy count (for copyIndex labeling)
 * @property spellName Display name of the original spell
 * @property controllerId Player who controls the copies and picks targets
 * @property sourceId Original spell's entity ID on the stack
 * @property chosenModes Inherited chosen mode indices — fixed for every copy
 * @property modeTargetRequirements Inherited per-mode target requirements
 * @property accumulatedOrdinalTargets Targets chosen so far for the current copy,
 *   aligned 1:1 with [chosenModes] positions 0..[currentOrdinal]-1
 * @property currentOrdinal Index into [chosenModes] whose targets we are collecting
 */
@Serializable
data class StormCopyModalTargetContinuation(
    override val decisionId: String,
    val remainingCopies: Int,
    val totalCopies: Int,
    val spellName: String,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val chosenModes: List<Int>,
    val modeTargetRequirements: Map<Int, List<TargetRequirement>>,
    val accumulatedOrdinalTargets: List<List<ChosenTarget>>,
    val currentOrdinal: Int
) : ContinuationFrame

/**
 * Resume after Meddle's controller chooses a new creature target for a spell.
 *
 * @property spellEntityId The spell whose target is being changed
 * @property sourceId The source of the change-target effect (Meddle)
 */
@Serializable
data class ChangeSpellTargetContinuation(
    override val decisionId: String,
    val spellEntityId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after player selects which legendary permanent to keep (legend rule 704.5j).
 *
 * The player chose one permanent to keep; all others with the same name are put into the graveyard.
 *
 * @property playerId The player who controls the duplicates
 * @property allDuplicates All entity IDs of the legendary permanents with the same name
 */
@Serializable
data class LegendRuleContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val allDuplicates: List<EntityId>
) : ContinuationFrame
