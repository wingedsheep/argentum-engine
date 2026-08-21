package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.EvidenceCollectedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CardMeasure

/**
 * Single source of truth for **collect evidence N** (CR 701.59 — "to collect evidence N means to
 * exile any number of cards from your graveyard with total mana value N or greater").
 *
 * The mechanic appears in four contexts — an activated-ability cost, a cast-time additional cost, a
 * ward / payable cost, and a resolution-time effect — and means exactly the same in all of them. All
 * four route through this object so reachability, selection validation, the exile itself and the
 * [EvidenceCollectedEvent] can never drift apart between them. (This is the lesson
 * [ForageCostResolver] was extracted for: before it, each forage entry point had its own payment and
 * each one dropped the player's choice differently.)
 *
 * **The threshold is a floor on total mana value, not on card count.** Two consequences the whole
 * implementation turns on:
 *  - exiling *more* than N is legal — the player picks any number of cards and only the sum is
 *    constrained, so the picker is a variable-size selection with a sum gate, not a counted one;
 *  - land cards and other mana-value-0 cards are legal selections that contribute nothing, so
 *    "enough cards" never implies "enough evidence".
 *
 * **CR 701.59b fails closed.** A player who cannot reach N *can't choose to collect evidence* — the
 * option must be absent, not offered and refused. [canCollect] is that gate and every enumerator,
 * feasibility check and affordability check calls it; there is deliberately no code path that
 * offers a collection it cannot complete.
 *
 * **The mechanics live in [GraveyardTotalExileResolver]**, the shared "exile any number of graveyard
 * cards whose summed measure reaches N" implementation that also backs the unnamed filtered form
 * (`CostAtom.ExileFromGraveyardForTotal`). What stays here is everything that is *collect evidence*
 * specifically rather than that shape: the keyword's name and wording, its `EvidenceCollectedEvent`,
 * and the unfiltered / mana-value choice of pool and measure.
 */
object CollectEvidenceResolver {

    /** Collect evidence measures every graveyard card by its mana value (CR 701.59a). */
    private val MEASURE = CardMeasure.ManaValue

    /** The cost-payload discriminator the client switches on to raise the evidence picker. */
    const val COST_TYPE: String = "CollectEvidence"

    /**
     * The graveyard cards [playerId] could spend, and what they are worth.
     *
     * A thin, evidence-named view over [GraveyardTotalExileResolver.Candidates] — same data, with
     * `manaValueById` naming the measure collect evidence actually uses. Every caller of this
     * resolver reads it under that name, so the alias stays.
     */
    data class Candidates(
        val cards: List<EntityId>,
        val manaValueById: Map<EntityId, Int>,
    ) {
        internal val shared: GraveyardTotalExileResolver.Candidates =
            GraveyardTotalExileResolver.Candidates(cards, manaValueById)

        /** Combined mana value of every available card — the most evidence that could be collected. */
        val totalManaValue: Int get() = manaValueById.values.sum()

        /** CR 701.59b: can this graveyard reach [amount] at all? */
        fun canReach(amount: Int): Boolean = totalManaValue >= amount
    }

    /**
     * Collect-evidence candidates for [playerId] — every card in their graveyard, since the keyword
     * action filters by nothing but zone (CR 701.59a).
     *
     * [excludeCardId] drops a card from the pool for the case where the object being paid for is
     * itself still in the graveyard at enumeration time and so can't help pay its own cost (the
     * graveyard-cast shape; mirrors [ForageCostResolver.candidates]).
     */
    fun candidates(state: GameState, playerId: EntityId, excludeCardId: EntityId? = null): Candidates {
        val shared = GraveyardTotalExileResolver.candidates(
            state, playerId, MEASURE, excludeCardId = excludeCardId
        )
        return Candidates(shared.cards, shared.weightById)
    }

    /**
     * CR 701.59b gate: could [playerId] collect evidence [amount] right now?
     *
     * The one affordability question every context asks. Fails closed — an unreadable entity
     * contributes 0, so a graveyard the engine can't price never looks payable.
     */
    fun canCollect(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        excludeCardId: EntityId? = null,
    ): Boolean = candidates(state, playerId, excludeCardId).canReach(amount)

    /**
     * The legal-action cost payload for a collect-evidence cost, or null when the graveyard can't
     * reach [amount] (CR 701.59b — the caller must then omit the action entirely).
     *
     * [AdditionalCostData.exileMinTotalWeight] + [AdditionalCostData.exileCardWeights] are what make
     * the client's picker a *sum* gate: the ordinary `exileMinCount` / `exileMaxCount` pair can only
     * express a counted selection, and this cost has no meaningful count. The weights here are the
     * mana values the client could have summed itself — shipping them anyway is what lets one client
     * path serve both sum-gated exile costs (see [GraveyardTotalExileResolver.costInfo]).
     */
    fun costInfo(candidates: Candidates, amount: Int): AdditionalCostData? {
        if (!candidates.canReach(amount)) return null
        return AdditionalCostData(
            description = "Collect evidence $amount — exile cards with total mana value " +
                "$amount or greater from your graveyard",
            costType = COST_TYPE,
            validExileTargets = candidates.cards,
            // A floor of one card and a ceiling of the whole graveyard: the binding constraint is
            // the mana-value sum below, not either of these.
            exileMinCount = 1,
            exileMaxCount = candidates.cards.size,
            exileMinTotalWeight = amount,
            exileCardWeights = candidates.manaValueById,
            exileWeightUnit = MEASURE.unitLabel,
        )
    }

