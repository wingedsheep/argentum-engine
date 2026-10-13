package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/** Announcement-time owner groups. Resolution validates the already chosen targets individually. */
object OwnerTargetDistribution {
    fun groups(state: GameState, candidates: List<EntityId>): List<List<EntityId>> =
        candidates.mapNotNull { id -> state.getEntity(id)?.get<OwnerComponent>()?.playerId?.let { it to id } }
            .groupBy({ it.first }, { it.second }).values.toList()

    fun bounds(state: GameState, requirement: TargetRequirement, candidates: List<EntityId>): DynamicTargetCount.Bounds? {
        val distribution = DynamicTargetCount.objectOf(requirement)?.ownerDistribution ?: return null
        val maximum = groups(state, candidates).size
        return DynamicTargetCount.Bounds(if (distribution == OwnerDistribution.EXACTLY_ONE) maximum else 0, maximum)
    }

    fun isSatisfied(state: GameState, targets: List<EntityId>): Boolean =
        targets.map { state.getEntity(it)?.get<OwnerComponent>()?.playerId }
            .let { owners -> owners.none { it == null } && owners.distinct().size == owners.size }
}

fun TargetRequirementInfo.withOwnerDistribution(
    state: GameState, requirement: TargetRequirement, candidates: List<EntityId>,
): TargetRequirementInfo {
    val bounds = OwnerTargetDistribution.bounds(state, requirement, candidates) ?: return this
    return copy(minTargets = bounds.min, maxTargets = bounds.max ?: 0,
        exclusiveGroups = OwnerTargetDistribution.groups(state, candidates))
}
