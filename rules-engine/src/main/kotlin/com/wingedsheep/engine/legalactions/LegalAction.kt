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

    // When true, prevents auto-pass whenever this action is available
    val holdPriority: Boolean = false
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
    val blightAmount: Int = 0
)

/**
 * Information about a creature that has +1/+1 counters available for removal.
 */
data class CounterRemovalCreatureData(
    val entityId: EntityId,
    val name: String,
    val availableCounters: Int,
    val imageUri: String? = null
)
