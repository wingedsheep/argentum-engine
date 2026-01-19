package com.wingedsheep.rulesengine.combat

import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Tracks the teams for the current combat phase.
 *
 * Note: Individual attacker/blocker status is stored on the entities
 * themselves via AttackingComponent and BlockingComponent (ECS pattern).
 * This class only tracks the phase context (who is attacking whom).
 *
 * To query attackers: state.entitiesWithComponent<AttackingComponent>()
 * To query blockers: state.entitiesWithComponent<BlockingComponent>()
 *
 * @property eligibleBlockers The set of creatures that were on the battlefield
 * when the declare blockers step began. Only these creatures can be declared
 * as blockers (MTG Rule 509.1a - declaring blockers is a turn-based action that
 * happens as the step begins, so creatures entering later cannot block).
 * This is null until the declare blockers step begins.
 */
@Serializable
data class CombatState(
    val attackingPlayer: EntityId,
    val defendingPlayer: EntityId,
    val eligibleBlockers: Set<EntityId>? = null
) {
    /**
     * Check if a creature is eligible to be declared as a blocker.
     * Returns true if eligibleBlockers hasn't been set yet (we're not in declare blockers)
     * or if the creature is in the eligible set.
     */
    fun isEligibleBlocker(creatureId: EntityId): Boolean {
        return eligibleBlockers == null || creatureId in eligibleBlockers
    }

    /**
     * Set the eligible blockers (called when transitioning to declare blockers step).
     */
    fun withEligibleBlockers(blockers: Set<EntityId>): CombatState =
        copy(eligibleBlockers = blockers)

    companion object {
        fun create(attackingPlayer: EntityId, defendingPlayer: EntityId): CombatState =
            CombatState(attackingPlayer, defendingPlayer)
    }
}
