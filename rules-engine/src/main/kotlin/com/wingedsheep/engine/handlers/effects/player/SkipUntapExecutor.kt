package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.SkipUntapEffect
import kotlin.reflect.KClass

/**
 * Executor for SkipUntapEffect.
 * "Creatures and lands target player controls don't untap during their next untap step."
 *
 * This adds a SkipUntapComponent to the target player. When that player's
 * next untap step occurs, the specified permanent types (creatures and/or lands)
 * are skipped and the component is removed.
 */
class SkipUntapExecutor : EffectExecutor<SkipUntapEffect> {

    override val effectType: KClass<SkipUntapEffect> = SkipUntapEffect::class

    override fun execute(
        state: GameState,
        effect: SkipUntapEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get the target player from the context
        val targetPlayerId = context.targets.firstOrNull()?.let { target ->
            when (target) {
                is ChosenTarget.Player -> target.playerId
                else -> null
            }
        } ?: return ExecutionResult.error(state, "No valid player target for SkipUntapEffect")

        // Add the SkipUntapComponent to the target player
        val newState = state.updateEntity(targetPlayerId) { container ->
            container.with(
                SkipUntapComponent(
                    affectsCreatures = effect.affectsCreatures,
                    affectsLands = effect.affectsLands
                )
            )
        }

        return ExecutionResult.success(newState)
    }
}
