package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.AddManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddManaEffect.
 * "Add {G}" or "Add {R}{R}"
 */
class AddManaExecutor : EffectExecutor<AddManaEffect> {

    override val effectType: KClass<AddManaEffect> = AddManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.add(effect.color, effect.amount))
        }

        return ExecutionResult.success(newState)
    }
}
