package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
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
    ): EffectResult {
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantAttack,
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context,
            dynamicGroupFilter = effect.filter
        )

        return EffectResult.success(newState)
    }
}
