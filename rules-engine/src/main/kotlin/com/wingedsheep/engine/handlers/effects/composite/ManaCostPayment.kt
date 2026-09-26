package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.tapForMana
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.ManaSource
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
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
    cardRegistry: CardRegistry,
    predicateEvaluator: PredicateEvaluator
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
        val manaSolver = ManaSolver(cardRegistry, predicateEvaluator = predicateEvaluator)
        val solution = manaSolver.solve(currentState, player, remainingCost)
            ?: return EffectResult.error(state, "Cannot pay mana cost")

        for (source in solution.sources) {
            val (tappedState, tapEvents) = tapForMana(currentState, source.entityId, player)
            currentState = tappedState
            events.addAll(tapEvents)
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

/**
 * Whether [player] can pay [cost] *through the path [payManaCostFromPool] actually takes*: floating
 * mana first, then auto-tap for the remainder.
 *
 * Deliberately **not** [ManaSolver.canPay]. `canPay` answers the broader question "is this cost
 * payable at all", counting mana the auto-tap solver refuses to spend on the player's behalf —
 * sacrificing a Treasure, Springleaf Drum's tap-a-creature sub-cost, an explicitly-activated
 * ability (see the extras fallback in `ManaSolver.canPay`, and the matching `requiresSacrifice` /
 * `tapPermanentsSubCost` filters in `ManaSolver.solve`). A caller that *gates* on `canPay` and then
 * *pays* with [payManaCostFromPool] can therefore offer a payment that errors mid-resolution. Gate
 * on this instead whenever the payment is the auto-tapped one.
 *
 * [precomputedSources] is threaded straight to [ManaSolver.solve]; hoist it once with
 * [ManaSolver.findAvailableManaSources] when probing the same state repeatedly.
 */
fun canAutoPayManaCost(
    state: GameState,
    player: EntityId,
    cost: ManaCost,
    cardRegistry: CardRegistry,
    precomputedSources: List<ManaSource>? = null,
    predicateEvaluator: PredicateEvaluator
): Boolean {
    val manaPoolComponent = state.getEntity(player)?.get<ManaPoolComponent>() ?: return false

    val manaPool = ManaPool(
        manaPoolComponent.white,
        manaPoolComponent.blue,
        manaPoolComponent.black,
        manaPoolComponent.red,
        manaPoolComponent.green,
        manaPoolComponent.colorless
    )

    val remainingCost = manaPool.payPartial(cost).remainingCost
    if (remainingCost.isEmpty()) return true

    return ManaSolver(cardRegistry, predicateEvaluator = predicateEvaluator)
        .solve(state, player, remainingCost, precomputedSources = precomputedSources) != null
}
