package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddManaEffect.
 * "Add {G}" or "Add {R}{R}" or "Add {R} for each Goblin on the battlefield."
 */
class AddManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddManaEffect> {

    override val effectType: KClass<AddManaEffect> = AddManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updatedPool = if (effect.restriction != null) {
                manaPool.addRestricted(effect.color, amount, effect.restriction!!)
            } else {
                manaPool.add(effect.color, amount)
            }
            container.with(updatedPool)
        }

        return EffectResult.success(newState)
    }
}
