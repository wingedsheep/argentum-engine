package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddColorlessManaEffect.
 * "Add {C}{C}" or "Add an amount of {C} equal to..."
 */
class AddColorlessManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddColorlessManaEffect> {

    override val effectType: KClass<AddColorlessManaEffect> = AddColorlessManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddColorlessManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.addColorless(amount))
        }

        return ExecutionResult.success(newState)
    }
}
