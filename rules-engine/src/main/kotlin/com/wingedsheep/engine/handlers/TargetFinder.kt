package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
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
            is TargetCardInGraveyard -> findGraveyardTargets(state, requirement)
            is TargetSpell -> findSpellTargets(state, requirement)
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

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) return@filter false

            // Check hexproof - can't be targeted by opponents
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                return@filter false
            }

            // Check shroud - can't be targeted by anyone
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            // Apply filter
            matchesCreatureFilter(requirement.filter, container, cardComponent, entityController, controllerId, state, sourceId)
        }
    }

    private fun matchesCreatureFilter(
        filter: CreatureTargetFilter,
        container: com.wingedsheep.engine.state.ComponentContainer,
        cardComponent: CardComponent,
        entityController: EntityId?,
        controllerId: EntityId,
        state: GameState,
        sourceId: EntityId?
    ): Boolean {
        val projected = stateProjector.project(state)
        val entityId = container.get<CardComponent>()?.let {
            // Get entity ID from the card - we need to look it up
            state.entities.entries.find { it.value == container }?.key
        }

        return when (filter) {
            is CreatureTargetFilter.Any -> true
            is CreatureTargetFilter.YouControl -> entityController == controllerId
            is CreatureTargetFilter.OpponentControls -> entityController != controllerId
            is CreatureTargetFilter.Attacking -> container.has<AttackingComponent>()
            is CreatureTargetFilter.Blocking -> container.has<BlockingComponent>()
            is CreatureTargetFilter.Tapped -> container.has<TappedComponent>()
            is CreatureTargetFilter.Untapped -> !container.has<TappedComponent>()
            is CreatureTargetFilter.WithKeyword -> {
                entityId != null && projected.hasKeyword(entityId, filter.keyword)
            }
            is CreatureTargetFilter.WithoutKeyword -> {
                entityId == null || !projected.hasKeyword(entityId, filter.keyword)
            }
            is CreatureTargetFilter.WithColor -> {
                cardComponent.colors.contains(filter.color)
            }
            is CreatureTargetFilter.WithPowerAtMost -> {
                val power = entityId?.let { projected.getPower(it) } ?: cardComponent.baseStats?.basePower ?: 0
                power <= filter.maxPower
            }
            is CreatureTargetFilter.WithPowerAtLeast -> {
                val power = entityId?.let { projected.getPower(it) } ?: cardComponent.baseStats?.basePower ?: 0
                power >= filter.minPower
            }
            is CreatureTargetFilter.WithToughnessAtMost -> {
                val toughness = entityId?.let { projected.getToughness(it) } ?: cardComponent.baseStats?.baseToughness ?: 0
                toughness <= filter.maxToughness
            }
            is CreatureTargetFilter.WithSubtype -> {
                cardComponent.typeLine.hasSubtype(filter.subtype)
            }
            is CreatureTargetFilter.And -> {
                filter.filters.all { matchesCreatureFilter(it, container, cardComponent, entityController, controllerId, state, sourceId) }
            }
            is CreatureTargetFilter.AttackingYouControl -> {
                entityController == controllerId && container.has<AttackingComponent>()
            }
            is CreatureTargetFilter.AttackingWithSubtypeYouControl -> {
                entityController == controllerId &&
                    container.has<AttackingComponent>() &&
                    cardComponent.typeLine.hasSubtype(filter.subtype)
            }
            is CreatureTargetFilter.NotColor -> {
                !cardComponent.colors.contains(filter.color)
            }
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

        return battlefield.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val entityController = container.get<ControllerComponent>()?.playerId

            // Check hexproof/shroud
            if (projected.hasKeyword(entityId, Keyword.HEXPROOF) && entityController != controllerId) {
                return@filter false
            }
            if (projected.hasKeyword(entityId, Keyword.SHROUD)) {
                return@filter false
            }

            // Apply filter
            matchesPermanentFilter(requirement.filter, cardComponent, entityController, controllerId)
        }
    }

    private fun matchesPermanentFilter(
        filter: PermanentTargetFilter,
        cardComponent: CardComponent,
        entityController: EntityId?,
        controllerId: EntityId
    ): Boolean {
        return when (filter) {
            is PermanentTargetFilter.Any -> true
            is PermanentTargetFilter.YouControl -> entityController == controllerId
            is PermanentTargetFilter.OpponentControls -> entityController != controllerId
            is PermanentTargetFilter.Creature -> cardComponent.typeLine.isCreature
            is PermanentTargetFilter.Artifact -> cardComponent.typeLine.isArtifact
            is PermanentTargetFilter.Enchantment -> cardComponent.typeLine.isEnchantment
            is PermanentTargetFilter.Land -> cardComponent.typeLine.isLand
            is PermanentTargetFilter.NonCreature -> !cardComponent.typeLine.isCreature
            is PermanentTargetFilter.NonLand -> !cardComponent.typeLine.isLand
            is PermanentTargetFilter.CreatureOrLand -> cardComponent.typeLine.isCreature || cardComponent.typeLine.isLand
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
        requirement: TargetCardInGraveyard
    ): List<EntityId> {
        val targets = mutableListOf<EntityId>()

        // Check all graveyards
        for (playerId in state.turnOrder) {
            val graveyardKey = ZoneKey(playerId, ZoneType.GRAVEYARD)
            val graveyard = state.getZone(graveyardKey)

            for (cardId in graveyard) {
                val container = state.getEntity(cardId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                // Apply filter
                val matches = when (requirement.filter) {
                    is GraveyardCardFilter.Any -> true
                    is GraveyardCardFilter.Creature -> cardComponent.typeLine.isCreature
                    is GraveyardCardFilter.Instant -> cardComponent.typeLine.isInstant
                    is GraveyardCardFilter.Sorcery -> cardComponent.typeLine.isSorcery
                    is GraveyardCardFilter.InstantOrSorcery ->
                        cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery
                    is GraveyardCardFilter.CreatureInYourGraveyard -> {
                        // This filter implies "your" graveyard - handle via context
                        cardComponent.typeLine.isCreature
                    }
                }

                if (matches) {
                    targets.add(cardId)
                }
            }
        }

        return targets
    }

    private fun findSpellTargets(
        state: GameState,
        requirement: TargetSpell
    ): List<EntityId> {
        val filter = requirement.filter
        return state.stack.filter { spellId ->
            val container = state.getEntity(spellId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false

            // Apply filter
            when (filter) {
                is SpellTargetFilter.Any -> true
                is SpellTargetFilter.Creature -> cardComponent.typeLine.isCreature
                is SpellTargetFilter.Noncreature -> !cardComponent.typeLine.isCreature
                is SpellTargetFilter.Instant -> cardComponent.typeLine.isInstant
                is SpellTargetFilter.Sorcery -> cardComponent.typeLine.isSorcery
                is SpellTargetFilter.WithManaValue -> cardComponent.manaValue == filter.manaValue
                is SpellTargetFilter.WithManaValueAtMost -> cardComponent.manaValue <= filter.manaValue
                is SpellTargetFilter.WithManaValueAtLeast -> cardComponent.manaValue >= filter.manaValue
            }
        }
    }
}