    /** Convenience overload building the candidates itself. */
    fun costInfo(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        excludeCardId: EntityId? = null,
    ): AdditionalCostData? = costInfo(candidates(state, playerId, excludeCardId), amount)

    /** Outcome of collecting evidence. */
    sealed interface Result {
        data class Success(
            val state: GameState,
            val events: List<GameEvent>,
            val exiledCards: List<EntityId>,
            val totalManaValue: Int,
        ) : Result

        data class Failure(val reason: String) : Result
    }

    /**
     * Collect evidence [amount] for [playerId], exiling [chosenCards] if they are a legal
     * collection and otherwise choosing a legal one.
     *
     * A supplied selection is honoured whenever it is legal — every card is in [playerId]'s
     * graveyard and their mana values sum to at least [amount] — including a deliberately
     * *overpaying* one, since exiling more than needed is the player's right (CR 701.59a).
     * An illegal or absent selection falls back to [autoSelect] for the AI / engine-direct paths,
     * matching how [ForageCostResolver.pay] handles the same situation.
     *
     * Returns [Result.Failure] — and changes nothing — when the graveyard cannot reach [amount].
     * Callers that gated on [canCollect] never see this; it is defense in depth for CR 701.59b.
     *
     * [linkToSourceId] tethers the exiled cards to that entity's linked-exile pile
     * ([com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent]), which is what
     * makes a later "cards exiled **with it**" ability on
     * the same permanent able to find them (Kylox's Voltstrider). Null — the default — is the
     * ordinary collection, which exiles the cards and forgets them. Only the cost-payment paths
     * pass it, and only when the paid atom asked for it: the keyword itself never links, so a
     * resolution-time `Effects.CollectEvidence` can't accidentally start a pile.
     */
    fun collect(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        chosenCards: List<EntityId> = emptyList(),
        sourceName: String = "Collect evidence",
        excludeCardId: EntityId? = null,
        linkToSourceId: EntityId? = null,
    ): Result {
        val candidates = candidates(state, playerId, excludeCardId)
        if (!candidates.canReach(amount)) {
            return Result.Failure(
                "Cannot collect evidence $amount: graveyard totals only ${candidates.totalManaValue}"
            )
        }

        val toExile = GraveyardTotalExileResolver
            .resolveSelection(candidates.shared, amount, chosenCards)
        // Collecting evidence 0 exiles nothing and is legal — "any number of cards" includes none,
        // and their total mana value of 0 meets a threshold of 0 (CR 701.59a). Per the 2024-02-02
        // Incinerator of the Guilty ruling it still *counts* as collecting evidence, so the event
        // below must fire for "whenever you collect evidence" payoffs. Only reachable from
        // `CollectEvidenceChosenAmountEffect`, the one shape whose X the player picks; every fixed
        // threshold in the corpus is at least 1.
        if (toExile.isEmpty() && amount > 0) {
            return Result.Failure("Cannot collect evidence $amount: no legal selection")
        }

        val totalManaValue = toExile.sumOf { candidates.manaValueById[it] ?: 0 }

        val (exiledState, events) = GraveyardTotalExileResolver.exile(state, toExile)
        // The link is applied after the exile, not during it: ZoneMovementUtils.linkExiledToSource
        // writes the pile onto the *source*, and only cards that actually reached exile belong in
        // it. Linking a card the move failed on would leave a dangling id the lookup has to filter
        // out on every read.
        val newState = if (linkToSourceId == null) exiledState else toExile.fold(exiledState) { acc, cardId ->
            ZoneMovementUtils.linkExiledToSource(acc, cardId, linkToSourceId)
        }
        val allEvents = events +
            EvidenceCollectedEvent(playerId, amount, toExile, totalManaValue, sourceName)

        return Result.Success(newState, allEvents, toExile, totalManaValue)
    }

    /**
     * Pick a legal collection for a player who didn't supply one (AI / engine-direct payment).
     * Takes the **highest** mana values first — see [GraveyardTotalExileResolver.autoSelect] for
     * why that ordering rather than another.
     */
    fun autoSelect(candidates: Candidates, amount: Int): List<EntityId> =
        GraveyardTotalExileResolver.autoSelect(candidates.shared, amount)

    /**
     * Whether [chosenCards] is a legal collection of evidence [amount] from [playerId]'s graveyard.
     * Used by the action validators to reject a client-supplied selection before it is paid
     * (a `GameAction` is client-supplied, so every field needs validating).
     */
    fun isLegalSelection(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        chosenCards: List<EntityId>,
        excludeCardId: EntityId? = null,
    ): Boolean = GraveyardTotalExileResolver.isLegalSelection(
        candidates(state, playerId, excludeCardId).shared, amount, chosenCards
    )
}
