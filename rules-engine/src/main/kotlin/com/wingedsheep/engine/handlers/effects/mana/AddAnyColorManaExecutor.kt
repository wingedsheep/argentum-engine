package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddAnyColorManaEffect.
 * "Add one mana of any color."
 *
 * The color is chosen by the player via [EffectContext.manaColorChoice].
 * Defaults to GREEN if no choice is provided (e.g., auto-pay fallback).
 */
class AddAnyColorManaExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<AddAnyColorManaEffect> {

    override val effectType: KClass<AddAnyColorManaEffect> = AddAnyColorManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddAnyColorManaEffect,
        context: EffectContext
    ): EffectResult {
        val color = context.manaColorChoice ?: Color.GREEN
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) {
            return EffectResult.success(state)
        }

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            val updatedPool = if (effect.restriction != null) {
                manaPool.addRestricted(color, amount, effect.restriction!!)
            } else {
                manaPool.add(color, amount)
            }
            container.with(updatedPool)
        }

        return EffectResult.success(newState)
    }
}
