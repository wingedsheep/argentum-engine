package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CantAttackGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for CantAttackGroupEffect.
 * "Creatures can't attack this turn." / "[filter] creatures can't attack this turn."
 *
 * Creates a floating effect with SetCantAttack that dynamically applies to all creatures
 * matching the filter. Per Rule 611.2c, rule-modifying effects like "can't attack" apply
 * to all matching objects including those entering the battlefield after the effect resolves.
 */
class CantAttackGroupExecutor : EffectExecutor<CantAttackGroupEffect> {

    override val effectType: KClass<CantAttackGroupEffect> = CantAttackGroupEffect::class

    override fun execute(
        state: GameState,
        effect: CantAttackGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.SetCantAttack,
                affectedEntities = emptySet(),
                dynamicGroupFilter = effect.filter
            ),
            duration = effect.duration,
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
