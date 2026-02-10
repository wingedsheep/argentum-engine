package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Validates and pays costs for spells and abilities.
 */
class CostHandler {

    private val predicateEvaluator = PredicateEvaluator()

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
                findMatchingPermanentsUnified(state, controllerId, cost.filter).isNotEmpty()
            }
            is AbilityCost.SacrificeChosenCreatureType -> {
                val chosenType = state.getEntity(sourceId)?.get<ChosenCreatureTypeComponent>()?.creatureType
                    ?: return false
                val filter = GameObjectFilter.Creature.withSubtype(chosenType)
                findMatchingPermanentsUnified(state, controllerId, filter).isNotEmpty()
            }
            is AbilityCost.Discard -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                findMatchingCardsUnified(state, state.getZone(handZone), cost.filter, controllerId).isNotEmpty()
            }
            is AbilityCost.ExileFromGraveyard -> {
                val graveyardZone = ZoneKey(controllerId, Zone.GRAVEYARD)
                findMatchingCardsUnified(state, state.getZone(graveyardZone), cost.filter, controllerId).size >= cost.count
            }
            is AbilityCost.DiscardSelf -> {
                // Card must be in hand
                val handZone = ZoneKey(controllerId, Zone.HAND)
                state.getZone(handZone).contains(sourceId)
            }
            is AbilityCost.SacrificeSelf -> {
                // Source must be on the battlefield
                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                state.getZone(battlefieldZone).contains(sourceId)
            }
            is AbilityCost.TapPermanents -> {
                findUntappedMatchingPermanentsUnified(state, controllerId, cost.filter).size >= cost.count
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return false
                val attachedEntity = state.getEntity(attachedId) ?: return false
                if (attachedEntity.has<TappedComponent>()) return false
                // Check summoning sickness on the attached creature
                val card = attachedEntity.get<CardComponent>()
                if (card != null && card.typeLine.isCreature) {
                    val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
                    val hasHaste = card.baseKeywords.contains(com.wingedsheep.sdk.core.Keyword.HASTE)
                    if (hasSummoningSickness && !hasHaste) return false
                }
                true
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
            is AbilityCost.Sacrifice, is AbilityCost.SacrificeChosenCreatureType -> {
                val toSacrifice = choices.sacrificeChoices.firstOrNull()
                    ?: return CostPaymentResult.failure("No sacrifice target chosen")

                // Get the controller of the permanent being sacrificed
                val sacrificeContainer = state.getEntity(toSacrifice)
                    ?: return CostPaymentResult.failure("Sacrifice target not found")
                val sacrificeController = sacrificeContainer.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Sacrifice target has no controller")
                val sacrificeName = sacrificeContainer.get<CardComponent>()?.name ?: "Unknown"

                // Validate the chosen sacrifice matches the required filter
                val sacrificeFilter = when (cost) {
                    is AbilityCost.Sacrifice -> cost.filter
                    is AbilityCost.SacrificeChosenCreatureType -> {
                        val chosenType = state.getEntity(sourceId)?.get<ChosenCreatureTypeComponent>()?.creatureType
                            ?: return CostPaymentResult.failure("No creature type chosen")
                        GameObjectFilter.Creature.withSubtype(chosenType)
                    }
                    else -> null
                }
                if (sacrificeFilter != null) {
                    val context = PredicateContext(controllerId = controllerId)
                    if (!predicateEvaluator.matches(state, toSacrifice, sacrificeFilter, context)) {
                        return CostPaymentResult.failure("Sacrifice target does not match the required filter")
                    }
                }

                // Move from battlefield to graveyard
                val battlefieldZone = ZoneKey(sacrificeController, Zone.BATTLEFIELD)
                val graveyardZone = ZoneKey(sacrificeController, Zone.GRAVEYARD)

                var newState = state.removeFromZone(battlefieldZone, toSacrifice)
                newState = newState.addToZone(graveyardZone, toSacrifice)

                val events = listOf(
                    PermanentsSacrificedEvent(sacrificeController, listOf(toSacrifice)),
                    ZoneChangeEvent(
                        entityId = toSacrifice,
                        entityName = sacrificeName,
                        fromZone = Zone.BATTLEFIELD,
                        toZone = Zone.GRAVEYARD,
                        ownerId = sacrificeController
                    )
                )

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.Discard -> {
                val toDiscard = choices.discardChoices.firstOrNull()
                    ?: return CostPaymentResult.failure("No discard target chosen")

                val discardContainer = state.getEntity(toDiscard)
                    ?: return CostPaymentResult.failure("Card to discard not found")
                val discardOwner = discardContainer.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Card to discard has no owner")

                val handZone = ZoneKey(discardOwner, Zone.HAND)
                val graveyardZone = ZoneKey(discardOwner, Zone.GRAVEYARD)

                var newState = state.removeFromZone(handZone, toDiscard)
                newState = newState.addToZone(graveyardZone, toDiscard)

                val events = listOf(
                    CardsDiscardedEvent(discardOwner, listOf(toDiscard)),
                    ZoneChangeEvent(
                        entityId = toDiscard,
                        entityName = discardContainer.get<CardComponent>()?.name ?: "Unknown",
                        fromZone = Zone.HAND,
                        toZone = Zone.GRAVEYARD,
                        ownerId = discardOwner
                    )
                )

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.ExileFromGraveyard -> {
                // TODO: Exile cards
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.DiscardSelf -> {
                // TODO: Discard self
                CostPaymentResult.success(state, manaPool)
            }
            is AbilityCost.SacrificeSelf -> {
                // Sacrifice the source permanent
                val sourceContainer = state.getEntity(sourceId)
                    ?: return CostPaymentResult.failure("Source permanent not found")
                val sourceController = sourceContainer.get<ControllerComponent>()?.playerId
                    ?: return CostPaymentResult.failure("Source permanent has no controller")
                val sourceName = sourceContainer.get<CardComponent>()?.name ?: "Unknown"

                val battlefieldZone = ZoneKey(sourceController, Zone.BATTLEFIELD)
                val graveyardZone = ZoneKey(sourceController, Zone.GRAVEYARD)

                var newState = state.removeFromZone(battlefieldZone, sourceId)
                newState = newState.addToZone(graveyardZone, sourceId)

                val events = listOf(
                    PermanentsSacrificedEvent(sourceController, listOf(sourceId)),
                    ZoneChangeEvent(
                        entityId = sourceId,
                        entityName = sourceName,
                        fromZone = Zone.BATTLEFIELD,
                        toZone = Zone.GRAVEYARD,
                        ownerId = sourceController
                    )
                )

                CostPaymentResult.success(newState, manaPool, events)
            }
            is AbilityCost.TapAttachedCreature -> {
                val attachedId = state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId
                    ?: return CostPaymentResult.failure("Source is not attached to a creature")
                val newState = state.updateEntity(attachedId) { it.with(TappedComponent) }
                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.TapPermanents -> {
                val toTap = choices.tapChoices
                if (toTap.size < cost.count) {
                    return CostPaymentResult.failure("Not enough permanents chosen to tap (need ${cost.count}, got ${toTap.size})")
                }

                var newState = state
                for (permanentId in toTap) {
                    newState = newState.updateEntity(permanentId) { it.with(TappedComponent) }
                }

                CostPaymentResult.success(newState, manaPool)
            }
            is AbilityCost.Composite -> {
                var currentState = state
                var currentPool = manaPool
                val allEvents = mutableListOf<GameEvent>()
                for (subCost in cost.costs) {
                    val result = payAbilityCost(currentState, subCost, sourceId, controllerId, currentPool, choices)
                    if (!result.success) return result
                    currentState = result.newState!!
                    currentPool = result.newManaPool!!
                    allEvents.addAll(result.events)
                }
                CostPaymentResult.success(currentState, currentPool, allEvents)
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
                findMatchingPermanentsUnified(state, controllerId, cost.filter).size >= cost.count
            }
            is AdditionalCost.DiscardCards -> {
                val handZone = ZoneKey(controllerId, Zone.HAND)
                findMatchingCardsUnified(state, state.getZone(handZone), cost.filter, controllerId).size >= cost.count
            }
            is AdditionalCost.PayLife -> {
                val life = state.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
                life > cost.amount
            }
            is AdditionalCost.ExileCards -> {
                val zone = ZoneKey(controllerId, cost.fromZone.toZone())
                findMatchingCardsUnified(state, state.getZone(zone), cost.filter, controllerId).size >= cost.count
            }
            is AdditionalCost.TapPermanents -> {
                findUntappedMatchingPermanentsUnified(state, controllerId, cost.filter).size >= cost.count
            }
        }
    }

    // Helper functions

    private fun findMatchingPermanentsUnified(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        return state.entities.filter { (entityId, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            predicateEvaluator.matches(state, entityId, filter, context)
        }.keys.toList()
    }

    private fun findUntappedMatchingPermanentsUnified(
        state: GameState,
        controllerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        return state.entities.filter { (entityId, container) ->
            container.get<ControllerComponent>()?.playerId == controllerId &&
            !container.has<TappedComponent>() &&
            predicateEvaluator.matches(state, entityId, filter, context)
        }.keys.toList()
    }

    private fun findMatchingCardsUnified(
        state: GameState,
        cardIds: List<EntityId>,
        filter: GameObjectFilter,
        controllerId: EntityId
    ): List<EntityId> {
        val context = PredicateContext(controllerId = controllerId)
        return cardIds.filter { cardId ->
            predicateEvaluator.matches(state, cardId, filter, context)
        }
    }

    private fun com.wingedsheep.sdk.scripting.CostZone.toZone(): Zone = when (this) {
        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
    }
}

/**
 * Result of paying a cost.
 */
data class CostPaymentResult(
    val success: Boolean,
    val newState: GameState?,
    val newManaPool: ManaPool?,
    val error: String?,
    val events: List<GameEvent> = emptyList()
) {
    companion object {
        fun success(state: GameState, manaPool: ManaPool, events: List<GameEvent> = emptyList()) =
            CostPaymentResult(true, state, manaPool, null, events)

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
