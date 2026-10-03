package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaSpendingObligationsContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.WithManaSpendingObligationsEffect
import kotlin.reflect.KClass

class WithManaSpendingObligationsExecutor(
    private val executeEffect: (GameState, Effect, EffectContext) -> EffectResult,
) : EffectExecutor<WithManaSpendingObligationsEffect> {
    override val effectType: KClass<WithManaSpendingObligationsEffect> = WithManaSpendingObligationsEffect::class

    override fun execute(state: GameState, effect: WithManaSpendingObligationsEffect, context: EffectContext): EffectResult {
        val player = context.resolvePlayerTarget(effect.player, state)?.takeIf { it in state.turnOrder }
            ?: return EffectResult.success(state)
        val (id, allocated) = state.newRoutingId()
        val frame = ManaSpendingObligationsContinuation(player, context, id)
        val result = executeEffect(allocated.pushContinuation(frame), effect.effect, context)
        if (result.error != null) return EffectResult.error(state, result.error!!)
        if (result.pendingDecision != null) return result
        val index = state.continuationStack.size
        val stack = result.state.continuationStack
        val completed = stack.getOrNull(index) as? ManaSpendingObligationsContinuation
        if (completed?.scopeId != id) return result
        if (completed.pendingIds.isNotEmpty()) return EffectResult.error(state, "Each mana ability must contribute mana to the instruction")
        return result.copy(state = result.state.copy(continuationStack = stack.take(index) + stack.drop(index + 1)))
    }
}
