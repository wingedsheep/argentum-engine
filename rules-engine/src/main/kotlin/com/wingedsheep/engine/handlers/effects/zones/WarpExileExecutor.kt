package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.WarpExiledComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import kotlin.reflect.KClass

/**
 * Executor for WarpExileEffect.
 *
 * Exiles a warped permanent and marks it with [WarpExiledComponent] so that
 * the card can be re-cast from exile using its warp cost on a later turn.
 *
 * Used by the warp mechanic's delayed trigger that fires at the beginning
 * of the next end step.
 */
class WarpExileExecutor : EffectExecutor<WarpExileEffect> {

    override val effectType: KClass<WarpExileEffect> = WarpExileEffect::class

    override fun execute(
        state: GameState,
        effect: WarpExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return ExecutionResult.success(state) // Permanent may have already left the battlefield

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        container.get<CardComponent>()
            ?: return ExecutionResult.success(state)

        // Only exile if the permanent is still on the battlefield
        if (targetId !in state.getBattlefield()) return ExecutionResult.success(state)

        // Use ZoneTransitionService for proper cleanup (strip battlefield components, etc.)
        val transitionResult = ZoneTransitionService.moveToZone(
            state = state,
            entityId = targetId,
            destinationZone = Zone.EXILE
        )

        // Add WarpExiledComponent so the card can be re-cast from exile via warp
        val newState = transitionResult.state.updateEntity(targetId) { c ->
            c.with(WarpExiledComponent(controllerId = context.controllerId))
        }

        return ExecutionResult.success(newState, transitionResult.events)
    }
}
