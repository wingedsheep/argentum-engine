package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import kotlin.reflect.KClass

/**
 * Executor for AddColorlessManaEffect.
 * "Add {C}{C}"
 */
class AddColorlessManaExecutor : EffectExecutor<AddColorlessManaEffect> {

    override val effectType: KClass<AddColorlessManaEffect> = AddColorlessManaEffect::class

    override fun execute(
        state: GameState,
        effect: AddColorlessManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.addColorless(effect.amount))
        }

        return ExecutionResult.success(newState)
    }
}
