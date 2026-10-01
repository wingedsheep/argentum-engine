package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.sdk.scripting.effects.AddMainPhaseEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddMainPhaseEffect] — "there is an additional main phase after this phase."
 *
 * Queues a single MAIN (postcombat main) phase on the active player. Composed after
 * [AddCombatPhaseEffect] it reproduces the "additional combat phase followed by an additional main
 * phase" shape (Aggravated Assault); on its own it inserts a standalone extra main phase.
 */
class AddMainPhaseExecutor : EffectExecutor<AddMainPhaseEffect> {

    override val effectType: KClass<AddMainPhaseEffect> = AddMainPhaseEffect::class

    override fun execute(
        state: GameState,
        effect: AddMainPhaseEffect,
        context: EffectContext
    ): EffectResult {
        val activePlayer = state.activePlayerId
            ?: return EffectResult.error(state, "No active player for AddMainPhaseEffect")
        return EffectResult.success(state.queueAdditionalPhase(activePlayer, ExtraPhaseKind.MAIN, context))
    }
}
