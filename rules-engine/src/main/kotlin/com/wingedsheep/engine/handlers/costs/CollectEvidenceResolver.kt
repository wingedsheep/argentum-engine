package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.EvidenceCollectedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

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
 */
object CollectEvidenceResolver {

    /** The cost-payload discriminator the client switches on to raise the evidence picker. */
    const val COST_TYPE: String = "CollectEvidence"

    /** The graveyard cards [playerId] could spend, and what they are worth. */
    data class Candidates(
        val cards: List<EntityId>,
        val manaValueById: Map<EntityId, Int>,
    ) {
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
        val cards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).filter { it != excludeCardId }
        val manaValues = cards.associateWith { manaValueOf(state, it) }
        return Candidates(cards, manaValues)
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
     * [AdditionalCostData.exileMinTotalManaValue] is what makes the client's picker a *sum* gate:
     * the ordinary `exileMinCount` / `exileMaxCount` pair can only express a counted selection, and
     * this cost has no meaningful count.
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
            exileMinTotalManaValue = amount,
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
     */
    fun collect(
        state: GameState,
        playerId: EntityId,
        amount: Int,
        chosenCards: List<EntityId> = emptyList(),
        sourceName: String = "Collect evidence",
        excludeCardId: EntityId? = null,
    ): Result {
        val candidates = candidates(state, playerId, excludeCardId)
        if (!candidates.canReach(amount)) {
            return Result.Failure(
                "Cannot collect evidence $amount: graveyard totals only ${candidates.totalManaValue}"
            )
        }

        val distinctChoice = chosenCards.distinct()
        val chosenIsLegal = distinctChoice.isNotEmpty() &&
            distinctChoice.all { it in candidates.manaValueById } &&
            distinctChoice.sumOf { candidates.manaValueById.getValue(it) } >= amount

        val toExile = if (chosenIsLegal) distinctChoice else autoSelect(candidates, amount)
        if (toExile.isEmpty()) {
            return Result.Failure("Cannot collect evidence $amount: no legal selection")
        }

        val totalManaValue = toExile.sumOf { candidates.manaValueById[it] ?: 0 }

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (cardId in toExile) {
            val transition = ZoneTransitionService.moveToZone(newState, cardId, Zone.EXILE)
            newState = transition.state
            events.addAll(transition.events)
        }
        events.add(EvidenceCollectedEvent(playerId, amount, toExile, totalManaValue, sourceName))

        return Result.Success(newState, events, toExile, totalManaValue)
    }

    /**
     * Pick a legal collection for a player who didn't supply one (AI / engine-direct payment).
     *
     * Takes the **highest** mana values first, which reaches [amount] while exiling the fewest
     * cards. That is the choice that costs the player least in cards, and — unlike a
     * lowest-first or arbitrary-order sweep — it never dumps a graveyard's worth of cheap
     * spells to pay a threshold two expensive ones would have covered.
     *
     * Returns an empty list only if the threshold is unreachable, which [collect] has already
     * excluded.
     */
    fun autoSelect(candidates: Candidates, amount: Int): List<EntityId> {
        val selected = mutableListOf<EntityId>()
        var total = 0
        for (cardId in candidates.cards.sortedByDescending { candidates.manaValueById[it] ?: 0 }) {
            if (total >= amount) break
            selected.add(cardId)
            total += candidates.manaValueById[cardId] ?: 0
        }
        return if (total >= amount) selected else emptyList()
    }

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
    ): Boolean {
        val candidates = candidates(state, playerId, excludeCardId)
        val distinct = chosenCards.distinct()
        return distinct.isNotEmpty() &&
            distinct.all { it in candidates.manaValueById } &&
            distinct.sumOf { candidates.manaValueById.getValue(it) } >= amount
    }

    /**
     * Mana value of a card in a non-battlefield zone. Mana value is intrinsic to the card
     * (CR 202.3), so the base [CardComponent] is the correct read here — graveyard cards are not
     * subject to the battlefield projection.
     */
    private fun manaValueOf(state: GameState, cardId: EntityId): Int =
        state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0
}
