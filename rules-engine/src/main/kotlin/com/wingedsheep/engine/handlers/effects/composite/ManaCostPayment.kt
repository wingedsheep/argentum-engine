package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Non-optional payment of an arbitrary [ManaCost] (colored pips included) shared by
 * [PayManaCostExecutor] (fixed cost) and [PayDynamicManaCostExecutor] (resolution-computed
 * generic amount + cross-player payer): spend whatever [player] already has floating, auto-tap
 * their mana sources for the remainder, and deduct [cost] from their pool. Auto-tapped sources
 * emit [TappedEvent]s. Insufficient mana is a recoverable [EffectResult.error] — callers that
 * pre-gate affordability (e.g. `Gate.MayPay`) only hit it on genuinely degenerate input.
 */
fun payManaCostFromPool(
    state: GameState,
    player: EntityId,
    cost: ManaCost,
    cardRegistry: CardRegistry
): EffectResult {
    val playerEntity = state.getEntity(player)
        ?: return EffectResult.error(state, "Paying player not found")

    val manaPoolComponent = playerEntity.get<ManaPoolComponent>()
        ?: return EffectResult.error(state, "Player has no mana pool")

    val manaPool = ManaPool(
        manaPoolComponent.white,
        manaPoolComponent.blue,
        manaPoolComponent.black,
        manaPoolComponent.red,
        manaPoolComponent.green,
        manaPoolComponent.colorless
    )

    val partialResult = manaPool.payPartial(cost)
    val remainingCost = partialResult.remainingCost
    var currentPool = manaPool
    var currentState = state
    val events = mutableListOf<GameEvent>()

    if (!remainingCost.isEmpty()) {
        val manaSolver = ManaSolver(cardRegistry)
        val solution = manaSolver.solve(currentState, player, remainingCost)
            ?: return EffectResult.error(state, "Cannot pay mana cost")

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

    val newPool = currentPool.pay(cost)
        ?: return EffectResult.error(state, "Cannot pay mana cost after auto-tap")

    currentState = currentState.updateEntity(player) { container ->
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

    return EffectResult.success(currentState, events)
}
