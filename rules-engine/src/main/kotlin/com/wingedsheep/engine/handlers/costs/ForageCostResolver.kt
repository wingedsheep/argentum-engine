package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ForagedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Single source of truth for the **cost-based** Forage mechanic (CR 701.59a — "To forage,
 * exile three cards from your graveyard or sacrifice a Food").
 *
 * Forage is a *choice* between two sub-costs, and that choice belongs to the foraging player.
 * Before this resolver each cost-shaped forage entry point reimplemented payment independently
 * and all of them dropped the player's choice on the floor: the activated-ability path
 * ([com.wingedsheep.sdk.scripting.AbilityCost.Forage]) auto-picked the exile mode, the
 * graveyard-cast path ([com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent])
 * auto-sacrificed the first Food (or exiled the first three cards), and the modal additional-cost
 * path ([com.wingedsheep.sdk.scripting.AdditionalCost.Forage], Feed the Cycle) silently paid
 * nothing at all. This object unifies all of them:
 *
 *  - [candidates] / [canPay] — what the player could forage with right now.
 *  - [costInfos] — the legal-action payload(s): one [AdditionalCostData] per *available* mode so
 *    the enumerators can surface the mode choice as separate legal actions (the same multi-action
 *    pattern the "OrPay" costs use). The client already renders an exile-from-graveyard picker for
 *    `ExileFromGraveyard` and a sacrifice picker for `SacrificePermanent`, so the player picks the
 *    mode *and* which cards/Food with zero new UI.
 *  - [pay] — the one payment implementation. It honors whatever the player chose and only falls
 *    back to an arbitrary legal payment when no valid choice was supplied (AI / engine-direct).
 *
 * The *effect*-based forage (`Patterns.Mechanic.forage()` → `ChooseActionEffect`, used by the
 * "you may forage" ETB cards) already resolves the choice correctly through the normal effect
 * pipeline and is intentionally left alone. That is also why
 * [com.wingedsheep.engine.core.ForagedEvent] has two emission sites rather than one: this object is
 * the chokepoint for the three *cost* contexts, and the effect form carries the
 * [com.wingedsheep.sdk.scripting.effects.ForagedEffect] marker instead because it lowers to generic
 * gather/select/move and sacrifice effects with nothing forage-shaped to emit from.
 */
object ForageCostResolver {

    /** A forage payment always exiles exactly this many cards in the exile mode. */
    const val EXILE_COUNT: Int = 3

    /** The permanents/cards the foraging player could spend on a forage cost. */
    data class Candidates(
        val exileCards: List<EntityId>,
        val foods: List<EntityId>,
    ) {
        val canExile: Boolean get() = exileCards.size >= EXILE_COUNT
        val canSacrifice: Boolean get() = foods.isNotEmpty()
        val canPay: Boolean get() = canExile || canSacrifice
    }

    /**
     * Forage candidates for [playerId]. [excludeCardId] drops a card from the exile pool — used by
     * the graveyard-cast path, where the spell being cast is still in the graveyard at enumeration
     * time but can't be one of the three cards it exiles to pay for itself.
     */
    fun candidates(state: GameState, playerId: EntityId, excludeCardId: EntityId? = null): Candidates {
        val projected = state.projectedState
        val exileCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).filter { it != excludeCardId }
        val foods = state.getBattlefield().filter { permId ->
            state.getEntity(permId) ?: return@filter false
            projected.getController(permId) == playerId &&
                projected.hasSubtype(permId, Subtype.FOOD.value)
        }
        return Candidates(exileCards, foods)
    }

    /** Convenience: can [playerId] pay a forage cost right now? */
    fun canPay(state: GameState, playerId: EntityId, excludeCardId: EntityId? = null): Boolean =
        candidates(state, playerId, excludeCardId).canPay

    /**
     * The legal-action cost payloads for the available forage modes — exile first, then sacrifice.
     * Returns 0, 1, or 2 entries; an enumerator emits one legal action per entry so the player gets
     * to choose the mode (when both are possible) and which cards/Food (within a mode).
     */
    fun costInfos(candidates: Candidates): List<AdditionalCostData> = buildList {
        if (candidates.canExile) {
            add(
                AdditionalCostData(
                    description = "Forage — exile three cards from your graveyard",
                    costType = "ExileFromGraveyard",
                    validExileTargets = candidates.exileCards,
                    exileMinCount = EXILE_COUNT,
                    exileMaxCount = EXILE_COUNT,
                )
            )
        }
        if (candidates.canSacrifice) {
            add(
                AdditionalCostData(
                    description = "Forage — sacrifice a Food",
                    costType = "SacrificePermanent",
                    validSacrificeTargets = candidates.foods,
                    sacrificeCount = 1,
                )
            )
        }
    }

    /** Outcome of paying a forage cost. */
    sealed interface Result {
        data class Success(val state: GameState, val events: List<GameEvent>) : Result
        data class Failure(val reason: String) : Result
    }

    /**
     * Pay the forage cost for [playerId].
     *
     * Priority:
     *  1. If [exileChoices] names at least [EXILE_COUNT] valid graveyard cards, the player chose the
     *     exile mode — exile exactly those three.
     *  2. Else if [sacrificeChoices] names a valid Food, the player chose the sacrifice mode —
     *     sacrifice it.
     *  3. Else no valid choice was supplied (AI / engine-direct): pay an arbitrary legal mode,
     *     preferring to sacrifice a Food (keeps a Food's other uses for the player no worse off than
     *     the historical default) and otherwise exiling the first three cards.
     */
    fun pay(
        state: GameState,
        playerId: EntityId,
        exileChoices: List<EntityId> = emptyList(),
        sacrificeChoices: List<EntityId> = emptyList(),
        excludeCardId: EntityId? = null,
    ): Result = payMode(state, playerId, exileChoices, sacrificeChoices, excludeCardId).foraged(playerId)

    /**
     * The forage event, appended to whichever mode was actually paid.
     *
     * Written as one wrapper over [payMode]'s four exits rather than a line in each of them, which is
     * the property that matters: a mode added later cannot forget to emit it, and a `Failure`
     * carries nothing — forage has no "even if you can't" clause, so an unpayable forage must not
     * fire "Whenever you forage".
     */
    private fun Result.foraged(playerId: EntityId): Result = when (this) {
        is Result.Success -> Result.Success(state, events + ForagedEvent(playerId))
        is Result.Failure -> this
    }

    private fun payMode(
        state: GameState,
        playerId: EntityId,
        exileChoices: List<EntityId>,
        sacrificeChoices: List<EntityId>,
        excludeCardId: EntityId?,
    ): Result {
        val candidates = candidates(state, playerId, excludeCardId)

        val validExile = exileChoices.filter { it in candidates.exileCards }
        if (validExile.size >= EXILE_COUNT) {
            return exile(state, validExile.take(EXILE_COUNT))
        }

        val chosenFood = sacrificeChoices.firstOrNull { it in candidates.foods }
        if (chosenFood != null) {
            return sacrifice(state, playerId, chosenFood)
        }

        if (candidates.canSacrifice) return sacrifice(state, playerId, candidates.foods.first())
        if (candidates.canExile) return exile(state, candidates.exileCards.take(EXILE_COUNT))
        return Result.Failure("Cannot forage: need three cards in your graveyard or a Food")
    }

    private fun exile(state: GameState, cardIds: List<EntityId>): Result {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (cardId in cardIds) {
            val transition = ZoneTransitionService.moveToZone(newState, cardId, Zone.EXILE)
            newState = transition.state
            events.addAll(transition.events)
        }
        return Result.Success(newState, events)
    }

    private fun sacrifice(state: GameState, playerId: EntityId, foodId: EntityId): Result {
        val container = state.getEntity(foodId)
        val foodName = container?.get<CardComponent>()?.name ?: "Food"
        val foodController = container?.get<ControllerComponent>()?.playerId ?: playerId
        val tracked = ZoneTransitionService.trackPermanentSacrifice(state, listOf(foodId), foodController)
        val transition = ZoneTransitionService.moveToZone(tracked, foodId, Zone.GRAVEYARD)
        val events = buildList {
            add(PermanentsSacrificedEvent(foodController, listOf(foodId), listOf(foodName)))
            addAll(transition.events)
        }
        return Result.Success(transition.state, events)
    }
}
