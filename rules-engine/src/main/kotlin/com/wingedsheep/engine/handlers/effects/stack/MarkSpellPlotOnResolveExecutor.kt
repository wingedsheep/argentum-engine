package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ExileAfterResolveComponent
import com.wingedsheep.sdk.scripting.effects.MarkSpellPlotOnResolveEffect
import kotlin.reflect.KClass

/**
 * Executor for [MarkSpellPlotOnResolveEffect].
 *
 * Tags the target spell on the stack with [ExileAfterResolveComponent] carrying `makePlotted = true`
 * and the `onlyIfResolved` flag. When the spell actually resolves,
 * [com.wingedsheep.engine.mechanics.stack.StackResolver] sees the component, sends the card to exile
 * instead of the graveyard, and makes it plotted for its owner. If the spell is countered or fizzles,
 * the component does not redirect it — it goes to its owner's graveyard normally (mirrors the Goliath
 * Daydreamer ruling and Lilah, Undefeated Slickshot's "If you do" clause).
 */
class MarkSpellPlotOnResolveExecutor : EffectExecutor<MarkSpellPlotOnResolveEffect> {

    override val effectType: KClass<MarkSpellPlotOnResolveEffect> =
        MarkSpellPlotOnResolveEffect::class

    override fun execute(
        state: GameState,
        effect: MarkSpellPlotOnResolveEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)
        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<ExileAfterResolveComponent>()
            val merged = existing?.copy(onlyIfResolved = true, makePlotted = true)
                ?: ExileAfterResolveComponent(onlyIfResolved = true, makePlotted = true)
            container.with(merged)
        }
        return EffectResult.success(newState)
    }
}
