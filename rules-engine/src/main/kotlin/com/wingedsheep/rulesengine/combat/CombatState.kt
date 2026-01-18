package com.wingedsheep.rulesengine.combat

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * Tracks the state of combat for a single combat phase.
 */
@Serializable
data class CombatState(
    val attackingPlayer: PlayerId,
    val defendingPlayer: PlayerId,
    val attackers: Map<CardId, AttackerInfo> = emptyMap(),
    val blockers: Map<CardId, BlockerInfo> = emptyMap(),
    val damageAssignmentOrder: Map<CardId, List<CardId>> = emptyMap()
) {
    val isActive: Boolean
        get() = attackers.isNotEmpty() || blockers.isNotEmpty()

    val attackerIds: Set<CardId>
        get() = attackers.keys

    val blockerIds: Set<CardId>
        get() = blockers.keys

    fun getAttackerInfo(cardId: CardId): AttackerInfo? = attackers[cardId]

    fun getBlockerInfo(cardId: CardId): BlockerInfo? = blockers[cardId]

    fun isAttacking(cardId: CardId): Boolean = cardId in attackers

    fun isBlocking(cardId: CardId): Boolean = cardId in blockers

    fun getBlockersFor(attackerId: CardId): List<CardId> =
        blockers.filter { it.value.blocking == attackerId }.keys.toList()

    fun isBlocked(attackerId: CardId): Boolean =
        blockers.any { it.value.blocking == attackerId }

    fun isUnblocked(attackerId: CardId): Boolean =
        attackerId in attackers && !isBlocked(attackerId)

    fun addAttacker(cardId: CardId): CombatState =
        copy(attackers = attackers + (cardId to AttackerInfo(cardId)))

    fun addBlocker(blockerId: CardId, attackerId: CardId): CombatState =
        copy(blockers = blockers + (blockerId to BlockerInfo(blockerId, attackerId)))

    fun setDamageAssignmentOrder(attackerId: CardId, blockerOrder: List<CardId>): CombatState =
        copy(damageAssignmentOrder = damageAssignmentOrder + (attackerId to blockerOrder))

    fun clear(): CombatState =
        copy(attackers = emptyMap(), blockers = emptyMap(), damageAssignmentOrder = emptyMap())

    companion object {
        fun create(attackingPlayer: PlayerId, defendingPlayer: PlayerId): CombatState =
            CombatState(attackingPlayer, defendingPlayer)
    }
}

/**
 * Information about an attacking creature.
 */
@Serializable
data class AttackerInfo(
    val cardId: CardId,
    val damageDealt: Int = 0
)

/**
 * Information about a blocking creature.
 */
@Serializable
data class BlockerInfo(
    val cardId: CardId,
    val blocking: CardId,
    val damageDealt: Int = 0
)
