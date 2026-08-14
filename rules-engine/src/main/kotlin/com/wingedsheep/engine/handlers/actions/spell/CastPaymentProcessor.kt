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
import com.wingedsheep.engine.mechanics.mana.SpentManaProvenance
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider

/**
 * Result of a mana payment attempt.
 *
 * @property consumedRiders Every [ManaSpellRider] carried by the mana actually spent on this
 *   payment (from both restricted floating mana and freshly-tapped sources). The caller applies
 *   each rider to the spell as it goes on the stack — e.g.
 *   [ManaSpellRider.MakesSpellUncounterable] stamps `CantBeCounteredComponent`. A **list**, not a
 *   set: two rider-carrying mana spent on one spell must fire the rider twice (Pyromancer's
 *   Goggles copies the spell once per {R} spent), so identical riders must not be deduplicated.
 */
data class PaymentResult(
    val state: GameState,
    val events: List<GameEvent>,
    val error: String?,
    val consumedRiders: List<ManaSpellRider> = emptyList(),
    /**
     * Provenance of the mana actually spent on this payment — which producing-source subtypes and
     * which producing sources contributed (see [SpentManaProvenance]). Combines mana pulled from the
     * floating pool (tags snapshotted at production) with mana freshly tapped by the solver during
     * this payment. The cast handler propagates it onto the engine
     * [com.wingedsheep.engine.core.SpellCastEvent] and the resolving permanent's
     * [com.wingedsheep.engine.state.components.battlefield.CastRecordComponent], driving
     * `SpellCastPredicate.PaidWithManaFromSubtype` / `PaidWithManaFromSource` triggers and the
     * `DynamicAmount.ManaSpentFromSubtype` count (Bat Colony). Treasure is just
     * `spentManaProvenance.bySubtype[Subtype.TREASURE]` (Alchemist's Talent level 3).
     */
    val spentManaProvenance: SpentManaProvenance = SpentManaProvenance(),
    /**
     * For a color-restricted `{X}` cost ("spend only [colors] on X"), the per-color
     * breakdown of mana spent on the X portion. The cast handler stores this on the spell's
     * stack object so it can be read at resolution via `DynamicAmount.ManaSpentOnX`
     * (e.g. Soul Burn's "gain life equal to the {B} spent on X"). Empty when X is unrestricted.
     */
    val xManaSpentByColor: Map<Color, Int> = emptyMap()
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
        restrictedMana = component.restrictedMana,
        manaBySubtype = component.manaBySubtype,
        manaBySource = component.manaBySource
    )

    private fun toComponent(pool: ManaPool) = ManaPoolComponent(
        white = pool.white,
        blue = pool.blue,
        black = pool.black,
        red = pool.red,
        green = pool.green,
        colorless = pool.colorless,
        restrictedMana = pool.restrictedMana,
        manaBySubtype = pool.manaBySubtype,
        manaBySource = pool.manaBySource
    )

    /**
     * Provenance of mana freshly tapped by the solver during a payment (AutoPay / Explicit). The
     * floating-pool tags don't cover it — this mana never entered the pool — so we read each tapped
     * source's subtypes from state and pair them with the source id. Combined with the pool's
     * consumed provenance to form the full [SpentManaProvenance] for the payment.
     */
    private fun tappedSourceProvenance(state: GameState, manaProduced: Map<EntityId, com.wingedsheep.engine.mechanics.mana.ManaProduction>): SpentManaProvenance {
        if (manaProduced.isEmpty()) return SpentManaProvenance()
        val bySubtype = mutableMapOf<com.wingedsheep.sdk.core.Subtype, Int>()
        val sourceIds = mutableSetOf<EntityId>()
        for ((sourceId, production) in manaProduced) {
            val amount = production.amount + production.colorless
            if (amount <= 0) continue
            sourceIds.add(sourceId)
            val subtypes = state.getEntity(sourceId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                ?.typeLine?.subtypes ?: emptySet()
            for (subtype in subtypes) bySubtype[subtype] = (bySubtype[subtype] ?: 0) + amount
        }
        return SpentManaProvenance(bySubtype, sourceIds)
    }

    /** Merge two provenance snapshots (summing subtype counts, unioning source ids). */
    private fun mergeProvenance(a: SpentManaProvenance, b: SpentManaProvenance): SpentManaProvenance {
        if (a.isEmpty) return b
        if (b.isEmpty) return a
        val bySubtype = a.bySubtype.toMutableMap()
        for ((subtype, count) in b.bySubtype) bySubtype[subtype] = (bySubtype[subtype] ?: 0) + count
        return SpentManaProvenance(bySubtype, a.sourceIds + b.sourceIds)
    }

    fun processPayment(
        state: GameState,
        action: com.wingedsheep.engine.core.CastSpell,
        effectiveCost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null,
        xManaRestriction: Set<Color> = emptySet()
    ): PaymentResult {
        return when (action.paymentStrategy) {
            is PaymentStrategy.FromPool -> payFromPool(state, action.playerId, effectiveCost, cardName, xValue, spellContext, xManaRestriction)
            is PaymentStrategy.AutoPay -> autoPay(state, action.playerId, effectiveCost, cardName, xValue, spellContext, xManaRestriction = xManaRestriction)
            is PaymentStrategy.Explicit -> explicitPay(
                state,
                action.playerId,
                action.paymentStrategy,
                effectiveCost,
                cardName,
                xValue,
                spellContext,
                xManaRestriction
            )
        }
    }

    private fun payFromPool(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null,
        xManaRestriction: Set<Color> = emptySet()
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
        // Per-color mana spent on the X portion (for DynamicAmount.ManaSpentOnX).
        val xSpentByColor = mutableMapOf<Color, Int>()
        // When X is color-restricted, only these colors may pay it (and colorless can't).
        val xColorsAllowed: Set<Color> =
            if (xManaRestriction.isEmpty()) Color.entries.toSet() else xManaRestriction

        // Spend eligible restricted mana for X first
        if (spellContext != null) {
            for (entry in poolAfterPayment.restrictedMana.toList()) {
                if (xRemainingToPay <= 0) break
                // A color-restricted X can't be paid with off-color or colorless restricted mana.
                if (entry.color != null && entry.color !in xColorsAllowed) continue
                if (entry.color == null && xManaRestriction.isNotEmpty()) continue
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
                            xSpentByColor[entry.color] = (xSpentByColor[entry.color] ?: 0) + 1
                        } else colorlessSpent++
                        xRemainingToPay--
                    }
                }
            }
        }

        // Spend unrestricted floating mana for the remaining X: colorless first (unless X is
        // color-restricted), then allowed colors. Same coverage rule as autoTapForManaCost.
        for (unit in poolAfterPayment.xCoveragePlan(xRemainingToPay, xManaRestriction)) {
            poolAfterPayment = if (unit == null) {
                colorlessSpent++
                poolAfterPayment.spendColorless()!!
            } else {
                when (unit) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                xSpentByColor[unit] = (xSpentByColor[unit] ?: 0) + 1
                poolAfterPayment.spend(unit)!!
            }
            xRemainingToPay--
        }

        // Check if we could pay for all of X
        if (xRemainingToPay > 0) {
            return PaymentResult(state, emptyList(), "Insufficient mana in pool for X cost")
        }

        // Consume provenance tags proportional to unrestricted mana pulled from the pool.
        // Restricted mana doesn't participate (tagged mana is always unrestricted). Everything is
        // paid from the pool here, so there is no freshly-tapped-source provenance to add.
        val unrestrictedSpent = (whiteSpent + blueSpent + blackSpent + redSpent + greenSpent + colorlessSpent) - restrictedSpent
        val (poolWithProvenanceUpdated, spentProvenance) = poolAfterPayment.consumeProvenance(maxOf(0, unrestrictedSpent))

        val newState = state.updateEntity(playerId) { container ->
            container.with(toComponent(poolWithProvenanceUpdated))
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

        val consumedRiders = ridersConsumedDuringPayment(poolComponent.restrictedMana, poolAfterPayment.restrictedMana)
        return PaymentResult(
            newState,
            listOf(event),
            null,
            consumedRiders,
            spentManaProvenance = spentProvenance,
            xManaSpentByColor = xSpentByColor
        )
    }

    private fun autoPay(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        cardName: String,
        xValue: Int,
        spellContext: SpellPaymentContext? = null,
        excludeSources: Set<EntityId> = emptySet(),
        xManaRestriction: Set<Color> = emptySet()
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
        // Per-color mana spent on the X portion (for DynamicAmount.ManaSpentOnX).
        val xSpentByColor = mutableMapOf<Color, Int>()
        // When X is color-restricted, only these colors may pay it (and colorless can't).
        val xColorsAllowed: Set<Color> =
            if (xManaRestriction.isEmpty()) Color.entries.toSet() else xManaRestriction

        // Spend eligible restricted mana for X first
        if (spellContext != null) {
            for (entry in poolAfterPayment.restrictedMana.toList()) {
                if (xRemainingToPay <= 0) break
                // A color-restricted X can't be paid with off-color or colorless restricted mana.
                if (entry.color != null && entry.color !in xColorsAllowed) continue
                if (entry.color == null && xManaRestriction.isNotEmpty()) continue
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
                            xSpentByColor[entry.color] = (xSpentByColor[entry.color] ?: 0) + 1
                        } else colorlessSpent++
                        xRemainingToPay--
                    }
                }
            }
        }

        // Spend unrestricted floating mana for the remaining X: colorless first (unless X is
        // color-restricted), then allowed colors. Same coverage rule as autoTapForManaCost.
        for (unit in poolAfterPayment.xCoveragePlan(xRemainingToPay, xManaRestriction)) {
            poolAfterPayment = if (unit == null) {
                colorlessSpent++
                poolAfterPayment.spendColorless()!!
            } else {
                when (unit) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                xSpentByColor[unit] = (xSpentByColor[unit] ?: 0) + 1
                poolAfterPayment.spend(unit)!!
            }
            xRemainingToPay--
        }

        // Consume provenance tags proportional to unrestricted mana pulled from the pool during the
        // floating-mana phase. Freshly-tapped sources (below) contribute their own provenance —
        // unlike the legacy Treasure counter, general provenance covers solver-tapped mana too
        // (Caves and the LCI mana-source lands are tapped directly, not filtered out like Treasure).
        val poolUnrestrictedSpent = maxOf(
            0,
            (poolComponent.white - poolAfterPayment.white) +
                (poolComponent.blue - poolAfterPayment.blue) +
                (poolComponent.black - poolAfterPayment.black) +
                (poolComponent.red - poolAfterPayment.red) +
                (poolComponent.green - poolAfterPayment.green) +
                (poolComponent.colorless - poolAfterPayment.colorless)
        )
        val (poolWithProvenanceUpdated, poolProvenance) = poolAfterPayment.consumeProvenance(poolUnrestrictedSpent)
        var spentProvenance = poolProvenance

        currentState = currentState.updateEntity(playerId) { container ->
            container.with(toComponent(poolWithProvenanceUpdated))
        }

        // Tap lands for remaining cost (using xRemainingToPay instead of full xValue)
        var solutionConsumedRiders: List<ManaSpellRider> = emptyList()
        if (!remainingCost.isEmpty() || xRemainingToPay > 0) {
            val solution = manaSolver.solve(currentState, playerId, remainingCost, xRemainingToPay, excludeSources = excludeSources, spellContext = spellContext, xManaRestriction = xManaRestriction)
                ?: return PaymentResult(currentState, events, "Not enough mana to auto-pay")
            solutionConsumedRiders = solution.consumedRiders
            // Mana tapped directly for this payment carries the provenance of its source (read from
            // the pre-payment [state], where every tapped source still exists with its type line).
            spentProvenance = mergeProvenance(spentProvenance, tappedSourceProvenance(state, solution.manaProduced))
            // Fold the X portion the solver tapped (allowed colors only) into the X-by-color tally.
            for ((color, amount) in solution.xRestrictedManaSpent) {
                xSpentByColor[color] = (xSpentByColor[color] ?: 0) + amount
            }

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

            // Aura bonus mana (Shimmerwilds Growth, Fertile Ground, …) spent on the cost is not in
            // `manaProduced`; fold it in so "if {B}{B} was spent" gates (Deceit) see it. See
            // [ManaSolution.bonusManaSpentByColor].
            for ((color, amount) in solution.bonusManaSpentByColor) {
                when (color) {
                    Color.WHITE -> whiteSpent += amount
                    Color.BLUE -> blueSpent += amount
                    Color.BLACK -> blackSpent += amount
                    Color.RED -> redSpent += amount
                    Color.GREEN -> greenSpent += amount
                }
            }

            // Add only the bonus mana that wasn't consumed by the solver to the floating pool.
            // Bonus mana from a restricted ability keeps its restriction so the player can't
            // launder e.g. Steelswarm Operator's artifact-only mana into unrestricted blue.
            if (solution.remainingBonusMana.isNotEmpty()) {
                currentState = currentState.updateEntity(playerId) { container ->
                    var pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                    for (entry in solution.remainingBonusMana) {
                        pool = when {
                            // Colorless excess (e.g. Sol Ring's unused second {C}) floats as colorless.
                            entry.colorless && entry.restriction != null ->
                                pool.addRestricted(null, entry.amount, entry.restriction)
                            entry.colorless -> pool.addColorless(entry.amount)
                            entry.restriction != null ->
                                pool.addRestricted(entry.color, entry.amount, entry.restriction)
                            else -> pool.add(entry.color, entry.amount)
                        }
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

        val consumedRiders =
            ridersConsumedDuringPayment(poolComponent.restrictedMana, poolAfterPayment.restrictedMana) + solutionConsumedRiders
        return PaymentResult(
            currentState,
            events,
            null,
            consumedRiders,
            spentManaProvenance = spentProvenance,
            xManaSpentByColor = xSpentByColor
        )
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
        spellContext: SpellPaymentContext? = null,
        xManaRestriction: Set<Color> = emptySet()
    ): PaymentResult {
        val chosenSet = strategy.manaAbilitiesToActivate.toSet()
        val excluded = manaSolver.findAvailableManaSources(state, playerId)
            .map { it.entityId }
            .filter { it !in chosenSet }
            .toSet()
        return autoPay(state, playerId, cost, cardName, xValue, spellContext, excluded, xManaRestriction)
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

    /**
     * Every [ManaSpellRider] carried by restricted mana entries that disappeared during payment
     * (present in [before], gone from [after] after multiset subtraction). Used to detect that
     * e.g. Cavern of Souls' floating restricted mana was spent on the cast. Multiplicity is
     * preserved — two spent entries carrying the same rider yield it twice (Pyromancer's
     * Goggles), so this returns a list rather than a set.
     */
    private fun ridersConsumedDuringPayment(
        before: List<RestrictedManaEntry>,
        after: List<RestrictedManaEntry>
    ): List<ManaSpellRider> {
        val remaining = after.toMutableList()
        val consumed = mutableListOf<ManaSpellRider>()
        for (entry in before) {
            val idx = remaining.indexOfFirst { it == entry }
            if (idx >= 0) {
                remaining.removeAt(idx)
            } else if (entry.riders.isNotEmpty()) {
                consumed.addAll(entry.riders)
            }
        }
        return consumed
    }
}
