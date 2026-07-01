package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.RetainUnspentManaComponent
import com.wingedsheep.sdk.scripting.effects.RetainUnspentManaEffect
import kotlin.reflect.KClass

/**
 * Executor for [RetainUnspentManaEffect] — "Until end of turn, you don't lose unspent mana of the
 * named colours as steps and phases end" (The Last Agni Kai).
 *
 * Confers a turn-scoped [RetainUnspentManaComponent] on the resolving controller; the colour-keeping
 * happens at end-of-turn cleanup ([com.wingedsheep.engine.core.CleanupPhaseManager]). If the player
 * already carries a retention this turn (a second copy resolved), the colour sets are unioned so all
 * named colours are kept.
 */
class RetainUnspentManaExecutor : EffectExecutor<RetainUnspentManaEffect> {

    override val effectType: KClass<RetainUnspentManaEffect> = RetainUnspentManaEffect::class

    override fun execute(
        state: GameState,
        effect: RetainUnspentManaEffect,
        context: EffectContext
    ): EffectResult {
        if (effect.colors.isEmpty()) return EffectResult.success(state)

        val newState = state.updateEntity(context.controllerId) { container ->
            val existing = container.get<RetainUnspentManaComponent>()
            val merged = (existing?.colors ?: emptySet()) + effect.colors
            container.with(
                RetainUnspentManaComponent(colors = merged, removeOn = PlayerEffectRemoval.EndOfTurn)
            )
        }
        return EffectResult.success(newState)
    }
}
