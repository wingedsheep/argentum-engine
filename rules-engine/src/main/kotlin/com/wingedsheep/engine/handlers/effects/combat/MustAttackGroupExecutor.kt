package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ResolutionFilterBaker
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.MustAttackDefender
import com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for [MustAttackGroupEffect] — "[filter] attack each combat if able" for a duration.
 *
 * Mirrors [CantAttackGroupExecutor]: one floating effect over a dynamic group, so creatures that
 * arrive later are bound too (CR 611.2c). The player a [MustAttackDefender.PlayerOtherThanYou]
 * requirement steers attacks away from is this effect's controller, baked in now: it stays the
 * same player if the source later changes hands or leaves.
 */
class MustAttackGroupExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<MustAttackGroupEffect> {

    override val effectType: KClass<MustAttackGroupEffect> = MustAttackGroupEffect::class

    override fun execute(
        state: GameState,
        effect: MustAttackGroupEffect,
        context: EffectContext
    ): EffectResult {
        val otherThan = context.controllerId.takeIf { effect.defender == MustAttackDefender.PlayerOtherThanYou }
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetMustAttack(effect.defender, otherThan),
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context,
            dynamicGroupFilter = ResolutionFilterBaker.bake(effect.filter, context, state, amountEvaluator)
        )
        return EffectResult.success(newState)
    }
}
