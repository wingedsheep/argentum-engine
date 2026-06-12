package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LoseAtEndStepComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for TakeExtraTurnEffect.
 * "Take an extra turn after this one."
 *
 * Implemented by making every other player skip their next turn, which inserts one
 * extra turn for the taker regardless of player count.
 *
 * If loseAtEndStep is true (e.g., Last Chance), the caster will also lose the game
 * at the beginning of their next end step.
 *
 * Checks for PreventExtraTurns replacement effects (e.g., Ugin's Nexus) before applying.
 */
class TakeExtraTurnExecutor : EffectExecutor<TakeExtraTurnEffect> {

    override val effectType: KClass<TakeExtraTurnEffect> = TakeExtraTurnEffect::class

    override fun execute(
        state: GameState,
        effect: TakeExtraTurnEffect,
        context: EffectContext
    ): EffectResult {
        // Resolve who takes the extra turn — defaults to the controller
        val turnTakerId = context.resolveTarget(effect.target, state)
            ?: context.controllerId

        // Check if extra turns are prevented (e.g., Ugin's Nexus on the battlefield)
        if (ReplacementEffectUtils.isExtraTurnPrevented(state)) {
            return EffectResult.success(state)
        }

        // "Take an extra turn" is modeled as every other player skipping their next turn,
        // which inserts one extra turn for the taker regardless of player count.
        val otherPlayerIds = state.getOpponents(turnTakerId)
        if (otherPlayerIds.isEmpty()) {
            return EffectResult.error(state, "No opponent found")
        }
        var newState = otherPlayerIds.fold(state) { acc, otherPlayerId ->
            acc.updateEntity(otherPlayerId) { container ->
                container.with(SkipNextTurnComponent)
            }
        }

        // If loseAtEndStep is true, mark the turn-taker to lose at their next end step
        // turnsUntilLoss=1 means skip this turn's end step, trigger on the next turn's end step
        if (effect.loseAtEndStep) {
            newState = newState.updateEntity(turnTakerId) { container ->
                container.with(
                    LoseAtEndStepComponent(
                        turnsUntilLoss = 1,
                        message = "You lose the game (Last Chance)"
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }
}
