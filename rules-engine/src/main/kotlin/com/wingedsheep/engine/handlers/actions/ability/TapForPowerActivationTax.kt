package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * The mana a cost-modifying static adds to crew or saddle.
 *
 * Crew N and saddle N are activated abilities (CR 702.122a, 702.171a) whose printed cost is only
 * "tap creatures with total power N", so they have no dedicated activation path through
 * [ActivationCostTotaller]. A tax on them — Anointed Peacekeeper or Skyseer's Chariot naming a
 * Vehicle, Suppression Field — still applies (CR 601.2f via 602.2b): the cost becomes
 * "{2}, tap creatures …". Both enumerator and handler read the tax here so they never disagree.
 * A reduction has no mana to shrink, so only a net increase produces anything.
 */
internal object TapForPowerActivationTax {

    /** The generic mana added to [sourceId]'s crew/saddle ability for [playerId]; empty when untaxed. */
    fun of(castPermissionUtils: CastPermissionUtils?, state: GameState, sourceId: EntityId, playerId: EntityId): ManaCost =
        castPermissionUtils?.applyActivatedAbilityCostReduction(ManaCost.ZERO, state, sourceId, playerId)
            ?: ManaCost.ZERO

    /**
     * Pay [tax] after the tapped creatures have been paid, so the solver never counts a creature
     * that is crewing as a mana source. Returns null (with the original [failState]) when the
     * mana can't be paid.
     */
    fun pay(
        costPaymentService: CostPaymentService?,
        state: GameState,
        failState: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        tax: ManaCost
    ): Pair<GameState, List<GameEvent>>? {
        if (tax.isEmpty()) return state to emptyList()
        val service = costPaymentService ?: return null
        val paid = service.performPayment(state, playerId, PayCost.Atom(CostAtom.Mana(tax)), sourceId, emptyMap())
        return if (paid.success) paid.state to paid.events else null
    }

    fun unaffordable(state: GameState, label: String): ExecutionResult =
        ExecutionResult.error(state, "Not enough mana to pay the $label cost")

    /** Whether [playerId] can pay [tax] without tapping any of [tappedForCost]. */
    fun canPay(
        manaSolver: ManaSolver?,
        state: GameState,
        playerId: EntityId,
        tax: ManaCost,
        tappedForCost: Collection<EntityId>
    ): Boolean = tax.isEmpty() ||
        manaSolver?.canPay(state, playerId, tax, excludeSources = tappedForCost.toSet()) == true
}
