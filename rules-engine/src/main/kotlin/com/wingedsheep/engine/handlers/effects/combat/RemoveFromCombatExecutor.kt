package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.sdk.scripting.RemoveFromCombatEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveFromCombatEffect.
 * "Remove [target] from combat."
 *
 * Removes all combat-related components from the target creature.
 * Also cleans up any blockers that were blocking the removed attacker
 * by removing its ID from their BlockingComponent.
 */
class RemoveFromCombatExecutor : EffectExecutor<RemoveFromCombatEffect> {

    override val effectType: KClass<RemoveFromCombatEffect> = RemoveFromCombatEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveFromCombatEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val entity = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        // Check if this creature is actually in combat
        val isAttacking = entity.has<AttackingComponent>()
        val isBlocking = entity.has<BlockingComponent>()
        if (!isAttacking && !isBlocking) {
            return ExecutionResult.success(state)
        }

        // Remove combat components from the target
        var newState = state.updateEntity(targetId) { container ->
            container
                .without<AttackingComponent>()
                .without<BlockingComponent>()
                .without<BlockedComponent>()
                .without<DamageAssignmentComponent>()
                .without<DamageAssignmentOrderComponent>()
                .without<RequiresManualDamageAssignmentComponent>()
        }

        // If the removed creature was an attacker, clean up blockers that were blocking it
        if (isAttacking) {
            for ((entityId, components) in newState.entities) {
                val blockingComponent = components.get<BlockingComponent>() ?: continue
                if (targetId in blockingComponent.blockedAttackerIds) {
                    val updatedIds = blockingComponent.blockedAttackerIds - targetId
                    newState = if (updatedIds.isEmpty()) {
                        newState.updateEntity(entityId) { container ->
                            container.without<BlockingComponent>()
                        }
                    } else {
                        newState.updateEntity(entityId) { container ->
                            container.with(BlockingComponent(updatedIds))
                        }
                    }
                }
            }
        }

        // If the removed creature was a blocker, clean up the attacker's BlockedComponent
        if (isBlocking) {
            val blockedAttackerIds = entity.get<BlockingComponent>()?.blockedAttackerIds ?: emptyList()
            for (attackerId in blockedAttackerIds) {
                val attackerEntity = newState.getEntity(attackerId) ?: continue
                val blockedComponent = attackerEntity.get<BlockedComponent>() ?: continue
                val updatedBlockerIds = blockedComponent.blockerIds - targetId
                newState = if (updatedBlockerIds.isEmpty()) {
                    newState.updateEntity(attackerId) { container ->
                        container.without<BlockedComponent>()
                    }
                } else {
                    newState.updateEntity(attackerId) { container ->
                        container.with(BlockedComponent(updatedBlockerIds))
                    }
                }
            }
        }

        return ExecutionResult.success(newState)
    }
}
