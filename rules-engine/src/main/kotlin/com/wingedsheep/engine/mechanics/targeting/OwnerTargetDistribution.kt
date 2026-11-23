package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.OwnerDistribution
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Announcement-time player groups for an `ownerDistribution` requirement: one group per owner, or
 * per *projected* controller when the requirement sets `distributeByController` ("for each
 * opponent, destroy target noncreature permanent that player controls"). Resolution validates the
 * already chosen targets individually.
 */
object OwnerTargetDistribution {
    fun groups(state: GameState, requirement: TargetRequirement, candidates: List<EntityId>): List<List<EntityId>> =
        candidates.mapNotNull { id -> playerOf(state, requirement, id)?.let { it to id } }
            .groupBy({ it.first }, { it.second }).values.toList()

    fun bounds(state: GameState, requirement: TargetRequirement, candidates: List<EntityId>): DynamicTargetCount.Bounds? {
        val distribution = DynamicTargetCount.objectOf(requirement)?.ownerDistribution ?: return null
        val maximum = groups(state, requirement, candidates).size
        return DynamicTargetCount.Bounds(if (distribution == OwnerDistribution.EXACTLY_ONE) maximum else 0, maximum)
    }

    fun isSatisfied(state: GameState, requirement: TargetRequirement, targets: List<EntityId>): Boolean =
        targets.map { playerOf(state, requirement, it) }
            .let { players -> players.none { it == null } && players.distinct().size == players.size }

    private fun playerOf(state: GameState, requirement: TargetRequirement, id: EntityId): EntityId? =
        if (DynamicTargetCount.objectOf(requirement)?.distributeByController == true) {
            state.projectedState.getController(id) ?: state.getEntity(id)?.get<ControllerComponent>()?.playerId
        } else {
            state.getEntity(id)?.get<OwnerComponent>()?.playerId
        }
}

fun TargetRequirementInfo.withOwnerDistribution(
    state: GameState, requirement: TargetRequirement, candidates: List<EntityId>,
): TargetRequirementInfo {
    val bounds = OwnerTargetDistribution.bounds(state, requirement, candidates) ?: return this
    return copy(minTargets = bounds.min, maxTargets = bounds.max ?: 0,
        exclusiveGroups = OwnerTargetDistribution.groups(state, requirement, candidates))
}
