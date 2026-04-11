package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
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
 *
 * Delegates zone movement to [ZoneTransitionService] for consistent cleanup
 * (now includes cleanupCombatReferences, cleanupReverseAttachmentLink, and
 * removeFloatingEffectsTargeting which were previously missing).
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

        val targetId = context.resolveTarget(effect.target)
            ?: return ExecutionResult.success(state)

        // Verify target is on the battlefield
        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        // Delegate zone movement to ZoneTransitionService
        val transitionResult = ZoneTransitionService.moveToZone(
            state, targetId, Zone.EXILE,
            ZoneEntryOptions(skipZoneChangeRedirect = true)
        )

        var newState = transitionResult.state

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
            events = transitionResult.events
        )
    }
}
