package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.sdk.scripting.effects.AddBeginningPhaseEffect
import kotlin.reflect.KClass

/**
 * Executor for [AddBeginningPhaseEffect] — "there is an additional beginning phase after this
 * phase." Queues a single BEGINNING phase on the active player; the TurnManager runs its untap,
 * upkeep and draw steps and then carries on with the rest of the queue, or the end step.
 */
class AddBeginningPhaseExecutor : EffectExecutor<AddBeginningPhaseEffect> {

    override val effectType: KClass<AddBeginningPhaseEffect> = AddBeginningPhaseEffect::class

    override fun execute(
        state: GameState,
        effect: AddBeginningPhaseEffect,
        context: EffectContext
    ): EffectResult {
        val activePlayer = state.activePlayerId
            ?: return EffectResult.error(state, "No active player for AddBeginningPhaseEffect")
        return EffectResult.success(state.queueAdditionalPhase(activePlayer, ExtraPhaseKind.BEGINNING, context))
    }
}
