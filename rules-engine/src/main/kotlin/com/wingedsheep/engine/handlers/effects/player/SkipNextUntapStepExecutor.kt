package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipNextUntapStepComponent
import com.wingedsheep.sdk.scripting.effects.SkipNextUntapStepEffect
import kotlin.reflect.KClass

/**
 * Executor for [SkipNextUntapStepEffect]: adds one pending skip to the player's
 * [SkipNextUntapStepComponent]. A second skip on the same player stacks rather than overwriting
 * (CR 614.10a — they skip the next two untap steps).
 */
class SkipNextUntapStepExecutor : EffectExecutor<SkipNextUntapStepEffect> {

    override val effectType: KClass<SkipNextUntapStepEffect> = SkipNextUntapStepEffect::class

    override fun execute(
        state: GameState,
        effect: SkipNextUntapStepEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerTarget(effect.target, context, state)
            ?: return EffectResult.error(state, "No valid player for SkipNextUntapStepEffect")

        val newState = state.updateEntity(playerId) { container ->
            val pending = container.get<SkipNextUntapStepComponent>()?.steps ?: 0
            container.with(SkipNextUntapStepComponent(steps = pending + 1))
        }
        return EffectResult.success(newState)
    }
}
