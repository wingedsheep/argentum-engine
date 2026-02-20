package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
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
class AddAnyColorManaExecutor : EffectExecutor<AddAnyColorManaEffect> {

    override val effectType: KClass<AddAnyColorManaEffect> = AddAnyColorManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddAnyColorManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val color = context.manaColorChoice ?: Color.GREEN

        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.add(color, effect.amount))
        }

        return ExecutionResult.success(newState)
    }
}
