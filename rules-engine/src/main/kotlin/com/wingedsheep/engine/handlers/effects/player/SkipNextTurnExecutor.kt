package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.sdk.scripting.effects.SkipNextTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for SkipNextTurnEffect.
 * Adds SkipNextTurnComponent to the target player so their next turn is skipped.
 */
class SkipNextTurnExecutor : EffectExecutor<SkipNextTurnEffect> {

    override val effectType: KClass<SkipNextTurnEffect> = SkipNextTurnEffect::class

    override fun execute(
        state: GameState,
        effect: SkipNextTurnEffect,
        context: EffectContext
    ): EffectResult {
        val target = effect.target
        val targetPlayerId = when (target) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.PlayerRef -> {
                com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
                    .resolvePlayerRef(target.player, context, state)
                    ?: return EffectResult.error(state, "Cannot resolve player for SkipNextTurnEffect")
            }
            else -> return EffectResult.error(state, "Unsupported target for SkipNextTurnEffect")
        }

        val newState = state.updateEntity(targetPlayerId) { container ->
            container.with(SkipNextTurnComponent)
        }

        return EffectResult.success(newState)
    }
}
