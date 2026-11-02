package com.wingedsheep.engine.handlers.effects.regeneration

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.HealDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for [HealDamageEffect] — removes all damage marked on the target now. The same heal the
 * regeneration and Pyramids shields apply when they fire, without the shield: the permanent is not
 * tapped and stays in combat. Healing isn't an event in the rules (nothing triggers on it), so like
 * those shields it emits none.
 */
class HealDamageExecutor : EffectExecutor<HealDamageEffect> {

    override val effectType: KClass<HealDamageEffect> = HealDamageEffect::class

    override fun execute(
        state: GameState,
        effect: HealDamageEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        return EffectResult.success(DamageUtils.healMarkedDamage(state, targetId))
    }
}
