package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CardMeasure
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * The one implementation of **"exile any number of cards from your graveyard whose summed
 * [CardMeasure] reaches N"** — a variable-size graveyard exile gated on a *sum* rather than a count.
 *
 * Two costs speak this shape and both route through here so they can never drift apart:
 *  - **collect evidence N** (CR 701.59a — every graveyard card, measured by mana value), via
 *    [CollectEvidenceResolver], which adds the keyword's name, its `EvidenceCollectedEvent` and its
 *    "evidence was collected" linkage on top;
 *  - **[com.wingedsheep.sdk.scripting.costs.CostAtom.ExileFromGraveyardForTotal]** — the unnamed,
 *    filtered form (Baron Helmut Zemo: black cards, measured by black mana symbols).
 *
 * **The threshold is a floor on the measure, not on the card count.** Three consequences the whole
 * implementation turns on:
 *  - exiling *more* than the threshold is legal — the payer picks any number of matching cards and
 *    only the sum is constrained, so the picker is a variable-size selection with a sum gate;
 *  - a matching card whose measure is 0 (a land under the mana-value measure, a `{2}{B}`-free black
 *    card under a black-pip measure) is a legal selection contributing nothing, so "enough cards"
 *    never implies "enough total";
 *  - the affordability question is *"can the matching cards reach the floor"*, never *"are there
 *    enough cards"*.
 *
 * **It fails closed.** A player who cannot reach the floor can't choose to pay at all — the option
 * must be absent, not offered and refused (CR 701.59b states this for collect evidence, and an
 * unpayable cost simply isn't a legal activation in general). [canPay] is that gate, and every
 * enumerator and affordability check calls it; an entity the engine can't price contributes 0, so a
 * graveyard it can't read never looks payable.
 *
 * Measures read the **base** card, not the projected state: mana value and printed mana cost are
 * intrinsic to the card (CR 202.3) and a graveyard card is not subject to the battlefield
 * projection. The *filter* still evaluates against projected state, matching every other
 * non-battlefield cost filter in the engine.
 */
object GraveyardTotalExileResolver {

    private val predicateEvaluator = PredicateEvaluator()

    /** The cost-payload discriminator the client switches on to raise the sum-gated exile picker. */
    const val COST_TYPE: String = "ExileForTotal"

    /** The graveyard cards a payer could spend, and what each is worth under the measure. */
    data class Candidates(
        val cards: List<EntityId>,
        val weightById: Map<EntityId, Int>,
    ) {
        /** Combined measure of every available card — the most that could possibly be paid. */
        val total: Int get() = weightById.values.sum()

        /** Can this graveyard reach [minTotal] at all? */
        fun canReach(minTotal: Int): Boolean = total >= minTotal
    }

    /**
     * The cards in [playerId]'s graveyard matching [filter], each priced by [measure].
     *
     * [excludeCardId] drops a card from the pool for the case where the object being paid for is
     * itself still in the graveyard at enumeration time and so can't help pay its own cost (the
     * graveyard-cast shape; mirrors [ForageCostResolver.candidates]).
     */
    fun candidates(
        state: GameState,
        playerId: EntityId,
        measure: CardMeasure,
        filter: GameObjectFilter = GameObjectFilter.Any,
        excludeCardId: EntityId? = null,
    ): Candidates {
        val inZone = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).filter { it != excludeCardId }
        val cards = if (filter == GameObjectFilter.Any) inZone else {
            val context = PredicateContext(controllerId = playerId)
            val projected = state.projectedState
            inZone.filter { predicateEvaluator.matches(state, projected, it, filter, context) }
        }
        return Candidates(cards, cards.associateWith { weightOf(state, it, measure) })
    }

    /**
     * Could [playerId] pay this cost right now? The one affordability question every context asks;
     * fails closed on an unreadable or short graveyard.
     */
    fun canPay(
        state: GameState,
        playerId: EntityId,
        measure: CardMeasure,
        minTotal: Int,
        filter: GameObjectFilter = GameObjectFilter.Any,
        excludeCardId: EntityId? = null,
    ): Boolean = candidates(state, playerId, measure, filter, excludeCardId).canReach(minTotal)

    /**
     * Whether [chosenCards] is a legal payment: a selection drawn entirely from [candidates] whose
     * weights sum to at least [minTotal]. A deliberately *overpaying* selection is legal — exiling
     * more than needed is the payer's right. Every `GameAction` field is client-supplied, so this
     * runs on the server before anything is exiled.
     *
     * **The empty selection is legal exactly when [minTotal] is 0**, which the sum test decides on
     * its own: "any number of cards" includes none, and their total of 0 meets a threshold of 0.
     * Only collect evidence can reach here with a threshold of 0 — the filtered form requires a
     * floor of at least 1 — and it does so for two printed shapes, Incinerator of the Guilty's
     * chosen X and Urgent Necropsy cast with no targets. Both still *count* as having collected
     * evidence per the 2024-02-02 ruling, which is why the payment must succeed rather than be
     * refused as an empty one.
     */
    fun isLegalSelection(
        candidates: Candidates,
        minTotal: Int,
        chosenCards: List<EntityId>,
    ): Boolean {
        val distinct = chosenCards.distinct()
        return distinct.all { it in candidates.weightById } &&
            distinct.sumOf { candidates.weightById.getValue(it) } >= minTotal
    }

    /**
     * The selection to actually exile: [chosenCards] when it is legal, otherwise one picked by
     * [autoSelect] for the AI / engine-direct paths that supply no choice. Empty only when the
     * threshold is unreachable, which every caller has already gated on via [canPay].
     */
    fun resolveSelection(
        candidates: Candidates,
        minTotal: Int,
        chosenCards: List<EntityId>,
    ): List<EntityId> =
        if (isLegalSelection(candidates, minTotal, chosenCards)) chosenCards.distinct()
        else autoSelect(candidates, minTotal)

    /**
     * Pick a legal selection for a payer who didn't supply one (AI / engine-direct payment).
     *
     * Takes the **heaviest** cards first, which reaches [minTotal] while exiling the fewest cards.
     * That is the choice that costs the payer least in cards, and — unlike a lightest-first or
     * arbitrary-order sweep — it never dumps a graveyard's worth of cheap cards to pay a threshold
     * two expensive ones would have covered.
     *
     * Returns an empty list when the threshold is unreachable.
     */
    fun autoSelect(candidates: Candidates, minTotal: Int): List<EntityId> {
        val selected = mutableListOf<EntityId>()
        var total = 0
        for (cardId in candidates.cards.sortedByDescending { candidates.weightById[it] ?: 0 }) {
            if (total >= minTotal) break
            selected.add(cardId)
            total += candidates.weightById[cardId] ?: 0
        }
        return if (total >= minTotal) selected else emptyList()
    }

    /**
     * The legal-action cost payload for an [CostAtom.ExileFromGraveyardForTotal] cost, or null when
     * the matching graveyard cards can't reach the floor — in which case the caller must omit the
     * action entirely rather than offer one that can't be paid.
     *
     * [AdditionalCostData.exileMinTotalWeight] + [AdditionalCostData.exileCardWeights] are what make
     * the client's picker a *sum* gate over a measure it can't compute itself; the ordinary
     * `exileMinCount` / `exileMaxCount` pair can only express a counted selection, so it is set to
     * "at least one, at most all of them" and carries none of the real constraint. Collect evidence
     * ships the same three fields ([CollectEvidenceResolver.costInfo]), so the client has one
     * sum-gated picker rather than one per cost.
     */
    fun costInfo(atom: CostAtom.ExileFromGraveyardForTotal, candidates: Candidates): AdditionalCostData? {
        if (!candidates.canReach(atom.minTotal)) return null
        return AdditionalCostData(
            description = atom.description.replaceFirstChar { it.uppercase() },
            costType = COST_TYPE,
            validExileTargets = candidates.cards,
            exileMinCount = 1,
            exileMaxCount = candidates.cards.size,
            exileMinTotalWeight = atom.minTotal,
            exileCardWeights = candidates.weightById,
            exileWeightUnit = atom.measure.unitLabel,
        )
    }

    /** Convenience overload building the candidates itself. */
    fun costInfo(
        state: GameState,
        playerId: EntityId,
        atom: CostAtom.ExileFromGraveyardForTotal,
        excludeCardId: EntityId? = null,
    ): AdditionalCostData? =
        costInfo(atom, candidates(state, playerId, atom.measure, atom.filter, excludeCardId))

    /** Move [cards] to exile in order, accumulating the zone-change events. */
    fun exile(state: GameState, cards: List<EntityId>): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (cardId in cards) {
            val transition = ZoneTransitionService.moveToZone(newState, cardId, Zone.EXILE)
            newState = transition.state
            events.addAll(transition.events)
        }
        return newState to events
    }

    /**
     * What one graveyard card is worth under [measure].
     *
     * Reads the base [CardComponent] — mana value (CR 202.3) and the printed mana cost are
     * intrinsic characteristics, and a card outside the battlefield has no projection to consult.
     * A card the engine can't read is worth 0, which is what makes every affordability check above
     * fail closed rather than open.
     */
    fun weightOf(state: GameState, cardId: EntityId, measure: CardMeasure): Int {
        val card = state.getEntity(cardId)?.get<CardComponent>() ?: return 0
        return when (measure) {
            is CardMeasure.ManaValue -> card.manaValue
            // The single counting rule shared with CardPredicate.ColoredManaSymbolsAtLeast and
            // EntityNumericProperty.ColoredManaSymbolCount, so the filter, the per-card amount and
            // this group total can never disagree (hybrid/Phyrexian pips count, generic/{X} don't).
            is CardMeasure.ColoredManaSymbols -> card.manaCost.coloredSymbolCount(measure.colors.toSet())
        }
    }
}
