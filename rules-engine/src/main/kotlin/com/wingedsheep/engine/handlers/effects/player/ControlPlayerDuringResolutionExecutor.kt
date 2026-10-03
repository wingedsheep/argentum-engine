package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ResolutionControlEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ResolutionControl
import com.wingedsheep.engine.state.isResolving
import com.wingedsheep.engine.state.rememberResolutionControlHands
import com.wingedsheep.sdk.scripting.effects.ControlPlayerDuringResolutionEffect
import kotlin.reflect.KClass

class ControlPlayerDuringResolutionExecutor : EffectExecutor<ControlPlayerDuringResolutionEffect> {
    override val effectType: KClass<ControlPlayerDuringResolutionEffect> = ControlPlayerDuringResolutionEffect::class

    override fun execute(state: GameState, effect: ControlPlayerDuringResolutionEffect, context: EffectContext): EffectResult {
        val player = context.resolvePlayerTarget(effect.target, state)
            ?.takeIf { it in state.turnOrder } ?: return EffectResult.success(state)
        val objectTarget = effect.resolvingObject
        val ref = if (objectTarget == null) {
            state.continuationStack.filterIsInstance<com.wingedsheep.engine.core.EndResolutionControlContinuation>()
                .lastOrNull()?.resolvingObject
        } else {
            context.resolveTarget(objectTarget, state)?.let(state::objectRef)
        } ?: return EffectResult.success(state)
        if (ref.entityId !in state.stack && !state.isResolving(ref)) return EffectResult.success(state)
        val grants = state.sharedTurnTeam(player).map { ResolutionControl(ref, it, context.controllerId) }
        val retained = state.resolutionControls.filter { state.isCurrentObject(it.resolvingObject) || state.isResolving(it.resolvingObject) }
        val granted = state.copy(resolutionControls = retained + grants)
        val informed = if (granted.isResolving(ref)) granted.rememberResolutionControlHands(ref) else granted
        return EffectResult.success(informed, grants.map {
            ResolutionControlEvent(it, ResolutionControlEvent.Stage.GRANTED)
        })
    }
}
