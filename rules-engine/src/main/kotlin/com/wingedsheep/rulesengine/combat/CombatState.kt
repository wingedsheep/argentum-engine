package com.wingedsheep.rulesengine.combat

import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Tracks the state of combat for a single combat phase.
 *
 * Uses EntityId for all entity identification (players, creatures).
 */
@Serializable
data class CombatState(
    val attackingPlayer: EntityId,
    val defendingPlayer: EntityId,
    val attackers: Map<EntityId, AttackerInfo> = emptyMap(),
    val blockers: Map<EntityId, BlockerInfo> = emptyMap(),
    val damageAssignmentOrder: Map<EntityId, List<EntityId>> = emptyMap()
) {
    val isActive: Boolean
        get() = attackers.isNotEmpty() || blockers.isNotEmpty()

    val attackerIds: Set<EntityId>
        get() = attackers.keys

    val blockerIds: Set<EntityId>
        get() = blockers.keys

    fun getAttackerInfo(entityId: EntityId): AttackerInfo? = attackers[entityId]

    fun getBlockerInfo(entityId: EntityId): BlockerInfo? = blockers[entityId]

    fun isAttacking(entityId: EntityId): Boolean = entityId in attackers

    fun isBlocking(entityId: EntityId): Boolean = entityId in blockers

    fun getBlockersFor(attackerId: EntityId): List<EntityId> =
        blockers.filter { it.value.blocking == attackerId }.keys.toList()

    fun isBlocked(attackerId: EntityId): Boolean =
        blockers.any { it.value.blocking == attackerId }

    fun isUnblocked(attackerId: EntityId): Boolean =
        attackerId in attackers && !isBlocked(attackerId)

    fun addAttacker(entityId: EntityId): CombatState =
        copy(attackers = attackers + (entityId to AttackerInfo(entityId)))

    fun addBlocker(blockerId: EntityId, attackerId: EntityId): CombatState =
        copy(blockers = blockers + (blockerId to BlockerInfo(blockerId, attackerId)))

    fun setDamageAssignmentOrder(attackerId: EntityId, blockerOrder: List<EntityId>): CombatState =
        copy(damageAssignmentOrder = damageAssignmentOrder + (attackerId to blockerOrder))

    fun clear(): CombatState =
        copy(attackers = emptyMap(), blockers = emptyMap(), damageAssignmentOrder = emptyMap())

    companion object {
        fun create(attackingPlayer: EntityId, defendingPlayer: EntityId): CombatState =
            CombatState(attackingPlayer, defendingPlayer)
    }
}

/**
 * Information about an attacking creature.
 */
@Serializable
data class AttackerInfo(
    val entityId: EntityId,
    val damageDealt: Int = 0
)

/**
 * Information about a blocking creature.
 */
@Serializable
data class BlockerInfo(
    val entityId: EntityId,
    val blocking: EntityId,
    val damageDealt: Int = 0
)
