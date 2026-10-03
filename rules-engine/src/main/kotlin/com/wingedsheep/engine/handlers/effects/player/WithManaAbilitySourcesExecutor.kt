package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ManaAbilitySourcesContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.WithManaAbilitySourcesEffect
import kotlin.reflect.KClass

class WithManaAbilitySourcesExecutor(
    private val executeEffect: (GameState, Effect, EffectContext) -> EffectResult,
) : EffectExecutor<WithManaAbilitySourcesEffect> {
    override val effectType: KClass<WithManaAbilitySourcesEffect> = WithManaAbilitySourcesEffect::class

    override fun execute(state: GameState, effect: WithManaAbilitySourcesEffect, context: EffectContext): EffectResult {
        val player = context.resolvePlayerTarget(effect.player, state)?.takeIf { it in state.turnOrder }
            ?: return EffectResult.success(state)
        val frame = ManaAbilitySourcesContinuation(player, effect.sources, context)
        val result = executeEffect(state.pushContinuation(frame), effect.effect, context)
        if (result.error != null) return EffectResult.error(state, result.error!!)
        if (result.pendingDecision != null) return result
        // The nested instruction completed synchronously; it cannot retain this scope.
        val index = state.continuationStack.size
        val stack = result.state.continuationStack
        return if (stack.getOrNull(index) == frame) result.copy(state = result.state.copy(
            continuationStack = stack.take(index) + stack.drop(index + 1))) else result
    }
}
