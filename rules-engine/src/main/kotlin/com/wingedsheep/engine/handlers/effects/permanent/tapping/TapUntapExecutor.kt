package com.wingedsheep.engine.handlers.effects.permanent.tapping

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import kotlin.reflect.KClass

/**
 * Executor for TapUntapEffect.
 * "Tap target creature" or "Untap target creature"
 *
 * The untap branch routes through [untapOrConsumeStun] so stun-counter
 * replacement (Rule 122.1d) is handled the same way as the natural untap step.
 */
class TapUntapExecutor : EffectExecutor<TapUntapEffect> {

    override val effectType: KClass<TapUntapEffect> = TapUntapEffect::class

    override fun execute(
        state: GameState,
        effect: TapUntapEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for tap/untap")

        val cardName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Permanent"

        if (!effect.tap) {
            val (newState, event) = untapOrConsumeStun(state, targetId)
            return EffectResult.success(newState, listOfNotNull(event))
        }

        val newState = state.updateEntity(targetId) { it.with(TappedComponent) }
        return EffectResult.success(newState, listOf(TappedEvent(targetId, cardName)))
    }
}
