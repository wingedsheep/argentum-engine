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
 * Implemented by making every other player skip their next turn. TurnManager walks all skipped
 * occurrences before starting a turn, including in multiplayer. This remains an approximation:
 * there is no ordered extra-turn queue, so differently owned extra turns do not have independent
 * scheduling identity. Synthetic bypasses are distinguished from real skips so turn replacements
 * cannot apply to an opponent while the inserted turn is being scheduled.
 *
 * If loseAtEndStep is true (e.g., Last Chance), the caster will also lose the game
 * at the beginning of their next end step.
 *
 * If powerUpAbilitiesCantBeActivated is true (Kang the Conqueror), `turnNumber + 1` is recorded in
 * [GameState.powerUpRestrictedTurns], locking every player out of power-up abilities for that turn.
 * The turn-number stamp has no extra-turn identity: when a second extra-turn effect resolves later
 * in the same turn, the most recently created turn goes first (CR 500.7), but this rider remains on
 * the next turn to begin, rather than following the particular turn this effect created.
 *
 * Checks for PreventExtraTurns replacement effects (e.g., Ugin's Nexus) before applying. Both riders
 * sit behind that check: the engine models Ugin's Nexus as preventing the extra turn outright, so
 * there is no "that turn" for them to apply to. Keeping the riders here rather than as sibling
 * effects in a `Composite` is what keeps "did a turn actually get created" in a single owner — a
 * sibling would have to re-derive this executor's preconditions, and would silently drift the moment
 * `TakeExtraTurn` gains another way to fail.
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
                val existing = container.get<SkipNextTurnComponent>() ?: SkipNextTurnComponent(0)
                container.with(existing.copy(turns = existing.turns + 1,
                    extraTurnBypasses = existing.extraTurnBypasses + 1))
            }
        }

        // "During that turn, power-up abilities can't be activated" (Kang the Conqueror). Stamp the
        // next turn to actually begin: skipped turns never call `TurnManager.startTurn`, so they
        // consume no turn numbers. A later extra-turn effect can move this stamp onto the wrong
        // inserted turn; see the scheduling limitation in the KDoc.
        if (effect.powerUpAbilitiesCantBeActivated) {
            newState = newState.copy(
                powerUpRestrictedTurns = newState.powerUpRestrictedTurns + (newState.turnNumber + 1)
            )
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
