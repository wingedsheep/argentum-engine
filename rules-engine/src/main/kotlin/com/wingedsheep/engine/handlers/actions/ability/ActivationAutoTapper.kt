package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.tapForMana
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Activates mana abilities for an activated ability's mana cost (CR 601.2g via CR 602.2b) — the
 * auto-tap fast path taken when the player didn't pick sources explicitly.
 *
 * **Why this isn't the shared cast/cost payer.** The other auto-tap paths —
 * `CastPaymentProcessor.autoPay` (spells) and `CostPaymentService.payMana` (non-activation costs)
 * — tap *and pay* in one step. The activation flow is split differently: this stage only *fills the
 * pool*, and `CostHandler.payAbilityCost` then deducts the whole ability cost (mana and non-mana
 * atoms together) from it. That split is load-bearing:
 *  - mana a source produces under a restriction (Steelswarm Operator's artifact-source-only
 *    `{U}{U}`) enters the pool *tagged*, so payment spends the eligible restricted mana first and an
 *    unspent remainder stays restricted instead of laundering into unrestricted mana;
 *  - the *total* per-tap bonus mana (Lavaleaper) lands in the pool, rather than whatever of it the
 *    solver's internal accounting left over;
 *  - floating mana is only *counted* toward X here; the X spend itself happens after payment.
 *
 * It also differs from `ManaAbilitySideEffectExecutor.tapSourcesWithSideEffects`, which the other
 * paths use to tap: this path emits the tapped source's activation event but does **not** run the
 * matched mana ability's non-mana side effects. Routing it through the shared helper would change
 * that, so it is kept as its own collaborator.
 */
