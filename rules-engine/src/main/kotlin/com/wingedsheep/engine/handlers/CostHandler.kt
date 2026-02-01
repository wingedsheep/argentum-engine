package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CardFilter

/**
 * Validates and pays costs for spells and abilities.
 */
class CostHandler {

    /**
     * Check if a mana cost can be paid from a player's mana pool.
     */
    fun canPayManaCost(manaPool: ManaPool, cost: ManaCost): Boolean {
        return manaPool.canPay(cost)
    }

    /**
     * Pay a mana cost from a player's mana pool.
     * Returns the new mana pool or null if can't pay.
     */
    fun payManaCost(manaPool: ManaPool, cost: ManaCost): ManaPool? {
        return manaPool.pay(cost)
    }

    /**
     * Check if an ability cost can be paid.
     */
    fun canPayAbilityCost(
        state: GameState,
        cost: AbilityCost,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool
    ): Boolean {
        return when (cost) {
            is AbilityCost.Tap -> {
                !state.getEntity(sourceId)!!.has<TappedComponent>()
            }
            is AbilityCost.Untap -> {
                state.getEntity(sourceId)!!.has<TappedComponent>()
            }
            is AbilityCost.Mana -> {
                canPayManaCost(manaPool, cost.cost)
            }
            is AbilityCost.PayLife -> {
                val life = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
                life > cost.amount // Can pay if life is > cost (not <= 0 after)
            }
            is AbilityCost.Sacrifice -> {
                findMatchingPermanents(state, controllerId, cost.filter).isNotEmpty()
            }
            is AbilityCost.Discard -> {
                val handZone = ZoneKey(controllerId, ZoneType.HAND)
                findMatchingCards(state, state.getZone(handZone), cost.filter).isNotEmpty()
            }
            is AbilityCost.ExileFromGraveyard -> {
                val graveyardZone = ZoneKey(controllerId, ZoneType.GRAVEYARD)
                findMatchingCards(state, state.getZone(graveyardZone), cost.filter).size >= cost.count
            }
            is AbilityCost.DiscardSelf -> {
                // Card must be in hand
                val handZone = ZoneKey(controllerId, ZoneType.HAND)
                state.getZone(handZone).contains(sourceId)
            }
            is AbilityCost.Composite -> {
                cost.costs.all { canPayAbilityCost(state, it, sourceId, controllerId, manaPool) }
            }
            is AbilityCost.Loyalty -> {
                // Check if we have enough loyalty to pay the cost
                // Positive changes (like +1) can always be paid
                // Negative changes (like -2) require at least that many counters
                if (cost.change >= 0) {
                    true
                } else {
                    val counters = state.getEntity(sourceId)?.get<CountersComponent>()
                    val currentLoyalty = counters?.getCount(CounterType.LOYALTY) ?: 0
                    currentLoyalty >= -cost.change
                }
            }
        }
    }

