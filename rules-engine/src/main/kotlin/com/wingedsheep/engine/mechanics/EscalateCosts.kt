package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils
import com.wingedsheep.engine.legalactions.utils.SelectionCostPresentation
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.repeated
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Escalate (CR 702.120a) — "Pay this cost for each mode chosen beyond the first."
 *
 * The mana form rides [ModalEffect.additionalManaCostPerExtraMode] and is folded straight into the
 * spell's mana cost. The non-mana form ([ModalEffect.additionalCostPerExtraMode] — Collective
 * Brutality's "discard a card", Collective Effort's "tap an untapped creature you control") has to
 * become a real [AdditionalCost] the cast pipeline can enumerate, validate, and charge.
 *
 * It is charged as **one scaled cost**, not as N repeats: every additional-cost payment channel is
 * a single flat list (`discardedCards`, `tappedPermanents`, …), so two `Discard(1)` entries would
 * both be satisfied by the same one discarded card. `Discard(2)` is the shape that validates and
 * charges the two cards escalate actually owes.
 */
object EscalateCosts {

    /**
     * The escalate cost owed for a cast that chose [chosenModeCount] modes, or null when the card
     * has no non-mana escalate cost or when only one mode was chosen (escalate charges nothing for
     * the first mode).
     */
    fun additionalCostFor(modalEffect: ModalEffect, chosenModeCount: Int): AdditionalCost? {
        val atom = modalEffect.additionalCostPerExtraMode ?: return null
        val extraModes = chosenModeCount - 1
        if (extraModes <= 0) return null
        return AdditionalCost.Atom(atom.repeated(extraModes))
    }

    /**
     * How the caster would pay one extra mode's worth of [modalEffect]'s non-mana escalate cost:
     * the candidate objects, the client cost data driving the picker, and the resulting cap on how
     * many modes beyond the first they can afford. Null when the card has no such cost.
     *
     * The cap exists because escalating past what you can pay dead-ends at payment, where the only
     * way out is cancelling the whole cast — the same reason mode selection is already gated on
     * mana affordability (rule 700.2h). A cost shape no picker covers caps the spell at one mode
     * rather than offering modes that can never be paid for.
     */
    fun payability(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        modalEffect: ModalEffect,
        costUtils: CostEnumerationUtils,
        predicateEvaluator: PredicateEvaluator,
    ): Payability? {
        val atom = modalEffect.additionalCostPerExtraMode ?: return null
        val cost = AdditionalCost.Atom(atom)
        val candidates = SelectionCostPresentation.candidates(
            state, playerId, castCardId, cost, costUtils, predicateEvaluator
        )
        val costData = SelectionCostPresentation.costData(
            state, playerId, castCardId, cost, candidates
        )?.second
        val perModeSelection = SelectionCostPresentation.selectionCount(cost)
        val maxExtraModes = when {
            costData == null -> 0
            perModeSelection <= 0 -> 0
            else -> candidates.size / perModeSelection
        }
        return Payability(costData, maxExtraModes)
    }

    /**
     * @property costData Client cost data for **one** extra mode's payment — the picker multiplies
     *   its count by the number of modes chosen beyond the first. Null when no picker covers the
     *   cost.
     * @property maxExtraModes Cap on modes chosen beyond the first, from what is available to pay.
     */
    data class Payability(
        val costData: AdditionalCostData?,
        val maxExtraModes: Int,
    )
}
