package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId

/**
 * Engine-level representation of a legal action a player can take.
 *
 * This is the rules engine's output — it carries structured data about the action,
 * NOT presentation/DTO concerns. The game-server enriches this into LegalActionInfo
 * for the client.
 */
data class LegalAction(
    val action: GameAction,
    val actionType: String,
    val description: String,
    val affordable: Boolean = true,

    // Targeting
    val validTargets: List<EntityId>? = null,
    val requiresTargets: Boolean = false,
    val targetCount: Int = 1,
    val minTargets: Int = targetCount,
    val targetDescription: String? = null,
    val targetRequirements: List<TargetInfo>? = null,

    // Combat
    val validAttackers: List<EntityId>? = null,
    val mandatoryAttackers: List<EntityId>? = null,
    val validAttackTargets: List<EntityId>? = null,
    val validBlockers: List<EntityId>? = null,
    val blockerMaxBlockCounts: Map<EntityId, Int>? = null,
    val mandatoryBlockerAssignments: Map<EntityId, List<EntityId>>? = null,

    // Costs
    val manaCostString: String? = null,
    val hasXCost: Boolean = false,
    val maxAffordableX: Int? = null,
    val minX: Int = 0,
    val additionalCostInfo: AdditionalCostData? = null,

    // Convoke / Delve
    val hasConvoke: Boolean = false,
    val convokeCreatures: List<ConvokeCreatureData>? = null,
    val hasDelve: Boolean = false,
    val delveCards: List<DelveCardData>? = null,
    val minDelveNeeded: Int? = null,

    // Mana abilities
    val isManaAbility: Boolean = false,
    val requiresManaColorChoice: Boolean = false,

    // Auto-tap (engine computes the solution; server enriches with full mana source info)
    val autoTapPreview: List<EntityId>? = null,

    // Damage distribution
    val requiresDamageDistribution: Boolean = false,
    val totalDamageToDistribute: Int? = null,
    val minDamagePerTarget: Int? = null,

    // Source zone
    val sourceZone: String? = null,

    // Crew
    val hasCrew: Boolean = false,
    val crewPower: Int? = null,
    val crewCreatures: List<CrewCreatureData>? = null,

    // Repetition
    val maxRepeatableActivations: Int? = null,

    // Forage (graveyard casting with forage cost, applies finality counter)
    val requiresForage: Boolean = false,

    // Additional life cost (e.g., Festival of Embers graveyard casting)
    val additionalLifeCost: Int = 0,

    // Modal cast-time enumeration payload (rules 700.2). Only populated when
    // [actionType] is "CastSpellModal" and the spell has [ModalEffect.chooseCount] > 1.
    // The client uses this to drive the cast-time mode/target decision loop.
    val modalEnumeration: ModalLegalEnumeration? = null,

    // When true, prevents auto-pass whenever this action is available
    val holdPriority: Boolean = false
)

/**
 * Per-mode enumeration data for a choose-N modal spell.
 *
 * Carries everything the client needs to offer mode selection at cast time:
 * which modes exist, which are unavailable (700.2a — can't target, can't pay
 * additional cost), and the per-mode cost deltas and target requirements.
 *
 * Modes marked `available = false` must not be pickable.
 *
 * @property chooseCount Maximum number of modes the player may pick.
 * @property minChooseCount Minimum number of modes required (< chooseCount for
 *           "choose one or both" / "choose one or more").
 * @property allowRepeat When true, the same mode index may be chosen more than
 *           once (rules 700.2d — Escalate / Spree).
 * @property modes One entry per declared mode, in printed order.
 * @property unavailableIndices Convenience list of mode indices flagged
 *           unavailable. Equal to `modes.filterNot { it.available }.map { it.index }`.
 */
data class ModalLegalEnumeration(
    val chooseCount: Int,
    val minChooseCount: Int,
    val allowRepeat: Boolean,
    val modes: List<ModalEnumerationMode>,
    val unavailableIndices: List<Int>
)

/**
 * A single mode offered for cast-time selection on a choose-N modal spell.
 *
 * @property index Printed mode index (0-based).
 * @property description Rendered mode text, e.g. "Target creature gets +3/+3 until end of turn".
 * @property available False when the mode has no legal targets (700.2a) or when
 *           the caster cannot pay this mode's [additionalManaCost] / additional costs.
 * @property additionalManaCost Extra mana this mode adds to the spell's cost, if any
 *           (rendered string form — pure-add deltas such as "{B}").
 * @property additionalCostInfo Per-mode non-mana additional cost info when the mode
 *           overrides the card-level [AdditionalCost]s (rules 700.2h). Null otherwise.
 * @property targetRequirements Target slots for this mode; empty if the mode has none.
 */
data class ModalEnumerationMode(
    val index: Int,
    val description: String,
    val available: Boolean,
    val additionalManaCost: String? = null,
    val additionalCostInfo: AdditionalCostData? = null,
    val targetRequirements: List<TargetInfo> = emptyList()
)

/**
 * Target requirement info for a single target slot.
 */
data class TargetInfo(
    val index: Int,
    val description: String,
    val minTargets: Int,
    val maxTargets: Int,
    val validTargets: List<EntityId>,
    val targetZone: String? = null
)

/**
 * Information about a creature that can be tapped for Convoke.
 */
data class ConvokeCreatureData(
    val entityId: EntityId,
    val name: String,
    val colors: Set<Color>
)

/**
 * Information about a card in graveyard that can be exiled for Delve.
 */
data class DelveCardData(
    val entityId: EntityId,
    val name: String,
    val imageUri: String? = null
)

/**
 * Information about a creature that can be tapped to crew a Vehicle.
 */
data class CrewCreatureData(
    val entityId: EntityId,
    val name: String,
    val power: Int
)

/**
 * Information about additional costs for a spell or ability.
 */
data class AdditionalCostData(
    val description: String,
    val costType: String,
    val validSacrificeTargets: List<EntityId> = emptyList(),
    val sacrificeCount: Int = 1,
    val validTapTargets: List<EntityId> = emptyList(),
    val tapCount: Int = 0,
    val validDiscardTargets: List<EntityId> = emptyList(),
    val discardCount: Int = 0,
    val validBounceTargets: List<EntityId> = emptyList(),
    val bounceCount: Int = 0,
    val validExileTargets: List<EntityId> = emptyList(),
    val exileMinCount: Int = 0,
    val exileMaxCount: Int = 0,
    val validBeholdTargets: List<EntityId> = emptyList(),
    val beholdCount: Int = 0,
    val counterRemovalCreatures: List<CounterRemovalCreatureData> = emptyList(),
    val validBlightTargets: List<EntityId> = emptyList(),
    val blightAmount: Int = 0,
    /**
     * For [AdditionalCost.RemoveCountersFromYourCreatures]: total counters to remove
     * across all creatures you control (any counter types qualify).
     */
    val distributedCounterRemovalTotal: Int = 0
)

/**
 * Information about a creature that has +1/+1 counters available for removal.
 */
data class CounterRemovalCreatureData(
    val entityId: EntityId,
    val name: String,
    val availableCounters: Int,
    /**
     * Counter-type breakdown so the UI can offer a per-type +/- when a creature
     * carries more than one type. Keyed by the canonical counter-type name
     * (e.g. "+1/+1", "-1/-1", "stun"). Sum of values equals [availableCounters].
     */
    val availableCountersByType: Map<String, Int> = emptyMap(),
    val imageUri: String? = null
)
