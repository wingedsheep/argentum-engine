package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.AddDynamicColorManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddDynamicColorManaEffect.
 * "Add {R} for each Goblin on the battlefield."
 */
class AddDynamicColorManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddDynamicColorManaEffect> {

    override val effectType: KClass<AddDynamicColorManaEffect> = AddDynamicColorManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddDynamicColorManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val amount = amountEvaluator.evaluate(state, effect.amountSource, context)
        if (amount <= 0) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.add(effect.color, amount))
        }

        return ExecutionResult.success(newState)
    }
}
