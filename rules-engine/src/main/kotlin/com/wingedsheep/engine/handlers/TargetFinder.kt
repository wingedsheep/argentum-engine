package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

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
     * @param ignoreTargetingRestrictions If true, hexproof and shroud are bypassed.
     *   Use for aura attachment (Rule 303.4f): when an aura enters the battlefield without
     *   being cast, the controller chooses what it enchants — normal targeting restrictions
     *   like hexproof and shroud do not apply.
     * @return List of valid target EntityIds
     */
    fun findLegalTargets(
        state: GameState,
        requirement: TargetRequirement,
        controllerId: EntityId,
        sourceId: EntityId? = null,
        ignoreTargetingRestrictions: Boolean = false
    ): List<EntityId> {
        return when (requirement) {
            is TargetPlayer -> findPlayerTargets(state, requirement, controllerId)
            is TargetOpponent -> findOpponentTargets(state, controllerId)
            is AnyTarget -> findAnyTargets(state, controllerId, sourceId)
            is TargetCreatureOrPlayer -> findCreatureOrPlayerTargets(state, controllerId, sourceId)
            is TargetCreatureOrPlaneswalker -> findCreatureOrPlaneswalkerTargets(state, controllerId, sourceId)
            is TargetObject -> findObjectTargets(state, requirement, controllerId, sourceId, ignoreTargetingRestrictions)
            is TargetSpellOrPermanent -> findSpellOrPermanentTargets(state, controllerId, sourceId)
            is TargetOther -> {
                // For TargetOther, find targets for the base requirement but exclude the source
                val baseTargets = findLegalTargets(state, requirement.baseRequirement, controllerId, sourceId, ignoreTargetingRestrictions)
                val excludeId = requirement.excludeSourceId ?: sourceId
                if (excludeId != null) baseTargets.filter { it != excludeId } else baseTargets
            }
        }
    }

    private fun findPlayerTargets(
        state: GameState,
        requirement: TargetPlayer,
        controllerId: EntityId
    ): List<EntityId> {
        return state.turnOrder.filter { playerId ->
            state.hasEntity(playerId) && !playerHasShroud(state, playerId)
        }
    }

    private fun findOpponentTargets(state: GameState, controllerId: EntityId): List<EntityId> {
        return state.turnOrder.filter { it != controllerId && state.hasEntity(it) && !playerHasShroud(state, it) }
    }

    private fun findPermanentTargets(
        state: GameState,
        requirement: TargetObject,
        controllerId: EntityId,
        sourceId: EntityId?,
        ignoreTargetingRestrictions: Boolean = false
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val battlefield = state.getBattlefield()
        val filter = requirement.filter

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            if (!ignoreTargetingRestrictions) {
                // Check hexproof/shroud
                if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                    return@filter false
                }
                if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                    return@filter false
                }
            }

            // Use unified filter with projected state
            val predicateContext = PredicateContext(controllerId = controllerId, sourceId = sourceId)
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

        // Add all players (excluding those with shroud)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) })

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

        // Add all players (excluding those with shroud)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) })

        // Add all creatures
        targets.addAll(findPermanentTargets(state, TargetCreature(), controllerId, sourceId))

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
        filter: TargetFilter,
        controllerId: EntityId
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()

        // Check all graveyards - the unified filter's OwnedByYou predicate handles "your graveyard" restriction
        for (playerId in state.turnOrder) {
            val graveyardKey = ZoneKey(playerId, Zone.GRAVEYARD)
            val graveyard = state.getZone(graveyardKey)

            for (cardId in graveyard) {
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
        requirement: TargetObject,
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
        sourceId: EntityId?,
        ignoreTargetingRestrictions: Boolean = false
    ): List<EntityId> {
        val filter = requirement.filter
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findPermanentTargets(state, requirement, controllerId, sourceId, ignoreTargetingRestrictions)
            Zone.GRAVEYARD -> findGraveyardTargets(state, filter, controllerId)
            Zone.STACK -> findSpellTargets(state, requirement, controllerId)
            else -> findCardTargetsInZone(state, filter, controllerId)
        }
    }

    /**
     * Find targets that are either permanents on the battlefield or spells on the stack.
     * Used by Artificial Evolution's "target spell or permanent" requirement.
     */
    private fun findSpellOrPermanentTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?
    ): List<EntityId> {
        val projected = stateProjector.project(state)
        val targets = mutableListOf<EntityId>()

        // Add all permanents on the battlefield
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue

            targets.add(entityId)
        }

        // Add all spells on the stack
        targets.addAll(state.stack.filter { spellId ->
            state.getEntity(spellId)?.get<CardComponent>() != null
        })

        return targets
    }

    /**
     * Check if a player has shroud (e.g., from True Believer's "You have shroud").
     * A player has shroud if any permanent on the battlefield controlled by that player
     * has the GrantsControllerShroudComponent.
     */
    private fun playerHasShroud(state: GameState, playerId: EntityId): Boolean {
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
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
        val zoneType = filter.zone
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
