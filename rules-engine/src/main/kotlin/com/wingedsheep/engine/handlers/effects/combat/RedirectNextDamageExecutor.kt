package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for RedirectNextDamageEffect.
 * "The next time damage would be dealt to [protected targets] this turn,
 *  that damage is dealt to [redirectTo] instead."
 *
 * Creates a floating effect with a RedirectNextDamage shield that is consumed
 * when damage is first dealt to any protected entity. The shield expires at end of turn.
 */
class RedirectNextDamageExecutor : EffectExecutor<RedirectNextDamageEffect> {

    override val effectType: KClass<RedirectNextDamageEffect> = RedirectNextDamageEffect::class

    override fun execute(
        state: GameState,
        effect: RedirectNextDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val redirectToId = context.resolveTarget(effect.redirectTo)
            ?: return ExecutionResult.error(state, "Could not resolve redirect target for RedirectNextDamageEffect")

        val protectedIds = effect.protectedTargets.mapNotNull { target ->
            context.resolveTarget(target)
        }.toSet()

        if (protectedIds.isEmpty()) {
            return ExecutionResult.error(state, "Could not resolve any protected targets for RedirectNextDamageEffect")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RedirectNextDamage(redirectToId, effect.amount),
            affectedEntities = protectedIds,
            duration = Duration.EndOfTurn,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
