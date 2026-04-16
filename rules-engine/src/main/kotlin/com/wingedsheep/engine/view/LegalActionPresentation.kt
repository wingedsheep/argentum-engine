package com.wingedsheep.engine.view

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
data class ManaSourceInfo(
    val entityId: EntityId,
    val name: String,
    val imageUri: String? = null,
    val producesColors: List<String> = emptyList(),
    val producesColorless: Boolean = false,
    val manaAmount: Int = 1
)

@Serializable
data class LegalActionTargetInfo(
    val index: Int,
    val description: String,
    val minTargets: Int,
    val maxTargets: Int,
    val validTargets: List<EntityId>,
    val targetZone: String? = null
)

@Serializable
data class LegalActionInfo(
    val actionType: String,
    val description: String,
    val action: GameAction,
    val isAffordable: Boolean = true,
    val validTargets: List<EntityId>? = null,
    val requiresTargets: Boolean = false,
    val targetCount: Int = 1,
    val minTargets: Int = targetCount,
    val targetDescription: String? = null,
    val targetRequirements: List<LegalActionTargetInfo>? = null,
    val validAttackers: List<EntityId>? = null,
    val mandatoryAttackers: List<EntityId>? = null,
    val validAttackTargets: List<EntityId>? = null,
    val validBlockers: List<EntityId>? = null,
    val hasXCost: Boolean = false,
    val maxAffordableX: Int? = null,
    val minX: Int = 0,
    val isManaAbility: Boolean = false,
    val additionalCostInfo: AdditionalCostInfo? = null,
    val hasConvoke: Boolean = false,
    val validConvokeCreatures: List<ConvokeCreatureInfo>? = null,
    val hasDelve: Boolean = false,
    val validDelveCards: List<DelveCardInfo>? = null,
    val minDelveNeeded: Int? = null,
    val manaCostString: String? = null,
    val requiresDamageDistribution: Boolean = false,
    val totalDamageToDistribute: Int? = null,
    val minDamagePerTarget: Int? = null,
    val autoTapPreview: List<EntityId>? = null,
    val availableManaSources: List<ManaSourceInfo>? = null,
    val requiresManaColorChoice: Boolean = false,
    val sourceZone: String? = null,
    val blockerMaxBlockCounts: Map<EntityId, Int>? = null,
    val mandatoryBlockerAssignments: Map<EntityId, List<EntityId>>? = null,
    val maxRepeatableActivations: Int? = null,
    val hasCrew: Boolean = false,
    val crewPower: Int? = null,
    val validCrewCreatures: List<CrewCreatureInfo>? = null,
    val holdPriority: Boolean = false
)

@Serializable
data class ConvokeCreatureInfo(
    val entityId: EntityId,
    val name: String,
    val colors: Set<Color>
)

@Serializable
data class DelveCardInfo(
    val entityId: EntityId,
    val name: String,
    val imageUri: String? = null
)

@Serializable
data class CrewCreatureInfo(
    val entityId: EntityId,
    val name: String,
    val power: Int
)

@Serializable
data class AdditionalCostInfo(
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
    val counterRemovalCreatures: List<CounterRemovalCreatureInfo> = emptyList(),
    val validBlightTargets: List<EntityId> = emptyList(),
    val blightAmount: Int = 0
)

@Serializable
data class CounterRemovalCreatureInfo(
    val entityId: EntityId,
    val name: String,
    val availableCounters: Int,
    val imageUri: String? = null
)
