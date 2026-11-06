package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.mechanics.sba.player.GameLoss
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
import kotlin.reflect.KClass

/**
 * Executor for LoseGameEffect.
 * Target player loses the game immediately (e.g., Phage the Untouchable).
 */
class LoseGameExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<LoseGameEffect> {

    override val effectType: KClass<LoseGameEffect> = LoseGameEffect::class

    override fun execute(
        state: GameState,
        effect: LoseGameEffect,
        context: EffectContext
    ): EffectResult {
        // State-aware resolution: the stateless overload only knows the four "chosen target"
        // references, so an attachment- or relation-derived player (Sinner's Judgment's
        // `Player.EnchantedPlayer`, `ChosenOpponent`, `OwnerOf`, …) silently resolved to null and
        // the loss never happened. `resolvePlayerRef` is the switch that knows all of them.
        val targetId = context.resolvePlayerTarget(effect.target, state)
            ?: return EffectResult.error(state, "No target player for LoseGameEffect")

        if (state.getEntity(targetId) == null) return EffectResult.error(state, "Target player not found")

        // Shared with the state-based-action checks, so a "can't lose the game" grant (and its CR
        // 810.8a team reach) and a "would lose the game, instead" replacement apply identically
        // wherever a player would lose. A replaced loss queues its rest, run at the end of this effect.
        val (newState, events) = GameLoss.lose(
            state, targetId, LossReason.CARD_EFFECT, GameEndReason.CARD_EFFECT, predicateEvaluator, effect.message)
        return EffectResult.success(newState, events)
    }
}
