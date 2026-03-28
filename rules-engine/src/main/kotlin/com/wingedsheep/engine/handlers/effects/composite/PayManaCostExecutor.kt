package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import kotlin.reflect.KClass

/**
 * Executor for PayManaCostEffect.
 * Non-optional mana payment: auto-taps lands and deducts the cost.
 */
class PayManaCostExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<PayManaCostEffect> {

    override val effectType: KClass<PayManaCostEffect> = PayManaCostEffect::class

    override fun execute(
        state: GameState,
        effect: PayManaCostEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        val playerEntity = state.getEntity(playerId)
            ?: return ExecutionResult.error(state, "Paying player not found")

        val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
            ?: return ExecutionResult.error(state, "Player has no mana pool")

        val manaPool = ManaPool(
            manaPoolComponent.white,
            manaPoolComponent.blue,
            manaPoolComponent.black,
            manaPoolComponent.red,
            manaPoolComponent.green,
            manaPoolComponent.colorless
        )

        val partialResult = manaPool.payPartial(effect.cost)
        val remainingCost = partialResult.remainingCost
        var currentPool = manaPool
        var currentState = state
        val events = mutableListOf<GameEvent>()

        if (!remainingCost.isEmpty()) {
            val manaSolver = ManaSolver(cardRegistry)
            val solution = manaSolver.solve(currentState, playerId, remainingCost)
                ?: return ExecutionResult.error(state, "Cannot pay mana cost")

            for (source in solution.sources) {
                currentState = currentState.updateEntity(source.entityId) { c ->
                    c.with(TappedComponent)
                }
                events.add(TappedEvent(source.entityId, source.name))
            }

            for ((_, production) in solution.manaProduced) {
                currentPool = if (production.color != null) {
                    currentPool.add(production.color)
                } else {
                    currentPool.addColorless(production.colorless)
                }
            }
        }

        val newPool = currentPool.pay(effect.cost)
            ?: return ExecutionResult.error(state, "Cannot pay mana cost after auto-tap")

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(
                ManaPoolComponent(
                    white = newPool.white,
                    blue = newPool.blue,
                    black = newPool.black,
                    red = newPool.red,
                    green = newPool.green,
                    colorless = newPool.colorless
                )
            )
        }

        return ExecutionResult.success(currentState, events)
    }
}
