package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ability.PendingTrigger
import com.wingedsheep.rulesengine.ability.StackedTrigger
import com.wingedsheep.rulesengine.combat.CombatState
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.player.ManaPool
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.game.TurnState
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable

/**
 * ECS-based game state.
 *
 * All game objects are entities with components attached.
 * Zones contain entity IDs rather than full objects.
 *
 * Key features:
 * - Entities are stored in a flat map by ID
 * - Components define entity properties (no fixed class schema)
 * - Zones store entity IDs, not card objects
 * - Players are entities too (with PlayerComponent)
 * - Dynamic component attachment for runtime extensibility
 *
 * Use [EcsGameEngine] for game orchestration (setup, action execution, etc.)
 */
@Serializable
data class EcsGameState(
    /**
     * All entities and their components.
     * Keys are entity IDs, values are containers with all attached components.
     */
    val entities: Map<EntityId, ComponentContainer>,

    /**
     * Zone contents as lists of entity IDs.
     * Keys are zone identifiers, values are ordered lists of entity IDs.
     */
    val zones: Map<ZoneId, List<EntityId>>,

    /**
     * Arbitrary key-value storage for scripts and effects.
     * Used for tracking game state that doesn't fit the component model.
     * Note: Values must be serializable types for persistence.
     */
    val globalFlags: Map<String, String> = emptyMap(),

    /**
     * Turn and phase tracking (shared with old system for compatibility).
     */
    val turnState: TurnState,

    /**
     * Combat state (if in combat phase).
     */
    val combat: CombatState? = null,

    /**
     * Game end state.
     */
    val isGameOver: Boolean = false,
    val winner: EntityId? = null,

    /**
     * Trigger tracking (shared with old system for compatibility).
     */
    val pendingTriggers: List<PendingTrigger> = emptyList(),
    val triggersOnStack: List<StackedTrigger> = emptyList(),

    /**
     * Pending Legend Rule choices that need player input.
     * When a player controls multiple legendary permanents with the same name,
     * they must choose which one to keep.
     */
    val pendingLegendRuleChoices: List<com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice> = emptyList()
) {
    // ==========================================================================
    // Entity Queries
    // ==========================================================================

    /**
     * Get an entity's component container.
     * Returns null if the entity doesn't exist.
     */
    fun getEntity(id: EntityId): ComponentContainer? = entities[id]

    /**
     * Check if an entity exists.
     */
    fun hasEntity(id: EntityId): Boolean = id in entities

    /**
     * Get a specific component from an entity.
     * Returns null if the entity doesn't exist or doesn't have the component.
     */
    inline fun <reified T : Component> getComponent(id: EntityId): T? =
        entities[id]?.get<T>()

    /**
     * Check if an entity has a specific component type.
     */
    inline fun <reified T : Component> hasComponent(id: EntityId): Boolean =
        entities[id]?.has<T>() == true

    /**
     * Get all entities with a specific component type.
     */
    inline fun <reified T : Component> entitiesWithComponent(): List<EntityId> =
        entities.filter { it.value.has<T>() }.keys.toList()

    /**
     * Get all player entity IDs.
     */
    fun getPlayerIds(): List<EntityId> = entitiesWithComponent<PlayerComponent>()

    /**
     * Get all card entity IDs.
     */
    fun getCardIds(): List<EntityId> = entitiesWithComponent<CardComponent>()

    // ==========================================================================
    // Zone Queries
    // ==========================================================================

    /**
     * Get the contents of a zone.
     */
    fun getZone(zoneId: ZoneId): List<EntityId> = zones[zoneId] ?: emptyList()

    /**
     * Find which zone an entity is in.
     * Returns null if the entity isn't in any zone.
     */
    fun findZone(entityId: EntityId): ZoneId? =
        zones.entries.find { entityId in it.value }?.key

    /**
     * Check if an entity is in a specific zone.
     */
    fun isInZone(entityId: EntityId, zoneId: ZoneId): Boolean =
        entityId in getZone(zoneId)

    /**
     * Get all entities on the battlefield.
     */
    fun getBattlefield(): List<EntityId> = getZone(ZoneId.BATTLEFIELD)

    /**
     * Get all entities on the stack.
     */
    fun getStack(): List<EntityId> = getZone(ZoneId.STACK)

    /**
     * Get all entities in exile.
     */
    fun getExile(): List<EntityId> = getZone(ZoneId.EXILE)

    /**
     * Check if the stack is empty.
     */
    val stackIsEmpty: Boolean get() = getStack().isEmpty()

    // ==========================================================================
    // Entity Update Operations
    // ==========================================================================

    /**
     * Update an entity's components using a transform function.
     * Returns unchanged state if the entity doesn't exist.
     */
    fun updateEntity(
        id: EntityId,
        transform: (ComponentContainer) -> ComponentContainer
    ): EcsGameState {
        val container = entities[id] ?: return this
        return copy(entities = entities + (id to transform(container)))
    }

    /**
     * Add or update a component on an entity.
     */
    fun addComponent(id: EntityId, component: Component): EcsGameState =
        updateEntity(id) { it.with(component) }

    /**
     * Remove a component from an entity.
     */
    inline fun <reified T : Component> removeComponent(id: EntityId): EcsGameState =
        updateEntity(id) { it.without<T>() }

    /**
     * Create a new entity with the given components.
     * Returns the new EntityId and the updated state.
     */
    fun createEntity(
        id: EntityId = EntityId.generate(),
        vararg components: Component
    ): Pair<EntityId, EcsGameState> {
        val container = ComponentContainer.of(*components)
        return id to copy(entities = entities + (id to container))
    }

    /**
     * Create a new entity with components from a list.
     */
    fun createEntity(
        id: EntityId = EntityId.generate(),
        components: List<Component>
    ): Pair<EntityId, EcsGameState> {
        val container = ComponentContainer.of(components)
        return id to copy(entities = entities + (id to container))
    }

    /**
     * Remove an entity completely (from entities map and all zones).
     */
    fun removeEntity(id: EntityId): EcsGameState {
        val newZones = zones.mapValues { (_, entityIds) ->
            entityIds.filter { it != id }
        }
        return copy(
            entities = entities - id,
            zones = newZones
        )
    }

    // ==========================================================================
    // Zone Operations
    // ==========================================================================

    /**
     * Add an entity to the top (end) of a zone.
     */
    fun addToZone(entityId: EntityId, zoneId: ZoneId): EcsGameState {
        val currentContents = zones[zoneId] ?: emptyList()
        return copy(zones = zones + (zoneId to currentContents + entityId))
    }

    /**
     * Add an entity to the bottom (start) of a zone.
     */
    fun addToZoneBottom(entityId: EntityId, zoneId: ZoneId): EcsGameState {
        val currentContents = zones[zoneId] ?: emptyList()
        return copy(zones = zones + (zoneId to listOf(entityId) + currentContents))
    }

    /**
     * Add an entity at a specific index in a zone.
     */
    fun addToZoneAt(entityId: EntityId, zoneId: ZoneId, index: Int): EcsGameState {
        val currentContents = zones[zoneId] ?: emptyList()
        val newContents = currentContents.toMutableList().apply { add(index.coerceIn(0, size), entityId) }
        return copy(zones = zones + (zoneId to newContents))
    }

    /**
     * Remove an entity from a specific zone.
     */
    fun removeFromZone(entityId: EntityId, zoneId: ZoneId): EcsGameState {
        val currentContents = zones[zoneId] ?: return this
        return copy(zones = zones + (zoneId to currentContents.filter { it != entityId }))
    }

    /**
     * Move an entity from one zone to another.
     */
    fun moveEntity(entityId: EntityId, fromZone: ZoneId, toZone: ZoneId): EcsGameState =
        removeFromZone(entityId, fromZone).addToZone(entityId, toZone)

    /**
     * Remove an entity from its current zone (wherever it is).
     */
    fun removeFromCurrentZone(entityId: EntityId): EcsGameState {
        val currentZone = findZone(entityId) ?: return this
        return removeFromZone(entityId, currentZone)
    }

    /**
     * Shuffle a zone's contents.
     */
    fun shuffleZone(zoneId: ZoneId): EcsGameState {
        val currentContents = zones[zoneId] ?: return this
        return copy(zones = zones + (zoneId to currentContents.shuffled()))
    }

    /**
     * Shuffle a zone's contents with a specific random source.
     */
    fun shuffleZone(zoneId: ZoneId, random: kotlin.random.Random): EcsGameState {
        val currentContents = zones[zoneId] ?: return this
        return copy(zones = zones + (zoneId to currentContents.shuffled(random)))
    }

    /**
     * Add multiple entities to a zone.
     */
    fun addEntitiesToZone(entityIds: List<EntityId>, zoneId: ZoneId): EcsGameState {
        val currentContents = zones[zoneId] ?: emptyList()
        return copy(zones = zones + (zoneId to currentContents + entityIds))
    }

    /**
     * Get the size of a zone.
     */
    fun getZoneSize(zoneId: ZoneId): Int = getZone(zoneId).size

    /**
     * Check if a zone is empty.
     */
    fun isZoneEmpty(zoneId: ZoneId): Boolean = getZone(zoneId).isEmpty()

    /**
     * Check if a zone is not empty.
     */
    fun isZoneNotEmpty(zoneId: ZoneId): Boolean = getZone(zoneId).isNotEmpty()

    /**
     * Get the top entity of a zone (last in list, like top of library).
     * Returns null if zone is empty.
     */
    fun getTopOfZone(zoneId: ZoneId): EntityId? = getZone(zoneId).lastOrNull()

    /**
     * Get the bottom entity of a zone (first in list).
     * Returns null if zone is empty.
     */
    fun getBottomOfZone(zoneId: ZoneId): EntityId? = getZone(zoneId).firstOrNull()

    /**
     * Remove and return the top entity from a zone.
     * Returns null and unchanged state if zone is empty.
     */
    fun removeTopFromZone(zoneId: ZoneId): Pair<EntityId?, EcsGameState> {
        val contents = getZone(zoneId)
        return if (contents.isNotEmpty()) {
            contents.last() to copy(zones = zones + (zoneId to contents.dropLast(1)))
        } else {
            null to this
        }
    }

    /**
     * Remove and return the bottom entity from a zone.
     * Returns null and unchanged state if zone is empty.
     */
    fun removeBottomFromZone(zoneId: ZoneId): Pair<EntityId?, EcsGameState> {
        val contents = getZone(zoneId)
        return if (contents.isNotEmpty()) {
            contents.first() to copy(zones = zones + (zoneId to contents.drop(1)))
        } else {
            null to this
        }
    }

    /**
     * Remove the top N entities from a zone.
     * Returns the removed entities and updated state.
     */
    fun removeTopNFromZone(zoneId: ZoneId, count: Int): Pair<List<EntityId>, EcsGameState> {
        val contents = getZone(zoneId)
        val toRemove = contents.takeLast(count)
        val remaining = contents.dropLast(count)
        return toRemove to copy(zones = zones + (zoneId to remaining))
    }

    /**
     * Filter entities in a zone by a predicate on their components.
     */
    fun filterZone(zoneId: ZoneId, predicate: (EntityId, ComponentContainer) -> Boolean): List<EntityId> =
        getZone(zoneId).filter { entityId ->
            entities[entityId]?.let { predicate(entityId, it) } ?: false
        }

    /**
     * Get all entities in a zone that have a specific component.
     */
    inline fun <reified T : Component> filterZoneByComponent(zoneId: ZoneId): List<EntityId> =
        getZone(zoneId).filter { hasComponent<T>(it) }

    // ==========================================================================
    // Global Flags
    // ==========================================================================

    /**
     * Set a global flag (string value).
     */
    fun setFlag(key: String, value: String): EcsGameState =
        copy(globalFlags = globalFlags + (key to value))

    /**
     * Get a global flag value.
     */
    fun getFlag(key: String): String? = globalFlags[key]

    /**
     * Remove a global flag.
     */
    fun removeFlag(key: String): EcsGameState =
        copy(globalFlags = globalFlags - key)

    /**
     * Check if a flag exists.
     */
    fun hasFlag(key: String): Boolean = key in globalFlags

    // ==========================================================================
    // Turn/Phase Helpers
    // ==========================================================================

    val activePlayerId: EntityId
        get() = turnState.activePlayer

    val priorityPlayerId: EntityId
        get() = turnState.priorityPlayer

    val currentPhase: Phase get() = turnState.phase

    val currentStep: Step get() = turnState.step

    val turnNumber: Int get() = turnState.turnNumber

    val isMainPhase: Boolean get() = turnState.isMainPhase

    // ==========================================================================
    // Combat Helpers
    // ==========================================================================

    val isInCombat: Boolean
        get() = combat != null && currentPhase == Phase.COMBAT

    fun startCombat(defendingPlayerId: EntityId): EcsGameState {
        return copy(
            combat = CombatState.create(
                turnState.activePlayer,
                defendingPlayerId
            )
        )
    }

    fun endCombat(): EcsGameState = copy(combat = null)

    // ==========================================================================
    // Game End Helpers
    // ==========================================================================

    fun endGame(winnerId: EntityId?): EcsGameState =
        copy(isGameOver = true, winner = winnerId)

    // ==========================================================================
    // Trigger Helpers
    // ==========================================================================

    fun addPendingTriggers(triggers: List<PendingTrigger>): EcsGameState =
        copy(pendingTriggers = pendingTriggers + triggers)

    fun clearPendingTriggers(): EcsGameState =
        copy(pendingTriggers = emptyList())

    val hasPendingTriggers: Boolean get() = pendingTriggers.isNotEmpty()

    val hasTriggersOnStack: Boolean get() = triggersOnStack.isNotEmpty()

    // ==========================================================================
    // Pending Legend Rule Choices
    // ==========================================================================

    /**
     * Check if there are any pending Legend Rule choices.
     */
    val hasPendingLegendRuleChoices: Boolean get() = pendingLegendRuleChoices.isNotEmpty()

    /**
     * Get pending Legend Rule choices for a specific player.
     */
    fun getPendingLegendRuleChoicesForPlayer(playerId: EntityId): List<com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice> =
        pendingLegendRuleChoices.filter { it.controllerId == playerId }

    /**
     * Add a pending Legend Rule choice.
     */
    fun addPendingLegendRuleChoice(choice: com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice): EcsGameState =
        copy(pendingLegendRuleChoices = pendingLegendRuleChoices + choice)

    /**
     * Remove a pending Legend Rule choice (after it's been resolved).
     */
    fun removePendingLegendRuleChoice(choice: com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice): EcsGameState =
        copy(pendingLegendRuleChoices = pendingLegendRuleChoices - choice)

    /**
     * Clear all pending Legend Rule choices for a player.
     */
    fun clearPendingLegendRuleChoicesForPlayer(playerId: EntityId): EcsGameState =
        copy(pendingLegendRuleChoices = pendingLegendRuleChoices.filter { it.controllerId != playerId })

    // ==========================================================================
    // Convenience Helpers
    // ==========================================================================

    /**
     * Get all creature entities on the battlefield.
     */
    fun getCreaturesOnBattlefield(): List<EntityId> =
        getBattlefield().filter { id ->
            getComponent<CardComponent>(id)?.isCreature == true
        }

    /**
     * Get all permanents controlled by a player.
     */
    fun getPermanentsControlledBy(playerId: EntityId): List<EntityId> =
        getBattlefield().filter { id ->
            getComponent<ControllerComponent>(id)?.controllerId == playerId
        }

    /**
     * Get all creatures controlled by a player.
     */
    fun getCreaturesControlledBy(playerId: EntityId): List<EntityId> =
        getCreaturesOnBattlefield().filter { id ->
            getComponent<ControllerComponent>(id)?.controllerId == playerId
        }

    /**
     * Get a player's library contents.
     */
    fun getLibrary(playerId: EntityId): List<EntityId> =
        getZone(ZoneId.library(playerId))

    /**
     * Get a player's hand contents.
     */
    fun getHand(playerId: EntityId): List<EntityId> =
        getZone(ZoneId.hand(playerId))

    /**
     * Get a player's graveyard contents.
     */
    fun getGraveyard(playerId: EntityId): List<EntityId> =
        getZone(ZoneId.graveyard(playerId))

    // ==========================================================================
    // Library Operations
    // ==========================================================================

    /**
     * Get the size of a player's library.
     */
    fun getLibrarySize(playerId: EntityId): Int =
        getZoneSize(ZoneId.library(playerId))

    /**
     * Check if a player's library is empty.
     */
    fun isLibraryEmpty(playerId: EntityId): Boolean =
        isZoneEmpty(ZoneId.library(playerId))

    /**
     * Get the top card of a player's library.
     * Returns null if library is empty.
     */
    fun getTopOfLibrary(playerId: EntityId): EntityId? =
        getTopOfZone(ZoneId.library(playerId))

    /**
     * Add a card to the top of a player's library.
     */
    fun addToTopOfLibrary(playerId: EntityId, entityId: EntityId): EcsGameState =
        addToZone(entityId, ZoneId.library(playerId))

    /**
     * Add a card to the bottom of a player's library.
     */
    fun addToBottomOfLibrary(playerId: EntityId, entityId: EntityId): EcsGameState =
        addToZoneBottom(entityId, ZoneId.library(playerId))

    /**
     * Draw from a player's library (remove top card).
     * Returns the drawn entity and updated state.
     */
    fun drawFromLibrary(playerId: EntityId): Pair<EntityId?, EcsGameState> =
        removeTopFromZone(ZoneId.library(playerId))

    /**
     * Draw multiple cards from a player's library.
     * Returns the drawn entities and updated state.
     */
    fun drawCardsFromLibrary(playerId: EntityId, count: Int): Pair<List<EntityId>, EcsGameState> =
        removeTopNFromZone(ZoneId.library(playerId), count)

    /**
     * Shuffle a player's library.
     */
    fun shuffleLibrary(playerId: EntityId): EcsGameState =
        shuffleZone(ZoneId.library(playerId))

    /**
     * Shuffle a player's library with a specific random source.
     */
    fun shuffleLibrary(playerId: EntityId, random: kotlin.random.Random): EcsGameState =
        shuffleZone(ZoneId.library(playerId), random)

    // ==========================================================================
    // Hand Operations
    // ==========================================================================

    /**
     * Get the size of a player's hand.
     */
    fun getHandSize(playerId: EntityId): Int =
        getZoneSize(ZoneId.hand(playerId))

    /**
     * Check if a player's hand is empty.
     */
    fun isHandEmpty(playerId: EntityId): Boolean =
        isZoneEmpty(ZoneId.hand(playerId))

    /**
     * Add a card to a player's hand.
     */
    fun addToHand(playerId: EntityId, entityId: EntityId): EcsGameState =
        addToZone(entityId, ZoneId.hand(playerId))

    /**
     * Remove a card from a player's hand.
     */
    fun removeFromHand(playerId: EntityId, entityId: EntityId): EcsGameState =
        removeFromZone(entityId, ZoneId.hand(playerId))

    /**
     * Draw a card (remove from library and add to hand).
     * Returns the drawn entity and updated state, or null if library is empty.
     */
    fun drawCard(playerId: EntityId): Pair<EntityId?, EcsGameState> {
        val (drawn, newState) = drawFromLibrary(playerId)
        return if (drawn != null) {
            drawn to newState.addToHand(playerId, drawn)
        } else {
            null to this
        }
    }

    /**
     * Draw multiple cards.
     * Returns the drawn entities and updated state.
     */
    fun drawCards(playerId: EntityId, count: Int): Pair<List<EntityId>, EcsGameState> {
        var state = this
        val drawn = mutableListOf<EntityId>()
        repeat(count) {
            val (card, newState) = state.drawCard(playerId)
            if (card != null) {
                drawn.add(card)
                state = newState
            }
        }
        return drawn to state
    }

    // ==========================================================================
    // Graveyard Operations
    // ==========================================================================

    /**
     * Get the size of a player's graveyard.
     */
    fun getGraveyardSize(playerId: EntityId): Int =
        getZoneSize(ZoneId.graveyard(playerId))

    /**
     * Check if a player's graveyard is empty.
     */
    fun isGraveyardEmpty(playerId: EntityId): Boolean =
        isZoneEmpty(ZoneId.graveyard(playerId))

    /**
     * Get the top card of a player's graveyard (most recently added).
     * Returns null if graveyard is empty.
     */
    fun getTopOfGraveyard(playerId: EntityId): EntityId? =
        getTopOfZone(ZoneId.graveyard(playerId))

    /**
     * Add a card to a player's graveyard.
     */
    fun addToGraveyard(playerId: EntityId, entityId: EntityId): EcsGameState =
        addToZone(entityId, ZoneId.graveyard(playerId))

    /**
     * Remove a card from a player's graveyard.
     */
    fun removeFromGraveyard(playerId: EntityId, entityId: EntityId): EcsGameState =
        removeFromZone(entityId, ZoneId.graveyard(playerId))

    // ==========================================================================
    // Battlefield Operations
    // ==========================================================================

    /**
     * Get all entities on the battlefield as a list.
     */
    fun getBattlefieldSize(): Int = getZoneSize(ZoneId.BATTLEFIELD)

    /**
     * Check if the battlefield is empty.
     */
    fun isBattlefieldEmpty(): Boolean = isZoneEmpty(ZoneId.BATTLEFIELD)

    /**
     * Add an entity to the battlefield.
     */
    fun addToBattlefield(entityId: EntityId): EcsGameState =
        addToZone(entityId, ZoneId.BATTLEFIELD)

    /**
     * Remove an entity from the battlefield.
     */
    fun removeFromBattlefield(entityId: EntityId): EcsGameState =
        removeFromZone(entityId, ZoneId.BATTLEFIELD)

    /**
     * Check if an entity is on the battlefield.
     */
    fun isOnBattlefield(entityId: EntityId): Boolean =
        isInZone(entityId, ZoneId.BATTLEFIELD)

    // ==========================================================================
    // Stack Operations
    // ==========================================================================

    /**
     * Get the top of the stack (most recently added spell/ability).
     * Returns null if stack is empty.
     */
    fun getTopOfStack(): EntityId? = getTopOfZone(ZoneId.STACK)

    /**
     * Add an entity to the stack (cast a spell).
     */
    fun addToStack(entityId: EntityId): EcsGameState =
        addToZone(entityId, ZoneId.STACK)

    /**
     * Remove and return the top spell/ability from the stack (resolve).
     */
    fun popFromStack(): Pair<EntityId?, EcsGameState> =
        removeTopFromZone(ZoneId.STACK)

    // ==========================================================================
    // Exile Operations
    // ==========================================================================

    /**
     * Add an entity to exile.
     */
    fun addToExile(entityId: EntityId): EcsGameState =
        addToZone(entityId, ZoneId.EXILE)

    /**
     * Remove an entity from exile.
     */
    fun removeFromExile(entityId: EntityId): EcsGameState =
        removeFromZone(entityId, ZoneId.EXILE)

    /**
     * Check if an entity is in exile.
     */
    fun isInExile(entityId: EntityId): Boolean =
        isInZone(entityId, ZoneId.EXILE)

    // ==========================================================================
    // Player State Accessors
    // ==========================================================================

    /**
     * Get a player's current life total.
     * Returns 0 if player doesn't exist or has no life component.
     */
    fun getLife(playerId: EntityId): Int =
        getComponent<LifeComponent>(playerId)?.life ?: 0

    /**
     * Get a player's mana pool.
     * Returns empty pool if player doesn't exist or has no mana component.
     */
    fun getManaPool(playerId: EntityId): ManaPool =
        getComponent<ManaPoolComponent>(playerId)?.pool ?: ManaPool.EMPTY

    /**
     * Get a player's poison counter count.
     * Returns 0 if player doesn't exist or has no poison component.
     */
    fun getPoisonCounters(playerId: EntityId): Int =
        getComponent<PoisonComponent>(playerId)?.counters ?: 0

    /**
     * Get the number of lands a player has played this turn.
     * Returns 0 if player doesn't exist or has no lands-played component.
     */
    fun getLandsPlayed(playerId: EntityId): Int =
        getComponent<LandsPlayedComponent>(playerId)?.count ?: 0

    /**
     * Check if a player can play a land this turn.
     * Returns false if player doesn't exist.
     */
    fun canPlayLand(playerId: EntityId): Boolean =
        getComponent<LandsPlayedComponent>(playerId)?.canPlayLand ?: false

    /**
     * Check if a player has lost the game.
     */
    fun hasLost(playerId: EntityId): Boolean =
        hasComponent<LostGameComponent>(playerId)

    /**
     * Check if a player has won the game.
     */
    fun hasWon(playerId: EntityId): Boolean =
        hasComponent<WonGameComponent>(playerId)

    /**
     * Check if a player is still alive (hasn't lost).
     */
    fun isAlive(playerId: EntityId): Boolean =
        hasEntity(playerId) && !hasLost(playerId)

    /**
     * Get a player's name.
     * Returns empty string if player doesn't exist.
     */
    fun getPlayerName(playerId: EntityId): String =
        getComponent<PlayerComponent>(playerId)?.name ?: ""

    // ==========================================================================
    // Player State Mutators
    // ==========================================================================

    /**
     * Gain life for a player.
     * Returns unchanged state if player doesn't exist.
     */
    fun gainLife(playerId: EntityId, amount: Int): EcsGameState {
        val life = getComponent<LifeComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(life.gainLife(amount)) }
    }

    /**
     * Lose life for a player.
     * Returns unchanged state if player doesn't exist.
     */
    fun loseLife(playerId: EntityId, amount: Int): EcsGameState {
        val life = getComponent<LifeComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(life.loseLife(amount)) }
    }

    /**
     * Set a player's life to a specific value.
     * Returns unchanged state if player doesn't exist.
     */
    fun setLife(playerId: EntityId, amount: Int): EcsGameState {
        val life = getComponent<LifeComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(life.setLife(amount)) }
    }

    /**
     * Add mana to a player's mana pool.
     * Returns unchanged state if player doesn't exist.
     */
    fun addMana(playerId: EntityId, color: Color, amount: Int = 1): EcsGameState {
        val manaPool = getComponent<ManaPoolComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(manaPool.add(color, amount)) }
    }

    /**
     * Add colorless mana to a player's mana pool.
     * Returns unchanged state if player doesn't exist.
     */
    fun addColorlessMana(playerId: EntityId, amount: Int = 1): EcsGameState {
        val manaPool = getComponent<ManaPoolComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(manaPool.addColorless(amount)) }
    }

    /**
     * Empty a player's mana pool.
     * Returns unchanged state if player doesn't exist.
     */
    fun emptyManaPool(playerId: EntityId): EcsGameState {
        val manaPool = getComponent<ManaPoolComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(manaPool.empty()) }
    }

    /**
     * Add poison counters to a player.
     * Returns unchanged state if player doesn't exist.
     */
    fun addPoisonCounters(playerId: EntityId, amount: Int): EcsGameState {
        val poison = getComponent<PoisonComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(poison.add(amount)) }
    }

    /**
     * Record that a player played a land this turn.
     * Returns unchanged state if player doesn't exist.
     */
    fun recordLandPlayed(playerId: EntityId): EcsGameState {
        val lands = getComponent<LandsPlayedComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(lands.playLand()) }
    }

    /**
     * Reset lands played count (called at start of turn).
     * Returns unchanged state if player doesn't exist.
     */
    fun resetLandsPlayed(playerId: EntityId): EcsGameState {
        val lands = getComponent<LandsPlayedComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(lands.reset()) }
    }

    /**
     * Mark a player as having lost the game.
     */
    fun markPlayerLost(playerId: EntityId, reason: String): EcsGameState =
        addComponent(playerId, LostGameComponent(reason))

    /**
     * Mark a player as having won the game.
     */
    fun markPlayerWon(playerId: EntityId): EcsGameState =
        addComponent(playerId, WonGameComponent)

    companion object {
        /**
         * Create initial game state for a new game.
         *
         * @param players List of (EntityId, playerName) pairs
         */
        fun newGame(players: List<Pair<EntityId, String>>): EcsGameState {
            require(players.size >= 2) { "Game requires at least 2 players" }

            // Create player entities
            val entities = mutableMapOf<EntityId, ComponentContainer>()
            val zones = mutableMapOf<ZoneId, List<EntityId>>()

            for ((playerId, playerName) in players) {
                // Create player entity with components
                entities[playerId] = ComponentContainer.of(
                    PlayerComponent(playerName),
                    LifeComponent.starting(),
                    ManaPoolComponent.EMPTY,
                    PoisonComponent.NONE,
                    LandsPlayedComponent.newTurn()
                )

                // Create player-specific zones (initially empty)
                zones[ZoneId.library(playerId)] = emptyList()
                zones[ZoneId.hand(playerId)] = emptyList()
                zones[ZoneId.graveyard(playerId)] = emptyList()
            }

            // Create shared zones
            zones[ZoneId.BATTLEFIELD] = emptyList()
            zones[ZoneId.STACK] = emptyList()
            zones[ZoneId.EXILE] = emptyList()

            // Create turn state with EntityId player order
            val playerOrder = players.map { it.first }

            return EcsGameState(
                entities = entities,
                zones = zones,
                turnState = TurnState.newGame(playerOrder)
            )
        }

        /**
         * Create initial game state with simple string player names.
         */
        fun newGame(player1Name: String, player2Name: String): EcsGameState {
            val player1 = EntityId.generate() to player1Name
            val player2 = EntityId.generate() to player2Name
            return newGame(listOf(player1, player2))
        }
    }
}
