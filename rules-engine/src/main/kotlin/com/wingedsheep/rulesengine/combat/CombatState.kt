package com.wingedsheep.rulesengine.combat

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.player.PlayerId
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

    // ==========================================================================
    // Backward Compatibility (PlayerId-based accessors)
    // ==========================================================================

    /**
     * Backward compatibility: get defending player as PlayerId.
     */
    @Deprecated("Use defendingPlayer (EntityId) instead", ReplaceWith("defendingPlayer"))
    val defendingPlayerId: PlayerId
        get() = defendingPlayer.toPlayerId()

    /**
     * Backward compatibility: check if a PlayerId is the defending player.
     */
    @Deprecated("Use defendingPlayer == entityId instead", ReplaceWith("defendingPlayer == EntityId.fromPlayerId(playerId)"))
    fun isDefendingPlayer(playerId: PlayerId): Boolean =
        defendingPlayer == EntityId.fromPlayerId(playerId)

    /**
     * Backward compatibility: get attacking player as PlayerId.
     */
    @Deprecated("Use attackingPlayer (EntityId) instead", ReplaceWith("attackingPlayer"))
    val attackingPlayerId: PlayerId
        get() = attackingPlayer.toPlayerId()

    fun getAttackerInfo(entityId: EntityId): AttackerInfo? = attackers[entityId]

    fun getBlockerInfo(entityId: EntityId): BlockerInfo? = blockers[entityId]

    fun isAttacking(entityId: EntityId): Boolean = entityId in attackers

    fun isBlocking(entityId: EntityId): Boolean = entityId in blockers

    /**
     * Backward compatibility: check if a card is attacking by CardId.
     */
    @Deprecated("Use isAttacking(EntityId) instead", ReplaceWith("isAttacking(EntityId.of(cardId.value))"))
    fun isAttacking(cardId: CardId): Boolean = isAttacking(EntityId.of(cardId.value))

    /**
     * Backward compatibility: check if a card is blocking by CardId.
     */
    @Deprecated("Use isBlocking(EntityId) instead", ReplaceWith("isBlocking(EntityId.of(cardId.value))"))
    fun isBlocking(cardId: CardId): Boolean = isBlocking(EntityId.of(cardId.value))

    fun getBlockersFor(attackerId: EntityId): List<EntityId> =
        blockers.filter { it.value.blocking == attackerId }.keys.toList()

    /**
     * Backward compatibility: get blockers by CardId.
     */
    @Deprecated("Use getBlockersFor(EntityId) instead", ReplaceWith("getBlockersFor(EntityId.of(attackerId.value))"))
    fun getBlockersFor(attackerId: CardId): List<EntityId> =
        getBlockersFor(EntityId.of(attackerId.value))

    fun isBlocked(attackerId: EntityId): Boolean =
        blockers.any { it.value.blocking == attackerId }

    fun isUnblocked(attackerId: EntityId): Boolean =
        attackerId in attackers && !isBlocked(attackerId)

    fun addAttacker(entityId: EntityId): CombatState =
        copy(attackers = attackers + (entityId to AttackerInfo(entityId)))

    /**
     * Backward compatibility: add attacker by CardId.
     */
    @Deprecated("Use addAttacker(EntityId) instead", ReplaceWith("addAttacker(EntityId.of(cardId.value))"))
    fun addAttacker(cardId: CardId): CombatState =
        addAttacker(EntityId.of(cardId.value))

    fun addBlocker(blockerId: EntityId, attackerId: EntityId): CombatState =
        copy(blockers = blockers + (blockerId to BlockerInfo(blockerId, attackerId)))

    /**
     * Backward compatibility: add blocker by CardId.
     */
    @Deprecated("Use addBlocker(EntityId, EntityId) instead", ReplaceWith("addBlocker(EntityId.of(blockerId.value), EntityId.of(attackerId.value))"))
    fun addBlocker(blockerId: CardId, attackerId: CardId): CombatState =
        addBlocker(EntityId.of(blockerId.value), EntityId.of(attackerId.value))

    fun setDamageAssignmentOrder(attackerId: EntityId, blockerOrder: List<EntityId>): CombatState =
        copy(damageAssignmentOrder = damageAssignmentOrder + (attackerId to blockerOrder))

    /**
     * Backward compatibility: set damage assignment order by CardId.
     */
    @Deprecated("Use setDamageAssignmentOrder(EntityId, List<EntityId>) instead")
    fun setDamageAssignmentOrder(attackerId: CardId, blockerOrder: List<CardId>): CombatState =
        setDamageAssignmentOrder(
            EntityId.of(attackerId.value),
            blockerOrder.map { EntityId.of(it.value) }
        )

    fun clear(): CombatState =
        copy(attackers = emptyMap(), blockers = emptyMap(), damageAssignmentOrder = emptyMap())

    companion object {
        /**
         * Create a new combat state with EntityId players (preferred).
         */
        fun create(attackingPlayer: EntityId, defendingPlayer: EntityId): CombatState =
            CombatState(attackingPlayer, defendingPlayer)

        /**
         * Create from PlayerId (backward compatibility).
         */
        @Deprecated(
            "Use create(EntityId, EntityId) instead",
            ReplaceWith("create(EntityId.fromPlayerId(attackingPlayer), EntityId.fromPlayerId(defendingPlayer))")
        )
        fun fromPlayerIds(attackingPlayer: PlayerId, defendingPlayer: PlayerId): CombatState =
            CombatState(
                EntityId.fromPlayerId(attackingPlayer),
                EntityId.fromPlayerId(defendingPlayer)
            )
    }
}

/**
 * Information about an attacking creature.
 */
@Serializable
data class AttackerInfo(
    val entityId: EntityId,
    val damageDealt: Int = 0
) {
    /**
     * Backward compatibility accessor.
     */
    @Deprecated("Use entityId instead", ReplaceWith("entityId"))
    val cardId: CardId get() = CardId(entityId.value)
}

/**
 * Information about a blocking creature.
 */
@Serializable
data class BlockerInfo(
    val entityId: EntityId,
    val blocking: EntityId,
    val damageDealt: Int = 0
) {
    /**
     * Backward compatibility accessor.
     */
    @Deprecated("Use entityId instead", ReplaceWith("entityId"))
    val cardId: CardId get() = CardId(entityId.value)
}
