package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.ConvokeCreatureData
import com.wingedsheep.engine.legalactions.CounterRemovalCreatureData
import com.wingedsheep.engine.legalactions.DelveCardData
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSource
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Extracted cost-checking helpers from LegalActionsCalculator.
 * These methods check cost payability, find sacrifice/tap/exile targets, etc.
 */
class CostEnumerationUtils(
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val predicateEvaluator: PredicateEvaluator,
    private val cardRegistry: CardRegistry
) {
    // --- Sacrifice targets ---

    fun findSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        cost: AdditionalCost.SacrificePermanent
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, cost.filter, predicateContext)
        }
    }

    fun findVariableSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    fun findAbilitySacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId? = null
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Tap targets ---

    fun findAbilityTapTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            if (container.has<TappedComponent>()) return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Bounce targets ---

    fun findAbilityBounceTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Exile targets ---

    fun findExileTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        fromZone: CostZone
    ): List<EntityId> {
        val zone = when (fromZone) {
            CostZone.GRAVEYARD -> Zone.GRAVEYARD
            CostZone.HAND -> Zone.HAND
            CostZone.LIBRARY -> Zone.LIBRARY
            CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
        }
        val zoneKey = ZoneKey(playerId, zone)
        val predicateContext = PredicateContext(controllerId = playerId)
        return state.getZone(zoneKey).filter { entityId ->
            predicateEvaluator.matches(state, entityId, filter, predicateContext)
        }
    }

    /**
     * Find cards in the player's hand matching the filter — used for discard costs on
     * activated abilities (e.g., "Discard a land card: ...").
     */
    fun findDiscardTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)
        val predicateContext = PredicateContext(controllerId = playerId)
        return hand.filter { predicateEvaluator.matches(state, it, filter, predicateContext) }
    }

    // --- Morph cost targets ---

    fun findMorphDiscardTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> =
        findDiscardTargets(state, playerId, filter)

    fun findMorphRevealTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> {
        return findMorphDiscardTargets(state, playerId, filter) // Same logic
    }

    fun findMorphSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    fun findMorphExileTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter, zone: Zone): List<EntityId> {
        val zoneKey = ZoneKey(playerId, zone)
        val cards = state.getZone(zoneKey)
        val predicateContext = PredicateContext(controllerId = playerId)
        return cards.filter { predicateEvaluator.matches(state, it, filter, predicateContext) }
    }

    fun findReturnToHandTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Convoke ---

    fun findConvokeCreatures(state: GameState, playerId: EntityId): List<ConvokeCreatureData> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            if (!cardComponent.typeLine.isCreature) return@mapNotNull null
            if (container.has<TappedComponent>()) return@mapNotNull null
            ConvokeCreatureData(entityId, cardComponent.name, cardComponent.colors)
        }
    }

    fun canAffordWithConvoke(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        convokeCreatures: List<ConvokeCreatureData>,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        val availableMana = manaSolver.getAvailableManaCount(state, playerId, precomputedSources)
        val totalResources = availableMana + convokeCreatures.size
        if (totalResources < manaCost.cmc) return false

        val coloredRequirements = manaCost.colorCount
        val creatureColors = mutableMapOf<Color, Int>()
        for (creature in convokeCreatures) {
            for (color in creature.colors) {
                creatureColors[color] = (creatureColors[color] ?: 0) + 1
            }
        }
        var creaturesUsedForColors = 0
        for ((color, needed) in coloredRequirements) {
            val creaturesOfColor = creatureColors[color] ?: 0
            creaturesUsedForColors += minOf(needed, creaturesOfColor)
        }
        val genericRequired = manaCost.genericAmount
        val creaturesForGeneric = convokeCreatures.size - creaturesUsedForColors
        val resourcesForGeneric = availableMana + creaturesForGeneric
        return resourcesForGeneric >= genericRequired
    }

    // --- Delve ---

    fun findDelveCards(state: GameState, playerId: EntityId): List<DelveCardData> {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        return state.getZone(graveyardZone).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            DelveCardData(entityId, cardComponent.name, cardDef?.metadata?.imageUri)
        }
    }

    fun canAffordWithDelve(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        delveCards: List<DelveCardData>,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        val maxDelve = minOf(delveCards.size, manaCost.genericAmount)
        val reducedCost = manaCost.reduceGeneric(maxDelve)
        return manaSolver.canPay(state, playerId, reducedCost, precomputedSources = precomputedSources)
    }

    fun calculateMinDelveNeeded(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        delveCards: List<DelveCardData>,
        precomputedSources: List<ManaSource>? = null
    ): Int {
        if (manaSolver.canPay(state, playerId, manaCost, precomputedSources = precomputedSources)) return 0
        val maxDelve = minOf(delveCards.size, manaCost.genericAmount)
        for (delveCount in 1..maxDelve) {
            if (manaSolver.canPay(state, playerId, manaCost.reduceGeneric(delveCount), precomputedSources = precomputedSources)) {
                return delveCount
            }
        }
        return maxDelve
    }

    // --- Ability cost checking ---

    /**
     * Check if a tap cost can be paid on the given entity.
     * Returns true if payable, false if not (entity is tapped or has summoning sickness).
     */
    fun canPayTapCost(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        if (container.has<TappedComponent>()) return false
        val cardComponent = container.get<CardComponent>() ?: return false
        if (!cardComponent.typeLine.isLand && cardComponent.typeLine.isCreature) {
            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
            val hasHaste = cardComponent.baseKeywords.contains(Keyword.HASTE)
            if (hasSummoningSickness && !hasHaste) return false
        }
        return true
    }

    /**
     * Check if a TapAttachedCreature cost can be paid on the given entity.
     */
    fun canPayTapAttachedCreatureCost(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
            ?: return false
        val attachedEntity = state.getEntity(attachedId) ?: return false
        if (attachedEntity.has<TappedComponent>()) return false
        val attachedCard = attachedEntity.get<CardComponent>()
        if (attachedCard != null && attachedCard.typeLine.isCreature) {
            val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
            val hasHaste = attachedCard.baseKeywords.contains(Keyword.HASTE)
            if (hasSummoningSickness && !hasHaste) return false
        }
        return true
    }

    /**
     * Build counter removal creature info for abilities with RemoveXPlusOnePlusOneCounters cost.
     */
    fun buildCounterRemovalCreatures(state: GameState, playerId: EntityId): List<CounterRemovalCreatureData> {
        return state.entities.mapNotNull { (eid, c) ->
            if (c.get<ControllerComponent>()?.playerId == playerId &&
                c.get<CardComponent>()?.typeLine?.isCreature == true) {
                val counters = c.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                if (counters > 0) {
                    val card = c.get<CardComponent>()!!
                    CounterRemovalCreatureData(eid, card.name, counters, card.imageUri)
                } else null
            } else null
        }
    }

    /**
     * Calculate max affordable X for activated abilities, considering various X cost types.
     */
    fun calculateMaxAffordableX(
        state: GameState,
        playerId: EntityId,
        abilityCost: AbilityCost,
        manaCost: ManaCost?,
        precomputedSources: List<ManaSource>? = null
    ): Int {
        var maxX = if (manaCost != null && manaCost.hasX) {
            val availableSources = manaSolver.getAvailableManaCount(state, playerId, precomputedSources)
            val fixedCost = manaCost.cmc
            (availableSources - fixedCost).coerceAtLeast(0)
        } else {
            Int.MAX_VALUE
        }

        // Cap by graveyard size if ExileXFromGraveyard
        val hasExileXCost = when (abilityCost) {
            is AbilityCost.Composite -> abilityCost.costs.any { it is AbilityCost.ExileXFromGraveyard }
            else -> false
        }
        if (hasExileXCost) {
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            maxX = minOf(maxX, state.getZone(graveyardZone).size)
        }

        // Cap by total +1/+1 counters if RemoveXPlusOnePlusOneCounters
        val hasRemoveCounters = when (abilityCost) {
            is AbilityCost.RemoveXPlusOnePlusOneCounters -> true
            is AbilityCost.Composite -> abilityCost.costs.any { it is AbilityCost.RemoveXPlusOnePlusOneCounters }
            else -> false
        }
        if (hasRemoveCounters) {
            var totalCounters = 0
            for ((_, container) in state.entities) {
                if (container.get<ControllerComponent>()?.playerId == playerId &&
                    container.get<CardComponent>()?.typeLine?.isCreature == true) {
                    totalCounters += container.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                }
            }
            maxX = minOf(maxX, totalCounters)
        }

        // Cap by untapped matching permanents if TapXPermanents
        val tapXCost = when (abilityCost) {
            is AbilityCost.TapXPermanents -> abilityCost
            is AbilityCost.Composite -> abilityCost.costs.filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()
            else -> null
        }
        if (tapXCost != null) {
            maxX = minOf(maxX, findAbilityTapTargets(state, playerId, tapXCost.filter).size)
        }

        return maxX
    }
}
