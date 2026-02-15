package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.RedirectNextDamageEffect
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
        val redirectToId = EffectExecutorUtils.resolveTarget(effect.redirectTo, context)
            ?: return ExecutionResult.error(state, "Could not resolve redirect target for RedirectNextDamageEffect")

        val protectedIds = effect.protectedTargets.mapNotNull { target ->
            EffectExecutorUtils.resolveTarget(target, context)
        }.toSet()

        if (protectedIds.isEmpty()) {
            return ExecutionResult.error(state, "Could not resolve any protected targets for RedirectNextDamageEffect")
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.RedirectNextDamage(redirectToId),
                affectedEntities = protectedIds
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
