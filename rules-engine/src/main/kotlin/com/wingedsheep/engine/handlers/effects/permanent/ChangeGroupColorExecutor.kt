package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.ChangeGroupColorEffect
import kotlin.reflect.KClass

/**
 * Executor for ChangeGroupColorEffect.
 * "Each creature you control becomes black until end of turn" and similar group color effects.
 */
class ChangeGroupColorExecutor : EffectExecutor<ChangeGroupColorEffect> {

    override val effectType: KClass<ChangeGroupColorEffect> = ChangeGroupColorEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeGroupColorEffect,
        context: EffectContext
    ): ExecutionResult {
        val filter = effect.filter
        val excludeSelfId = if (filter.excludeSelf) context.sourceId else null
        val affectedEntities = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.baseFilter, context, excludeSelfId
        ).toSet()

        if (affectedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.COLOR,
            modification = SerializableModification.ChangeColor(effect.colors),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
