package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackerOrderComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent
import com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent
import com.wingedsheep.engine.state.components.combat.RequiresManualDamageAssignmentComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Strips all combat components from [targetId] and propagates the removal to any creatures
 * that were blocking it (if it was an attacker) or any attackers it was blocking (if it was
 * a blocker). Mirrors the cleanup formerly inlined in
 * [com.wingedsheep.engine.handlers.effects.combat.RemoveFromCombatExecutor]; extracted so the
 * CR 506.4 state-based check (controller-changed → removed from combat) can share it.
 */
object CombatRemovalHelper {

    /**
     * Remove [targetId] from combat in [state]. Returns the new state, or [state] unchanged
     * if the entity wasn't in combat. Idempotent.
     *
     * @param unblockSoleBlockedAttackers When true and [targetId] was a blocker, attackers
     *   whose blocker list becomes empty after this removal have their [BlockedComponent]
     *   and damage-assignment components stripped — they become unblocked. Defaults to
     *   false (CR 509.1h: a blocked creature stays blocked even if all blockers leave
     *   combat). Some older cards explicitly override the usual blocked status and consult the
     *   complete history of blocker object identities this combat.
     */
    fun removeFromCombat(
        state: GameState,
        targetId: EntityId,
        unblockSoleBlockedAttackers: Boolean = false,
    ): GameState {
        val entity = state.getEntity(targetId) ?: return state
        val isAttacking = entity.has<AttackingComponent>()
        val isBlocking = entity.has<BlockingComponent>()
        if (!isAttacking && !isBlocking) return state

        var newState = state.updateEntity(targetId) { container ->
            container
                .without<AttackingComponent>()
                .without<BlockingComponent>()
                .without<BlockedComponent>()
                .without<DamageAssignmentComponent>()
                .without<DamageAssignmentOrderComponent>()
                .without<AttackerOrderComponent>()
                .without<RequiresManualDamageAssignmentComponent>()
        }

        if (isAttacking) {
            for ((entityId, components) in newState.entities) {
                val blockingComponent = components.get<BlockingComponent>() ?: continue
                if (targetId in blockingComponent.blockedAttackerIds) {
                    // A blocker stays a blocking creature after the attackers it blocked are
                    // removed from combat (CR 509.1g); it just assigns no damage (CR 510.1d).
                    val updatedIds = blockingComponent.blockedAttackerIds - targetId
                    newState = newState.updateEntity(entityId) { container ->
                        var updated = container.with(BlockingComponent(updatedIds))
                        val attackerOrder = updated.get<AttackerOrderComponent>()
                        if (attackerOrder != null) {
                            updated = updated.with(AttackerOrderComponent(
                                attackerOrder.orderedAttackers.filter { it != targetId }
                            ))
                        }
                        updated
                    }
                }
            }
        }

        if (isBlocking) {
            val blockedAttackerIds = entity.get<BlockingComponent>()?.blockedAttackerIds ?: emptyList()
            for (attackerId in blockedAttackerIds) {
                val attackerEntity = newState.getEntity(attackerId) ?: continue
                val blockedComponent = attackerEntity.get<BlockedComponent>() ?: continue
                val updatedBlockerIds = blockedComponent.blockerIds - targetId
                val history = attackerEntity.get<com.wingedsheep.engine.state.components.combat.BlockersThisCombatComponent>()?.blockers
                val wasOnlyBlocker = history == setOf(state.objectRef(targetId))
                newState = if (unblockSoleBlockedAttackers && updatedBlockerIds.isEmpty() && wasOnlyBlocker) {
                    newState.updateEntity(attackerId) { container ->
                        container
                            .without<BlockedComponent>()
                            .without<DamageAssignmentComponent>()
                            .without<DamageAssignmentOrderComponent>()
                            .without<RequiresManualDamageAssignmentComponent>()
                    }
                } else {
                    newState.updateEntity(attackerId) { container ->
                        container.with(BlockedComponent(updatedBlockerIds))
                    }
                }
            }
        }

        return newState
    }
}
