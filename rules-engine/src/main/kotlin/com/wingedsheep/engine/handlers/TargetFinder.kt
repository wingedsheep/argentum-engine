package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.Zone
import com.wingedsheep.sdk.scripting.toZoneType
import com.wingedsheep.sdk.targeting.*

/**
 * Finds legal targets for a given target requirement.
 *
 * This class evaluates a TargetRequirement against the current game state
 * and returns a list of valid target EntityIds.
 */
class TargetFinder(
    private val stateProjector: StateProjector = StateProjector()
) {
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Find all legal targets for a given requirement.
     *
     * @param state The current game state
     * @param requirement The target requirement to satisfy
     * @param controllerId The player who is choosing targets (for "you control" filters)
     * @param sourceId The source of the targeting ability (to exclude "other" targets)
     * @return List of valid target EntityIds
     */
    fun findLegalTargets(
        state: GameState,
        requirement: TargetRequirement,
        controllerId: EntityId,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (requirement) {
            is TargetCreature -> findCreatureTargets(state, requirement, controllerId, sourceId)
            is TargetPlayer -> findPlayerTargets(state, requirement, controllerId)
            is TargetOpponent -> findOpponentTargets(state, controllerId)
            is TargetPermanent -> findPermanentTargets(state, requirement, controllerId, sourceId)
            is AnyTarget -> findAnyTargets(state, controllerId, sourceId)
            is TargetCreatureOrPlayer -> findCreatureOrPlayerTargets(state, controllerId, sourceId)
            is TargetCreatureOrPlaneswalker -> findCreatureOrPlaneswalkerTargets(state, controllerId, sourceId)
            is TargetCardInGraveyard -> findGraveyardTargets(state, requirement, controllerId)
            is TargetSpell -> findSpellTargets(state, requirement, controllerId)
            is TargetObject -> findObjectTargets(state, requirement, controllerId, sourceId)
            is TargetOther -> {
                // For TargetOther, find targets for the base requirement but exclude the source
                val baseTargets = findLegalTargets(state, requirement.baseRequirement, controllerId, sourceId)
                val excludeId = requirement.excludeSourceId ?: sourceId
                if (excludeId != null) baseTargets.filter { it != excludeId } else baseTargets
            }
        }
    }

    private fun findCreatureTargets(
        state: GameState,
        requirement: TargetCreature,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val filter = requirement.filter

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            // Must be a creature - use projected state for face-down creatures (Rule 707.2)
            if (!projected.hasType(entityId, "CREATURE")) return@filter false

            // Check hexproof - can't be targeted by opponents
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                return@filter false
            }

            // Check shroud - can't be targeted by anyone
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            // Use unified filter with projected state
            val predicateContext = PredicateContext(controllerId = controllerId)
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, predicateContext)
        }
    }

    private fun findPlayerTargets(
        state: GameState,
        requirement: TargetPlayer,
        controllerId: EntityId
    ): List<EntityId> {
        // All players in the game
        return state.turnOrder.filter { playerId ->
            // Check if player has hexproof (e.g., Leyline of Sanctity)
            // For now, all players are valid targets
            state.hasEntity(playerId)
        }
    }

    private fun findOpponentTargets(state: GameState, controllerId: EntityId): List<EntityId> {
        return state.turnOrder.filter { it != controllerId && state.hasEntity(it) }
    }

    private fun findPermanentTargets(
        state: GameState,
        requirement: TargetPermanent,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val filter = requirement.filter

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                return@filter false
            }
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            // Use unified filter with projected state
            val predicateContext = PredicateContext(controllerId = controllerId)
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, predicateContext)
        }
    }

    private fun findAnyTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val targets = mutableListOf<EntityId>()

        // Add all players
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) })

        // Add all creatures and planeswalkers
        val battlefield = state.getBattlefield()
        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            // Only creatures and planeswalkers for "any target"
            if (!cardComponent.typeLine.isCreature && !cardComponent.isPlaneswalker) {
                continue
            }

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                continue
            }
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                continue
            }

            targets.add(entityId)
        }

        return targets
    }

    private fun findCreatureOrPlayerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()

        // Add all players
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) })

        // Add all creatures
        targets.addAll(findCreatureTargets(state, TargetCreature(), controllerId, sourceId))

        return targets
    }

    private fun findCreatureOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            // Must be creature or planeswalker
            if (!cardComponent.typeLine.isCreature && !cardComponent.isPlaneswalker) {
                return@filter false
            }

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                return@filter false
            }
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            true
        }
    }

    private fun findGraveyardTargets(
        state: GameState,
        requirement: TargetCardInGraveyard,
        controllerId: EntityId
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()
        val filter = requirement.filter

        // Check all graveyards - the unified filter's OwnedByYou predicate handles "your graveyard" restriction
        for (playerId in state.turnOrder) {
            val graveyardKey = ZoneKey(playerId, ZoneType.GRAVEYARD)
            val graveyard = state.getZone(graveyardKey)

            for (cardId in graveyard) {
                // Use unified filter - OwnedByYou predicate handles "your graveyard" restriction
                val predicateContext = PredicateContext(controllerId = controllerId, ownerId = playerId)
                if (predicateEvaluator.matches(state, cardId, filter.baseFilter, predicateContext)) {
                    targets.add(cardId)
                }
            }
        }

        return targets
    }

    private fun findSpellTargets(
        state: GameState,
        requirement: TargetSpell,
        controllerId: EntityId
    ): List<EntityId> {
        val filter = requirement.filter
        val predicateContext = PredicateContext(controllerId = controllerId)
        return state.stack.filter { spellId ->
            predicateEvaluator.matches(state, spellId, filter.baseFilter, predicateContext)
        }
    }

    /**
     * Find targets for TargetObject, dispatching based on the filter's zone.
     */
    private fun findObjectTargets(
        state: GameState,
        requirement: TargetObject,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val filter = requirement.filter
        return when (filter.zone) {
            Zone.Battlefield -> findPermanentTargets(
                state,
                TargetPermanent(count = requirement.count, optional = requirement.optional, filter = filter),
                controllerId,
                sourceId
            )
            Zone.Graveyard -> findGraveyardTargets(
                state,
                TargetCardInGraveyard(count = requirement.count, optional = requirement.optional, filter = filter),
                controllerId
            )
            Zone.Stack -> findSpellTargets(
                state,
                TargetSpell(count = requirement.count, optional = requirement.optional, filter = filter),
                controllerId
            )
            else -> findCardTargetsInZone(state, filter, controllerId)
        }
    }

    /**
     * Find card targets in non-battlefield, non-stack zones (hand, library, exile, command).
     */
    private fun findCardTargetsInZone(
        state: GameState,
        filter: TargetFilter,
        controllerId: EntityId
    ): List<EntityId> {
        val zoneType = filter.zone.toZoneType()
        val targets = mutableListOf<EntityId>()

        for (playerId in state.turnOrder) {
            val zoneKey = ZoneKey(playerId, zoneType)
            val zone = state.getZone(zoneKey)

            for (cardId in zone) {
                val predicateContext = PredicateContext(controllerId = controllerId, ownerId = playerId)
                if (predicateEvaluator.matches(state, cardId, filter.baseFilter, predicateContext)) {
                    targets.add(cardId)
                }
            }
        }

        return targets
    }
}
