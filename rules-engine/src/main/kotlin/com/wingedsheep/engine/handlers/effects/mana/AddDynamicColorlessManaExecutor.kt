package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.AddDynamicColorlessManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddDynamicColorlessManaEffect.
 * "Add an amount of {C} equal to..."
 */
class AddDynamicColorlessManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddDynamicColorlessManaEffect> {

    override val effectType: KClass<AddDynamicColorlessManaEffect> = AddDynamicColorlessManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddDynamicColorlessManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val amount = amountEvaluator.evaluate(state, effect.amountSource, context)
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
