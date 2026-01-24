package com.wingedsheep.engine.state

import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of the entire game state.
 *
 * The GameState is the single source of truth for the game.
 * All game operations are pure functions: (GameState, Action) -> (GameState, Events)
 */
@Serializable
data class GameState(
    /** All entities in the game, keyed by their ID */
    val entities: Map<EntityId, ComponentContainer> = emptyMap(),

    /** Zone contents - maps zone keys to lists of entity IDs */
    val zones: Map<ZoneKey, List<EntityId>> = emptyMap(),

    /** Current turn number (starts at 1) */
    val turnNumber: Int = 1,

    /** ID of the player whose turn it is */
    val activePlayerId: EntityId? = null,

    /** Current phase */
    val phase: Phase = Phase.BEGINNING,

    /** Current step */
    val step: Step = Step.UNTAP,

    /** ID of the player who currently has priority */
    val priorityPlayerId: EntityId? = null,

    /** The stack (spells and abilities waiting to resolve) */
    val stack: List<EntityId> = emptyList(),

    /** Players who have passed priority in sequence */
    val priorityPassedBy: Set<EntityId> = emptySet(),

    /** Timestamp counter for ordering effects */
    val timestamp: Long = 0,

    /** Player IDs in turn order */
    val turnOrder: List<EntityId> = emptyList(),

    /** ID of the player who won (null if game ongoing) */
    val winnerId: EntityId? = null,

    /** Whether the game has ended */
    val gameOver: Boolean = false,

    /** Current pending decision awaiting player input (null if engine is not paused) */
    val pendingDecision: com.wingedsheep.engine.core.PendingDecision? = null,

    /** Active floating effects (temporary effects from spells like Giant Growth) */
    val floatingEffects: List<ActiveFloatingEffect> = emptyList()
) {
    // =========================================================================
    // Entity Operations
    // =========================================================================

    /**
     * Get an entity's components, or null if entity doesn't exist.
     */
    fun getEntity(id: EntityId): ComponentContainer? = entities[id]

    /**
     * Get an entity's components, throwing if entity doesn't exist.
     */
    fun requireEntity(id: EntityId): ComponentContainer =
        entities[id] ?: throw IllegalArgumentException("Entity not found: $id")

    /**
     * Check if an entity exists.
     */
    fun hasEntity(id: EntityId): Boolean = entities.containsKey(id)

    /**
     * Add or update an entity (returns new state).
     */
    fun withEntity(id: EntityId, container: ComponentContainer): GameState =
        copy(entities = entities + (id to container))

    /**
     * Remove an entity (returns new state).
     */
    fun withoutEntity(id: EntityId): GameState =
        copy(entities = entities - id)

    /**
     * Update an entity's components (returns new state).
     */
    fun updateEntity(id: EntityId, update: (ComponentContainer) -> ComponentContainer): GameState {
        val existing = entities[id] ?: ComponentContainer.EMPTY
        return withEntity(id, update(existing))
    }

    // =========================================================================
    // Zone Operations
    // =========================================================================

    /**
     * Get all entities in a zone.
     */
    fun getZone(key: ZoneKey): List<EntityId> = zones[key] ?: emptyList()

    /**
     * Get all entities in a player's zone.
     */
    fun getZone(playerId: EntityId, zoneType: ZoneType): List<EntityId> =
        getZone(ZoneKey(playerId, zoneType))

    /**
     * Add an entity to a zone (returns new state).
     */
    fun addToZone(key: ZoneKey, entityId: EntityId): GameState {
        val current = zones[key] ?: emptyList()
        return copy(zones = zones + (key to current + entityId))
    }

    /**
     * Remove an entity from a zone (returns new state).
     */
    fun removeFromZone(key: ZoneKey, entityId: EntityId): GameState {
        val current = zones[key] ?: return this
        return copy(zones = zones + (key to current - entityId))
    }

    /**
     * Move an entity between zones (returns new state).
     */
    fun moveToZone(entityId: EntityId, from: ZoneKey, to: ZoneKey): GameState {
        return removeFromZone(from, entityId).addToZone(to, entityId)
    }

    // =========================================================================
    // Query Helpers
    // =========================================================================

    /**
     * Find all entities with a specific component type.
     */
    inline fun <reified T : Component> findEntitiesWith(): List<Pair<EntityId, T>> {
        return entities.mapNotNull { (id, container) ->
            container.get<T>()?.let { id to it }
        }
    }

    /**
     * Find all entities matching a predicate.
     */
    fun findEntities(predicate: (EntityId, ComponentContainer) -> Boolean): List<EntityId> {
        return entities.filter { (id, container) -> predicate(id, container) }.keys.toList()
    }

    /**
     * Get opponent of a player (for 2-player games).
     */
    fun getOpponent(playerId: EntityId): EntityId? {
        return turnOrder.find { it != playerId }
    }

    // =========================================================================
    // Stack Operations
    // =========================================================================

    /**
     * Push an entity onto the stack (returns new state).
     */
    fun pushToStack(entityId: EntityId): GameState =
        copy(stack = stack + entityId)

    /**
     * Pop the top entity from the stack (returns entity ID and new state).
     */
    fun popFromStack(): Pair<EntityId?, GameState> {
        if (stack.isEmpty()) return null to this
        val top = stack.last()
        return top to copy(stack = stack.dropLast(1))
    }

    /**
     * Get the top entity on the stack, or null if empty.
     */
    fun getTopOfStack(): EntityId? = stack.lastOrNull()

    /**
     * Remove a specific entity from the stack (for countering).
     */
    fun removeFromStack(entityId: EntityId): GameState =
        copy(stack = stack - entityId)

    // =========================================================================
    // Convenience Zone Accessors
    // =========================================================================

    /**
     * Get all entities on the battlefield.
     */
    fun getBattlefield(): List<EntityId> {
        return zones.filterKeys { it.zoneType == ZoneType.BATTLEFIELD }
            .values.flatten()
    }

    /**
     * Get entities on a player's battlefield.
     */
    fun getBattlefield(playerId: EntityId): List<EntityId> =
        getZone(playerId, ZoneType.BATTLEFIELD)

    /**
     * Get a player's hand.
     */
    fun getHand(playerId: EntityId): List<EntityId> =
        getZone(playerId, ZoneType.HAND)

    /**
     * Get a player's library.
     */
    fun getLibrary(playerId: EntityId): List<EntityId> =
        getZone(playerId, ZoneType.LIBRARY)

    /**
     * Get a player's graveyard.
     */
    fun getGraveyard(playerId: EntityId): List<EntityId> =
        getZone(playerId, ZoneType.GRAVEYARD)

    /**
     * Remove an entity completely from the game (from all zones).
     */
    fun removeEntity(entityId: EntityId): GameState {
        var newState = withoutEntity(entityId)
        // Also remove from all zones
        zones.forEach { (key, ids) ->
            if (entityId in ids) {
                newState = newState.removeFromZone(key, entityId)
            }
        }
        // Also remove from stack if present
        if (entityId in stack) {
            newState = newState.removeFromStack(entityId)
        }
        return newState
    }

    // =========================================================================
    // State Transitions
    // =========================================================================

    /**
     * Advance to the next timestamp (returns new state).
     */
    fun tick(): GameState = copy(timestamp = timestamp + 1)

    /**
     * Set the priority player (returns new state).
     */
    fun withPriority(playerId: EntityId?): GameState =
        copy(priorityPlayerId = playerId, priorityPassedBy = emptySet())

    /**
     * Record that a player passed priority (returns new state).
     */
    fun withPriorityPassed(playerId: EntityId): GameState =
        copy(priorityPassedBy = priorityPassedBy + playerId)

    /**
     * Check if all players have passed priority.
     */
    fun allPlayersPassed(): Boolean = priorityPassedBy.containsAll(turnOrder)

    /**
     * Check if the engine is paused awaiting a decision.
     */
    fun isPaused(): Boolean = pendingDecision != null

    /**
     * Set a pending decision (pauses the engine).
     */
    fun withPendingDecision(decision: com.wingedsheep.engine.core.PendingDecision): GameState =
        copy(pendingDecision = decision)

    /**
     * Clear the pending decision (resumes the engine).
     */
    fun clearPendingDecision(): GameState =
        copy(pendingDecision = null)

    /**
     * Get the next player in turn order after the given player.
     */
    fun getNextPlayer(afterPlayer: EntityId): EntityId {
        val index = turnOrder.indexOf(afterPlayer)
        return turnOrder[(index + 1) % turnOrder.size]
    }

    companion object {
        /**
         * Create an initial game state for a new game.
         */
        fun initial(playerIds: List<EntityId>): GameState {
            require(playerIds.size >= 2) { "Need at least 2 players" }
            return GameState(
                turnOrder = playerIds,
                activePlayerId = playerIds.first(),
                priorityPlayerId = playerIds.first()
            )
        }
    }
}

/**
 * Key for identifying a specific zone (player + zone type).
 */
@Serializable
data class ZoneKey(
    val ownerId: EntityId,
    val zoneType: ZoneType
) {
    override fun toString(): String = "${ownerId.value}:${zoneType.name}"
}
