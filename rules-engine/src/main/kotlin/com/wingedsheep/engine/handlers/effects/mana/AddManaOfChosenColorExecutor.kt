package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import kotlin.reflect.KClass

/**
 * Executor for AddManaOfChosenColorEffect.
 * "{T}: Add one mana of the chosen color."
 *
 * Reads the [ChosenColorComponent] from the source permanent and produces
 * mana of that color. If no color was chosen, no mana is produced.
 */
class AddManaOfChosenColorExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddManaOfChosenColorEffect> {

    override val effectType: KClass<AddManaOfChosenColorEffect> = AddManaOfChosenColorEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaOfChosenColorEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state)
        val sourceEntity = state.getEntity(sourceId)
            ?: return ExecutionResult.success(state)

        val chosenColor = sourceEntity.get<ChosenColorComponent>()?.color
            ?: return ExecutionResult.success(state)

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updatedPool = if (effect.restriction != null) {
                manaPool.addRestricted(chosenColor, amount, effect.restriction!!)
            } else {
                manaPool.add(chosenColor, amount)
            }
            container.with(updatedPool)
        }

        return ExecutionResult.success(newState)
    }
}
