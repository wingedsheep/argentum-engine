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
 */
@Serializable
data class CombatState(
    val attackingPlayer: EntityId,
    val defendingPlayer: EntityId
) {
    companion object {
        fun create(attackingPlayer: EntityId, defendingPlayer: EntityId): CombatState =
            CombatState(attackingPlayer, defendingPlayer)
    }
}
