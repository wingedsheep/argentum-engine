package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ExtraTurnsComponent
import com.wingedsheep.engine.state.components.player.PendingExtraTurn
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.sdk.model.EntityId
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
 * The extra turn is always created. A replacement such as Ugin's Nexus or Trouble in Pairs
 * (`SkipExtraTurns`) skips it only when it would *begin* — `TurnManager` applies that — so if the
 * source has left the battlefield by then the turn happens. The riders ([TakeExtraTurnEffect.loseAtEndStep],
 * [TakeExtraTurnEffect.powerUpAbilitiesCantBeActivated], [TakeExtraTurnEffect.damageCantBePrevented]) are recorded on the pending extra turn
 * ([PendingExtraTurn]) and applied by `TurnManager` when that turn begins, so a skipped turn never
 * applies them and the lockout lands on exactly the turn this effect created.
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

        if (state.getOpponents(turnTakerId).isEmpty()) {
            return EffectResult.error(state, "No opponent found")
        }
        return EffectResult.success(
            insertExtraTurn(
                state, turnTakerId,
                PendingExtraTurn(
                    loseAtEndStep = effect.loseAtEndStep,
                    powerUpAbilitiesCantBeActivated = effect.powerUpAbilitiesCantBeActivated,
                    damageCantBePrevented = effect.damageCantBePrevented,
                )
            )
        )
    }

    companion object {
        /**
         * Insert one extra turn for [takerId]. "Take an extra turn" is modeled as every other player
         * skipping their next turn, which inserts one extra turn for the taker regardless of player
         * count; the taker's [ExtraTurnsComponent.pending] marks their next turn to begin as that
         * extra turn. The most recently created extra turn goes first (CR 500.7).
         */
        fun insertExtraTurn(state: GameState, takerId: EntityId, turn: PendingExtraTurn = PendingExtraTurn()): GameState {
            val bypassed = state.getOpponents(takerId).fold(state) { acc, otherPlayerId ->
                acc.updateEntity(otherPlayerId) { container ->
                    val existing = container.get<SkipNextTurnComponent>() ?: SkipNextTurnComponent(0)
                    container.with(existing.copy(turns = existing.turns + 1,
                        extraTurnBypasses = existing.extraTurnBypasses + 1))
                }
            }
            return bypassed.updateEntity(takerId) { container ->
                val extra = container.get<ExtraTurnsComponent>() ?: ExtraTurnsComponent()
                container.with(extra.copy(pending = listOf(turn) + extra.pending))
            }
        }
    }
}
