package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnLinkedExileEffect
import kotlin.reflect.KClass

/**
 * Executor for ReturnLinkedExileEffect.
 *
 * Returns all cards linked to the source permanent (via LinkedExileComponent)
 * from exile to the battlefield under the controller's control.
 *
 * The source permanent may be in the graveyard or exile at this point (since
 * this is typically used in a leaves-the-battlefield trigger), but its entity
 * data including LinkedExileComponent is still accessible.
 */
class ReturnLinkedExileExecutor : EffectExecutor<ReturnLinkedExileEffect> {

    override val effectType: KClass<ReturnLinkedExileEffect> = ReturnLinkedExileEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnLinkedExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state)

        val controllerId = context.controllerId

        // Read the LinkedExileComponent from the source entity
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.success(state)

        val linkedExile = sourceContainer.get<LinkedExileComponent>()
            ?: return ExecutionResult.success(state)

        if (linkedExile.exiledIds.isEmpty()) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (entityId in linkedExile.exiledIds) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: controllerId

            // Find the card in exile
            val exileZone = ZoneKey(ownerId, Zone.EXILE)
            if (entityId !in newState.getZone(exileZone)) continue

            // Move from exile to battlefield under controller's control
            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)

            newState = newState.updateEntity(entityId) { c ->
                c.with(ControllerComponent(controllerId))
                    .with(SummoningSicknessComponent)
            }
            newState = newState.removeFromZone(exileZone, entityId)
            newState = newState.addToZone(battlefieldZone, entityId)

            events.add(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = Zone.EXILE,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
