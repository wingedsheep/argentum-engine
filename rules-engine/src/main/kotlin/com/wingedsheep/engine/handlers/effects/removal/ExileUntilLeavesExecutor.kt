package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.stripBattlefieldComponents
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import kotlin.reflect.KClass

/**
 * Executor for ExileUntilLeavesEffect.
 *
 * Exiles the target permanent and links it to the source permanent via
 * LinkedExileComponent. The source's LeavesBattlefield trigger (defined on
 * the card) handles returning the exiled card via ReturnLinkedExile.
 *
 * If the source is no longer on the battlefield when this effect resolves
 * (modern template safety), the target is not exiled.
 */
class ExileUntilLeavesExecutor : EffectExecutor<ExileUntilLeavesEffect> {

    override val effectType: KClass<ExileUntilLeavesEffect> = ExileUntilLeavesEffect::class

    override fun execute(
        state: GameState,
        effect: ExileUntilLeavesEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state)

        // Modern template: if source is no longer on the battlefield, do nothing
        if (sourceId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Verify target is on the battlefield
        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)
        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.success(state)

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: context.controllerId

        // Find the zone the target is currently in
        val currentZone = state.zones.entries.find { (_, cards) -> targetId in cards }?.key
            ?: return ExecutionResult.success(state)

        val exileZone = ZoneKey(ownerId, Zone.EXILE)

        // Strip battlefield components and move to exile
        var newState = state.updateEntity(targetId) { c -> stripBattlefieldComponents(c) }
        newState = newState.removeFromZone(currentZone, targetId)
        newState = newState.addToZone(exileZone, targetId)

        val events = listOf(
            ZoneChangeEvent(
                entityId = targetId,
                entityName = cardComponent.name,
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.EXILE,
                ownerId = ownerId
            )
        )

        // Store exiled ID on the source permanent as LinkedExileComponent
        val sourceContainer = newState.getEntity(sourceId)
        if (sourceContainer != null) {
            val existingLinked = sourceContainer.get<LinkedExileComponent>()
            val allExiled = (existingLinked?.exiledIds ?: emptyList()) + listOf(targetId)
            newState = newState.updateEntity(sourceId) { c ->
                c.with(LinkedExileComponent(allExiled))
            }
        }

        return ExecutionResult(
            state = newState,
            events = events
        )
    }
}
