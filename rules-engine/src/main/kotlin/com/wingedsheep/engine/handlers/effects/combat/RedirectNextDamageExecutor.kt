package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
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
    ): EffectResult {
        val redirectToId = context.resolveTarget(effect.redirectTo)
            ?: return EffectResult.error(state, "Could not resolve redirect target for RedirectNextDamageEffect")

        val protectedIds = effect.protectedTargets.mapNotNull { target ->
            context.resolveTarget(target)
        }.toSet()

        // A creatures-only shield protects a *class*, not a list, so it carries no protected ids
        // and must not be rejected for having none (Blood of the Martyr).
        if (protectedIds.isEmpty() && !effect.creaturesOnly) {
            return EffectResult.error(state, "Could not resolve any protected targets for RedirectNextDamageEffect")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RedirectNextDamage(
                redirectToId, effect.amount, effect.scope, effect.creaturesOnly, effect.optional
            ),
            affectedEntities = protectedIds,
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
