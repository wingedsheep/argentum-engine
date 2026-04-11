package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.destroyPermanent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DestroyAllEquipmentOnTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllEquipmentOnTargetEffect.
 * Destroys all Equipment attached to the target permanent.
 */
class DestroyAllEquipmentOnTargetExecutor : EffectExecutor<DestroyAllEquipmentOnTargetEffect> {

    override val effectType: KClass<DestroyAllEquipmentOnTargetEffect> = DestroyAllEquipmentOnTargetEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllEquipmentOnTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.success(state)

        // Verify target is still on the battlefield
        if (!state.getBattlefield().contains(targetId)) {
            return ExecutionResult.success(state)
        }

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val attachments = container.get<AttachmentsComponent>()
            ?: return ExecutionResult.success(state)

        // Find all equipment IDs
        val equipmentIds = attachments.attachedIds.filter { attachId ->
            val attachContainer = state.getEntity(attachId)
            val card = attachContainer?.get<CardComponent>()
            card?.typeLine?.isEquipment == true
        }

        if (equipmentIds.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Destroy each equipment
        var currentState = state
        val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (equipmentId in equipmentIds) {
            if (!currentState.getBattlefield().contains(equipmentId)) continue
            val result = destroyPermanent(currentState, equipmentId)
            if (result.isSuccess) {
                currentState = result.state
                allEvents.addAll(result.events)
            }
        }

        return ExecutionResult.success(currentState, allEvents)
    }
}