    /**
     * Pay an ability cost.
     */
    fun payAbilityCost(
        state: GameState,
        cost: AbilityCost,
        sourceId: EntityId,
        controllerId: EntityId,
        manaPool: ManaPool,
        choices: CostPaymentChoices = CostPaymentChoices()
    ): CostPaymentResult {
        return when (cost) {
            is AbilityCost.Tap -> {
                val newState = state.updateEntity(sourceId) { it.with(TappedComponent) }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Untap -> {
                val newState = state.updateEntity(sourceId) { it.without<TappedComponent>() }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Mana -> {
                val newPool = payManaCost(manaPool, cost.cost)
                    ?: return CostPaymentResult.failure("Cannot pay mana cost")
                CostPaymentResult.success(state, newPool)
            }
            is AbilityCost.PayLife -> {
                val currentLife = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life
                    ?: return CostPaymentResult.failure("Player has no life total")
                val newState = state.updateEntity(controllerId) { container ->
                    container.with(LifeTotalComponent(currentLife - cost.amount))
                }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Sacrifice -> {
                val toSacrifice = choices.sacrificeChoices.firstOrNull()
                    ?: return CostPaymentResult.failure("No sacrifice target chosen")
                // TODO: Move to graveyard
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.Discard -> {
                val toDiscard = choices.discardChoices.firstOrNull()
                    ?: return CostPaymentResult.failure("No discard target chosen")
                // TODO: Move to graveyard
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.ExileFromGraveyard -> {
                // TODO: Exile cards
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.DiscardSelf -> {
                // TODO: Discard self
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.Composite -> {
                var currentState = state
                var currentPool = manaPool
                for (subCost in cost.costs) {
                    val result = payAbilityCost(currentState, subCost, sourceId, controllerId, currentPool, choices)
                    if (!result.success) return result
                    currentState = result.newState!!
                    currentPool = result.newManaPool!!
                }
                CostPaymentResult.success(currentState, currentPool)
            }
            is AbilityCost.Loyalty -> {
                // Adjust loyalty counters based on the cost change
                val newState = state.updateEntity(sourceId) { container ->
                    val counters = container.get<CountersComponent>() ?: CountersComponent()
                    val newCounters = if (cost.change >= 0) {
                        counters.withAdded(CounterType.LOYALTY, cost.change)
                    } else {
                        counters.withRemoved(CounterType.LOYALTY, -cost.change)
                    }
                    container.with(newCounters)
                }
                CostPaymentResult.success(newState, manaPool)
            }
        }
    }

    /**
     * Check if additional costs can be paid.
     */
    fun canPayAdditionalCost(
        state: GameState,
        cost: AdditionalCost,
        controllerId: EntityId
    ): Boolean {
        return when (cost) {
            is AdditionalCost.SacrificePermanent -> {
                findMatchingPermanents(state, controllerId, cost.filter).size >= cost.count
            }
            is AdditionalCost.DiscardCards -> {
                val handZone = ZoneKey(controllerId, ZoneType.HAND)
                findMatchingCards(state, state.getZone(handZone), cost.filter).size >= cost.count
            }
            is AdditionalCost.PayLife -> {
                val life = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
                life > cost.amount
            }
            is AdditionalCost.ExileCards -> {
                val zone = ZoneKey(controllerId, cost.fromZone.toZoneType())
                findMatchingCards(state, state.getZone(zone), cost.filter).size >= cost.count
            }
            is AdditionalCost.TapPermanents -> {
                findUntappedMatchingPermanents(state, controllerId, cost.filter).size >= cost.count
            }
        }
    }

    // Helper functions

    private fun findMatchingPermanents(
        state: GameState,
        controllerId: EntityId,
        filter: CardFilter
    ): List<EntityId> {
        return state.entities.filter { (_, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            matchesFilter(container.get<CardComponent>(), filter)
        }.keys.toList()
    }

    private fun findUntappedMatchingPermanents(
        state: GameState,
        controllerId: EntityId,
        filter: CardFilter
    ): List<EntityId> {
        return state.entities.filter { (_, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            !container.has<TappedComponent>() &&
            matchesFilter(container.get<CardComponent>(), filter)
        }.keys.toList()
    }

    private fun findMatchingCards(
        state: GameState,
        cardIds: List<EntityId>,
        filter: CardFilter
    ): List<EntityId> {
        return cardIds.filter { cardId ->
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            matchesFilter(cardComponent, filter)
        }
    }

    private fun matchesFilter(card: CardComponent?, filter: CardFilter): Boolean {
        if (card == null) return false

        return when (filter) {
            is CardFilter.AnyCard -> true
            is CardFilter.CreatureCard -> card.typeLine.isCreature
            is CardFilter.LandCard -> card.typeLine.isLand
            is CardFilter.BasicLandCard -> card.typeLine.isBasicLand
            is CardFilter.SorceryCard -> card.typeLine.isSorcery
            is CardFilter.InstantCard -> card.typeLine.isInstant
            is CardFilter.HasSubtype -> card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.subtype))
            is CardFilter.HasColor -> card.colors.contains(filter.color)
            is CardFilter.And -> filter.filters.all { matchesFilter(card, it) }
            is CardFilter.Or -> filter.filters.any { matchesFilter(card, it) }
            is CardFilter.PermanentCard -> card.typeLine.isPermanent
            is CardFilter.NonlandPermanentCard -> card.typeLine.isPermanent && !card.typeLine.isLand
            is CardFilter.ManaValueAtMost -> card.manaCost.cmc <= filter.maxManaValue
            is CardFilter.Not -> !matchesFilter(card, filter.filter)
        }
    }

    private fun com.wingedsheep.sdk.scripting.CostZone.toZoneType(): ZoneType = when (this) {
        com.wingedsheep.sdk.scripting.CostZone.HAND -> ZoneType.HAND
        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> ZoneType.GRAVEYARD
        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> ZoneType.LIBRARY
        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> ZoneType.BATTLEFIELD
    }
}

/**
 * Result of paying a cost.
 */
data class CostPaymentResult(
    val success: Boolean,
    val newState: GameState?,
    val newManaPool: ManaPool?,
    val error: String?
) {
    companion object {
        fun success(state: GameState, manaPool: ManaPool) =
            CostPaymentResult(true, state, manaPool, null)

        fun failure(error: String) =
            CostPaymentResult(false, null, null, error)
    }
}

/**
 * Player choices for paying costs.
 */
data class CostPaymentChoices(
    val sacrificeChoices: List<EntityId> = emptyList(),
    val discardChoices: List<EntityId> = emptyList(),
    val exileChoices: List<EntityId> = emptyList(),
    val tapChoices: List<EntityId> = emptyList()
)
