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
     *   combat). Ydwen Efreet's oracle text explicitly overrides 509.1h and is the only
     *   current use site.
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
                // "Sole blocker" is judged from the CURRENT blocker set (empty after removing this
                // creature), not from who blocked it when combat began. Ydwen's oracle says "blocked by
                // only this creature this combat"; we have no per-combat became-blocked-by history, so a
                // contrived case (another blocker left earlier in the same combat) is treated as sole.
                newState = if (unblockSoleBlockedAttackers && updatedBlockerIds.isEmpty()) {
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
