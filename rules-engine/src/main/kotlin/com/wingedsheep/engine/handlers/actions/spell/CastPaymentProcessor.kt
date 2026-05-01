package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry

/**
 * Result of a mana payment attempt.
 */
data class PaymentResult(
    val state: GameState,
    val events: List<GameEvent>,
    val error: String?
)

/**
 * Processes mana payment for spell casting using one of three strategies:
 * AutoPay (solver taps lands), FromPool (use floating mana), or Explicit (specific sources).
 */
class CastPaymentProcessor(
    private val manaSolver: ManaSolver,
    private val costHandler: CostHandler,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor
) {
    private fun toManaPool(component: ManaPoolComponent) = ManaPool(
        white = component.white,
        blue = component.blue,
        black = component.black,
        red = component.red,
        green = component.green,
        colorless = component.colorless,
        restrictedMana = component.restrictedMana
    )

    private fun toComponent(pool: ManaPool) = ManaPoolComponent(
        white = pool.white,
        blue = pool.blue,
        black = pool.black,
        red = pool.red,
        green = pool.green,
        colorless = pool.colorless,
        restrictedMana = pool.restrictedMana
    )

    fun processPayment(
        state: GameState,
        action: com.wingedsheep.engine.core.CastSpell,
        effectiveCost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null
    ): PaymentResult {
        return when (action.paymentStrategy) {
            is PaymentStrategy.FromPool -> payFromPool(state, action.playerId, effectiveCost, cardName, xValue, spellContext)
            is PaymentStrategy.AutoPay -> autoPay(state, action.playerId, effectiveCost, cardName, xValue, spellContext)
            is PaymentStrategy.Explicit -> explicitPay(
                state,
                action.playerId,
                action.paymentStrategy,
                effectiveCost,
                cardName,
                xValue,
                spellContext
            )
        }
    }

    private fun payFromPool(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null
    ): PaymentResult {
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = toManaPool(poolComponent)

        // Pay base cost first
        var poolAfterPayment = costHandler.payManaCost(pool, cost, spellContext)
            ?: return PaymentResult(state, emptyList(), "Insufficient mana in pool")

        // Track mana spent for the event (unrestricted only — restricted changes tracked by count difference)
        val unrestrictedBefore = ManaPool(poolComponent.white, poolComponent.blue, poolComponent.black, poolComponent.red, poolComponent.green, poolComponent.colorless)
        val unrestrictedAfter = ManaPool(poolAfterPayment.white, poolAfterPayment.blue, poolAfterPayment.black, poolAfterPayment.red, poolAfterPayment.green, poolAfterPayment.colorless)
        val restrictedSpent = poolComponent.restrictedMana.size - poolAfterPayment.restrictedMana.size

        var whiteSpent = poolComponent.white - poolAfterPayment.white
        var blueSpent = poolComponent.blue - poolAfterPayment.blue
        var blackSpent = poolComponent.black - poolAfterPayment.black
        var redSpent = poolComponent.red - poolAfterPayment.red
        var greenSpent = poolComponent.green - poolAfterPayment.green
        var colorlessSpent = poolComponent.colorless - poolAfterPayment.colorless

        // Count restricted mana spent by color for tracking
        val restrictedSpentByColor = countRestrictedSpentByColor(poolComponent.restrictedMana, poolAfterPayment.restrictedMana)
        whiteSpent += restrictedSpentByColor.getOrDefault(Color.WHITE, 0)
        blueSpent += restrictedSpentByColor.getOrDefault(Color.BLUE, 0)
        blackSpent += restrictedSpentByColor.getOrDefault(Color.BLACK, 0)
        redSpent += restrictedSpentByColor.getOrDefault(Color.RED, 0)
        greenSpent += restrictedSpentByColor.getOrDefault(Color.GREEN, 0)
        colorlessSpent += restrictedSpentByColor.getOrDefault(null, 0)

        // Pay for X from remaining pool (multiply by X symbol count for XX costs)
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        var xRemainingToPay = xValue * xSymbolCount

        // Spend eligible restricted mana for X first
        if (spellContext != null) {
            for (entry in poolAfterPayment.restrictedMana.toList()) {
                if (xRemainingToPay <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    val spent = poolAfterPayment.spendRestricted(entry.color, spellContext)
                    if (spent != null) {
                        poolAfterPayment = spent
                        if (entry.color != null) {
                            when (entry.color) {
                                Color.WHITE -> whiteSpent++
                                Color.BLUE -> blueSpent++
                                Color.BLACK -> blackSpent++
                                Color.RED -> redSpent++
                                Color.GREEN -> greenSpent++
                            }
                        } else colorlessSpent++
                        xRemainingToPay--
                    }
                }
            }
        }

        // Spend colorless first for X
        while (xRemainingToPay > 0 && poolAfterPayment.colorless > 0) {
            poolAfterPayment = poolAfterPayment.spendColorless()!!
            colorlessSpent++
            xRemainingToPay--
        }

        // Spend colored mana for remaining X
        for (color in Color.entries) {
            while (xRemainingToPay > 0 && poolAfterPayment.get(color) > 0) {
                poolAfterPayment = poolAfterPayment.spend(color)!!
                when (color) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                xRemainingToPay--
            }
        }

        // Check if we could pay for all of X
        if (xRemainingToPay > 0) {
            return PaymentResult(state, emptyList(), "Insufficient mana in pool for X cost")
        }

        val newState = state.updateEntity(playerId) { container ->
            container.with(toComponent(poolAfterPayment))
        }

        val event = ManaSpentEvent(
            playerId = playerId,
            reason = "Cast $cardName",
            white = whiteSpent,
            blue = blueSpent,
            black = blackSpent,
            red = redSpent,
            green = greenSpent,
            colorless = colorlessSpent
        )

        return PaymentResult(newState, listOf(event), null)
    }

    private fun autoPay(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null,
        excludeSources: Set<EntityId> = emptySet()
    ): PaymentResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Use floating mana first
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = toManaPool(poolComponent)

        val partialResult = pool.payPartial(cost, spellContext)
        var poolAfterPayment = partialResult.newPool
        val remainingCost = partialResult.remainingCost
        val manaSpentFromPool = partialResult.manaSpent

        var whiteSpent = manaSpentFromPool.white
        var blueSpent = manaSpentFromPool.blue
        var blackSpent = manaSpentFromPool.black
        var redSpent = manaSpentFromPool.red
        var greenSpent = manaSpentFromPool.green
        var colorlessSpent = manaSpentFromPool.colorless

        // Use remaining floating mana for X cost (multiply by X symbol count for XX costs)
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        var xRemainingToPay = xValue * xSymbolCount

        // Spend eligible restricted mana for X first
        if (spellContext != null) {
            for (entry in poolAfterPayment.restrictedMana.toList()) {
                if (xRemainingToPay <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    val spent = poolAfterPayment.spendRestricted(entry.color, spellContext)
                    if (spent != null) {
                        poolAfterPayment = spent
                        if (entry.color != null) {
                            when (entry.color) {
                                Color.WHITE -> whiteSpent++
                                Color.BLUE -> blueSpent++
                                Color.BLACK -> blackSpent++
                                Color.RED -> redSpent++
                                Color.GREEN -> greenSpent++
                            }
                        } else colorlessSpent++
                        xRemainingToPay--
                    }
                }
            }
        }

        // Spend colorless first for X
        while (xRemainingToPay > 0 && poolAfterPayment.colorless > 0) {
            poolAfterPayment = poolAfterPayment.spendColorless()!!
            colorlessSpent++
            xRemainingToPay--
        }

        // Spend colored mana for remaining X
        for (color in Color.entries) {
            while (xRemainingToPay > 0 && poolAfterPayment.get(color) > 0) {
                poolAfterPayment = poolAfterPayment.spend(color)!!
                when (color) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                xRemainingToPay--
            }
        }

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(toComponent(poolAfterPayment))
        }

        // Tap lands for remaining cost (using xRemainingToPay instead of full xValue)
        if (!remainingCost.isEmpty() || xRemainingToPay > 0) {
            val solution = manaSolver.solve(currentState, playerId, remainingCost, xRemainingToPay, excludeSources = excludeSources, spellContext = spellContext)
                ?: return PaymentResult(currentState, events, "Not enough mana to auto-pay")

            // Tap each source AND run any non-mana side effects of the matching
            // activated mana ability (e.g. Adarkar Wastes' "this land deals 1
            // damage to you"). The mana itself is consumed via
            // `solution.manaProduced` below.
            val (stateAfterTaps, tapEvents) = manaAbilitySideEffectExecutor
                .tapSourcesWithSideEffects(currentState, solution, playerId)
            currentState = stateAfterTaps
            events.addAll(tapEvents)

            for ((_, production) in solution.manaProduced) {
                when (production.color) {
                    Color.WHITE -> whiteSpent += production.amount
                    Color.BLUE -> blueSpent += production.amount
                    Color.BLACK -> blackSpent += production.amount
                    Color.RED -> redSpent += production.amount
                    Color.GREEN -> greenSpent += production.amount
                    null -> colorlessSpent += production.colorless
                }
            }

            // Add only the bonus mana that wasn't consumed by the solver to the floating pool
            if (solution.remainingBonusMana.isNotEmpty()) {
                currentState = currentState.updateEntity(playerId) { container ->
                    var pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    for ((color, amount) in solution.remainingBonusMana) {
                        pool = pool.add(color, amount)
                    }
                    container.with(pool)
                }
            }
        }

        events.add(
            ManaSpentEvent(
                playerId = playerId,
                reason = "Cast $cardName",
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )

        return PaymentResult(currentState, events, null)
    }

    /**
     * Pay a spell's mana cost using only the player-chosen sources as candidates.
     *
     * The client's mana selection UI can over-specify sources — for example, when a
     * spell with convoke reduces its cost after creatures are tapped, the pre-cast
     * auto-tap preview (computed against the full cost) over-selects lands. Rather
     * than tapping every chosen source unconditionally, we delegate to the mana
     * solver with the non-chosen sources excluded, so only the minimum subset
     * actually needed to cover the (already cost-reduced) payment gets tapped.
     *
     * Validation (`CastSpellHandler.validatePayment`) already uses the same solver
     * call with the same exclusion — execution matching validation ensures we never
     * tap lands that weren't required.
     */
    private fun explicitPay(
        state: GameState,
        playerId: EntityId,
        strategy: PaymentStrategy.Explicit,
        cost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null
    ): PaymentResult {
        val chosenSet = strategy.manaAbilitiesToActivate.toSet()
        val excluded = manaSolver.findAvailableManaSources(state, playerId)
            .map { it.entityId }
            .filter { it !in chosenSet }
            .toSet()
        return autoPay(state, playerId, cost, cardName, xValue, spellContext, excluded)
    }

    /**
     * Count restricted mana spent by color by comparing before/after restricted mana lists.
     */
    private fun countRestrictedSpentByColor(
        before: List<RestrictedManaEntry>,
        after: List<RestrictedManaEntry>
    ): Map<Color?, Int> {
        val beforeCounts = before.groupingBy { it.color }.eachCount()
        val afterCounts = after.groupingBy { it.color }.eachCount()
        return beforeCounts.mapValues { (color, count) ->
            count - (afterCounts[color] ?: 0)
        }.filter { it.value > 0 }
    }
}
