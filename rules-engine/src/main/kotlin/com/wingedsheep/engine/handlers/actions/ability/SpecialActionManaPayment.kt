package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.withSpendingColors
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Pays the fixed mana cost of a **special action** (CR 116.2) — foretell's {2}, the companion's
 * {3}: floating mana first, then the player's chosen sources ([PaymentStrategy.Explicit]) or the
 * auto-tapper for the remainder, ending in one [ManaSpentEvent] labelled [reason].
 *
 * A special action has no spell context, so there is nothing for a cost reducer or a spending
 * restriction to read; that is what keeps this smaller than the cast and activation payers.
 */
internal class SpecialActionManaPayment(
    private val manaSolver: ManaSolver,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
) {
    /** The validation half: an error message, or null when [cost] can be paid as [strategy] says. */
    fun validate(state: GameState, playerId: EntityId, cost: ManaCost, strategy: PaymentStrategy): String? {
        if (strategy is PaymentStrategy.Explicit) {
            for (sourceId in strategy.manaAbilitiesToActivate) {
                val sourceContainer = state.getEntity(sourceId) ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) return "Mana source is already tapped: $sourceId"
            }
            return null
        }
        return if (manaSolver.canPay(state, playerId, cost)) null else "Not enough mana to pay $cost"
    }

    /** Pays [cost]; null when the auto-tapper can't find the mana after all. */
    fun pay(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        strategy: PaymentStrategy,
        reason: String,
    ): Pair<GameState, List<GameEvent>>? {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        val poolComponent = currentState.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val pool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana,
            snowMana = poolComponent.snowMana,
            manaUnits = poolComponent.manaUnits,
            snowColorless = poolComponent.snowColorless
        ).withSpendingColors(currentState, playerId)
        val partial = pool.payPartial(cost)
        val poolAfter = partial.newPool
        val spentFromPool = partial.manaSpent

        var white = spentFromPool.white
        var blue = spentFromPool.blue
        var black = spentFromPool.black
        var red = spentFromPool.red
        var green = spentFromPool.green
        var colorless = spentFromPool.colorless

        currentState = currentState.updateEntity(playerId) { c ->
            c.with(
                ManaPoolComponent(
                    white = poolAfter.white,
                    blue = poolAfter.blue,
                    black = poolAfter.black,
                    red = poolAfter.red,
                    green = poolAfter.green,
                    colorless = poolAfter.colorless,
                    restrictedMana = poolAfter.restrictedMana,
                    snowMana = poolAfter.snowMana,
                    manaUnits = poolAfter.manaUnits,
                    snowColorless = poolAfter.snowColorless
                )
            )
        }

        if (!partial.remainingCost.isEmpty()) {
            if (strategy is PaymentStrategy.Explicit) {
                val credited = manaSolver.creditExplicitSources(currentState, playerId, strategy.manaAbilitiesToActivate, partial.remainingCost)
                for (sourceId in strategy.manaAbilitiesToActivate) {
                    val (tapped, tapEvents) = manaAbilitySideEffectExecutor
                        .tapForManaPayingLife(currentState, sourceId, playerId, credited[sourceId])
                    currentState = tapped
                    events.addAll(tapEvents)
                }
            } else {
                val solution = manaSolver.solve(currentState, playerId, partial.remainingCost, 0) ?: return null
                val (tapped, tapEvents) = manaAbilitySideEffectExecutor
                    .tapSourcesWithSideEffects(currentState, solution, playerId)
                currentState = tapped
                events.addAll(tapEvents)
                for ((_, production) in solution.manaProduced) {
                    when (production.color) {
                        Color.WHITE -> white++
                        Color.BLUE -> blue++
                        Color.BLACK -> black++
                        Color.RED -> red++
                        Color.GREEN -> green++
                        null -> colorless += production.colorless
                    }
                }
            }
        }

        events.add(
            ManaSpentEvent(
                playerId = playerId,
                reason = reason,
                white = white,
                blue = blue,
                black = black,
                red = red,
                green = green,
                colorless = colorless
            )
        )
        return currentState to events
    }
}
