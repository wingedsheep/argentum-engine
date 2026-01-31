package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.CreatureDamageFilter
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for DealDamageToGroupEffect.
 * Deals damage to all creatures matching a filter.
 *
 * Examples:
 * - Pyroclasm: 2 damage to each creature
 * - Needle Storm: 4 damage to each creature with flying
 */
class DealDamageToGroupExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<DealDamageToGroupEffect> {

    override val effectType: KClass<DealDamageToGroupEffect> = DealDamageToGroupEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamageToGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val damageAmount = amountEvaluator.evaluate(state, effect.amount, context)

        // If damage is 0, no damage is dealt
        if (damageAmount == 0) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Apply creature filter
            val matches = when (val filter = effect.filter) {
                CreatureDamageFilter.All -> true
                is CreatureDamageFilter.WithKeyword -> cardComponent.baseKeywords.contains(filter.keyword)
                is CreatureDamageFilter.WithoutKeyword -> !cardComponent.baseKeywords.contains(filter.keyword)
                is CreatureDamageFilter.OfColor -> cardComponent.colors.contains(filter.color)
                is CreatureDamageFilter.NotOfColor -> !cardComponent.colors.contains(filter.color)
                CreatureDamageFilter.Attacking -> container.has<AttackingComponent>()
            }

            if (!matches) continue

            val result = dealDamageToTarget(newState, entityId, damageAmount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