internal class ActivationAutoTapper(
    private val manaSolver: ManaSolver,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
) {

    data class Result(
        val newState: GameState,
        val newPool: ManaPool,
        val events: List<GameEvent>
    )

    /**
     * Auto-tap mana sources to cover a mana cost that can't be fully paid from the floating pool.
     * Taps sources for the shortfall and adds their mana to the pool so costHandler can consume it.
     * Returns null if the cost cannot be paid.
     */
    fun autoTapForManaCost(
        state: GameState,
        playerId: EntityId,
        pool: ManaPool,
        cost: ManaCost,
        xValue: Int = 0,
        excludeSources: Set<EntityId> = emptySet(),
        abilityContext: SpellPaymentContext? = null,
        xManaRestriction: Set<Color> = emptySet(),
    ): Result? {
        // Determine what the floating pool can cover (with the ability context so restricted
        // mana eligible for this activation counts toward coverage)
        val partialResult = pool.payPartial(cost, abilityContext)
        val remainingCost = partialResult.remainingCost

        // The floating pool also pays toward the {X} portion before any sources are tapped —
        // sharing the same coverage rule as CastPaymentProcessor.autoPay (ManaPool.xCoveragePlan).
        // Without this, an {X} ability whose X is solved purely by tapping sources reports "Not
        // enough mana" even when the pool already holds enough (e.g. Aladdin's Lamp activated with
        // X=4 while 4 mana float in the pool). We only reduce how much X the solver must tap for
        // here; the actual pool spend for X happens later in `payAbilityCost`.
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        var xToTap = xValue * xSymbolCount
        if (xToTap > 0) {
            xToTap -= partialResult.newPool.xCoveragePlan(xToTap, xManaRestriction).size
        }

        // If floating pool covers everything (and no X left to tap for), no tapping needed.
        // Return the original pool unchanged — `payAbilityCost` performs the actual deduction.
        if (remainingCost.isEmpty() && xToTap == 0) {
            return Result(state, pool, emptyList())
        }

        // Tap sources for the remaining cost (xToTap is the X mana the floating pool couldn't
        // cover, treated as additional generic mana — or restricted to xManaRestriction colors
        // for "spend only [colors] on X" abilities)
        val solution = manaSolver.solve(state, playerId, remainingCost, xToTap, excludeSources = excludeSources, spellContext = abilityContext, xManaRestriction = xManaRestriction)
            ?: return null

        var currentState = state
        var currentPool = pool
        val events = mutableListOf<GameEvent>()

        for (source in solution.sources) {
            val (tappedState, tapEvents) = tapForMana(currentState, source.entityId, playerId)
            currentState = tappedState
            events.addAll(tapEvents)
            // Auto-tapping a source to pay an ability's mana cost activates that source's mana
            // ability just as a manual tap would (CR 605.3) — emit the same activation event the
            // shared cast/cycling/plot auto-tap path emits, so "whenever you activate an ability"
            // triggers (Elrond, Moon-Reader) don't silently miss the fast path.
            manaAbilitySideEffectExecutor.activationEvent(
                currentState,
                source.entityId,
                solution.manaProduced[source.entityId]?.color,
                playerId
            )?.let(events::add)
        }

        // Add produced mana to floating pool so costHandler.payAbilityCost can consume it.
        // When the source's ability is restricted (e.g. Steelswarm Operator's
        // {T}: Add {U}{U} restricted to artifact-source ability activations), tag the
        // produced mana with that restriction. payAbilityCost will preferentially spend
        // the eligible restricted mana for the cost — and any unconsumed remainder stays
        // restricted in the pool instead of laundering into unrestricted mana.
        for (source in solution.sources) {
            // A tapped source may legitimately have no manaProduced entry: ManaSolver taps
            // extra sources to pay the *internal* activation cost of a mana ability (e.g. the
            // {1} in Hidden Grotto's "{1}, {T}: Add one mana of any color"). That mana is
            // consumed by the ability's own cost rather than flowing into the spell/ability
            // payment pool, so the solver intentionally omits it from manaProduced. Such a
            // source is still tapped above; it just contributes nothing to the pool here.
            val production = solution.manaProduced[source.entityId] ?: continue
            val color = production.color
            val restriction = if (color != null) {
                source.colorRestrictions[color] ?: source.restriction
            } else source.restriction
            currentPool = when {
                color != null && restriction != null ->
                    currentPool.addRestricted(color, production.amount, restriction)
                color != null ->
                    currentPool.add(color, production.amount)
                else ->
                    currentPool.addColorless(production.colorless)
            }
        }

        // Add per-source bonus mana from AdditionalManaOnSourceTap auras/statics (e.g.,
        // Lavaleaper: tapping a basic land adds an extra mana of its produced color).
        // Unlike the cast flow — which uses solve's internal accounting as the payment —
        // the activate flow funnels all produced mana through the pool and then deducts
        // the cost via payAbilityCost, so the *total* bonus from tapping must land in the
        // pool. solution.remainingBonusMana would drop any bonus consumed during solve.
        // (Multi-mana excess is already included via manaProduced.amount above.)
        // Aura bonus mana is unrestricted — the source's restriction belongs to the
        // printed ability, not to the aura-granted extras.
        for (source in solution.sources) {
            if (source.bonusManaPerTap > 0 && source.bonusManaColor != null) {
                currentPool = currentPool.add(source.bonusManaColor, source.bonusManaPerTap)
            }
        }

        // Update state with enriched pool — carry restrictedMana and mana-source provenance through
        // so the ability-payment context can spend (and the leftover can stay) restricted, and so the
        // caller's final writeback still sees tags for mana floated before this auto-tap. This write
        // is transient (the caller overwrites the post-payment pool), but keeps intermediate state
        // consistent for anything that reads the pool between auto-tap and payment.
        currentState = currentState.updateEntity(playerId) { c ->
            c.with(ManaPoolComponent(
                white = currentPool.white,
                blue = currentPool.blue,
                black = currentPool.black,
                red = currentPool.red,
                green = currentPool.green,
                colorless = currentPool.colorless,
                restrictedMana = currentPool.restrictedMana,
                manaBySubtype = currentPool.manaBySubtype,
                manaBySource = currentPool.manaBySource,
            ))
        }

        return Result(currentState, currentPool, events)
    }
}
