package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import kotlin.reflect.KClass

/**
 * Executor for TapUntapEffect.
 * "Tap target creature" or "Untap target creature"
 */
class TapUntapExecutor : EffectExecutor<TapUntapEffect> {

    override val effectType: KClass<TapUntapEffect> = TapUntapEffect::class

    override fun execute(
        state: GameState,
        effect: TapUntapEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for tap/untap")

        val cardName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Permanent"

        val newState = state.updateEntity(targetId) { container ->
            if (effect.tap) {
                container.with(TappedComponent)
            } else {
                container.without<TappedComponent>()
            }
        }

        val event = if (effect.tap) {
            TappedEvent(targetId, cardName)
        } else {
            UntappedEvent(targetId, cardName)
        }

        return ExecutionResult.success(newState, listOf(event))
    }
}
