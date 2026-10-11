package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.AdaptEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import kotlin.reflect.KClass

/**
 * Executor for [AdaptEffect] — "If this permanent has no +1/+1 counters on it, put N +1/+1
 * counters on it" (CR 701.46a), checked as the effect resolves.
 *
 * An [SerializableModification.AdaptAsThoughNoCountersShield] on the permanent (Biomancer's
 * Familiar) makes this adapt ignore its existing counters, and this adapt uses the shield up
 * whether or not it needed it — it was "the next time" the creature adapted. Placement goes through
 * [AddCountersExecutor], so counter-placement replacements (Hardened Scales) and counter events
 * apply as for any other placement.
 */
class AdaptExecutor(
    predicateEvaluator: PredicateEvaluator
) : EffectExecutor<AdaptEffect> {

    override val effectType: KClass<AdaptEffect> = AdaptEffect::class

    private val placement = AddCountersExecutor(predicateEvaluator)

    override fun execute(
        state: GameState,
        effect: AdaptEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state, emptyList())
        if (targetId !in state.getBattlefield()) return EffectResult.success(state, emptyList())

        val shieldIndex = state.floatingEffects.indexOfFirst {
            it.effect.modification is SerializableModification.AdaptAsThoughNoCountersShield &&
                targetId in it.effect.affectedEntities
        }
        val afterShield = if (shieldIndex == -1) state else
            state.copy(floatingEffects = state.floatingEffects.filterIndexed { i, _ -> i != shieldIndex })

        val hasPlusOneCounters = (state.getEntity(targetId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) > 0
        if (hasPlusOneCounters && shieldIndex == -1) return EffectResult.success(afterShield, emptyList())

        return placement.execute(
            afterShield,
            AddCountersEffect(CounterType.PLUS_ONE_PLUS_ONE, effect.amount, effect.target),
            context
        )
    }
}
