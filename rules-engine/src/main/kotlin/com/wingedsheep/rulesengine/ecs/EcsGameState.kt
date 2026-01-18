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
    val triggersOnStack: List<StackedTrigger> = emptyList()
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
        get() = EntityId.fromPlayerId(turnState.activePlayer)

    val priorityPlayerId: EntityId
        get() = EntityId.fromPlayerId(turnState.priorityPlayer)

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
                defendingPlayerId.toPlayerId()
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

            // Create turn state using PlayerId (for compatibility with existing system)
            val playerOrder = players.map { PlayerId.of(it.first.value) }

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
