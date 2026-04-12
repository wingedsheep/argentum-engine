package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.SetGroupCreatureSubtypesEffect
import kotlin.reflect.KClass

/**
 * Executor for SetGroupCreatureSubtypesEffect.
 * "Each creature you control becomes a Shade until end of turn" and similar group type effects.
 */
class SetGroupCreatureSubtypesExecutor : EffectExecutor<SetGroupCreatureSubtypesEffect> {

    override val effectType: KClass<SetGroupCreatureSubtypesEffect> = SetGroupCreatureSubtypesEffect::class

    override fun execute(
        state: GameState,
        effect: SetGroupCreatureSubtypesEffect,
        context: EffectContext
    ): EffectResult {
        val filter = effect.filter
        val excludeSelfId = if (filter.excludeSelf) context.sourceId else null
        val affectedEntities = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.baseFilter, context, excludeSelfId
        ).toSet()

        if (affectedEntities.isEmpty()) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.SetCreatureSubtypes(effect.subtypes),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
