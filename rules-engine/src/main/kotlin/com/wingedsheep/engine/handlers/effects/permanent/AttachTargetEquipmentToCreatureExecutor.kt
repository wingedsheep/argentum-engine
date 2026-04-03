package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.sdk.scripting.effects.AttachTargetEquipmentToCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for [AttachTargetEquipmentToCreatureEffect].
 * Attaches a targeted Equipment to a targeted creature.
 * Both the Equipment and creature are explicit targets (not the source).
 */
class AttachTargetEquipmentToCreatureExecutor : EffectExecutor<AttachTargetEquipmentToCreatureEffect> {

    override val effectType: KClass<AttachTargetEquipmentToCreatureEffect> =
        AttachTargetEquipmentToCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: AttachTargetEquipmentToCreatureEffect,
        context: EffectContext
    ): ExecutionResult {
        val equipmentId = resolveTarget(effect.equipmentTarget, context, state)
            ?: return ExecutionResult.error(state, "No valid equipment target for attach")

        val creatureId = resolveTarget(effect.creatureTarget, context, state)
            ?: return ExecutionResult.success(state) // "up to one" — no creature target is OK

        var newState = state

        // Detach from current creature if already attached
        val currentAttachment = newState.getEntity(equipmentId)?.get<AttachedToComponent>()
        if (currentAttachment != null) {
            val oldTargetId = currentAttachment.targetId
            newState = newState.updateEntity(oldTargetId) { container ->
                val attachments = container.get<AttachmentsComponent>()
                if (attachments != null) {
                    val updatedIds = attachments.attachedIds.filter { it != equipmentId }
                    if (updatedIds.isEmpty()) {
                        container.without<AttachmentsComponent>()
                    } else {
                        container.with(AttachmentsComponent(updatedIds))
                    }
                } else {
                    container
                }
            }
        }

        // Attach to new creature
        newState = newState.updateEntity(equipmentId) { container ->
            container.with(AttachedToComponent(creatureId))
        }

        newState = newState.updateEntity(creatureId) { container ->
            val existing = container.get<AttachmentsComponent>()
            val updatedIds = (existing?.attachedIds ?: emptyList()) + equipmentId
            container.with(AttachmentsComponent(updatedIds))
        }

        return ExecutionResult.success(newState)
    }
}
