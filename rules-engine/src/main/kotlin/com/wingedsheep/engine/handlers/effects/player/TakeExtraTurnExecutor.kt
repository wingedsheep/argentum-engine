package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for TakeExtraTurnEffect.
 * "Take an extra turn after this one."
 *
 * In a 2-player game, this is implemented by making the opponent skip their next turn,
 * which effectively gives the caster an extra turn.
 *
 * If loseAtEndStep is true (e.g., Last Chance), the caster will also lose the game
 * at the beginning of their next end step.
 */
class TakeExtraTurnExecutor : EffectExecutor<TakeExtraTurnEffect> {

    override val effectType: KClass<TakeExtraTurnEffect> = TakeExtraTurnEffect::class

    override fun execute(
        state: GameState,
        effect: TakeExtraTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
            ?: return ExecutionResult.error(state, "No controller for TakeExtraTurnEffect")

        // In a 2-player game, "take an extra turn" means the opponent skips their next turn
        val opponentId = state.getOpponent(controllerId)
            ?: return ExecutionResult.error(state, "No opponent found")

        // Add SkipNextTurnComponent to opponent
        var newState = state.updateEntity(opponentId) { container ->
            container.with(SkipNextTurnComponent)
        }

        // If loseAtEndStep is true, mark the caster to lose at their next end step
        // turnsUntilLoss=1 means skip this turn's end step, trigger on the next turn's end step
        if (effect.loseAtEndStep) {
            newState = newState.updateEntity(controllerId) { container ->
                container.with(
                    LoseAtEndStepComponent(
                        turnsUntilLoss = 1,
                        message = "You lose the game (Last Chance)"
                    )
                )
            }
        }

        return ExecutionResult.success(newState)
    }
}
