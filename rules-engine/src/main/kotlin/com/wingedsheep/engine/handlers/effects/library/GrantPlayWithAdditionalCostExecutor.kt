package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithAdditionalCostEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantPlayWithAdditionalCostEffect.
 *
 * Marks all cards in a named collection with PlayWithAdditionalCostComponent,
 * requiring the specified additional cost when casting from exile.
 */
class GrantPlayWithAdditionalCostExecutor : EffectExecutor<GrantPlayWithAdditionalCostEffect> {

    override val effectType: KClass<GrantPlayWithAdditionalCostEffect> =
        GrantPlayWithAdditionalCostEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayWithAdditionalCostEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(
                    PlayWithAdditionalCostComponent(
                        controllerId = controllerId,
                        additionalCosts = listOf(effect.additionalCost)
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }
}
