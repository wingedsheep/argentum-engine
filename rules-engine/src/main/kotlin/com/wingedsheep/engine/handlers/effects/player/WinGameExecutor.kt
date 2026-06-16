package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.scripting.effects.WinGameEffect
import kotlin.reflect.KClass

/**
 * Executor for [WinGameEffect].
 *
 * Marks every opponent of the target as having lost the game; [GameEndCheck]
 * then resolves `gameOver` with the target as the surviving (winning) player.
 * Skips opponents who already lost or who control a permanent that grants
 * "can't lose the game".
 */
class WinGameExecutor : EffectExecutor<WinGameEffect> {

    override val effectType: KClass<WinGameEffect> = WinGameEffect::class

    override fun execute(
        state: GameState,
        effect: WinGameEffect,
        context: EffectContext
    ): EffectResult {
        val winnerId = context.resolvePlayerTarget(effect.target)
            ?: return EffectResult.error(state, "No target player for WinGameEffect")

        // CR 810.8a — "if either player on a team wins, the entire team wins": only players on
        // opposing teams lose, never the winner's teammate. getOpponents is team-aware (excludes the
        // winner's whole team); in a non-team game it is every other player as before. The defeated
        // opponents' own teammates are then marked by TeamLossPropagationCheck.
        val opponentIds = state.getOpponents(winnerId)
        if (opponentIds.isEmpty()) return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (opponentId in opponentIds) {
            val container = newState.getEntity(opponentId) ?: continue
            if (container.has<PlayerLostComponent>()) continue
            // CR 810.8a — a can't-lose grant controlled by any teammate protects the whole team.
            if (com.wingedsheep.engine.mechanics.sba.player.playerCantLoseGame(newState, opponentId)) continue

            newState = newState.updateEntity(opponentId) { c ->
                c.with(PlayerLostComponent(LossReason.CARD_EFFECT))
            }
            events.add(PlayerLostEvent(opponentId, GameEndReason.CARD_EFFECT, effect.message))
        }

        return EffectResult.success(newState, events)
    }
}
