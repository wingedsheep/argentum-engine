package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Extracted target-finding helpers from LegalActionsCalculator.
 * These methods find valid targets for spells, abilities, and effects.
 */
class TargetEnumerationUtils(
    private val predicateEvaluator: PredicateEvaluator
) {
    fun findValidTargets(
        state: GameState,
        playerId: EntityId,
        requirement: TargetRequirement,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (requirement) {
            is TargetPlayer -> state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                !playerHasHexproofAgainst(state, it, playerId) }
            is TargetOpponent -> state.turnOrder.filter { it != playerId && state.hasEntity(it) && !playerHasShroud(state, it) &&
                !playerHasHexproof(state, it) }
            is AnyTarget -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) }
                creatures + players
            }
            is TargetCreatureOrPlayer -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) }
                creatures + players
            }
            is TargetPlayerOrPlaneswalker -> {
                val players = state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
                    !playerHasHexproofAgainst(state, it, playerId) }
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                players + planeswalkers
            }
            is TargetCreatureOrPlaneswalker -> {
                val creatures = findValidPermanentTargets(state, playerId, TargetFilter.Creature, sourceId)
                val planeswalkers = findValidPermanentTargets(state, playerId, TargetFilter.Planeswalker, sourceId)
                creatures + planeswalkers
            }
            is TargetObject -> findValidObjectTargets(state, playerId, requirement.filter, sourceId)
            is TargetSpellOrPermanent -> {
                val permanents = findValidPermanentTargets(state, playerId, TargetFilter.Permanent, sourceId)
                val spells = findValidSpellTargets(state, playerId, TargetFilter.SpellOnStack)
                permanents + spells
            }
            else -> emptyList()
        }
    }

    fun shouldAutoSelectPlayerTarget(
        requirement: TargetRequirement,
        validTargets: List<EntityId>
    ): Boolean {
        val isPlayerTarget = requirement is TargetPlayer || requirement is TargetOpponent
        val requiresExactlyOne = requirement.count == 1 && requirement.effectiveMinCount == 1
        val hasExactlyOneChoice = validTargets.size == 1
        return isPlayerTarget && requiresExactlyOne && hasExactlyOneChoice
    }

    fun findValidPermanentTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        return battlefield.filter { entityId ->
            if (filter.excludeSelf && entityId == sourceId) return@filter false
            val entityController = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != playerId) return@filter false
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) return@filter false
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, context)
        }
    }

    fun findValidGraveyardTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val playerIds = if (filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou) {
            listOf(playerId)
        } else {
            state.turnOrder.toList()
        }
        val context = PredicateContext(controllerId = playerId)
        return playerIds.flatMap { pid ->
            state.getGraveyard(pid).filter { entityId ->
                predicateEvaluator.matches(state, entityId, filter.baseFilter, context)
            }
        }
    }

    fun findValidObjectTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter,
        sourceId: EntityId? = null
    ): List<EntityId> {
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findValidPermanentTargets(state, playerId, filter, sourceId)
            Zone.GRAVEYARD -> findValidGraveyardTargets(state, playerId, filter)
            Zone.STACK -> findValidSpellTargets(state, playerId, filter)
            else -> emptyList()
        }
    }

    fun findValidSpellTargets(
        state: GameState,
        playerId: EntityId,
        filter: TargetFilter
    ): List<EntityId> {
        val context = PredicateContext(controllerId = playerId)
        return state.stack.filter { spellId ->
            predicateEvaluator.matches(state, spellId, filter.baseFilter, context)
        }
    }

    fun getTargetZone(requirement: TargetRequirement): String? {
        return when (requirement) {
            is TargetObject -> requirement.filter.zone.takeIf { it != Zone.BATTLEFIELD }?.let {
                when (requirement.filter.zone) {
                    Zone.GRAVEYARD -> "Graveyard"
                    Zone.STACK -> "Stack"
                    Zone.EXILE -> "Exile"
                    Zone.HAND -> "Hand"
                    Zone.LIBRARY -> "Library"
                    Zone.COMMAND -> "Command"
                    else -> null
                }
            }
            else -> null
        }
    }

    fun buildTargetInfos(
        state: GameState,
        playerId: EntityId,
        targetReqs: List<TargetRequirement>,
        sourceId: EntityId? = null
    ): List<TargetInfo> {
        return targetReqs.mapIndexed { index, req ->
            val validTargets = findValidTargets(state, playerId, req, sourceId)
            TargetInfo(
                index = index,
                description = req.description,
                minTargets = req.effectiveMinCount,
                maxTargets = req.count,
                validTargets = validTargets,
                targetZone = getTargetZone(req)
            )
        }
    }

    fun allRequirementsSatisfied(targetInfos: List<TargetInfo>): Boolean {
        return targetInfos.all { it.validTargets.isNotEmpty() || it.minTargets == 0 }
    }

    // Player protection checks

    fun playerHasShroud(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerShroudComponent>() == true) return true
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean {
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerHexproofComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, controllerId: EntityId): Boolean {
        return playerId != controllerId && playerHasHexproof(state, playerId)
    }
}
