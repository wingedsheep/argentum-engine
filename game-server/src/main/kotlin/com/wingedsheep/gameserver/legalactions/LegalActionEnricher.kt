package com.wingedsheep.gameserver.legalactions

import com.wingedsheep.engine.legalactions.*
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.protocol.AdditionalCostInfo
import com.wingedsheep.gameserver.protocol.ConvokeCreatureInfo
import com.wingedsheep.gameserver.protocol.CounterRemovalCreatureInfo
import com.wingedsheep.gameserver.protocol.CrewCreatureInfo
import com.wingedsheep.gameserver.protocol.DelveCardInfo
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.LegalActionTargetInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId

/**
 * Thin mapping layer from engine [LegalAction] to server [LegalActionInfo] DTO.
 *
 * Adds presentation-only data: mana source info for the pre-cast UI.
 * The client protocol (LegalActionInfo) remains unchanged.
 */
class LegalActionEnricher(
    private val manaSolver: ManaSolver,
    private val cardRegistry: CardRegistry
) {
    fun enrich(actions: List<LegalAction>, state: GameState, playerId: EntityId): List<LegalActionInfo> {
        val manaSourceInfos = buildManaSourceInfos(state, playerId)
        return actions.map { action -> toLegalActionInfo(action, manaSourceInfos) }
    }

    private fun toLegalActionInfo(
        action: LegalAction,
        manaSourceInfos: List<ServerMessage.ManaSourceInfo>?
    ): LegalActionInfo {
        return LegalActionInfo(
            actionType = action.actionType,
            description = action.description,
            action = action.action,
            isAffordable = action.affordable,
            validTargets = action.validTargets,
            requiresTargets = action.requiresTargets,
            targetCount = action.targetCount,
            minTargets = action.minTargets,
            targetDescription = action.targetDescription,
            targetRequirements = action.targetRequirements?.map { it.toDto() },
            validAttackers = action.validAttackers,
            mandatoryAttackers = action.mandatoryAttackers,
            validAttackTargets = action.validAttackTargets,
            validBlockers = action.validBlockers,
            blockerMaxBlockCounts = action.blockerMaxBlockCounts,
            mandatoryBlockerAssignments = action.mandatoryBlockerAssignments,
            hasXCost = action.hasXCost,
            maxAffordableX = action.maxAffordableX,
            minX = action.minX,
            isManaAbility = action.isManaAbility,
            requiresManaColorChoice = action.requiresManaColorChoice,
            additionalCostInfo = action.additionalCostInfo?.toDto(),
            hasConvoke = action.hasConvoke,
            validConvokeCreatures = action.convokeCreatures?.map { it.toDto() },
            hasDelve = action.hasDelve,
            validDelveCards = action.delveCards?.map { it.toDto() },
            minDelveNeeded = action.minDelveNeeded,
            manaCostString = action.manaCostString,
            requiresDamageDistribution = action.requiresDamageDistribution,
            totalDamageToDistribute = action.totalDamageToDistribute,
            minDamagePerTarget = action.minDamagePerTarget,
            autoTapPreview = action.autoTapPreview,
            availableManaSources = if (action.autoTapPreview != null) manaSourceInfos else null,
            sourceZone = action.sourceZone,
            hasCrew = action.hasCrew,
            crewPower = action.crewPower,
            validCrewCreatures = action.crewCreatures?.map { it.toDto() },
            maxRepeatableActivations = action.maxRepeatableActivations,
            holdPriority = action.holdPriority
        )
    }

    private fun buildManaSourceInfos(state: GameState, playerId: EntityId): List<ServerMessage.ManaSourceInfo>? {
        val sources = manaSolver.findAvailableManaSources(state, playerId)
        if (sources.isEmpty()) return null
        return sources.map { source ->
            val card = state.getEntity(source.entityId)?.get<CardComponent>()
            val imageUri = card?.let { cardRegistry.getCard(it.cardDefinitionId)?.metadata?.imageUri }
            ServerMessage.ManaSourceInfo(
                entityId = source.entityId,
                name = source.name,
                imageUri = imageUri,
                producesColors = source.producesColors.map { it.symbol.toString() },
                producesColorless = source.producesColorless,
                manaAmount = source.manaAmount
            )
        }
    }

    // Extension functions for DTO conversion

    private fun TargetInfo.toDto() = LegalActionTargetInfo(
        index = index,
        description = description,
        minTargets = minTargets,
        maxTargets = maxTargets,
        validTargets = validTargets,
        targetZone = targetZone
    )

    private fun AdditionalCostData.toDto() = AdditionalCostInfo(
        description = description,
        costType = costType,
        validSacrificeTargets = validSacrificeTargets,
        sacrificeCount = sacrificeCount,
        validTapTargets = validTapTargets,
        tapCount = tapCount,
        validDiscardTargets = validDiscardTargets,
        discardCount = discardCount,
        validBounceTargets = validBounceTargets,
        bounceCount = bounceCount,
        validExileTargets = validExileTargets,
        exileMinCount = exileMinCount,
        exileMaxCount = exileMaxCount,
        validBeholdTargets = validBeholdTargets,
        beholdCount = beholdCount,
        counterRemovalCreatures = counterRemovalCreatures.map { it.toDto() },
        validBlightTargets = validBlightTargets,
        blightAmount = blightAmount
    )

    private fun ConvokeCreatureData.toDto() = ConvokeCreatureInfo(
        entityId = entityId,
        name = name,
        colors = colors
    )

    private fun DelveCardData.toDto() = DelveCardInfo(
        entityId = entityId,
        name = name,
        imageUri = imageUri
    )

    private fun CrewCreatureData.toDto() = CrewCreatureInfo(
        entityId = entityId,
        name = name,
        power = power
    )

    private fun CounterRemovalCreatureData.toDto() = CounterRemovalCreatureInfo(
        entityId = entityId,
        name = name,
        availableCounters = availableCounters,
        imageUri = imageUri
    )
}
