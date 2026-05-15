package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ExileAfterResolveComponent
import com.wingedsheep.sdk.scripting.effects.MarkSpellExileWithCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [MarkSpellExileWithCountersEffect].
 *
 * Tags the target spell on the stack with [ExileAfterResolveComponent] carrying
 * the requested counter list and the `onlyIfResolved` flag. When the spell
 * actually resolves, [com.wingedsheep.engine.mechanics.stack.StackResolver] sees
 * the component, sends the card to exile instead of the graveyard, and adds the
 * specified counters. If the spell is countered or fizzles, the component does
 * not redirect the spell — it goes to its owner's graveyard normally
 * (Goliath Daydreamer ruling: "If a spell is countered or otherwise fails to
 * resolve, Goliath Daydreamer's first ability won't exile it.").
 */
class MarkSpellExileWithCountersExecutor : EffectExecutor<MarkSpellExileWithCountersEffect> {

    override val effectType: KClass<MarkSpellExileWithCountersEffect> =
        MarkSpellExileWithCountersEffect::class

    override fun execute(
        state: GameState,
        effect: MarkSpellExileWithCountersEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)
        val counterType = resolveCounterType(effect.counterType)
        val counters = List(effect.count.coerceAtLeast(0)) { counterType }

        val sourceId = context.sourceId
        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<ExileAfterResolveComponent>()
            val merged = if (existing != null) {
                existing.copy(
                    withCounters = existing.withCounters + counters,
                    onlyIfResolved = existing.onlyIfResolved || true,
                    linkedSourceId = existing.linkedSourceId ?: sourceId
                )
            } else {
                ExileAfterResolveComponent(
                    withCounters = counters,
                    onlyIfResolved = true,
                    linkedSourceId = sourceId
                )
            }
            container.with(merged)
        }
        return EffectResult.success(newState)
    }
}
