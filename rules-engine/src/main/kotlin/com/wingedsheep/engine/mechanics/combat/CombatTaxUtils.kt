package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId

/**
 * Pay a generic mana tax (attack or block) for a player.
 * Auto-taps mana sources if needed.
 *
 * @param taxType "attack" or "block" for error messages
 */
internal fun payManaTax(
    state: GameState,
    playerId: EntityId,
    totalGenericTax: Int,
    taxType: String,
    cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor
): ExecutionResult {
    val totalCost = ManaCost(List(totalGenericTax) { ManaSymbol.generic(1) })

    val playerEntity = state.getEntity(playerId)
        ?: return ExecutionResult.error(state, "Player not found")
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

    val partialResult = manaPool.payPartial(totalCost)
    val remainingCost = partialResult.remainingCost
    var currentPool = manaPool
    var currentState = state
    val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

    if (!remainingCost.isEmpty()) {
        val manaSolver = ManaSolver(cardRegistry)
        val solution = manaSolver.solve(currentState, playerId, remainingCost)
            ?: return ExecutionResult.error(
                currentState,
                "Cannot pay $taxType tax of {$totalGenericTax} (not enough mana available)"
            )

        val (stateAfterTaps, tapEvents) = manaAbilitySideEffectExecutor
            .tapSourcesWithSideEffects(currentState, solution, playerId)
        currentState = stateAfterTaps
        events.addAll(tapEvents)

        for ((_, production) in solution.manaProduced) {
            currentPool = if (production.color != null) {
                currentPool.add(production.color, production.amount)
            } else {
                currentPool.addColorless(production.colorless)
            }
        }
    }

    val newPool = currentPool.pay(totalCost)
        ?: return ExecutionResult.error(currentState, "Cannot pay $taxType tax after auto-tap")

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
