package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.player.PlayerHexproofComponent
import com.wingedsheep.engine.state.components.player.PlayerShroudComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Identifies the type of source that is doing the targeting.
 * Used to implement restrictions like "can't be the target of abilities your opponents control"
 * which only block abilities, not spells.
 */
enum class TargetingSourceType {
    /** The source is a spell (instant/sorcery/aura/etc.) */
    SPELL,
    /** The source is an activated or triggered ability */
    ABILITY,
    /** Unknown or default — no source-type-based restrictions apply */
    ANY
}

/**
 * Finds legal targets for a given target requirement.
 *
 * This class evaluates a TargetRequirement against the current game state
 * and returns a list of valid target EntityIds.
 */
class TargetFinder(
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
        ignoreTargetingRestrictions: Boolean = false,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        return when (requirement) {
            is TargetPlayer -> findPlayerTargets(state, requirement, controllerId)
            is TargetOpponent -> findOpponentTargets(state, controllerId)
            is AnyTarget -> findAnyTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetCreatureOrPlayer -> findCreatureOrPlayerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetOpponentOrPlaneswalker -> findOpponentOrPlaneswalkerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetPlayerOrPlaneswalker -> findPlayerOrPlaneswalkerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetCreatureOrPlaneswalker -> findCreatureOrPlaneswalkerTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetObject -> findObjectTargets(state, requirement, controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType)
            is TargetSpellOrPermanent -> findSpellOrPermanentTargets(state, controllerId, sourceId, targetingSourceType)
            is TargetOther -> {
                // For TargetOther, find targets for the base requirement but exclude the source
                val baseTargets = findLegalTargets(state, requirement.baseRequirement, controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType)
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
            state.hasEntity(playerId) && !playerHasShroud(state, playerId) &&
                !playerHasHexproofAgainst(state, playerId, controllerId)
        }
    }

    private fun findOpponentTargets(state: GameState, controllerId: EntityId): List<EntityId> {
        return state.turnOrder.filter { it != controllerId && state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproof(state, it) }
    }

    /**
     * Check if a permanent is restricted from being targeted by the given source type.
     * Checks for CantBeTargetedByOpponentAbilitiesComponent — which blocks opponent abilities
     * but not opponent spells.
     */
    private fun hasCantBeTargetedRestriction(
        state: GameState,
        entityId: EntityId,
        entityController: EntityId?,
        controllerId: EntityId,
        targetingSourceType: TargetingSourceType
    ): Boolean {
        if (entityController == controllerId) return false  // own permanents are never restricted
        if (targetingSourceType == TargetingSourceType.SPELL) return false  // spells bypass this restriction

        val container = state.getEntity(entityId) ?: return false
        if (container.has<CantBeTargetedByOpponentAbilitiesComponent>()) {
            // For ABILITY source type, always blocked
            // For ANY (unknown), conservatively block since we don't know the source type
            return true
        }
        return false
    }

    private fun findOpponentOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add opponents (excluding those with shroud or hexproof)
        targets.addAll(state.turnOrder.filter { it != controllerId && state.hasEntity(it) &&
            !playerHasShroud(state, it) && !playerHasHexproof(state, it) })

        // Add all planeswalkers on the battlefield
        val battlefield = state.getBattlefield()
        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            if (!cardComponent.isPlaneswalker) continue

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue
            // Check hexproof from color
            if (hasHexproofFromSourceColors(state, projected, entityId, entityController, controllerId, sourceId)) continue
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType)) continue

            targets.add(entityId)
        }

        return targets
    }

    private fun findPlayerOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add all players (excluding those with shroud or hexproof from opponents)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproofAgainst(state, it, controllerId) })

        // Add all planeswalkers on the battlefield
        val battlefield = state.getBattlefield()
        for (entityId in battlefield) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            if (!cardComponent.isPlaneswalker) continue

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue
            // Check hexproof from color
            if (hasHexproofFromSourceColors(state, projected, entityId, entityController, controllerId, sourceId)) continue
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType)) continue

            targets.add(entityId)
        }

        return targets
    }

    private fun findPermanentTargets(
        state: GameState,
        requirement: TargetObject,
        controllerId: EntityId,
        sourceId: EntityId?,
        ignoreTargetingRestrictions: Boolean = false,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        val filter = requirement.filter

        return battlefield.filter { entityId ->
            // Exclude self if filter says "other"
            if (filter.excludeSelf && entityId == sourceId) {
                return@filter false
            }

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
                // Check hexproof from color
                if (hasHexproofFromSourceColors(state, projected, entityId, entityController, controllerId, sourceId)) {
                    return@filter false
                }
                // Check can't-be-targeted-by-abilities
                if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType)) {
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
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add all players (excluding those with shroud or hexproof from opponents)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproofAgainst(state, it, controllerId) })

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
            // Check hexproof from color
            if (hasHexproofFromSourceColors(state, projected, entityId, entityController, controllerId, sourceId)) {
                continue
            }
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType)) {
                continue
            }

            targets.add(entityId)
        }

        return targets
    }

    private fun findCreatureOrPlayerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()

        // Add all players (excluding those with shroud or hexproof from opponents)
        targets.addAll(state.turnOrder.filter { state.hasEntity(it) && !playerHasShroud(state, it) &&
            !playerHasHexproofAgainst(state, it, controllerId) })

        // Add all creatures
        targets.addAll(findPermanentTargets(state, TargetCreature(), controllerId, sourceId, targetingSourceType = targetingSourceType))

        return targets
    }

    private fun findCreatureOrPlaneswalkerTargets(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
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
            // Check hexproof from color
            if (hasHexproofFromSourceColors(state, projected, entityId, entityController, controllerId, sourceId)) {
                return@filter false
            }
            // Check can't-be-targeted-by-abilities
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType)) {
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
        ignoreTargetingRestrictions: Boolean = false,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val filter = requirement.filter
        return when (filter.zone) {
            Zone.BATTLEFIELD -> findPermanentTargets(state, requirement, controllerId, sourceId, ignoreTargetingRestrictions, targetingSourceType)
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
        sourceId: EntityId?,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY
    ): List<EntityId> {
        val projected = state.projectedState
        val targets = mutableListOf<EntityId>()

        // Add all permanents on the battlefield
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue
            val entityController = container.get<ControllerComponent>()?.playerId

            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) continue
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) continue
            // Check hexproof from color
            if (hasHexproofFromSourceColors(state, projected, entityId, entityController, controllerId, sourceId)) continue
            if (hasCantBeTargetedRestriction(state, entityId, entityController, controllerId, targetingSourceType)) continue

            targets.add(entityId)
        }

        // Add all spells on the stack
        targets.addAll(state.stack.filter { spellId ->
            state.getEntity(spellId)?.get<CardComponent>() != null
        })

        return targets
    }

    /**
     * Check if a player has shroud (e.g., from True Believer's "You have shroud"
     * or Gilded Light's "You gain shroud until end of turn").
     * A player has shroud if:
     * - Any permanent on the battlefield controlled by that player has GrantsControllerShroudComponent, OR
     * - The player entity has PlayerShroudComponent (from a spell effect)
     */
    private fun playerHasShroud(state: GameState, playerId: EntityId): Boolean {
        // Check for temporary player shroud (e.g., Gilded Light)
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerShroudComponent>() == true) return true

        // Check for permanent-based shroud (e.g., True Believer)
        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerShroudComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    /**
     * Check if a player has hexproof (from a permanent like Shalai, Voice of Plenty).
     * Unlike shroud, hexproof only prevents opponents from targeting — the player can still
     * target themselves.
     */
    private fun playerHasHexproof(state: GameState, playerId: EntityId): Boolean {
        val playerEntity = state.getEntity(playerId)
        if (playerEntity?.has<PlayerHexproofComponent>() == true) return true

        return state.getBattlefield().any { entityId ->
            val container = state.getEntity(entityId) ?: return@any false
            container.get<GrantsControllerHexproofComponent>() != null &&
                container.get<ControllerComponent>()?.playerId == playerId
        }
    }

    /**
     * Check if a player has hexproof against a specific controller.
     * Returns true if the player has hexproof AND the controller is an opponent.
     */
    private fun playerHasHexproofAgainst(state: GameState, playerId: EntityId, controllerId: EntityId): Boolean {
        return playerId != controllerId && playerHasHexproof(state, playerId)
    }

    /**
     * Check if a permanent has hexproof from a color that matches the targeting source's colors.
     * "Hexproof from [color]" means opponents can't target it with spells/abilities of that color.
     *
     * Gets source colors from projected state (for battlefield permanents) and falls back to
     * the base CardComponent colors (for spells in hand/on the stack that aren't projected).
     *
     * @return true if the entity is protected by hexproof-from-color against the source
     */
    private fun hasHexproofFromSourceColors(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        entityController: EntityId?,
        controllerId: EntityId,
        sourceId: EntityId?
    ): Boolean {
        if (entityController == controllerId || sourceId == null) return false
        // Try projected colors first (for permanents on the battlefield),
        // then fall back to base CardComponent colors (for spells in hand/on stack)
        var sourceColors = projected.getColors(sourceId)
        if (sourceColors.isEmpty()) {
            sourceColors = state.getEntity(sourceId)?.get<CardComponent>()
                ?.colors?.map { it.name }?.toSet() ?: emptySet()
        }
        return sourceColors.any { colorName ->
            projected.hasKeyword(entityId, "HEXPROOF_FROM_$colorName")
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
