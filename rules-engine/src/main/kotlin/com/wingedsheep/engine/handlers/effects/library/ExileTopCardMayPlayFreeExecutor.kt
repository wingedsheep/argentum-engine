package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantMayPlayFromExileEffect.
 *
 * Marks all cards in a named collection with MayPlayFromExileComponent,
 * granting the controller permission to play them from exile until end of turn.
 */
class GrantMayPlayFromExileExecutor : EffectExecutor<GrantMayPlayFromExileEffect> {

    override val effectType: KClass<GrantMayPlayFromExileEffect> = GrantMayPlayFromExileEffect::class

    override fun execute(
        state: GameState,
        effect: GrantMayPlayFromExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val collection = context.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(MayPlayFromExileComponent(controllerId = controllerId))
            }
        }

        return ExecutionResult.success(newState)
    }
}

/**
 * Executor for GrantPlayWithoutPayingCostEffect.
 *
 * Marks all cards in a named collection with PlayWithoutPayingCostComponent,
 * allowing the controller to play them without paying mana cost until end of turn.
 */
class GrantPlayWithoutPayingCostExecutor : EffectExecutor<GrantPlayWithoutPayingCostEffect> {

    override val effectType: KClass<GrantPlayWithoutPayingCostEffect> = GrantPlayWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayWithoutPayingCostEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val collection = context.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            }
        }

        return ExecutionResult.success(newState)
    }
}
