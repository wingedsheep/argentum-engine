package com.wingedsheep.engine.state.components.combat

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a creature as attacking.
 */
@Serializable
data class AttackingComponent(
    val defenderId: EntityId  // Player or planeswalker being attacked
) : Component

/**
 * Marks a creature as blocking.
 */
@Serializable
data class BlockingComponent(
    val blockedAttackerIds: List<EntityId>
) : Component

/**
 * Marks a creature as blocked (one or more blockers declared).
 */
@Serializable
data class BlockedComponent(
    val blockerIds: List<EntityId>
) : Component

/**
 * Combat damage assignment for a creature.
 */
@Serializable
data class DamageAssignmentComponent(
    val assignments: Map<EntityId, Int>  // target -> damage amount
) : Component

/**
 * Marks a creature as having dealt first strike damage this combat.
 */
@Serializable
data object DealtFirstStrikeDamageComponent : Component
