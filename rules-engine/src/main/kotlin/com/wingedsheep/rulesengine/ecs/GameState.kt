package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.ability.PendingTrigger
import com.wingedsheep.rulesengine.ability.StackedTrigger
import com.wingedsheep.rulesengine.combat.CombatState
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.decision.DecisionContext
import com.wingedsheep.rulesengine.decision.PlayerDecision
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.ActiveContinuousEffect
import com.wingedsheep.rulesengine.ecs.layers.EffectDuration
import com.wingedsheep.rulesengine.player.ManaPool
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.game.TurnState
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
 * Use [GameEngine] for game orchestration (setup, action execution, etc.)
 */
@Serializable
data class GameState(
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
    val pendingLegendRuleChoices: List<com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice> = emptyList(),

    /**
     * Active continuous effects that modify game objects.
     *
     * These are "floating" effects created by spells and abilities that persist
     * independently of their source. Examples:
     * - "Target creature gets +3/+3 until end of turn" (Giant Growth)
     * - "Creatures you control gain flying until end of turn"
     *
     * Unlike static abilities (which come from permanents on the battlefield),
     * continuous effects persist based on their duration, even if the source
     * that created them leaves play.
     *
     * Effects are applied in layer order (Rule 613), then by timestamp.
     * They expire based on their duration (end of turn, end of combat, etc.)
     */
    val continuousEffects: List<ActiveContinuousEffect> = emptyList(),

    /**
     * Pending cleanup discards that need player input.
     * During the cleanup step, players with more cards than their max hand size
     * must discard down to the limit.
     */
    val pendingCleanupDiscards: List<com.wingedsheep.rulesengine.ecs.components.PendingCleanupDiscard> = emptyList(),

    /**
     * Pending decision that needs player input before the game can proceed.
     *
     * When an effect (like library search) needs player input, the decision
     * is stored here. The game loop presents this to the player, collects
     * their response, and submits it via [SubmitDecision] action.
     *
     * Null if no pending decision (game can proceed normally).
     */
    val pendingDecision: PlayerDecision? = null,

    /**
     * Context for resuming the pending decision's effect.
     *
     * Contains all serializable state needed to complete the effect once
     * the player responds. This replaces lambda-based continuations with
     * data that can be persisted and resumed after server restart.
     *
     * Must be non-null whenever [pendingDecision] is non-null.
     *
     * @see DecisionContext
     */
    val decisionContext: DecisionContext? = null
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
    ): GameState {
        val container = entities[id] ?: return this
        return copy(entities = entities + (id to transform(container)))
    }

    /**
     * Add or update a component on an entity.
     */
    fun addComponent(id: EntityId, component: Component): GameState =
        updateEntity(id) { it.with(component) }

    /**
     * Remove a component from an entity.
     */
    inline fun <reified T : Component> removeComponent(id: EntityId): GameState =
        updateEntity(id) { it.without<T>() }

    /**
     * Create a new entity with the given components.
     * Returns the new EntityId and the updated state.
     */
    fun createEntity(
        id: EntityId = EntityId.generate(),
        vararg components: Component
    ): Pair<EntityId, GameState> {
        val container = ComponentContainer.of(*components)
        return id to copy(entities = entities + (id to container))
    }

    /**
     * Create a new entity with components from a list.
     */
    fun createEntity(
        id: EntityId = EntityId.generate(),
        components: List<Component>
    ): Pair<EntityId, GameState> {
        val container = ComponentContainer.of(components)
        return id to copy(entities = entities + (id to container))
    }

    /**
     * Remove an entity completely (from entities map and all zones).
     */
    fun removeEntity(id: EntityId): GameState {
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
    fun addToZone(entityId: EntityId, zoneId: ZoneId): GameState {
        val currentContents = zones[zoneId] ?: emptyList()
        return copy(zones = zones + (zoneId to currentContents + entityId))
    }

    /**
     * Add an entity to the bottom (start) of a zone.
     */
    fun addToZoneBottom(entityId: EntityId, zoneId: ZoneId): GameState {
        val currentContents = zones[zoneId] ?: emptyList()
        return copy(zones = zones + (zoneId to listOf(entityId) + currentContents))
    }

    /**
     * Add an entity at a specific index in a zone.
     */
    fun addToZoneAt(entityId: EntityId, zoneId: ZoneId, index: Int): GameState {
        val currentContents = zones[zoneId] ?: emptyList()
        val newContents = currentContents.toMutableList().apply { add(index.coerceIn(0, size), entityId) }
        return copy(zones = zones + (zoneId to newContents))
    }

    /**
     * Remove an entity from a specific zone.
     */
    fun removeFromZone(entityId: EntityId, zoneId: ZoneId): GameState {
        val currentContents = zones[zoneId] ?: return this
        return copy(zones = zones + (zoneId to currentContents.filter { it != entityId }))
    }

    /**
     * Move an entity from one zone to another.
     */
    fun moveEntity(entityId: EntityId, fromZone: ZoneId, toZone: ZoneId): GameState =
        removeFromZone(entityId, fromZone).addToZone(entityId, toZone)

    /**
     * Remove an entity from its current zone (wherever it is).
     */
    fun removeFromCurrentZone(entityId: EntityId): GameState {
        val currentZone = findZone(entityId) ?: return this
        return removeFromZone(entityId, currentZone)
    }

    /**
     * Shuffle a zone's contents.
     */
    fun shuffleZone(zoneId: ZoneId): GameState {
        val currentContents = zones[zoneId] ?: return this
        return copy(zones = zones + (zoneId to currentContents.shuffled()))
    }

    /**
     * Shuffle a zone's contents with a specific random source.
     */
    fun shuffleZone(zoneId: ZoneId, random: kotlin.random.Random): GameState {
        val currentContents = zones[zoneId] ?: return this
        return copy(zones = zones + (zoneId to currentContents.shuffled(random)))
    }

    /**
     * Add multiple entities to a zone.
     */
    fun addEntitiesToZone(entityIds: List<EntityId>, zoneId: ZoneId): GameState {
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
    fun removeTopFromZone(zoneId: ZoneId): Pair<EntityId?, GameState> {
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
    fun removeBottomFromZone(zoneId: ZoneId): Pair<EntityId?, GameState> {
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
    fun removeTopNFromZone(zoneId: ZoneId, count: Int): Pair<List<EntityId>, GameState> {
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
    fun setFlag(key: String, value: String): GameState =
        copy(globalFlags = globalFlags + (key to value))

    /**
     * Get a global flag value.
     */
    fun getFlag(key: String): String? = globalFlags[key]

    /**
     * Remove a global flag.
     */
    fun removeFlag(key: String): GameState =
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

    fun startCombat(defendingPlayerId: EntityId): GameState {
        return copy(
            combat = CombatState.create(
                turnState.activePlayer,
                defendingPlayerId
            )
        )
    }

    fun endCombat(): GameState = copy(combat = null)

    // ==========================================================================
    // Game End Helpers
    // ==========================================================================

    fun endGame(winnerId: EntityId?): GameState =
        copy(isGameOver = true, winner = winnerId)

    // ==========================================================================
    // Trigger Helpers
    // ==========================================================================

    fun addPendingTriggers(triggers: List<PendingTrigger>): GameState =
        copy(pendingTriggers = pendingTriggers + triggers)

    fun clearPendingTriggers(): GameState =
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
    fun addPendingLegendRuleChoice(choice: com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice): GameState =
        copy(pendingLegendRuleChoices = pendingLegendRuleChoices + choice)

    /**
     * Remove a pending Legend Rule choice (after it's been resolved).
     */
    fun removePendingLegendRuleChoice(choice: com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice): GameState =
        copy(pendingLegendRuleChoices = pendingLegendRuleChoices - choice)

    /**
     * Clear all pending Legend Rule choices for a player.
     */
    fun clearPendingLegendRuleChoicesForPlayer(playerId: EntityId): GameState =
        copy(pendingLegendRuleChoices = pendingLegendRuleChoices.filter { it.controllerId != playerId })

    // ==========================================================================
    // Continuous Effects Helpers
    // ==========================================================================

    /**
     * Check if there are any active continuous effects.
     */
    val hasContinuousEffects: Boolean get() = continuousEffects.isNotEmpty()

    /**
     * Add a continuous effect to the game state.
     */
    fun addContinuousEffect(effect: ActiveContinuousEffect): GameState =
        copy(continuousEffects = continuousEffects + effect)

    /**
     * Add multiple continuous effects to the game state.
     */
    fun addContinuousEffects(effects: List<ActiveContinuousEffect>): GameState =
        if (effects.isEmpty()) this else copy(continuousEffects = continuousEffects + effects)

    /**
     * Remove a specific continuous effect.
     */
    fun removeContinuousEffect(effectId: EntityId): GameState =
        copy(continuousEffects = continuousEffects.filter { it.id != effectId })

    /**
     * Remove all continuous effects matching a predicate.
     */
    fun removeContinuousEffectsWhere(predicate: (ActiveContinuousEffect) -> Boolean): GameState =
        copy(continuousEffects = continuousEffects.filterNot(predicate))

    /**
     * Remove all continuous effects created by a specific source.
     */
    fun removeContinuousEffectsFromSource(sourceId: EntityId): GameState =
        copy(continuousEffects = continuousEffects.filter { it.sourceId != sourceId })

    /**
     * Remove all "until end of turn" effects.
     * Called during the cleanup step.
     */
    fun expireEndOfTurnEffects(): GameState =
        copy(continuousEffects = continuousEffects.filter {
            it.duration != EffectDuration.UntilEndOfTurn
        })

    /**
     * Remove all "until end of combat" effects.
     * Called when combat ends.
     */
    fun expireEndOfCombatEffects(): GameState =
        copy(continuousEffects = continuousEffects.filter {
            it.duration != EffectDuration.UntilEndOfCombat
        })

    /**
     * Remove effects that depend on a permanent that left the battlefield.
     * Handles WhileOnBattlefield and WhileAttached durations.
     */
    fun expireEffectsForLeavingPermanent(permanentId: EntityId): GameState =
        copy(continuousEffects = continuousEffects.filter { effect ->
            when (val duration = effect.duration) {
                is EffectDuration.WhileOnBattlefield -> duration.permanentId != permanentId
                is EffectDuration.WhileAttached -> duration.attachmentId != permanentId
                else -> true
            }
        })

    /**
     * Expire "until your next turn" effects for a specific player.
     * Called at the beginning of that player's turn.
     */
    fun expireUntilNextTurnEffects(playerId: EntityId): GameState =
        copy(continuousEffects = continuousEffects.filter { effect ->
            when (val duration = effect.duration) {
                is EffectDuration.UntilNextTurn -> duration.playerId != playerId
                else -> true
            }
        })

    /**
     * Expire "until your next upkeep" effects for a specific player.
     * Called at the beginning of that player's upkeep.
     */
    fun expireUntilNextUpkeepEffects(playerId: EntityId): GameState =
        copy(continuousEffects = continuousEffects.filter { effect ->
            when (val duration = effect.duration) {
                is EffectDuration.UntilYourNextUpkeep -> duration.playerId != playerId
                else -> true
            }
        })

    /**
     * Decrement and expire "for N turns" effects.
     * Called at the beginning of the counting player's turn.
     * Returns updated state with decremented counters and expired effects removed.
     */
    fun tickForTurnsEffects(playerId: EntityId): GameState {
        val (remaining, updated) = continuousEffects.partition { effect ->
            when (val duration = effect.duration) {
                is EffectDuration.ForTurns -> duration.countingPlayerId != playerId
                else -> true
            }
        }

        val decremented = updated.mapNotNull { effect ->
            val duration = effect.duration as EffectDuration.ForTurns
            val newRemaining = duration.remainingTurns - 1
            if (newRemaining <= 0) {
                null // Effect expires
            } else {
                effect.copy(duration = EffectDuration.ForTurns(newRemaining, duration.countingPlayerId))
            }
        }

        return copy(continuousEffects = remaining + decremented)
    }

    /**
     * Get all continuous effects affecting a specific entity.
     */
    fun getContinuousEffectsAffecting(entityId: EntityId): List<ActiveContinuousEffect> =
        continuousEffects.filter { effect ->
            when (val filter = effect.filter) {
                is com.wingedsheep.rulesengine.ecs.layers.ModifierFilter.Specific -> filter.entityId == entityId
                is com.wingedsheep.rulesengine.ecs.layers.ModifierFilter.Self -> effect.sourceId == entityId
                // Other filter types require full evaluation against the game state
                else -> false
            }
        }

    // ==========================================================================
    // Pending Cleanup Discard Helpers
    // ==========================================================================

    /**
     * Check if there are any pending cleanup discards.
     */
    val hasPendingCleanupDiscards: Boolean get() = pendingCleanupDiscards.isNotEmpty()

    /**
     * Get pending cleanup discard for a specific player.
     */
    fun getPendingCleanupDiscardForPlayer(playerId: EntityId): com.wingedsheep.rulesengine.ecs.components.PendingCleanupDiscard? =
        pendingCleanupDiscards.find { it.playerId == playerId }

    /**
     * Add a pending cleanup discard.
     */
    fun addPendingCleanupDiscard(discard: com.wingedsheep.rulesengine.ecs.components.PendingCleanupDiscard): GameState =
        copy(pendingCleanupDiscards = pendingCleanupDiscards + discard)

    /**
     * Remove a pending cleanup discard (after it's been resolved).
     */
    fun removePendingCleanupDiscard(discard: com.wingedsheep.rulesengine.ecs.components.PendingCleanupDiscard): GameState =
        copy(pendingCleanupDiscards = pendingCleanupDiscards - discard)

    /**
     * Remove pending cleanup discard for a specific player.
     */
    fun removePendingCleanupDiscardForPlayer(playerId: EntityId): GameState =
        copy(pendingCleanupDiscards = pendingCleanupDiscards.filter { it.playerId != playerId })

    // ==========================================================================
    // Pending Decision Helpers
    // ==========================================================================

    /**
     * Check if the game is paused waiting for a player decision.
     *
     * When true, no game actions should be processed until the pending
     * decision is resolved via [SubmitDecision].
     */
    val isPausedForDecision: Boolean get() = pendingDecision != null

    /**
     * Set a pending decision that needs player input.
     *
     * This pauses the game until the player responds. The decision and context
     * are stored in the game state and can survive serialization/deserialization.
     *
     * @param decision The decision to present to the player
     * @param context The serializable context for resuming the effect
     * @return New game state with the pending decision set
     */
    fun setPendingDecision(decision: PlayerDecision, context: DecisionContext): GameState =
        copy(pendingDecision = decision, decisionContext = context)

    /**
     * Clear the pending decision after it has been resolved.
     *
     * Called after processing the player's response via [DecisionResumer].
     *
     * @return New game state with no pending decision
     */
    fun clearPendingDecision(): GameState =
        copy(pendingDecision = null, decisionContext = null)

    /**
     * Get the player who must make the pending decision.
     *
     * @return The player's entity ID, or null if no pending decision
     */
    fun getPendingDecisionPlayer(): EntityId? = pendingDecision?.playerId

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
    fun addToTopOfLibrary(playerId: EntityId, entityId: EntityId): GameState =
        addToZone(entityId, ZoneId.library(playerId))

    /**
     * Add a card to the bottom of a player's library.
     */
    fun addToBottomOfLibrary(playerId: EntityId, entityId: EntityId): GameState =
        addToZoneBottom(entityId, ZoneId.library(playerId))

    /**
     * Draw from a player's library (remove top card).
     * Returns the drawn entity and updated state.
     */
    fun drawFromLibrary(playerId: EntityId): Pair<EntityId?, GameState> =
        removeTopFromZone(ZoneId.library(playerId))

    /**
     * Draw multiple cards from a player's library.
     * Returns the drawn entities and updated state.
     */
    fun drawCardsFromLibrary(playerId: EntityId, count: Int): Pair<List<EntityId>, GameState> =
        removeTopNFromZone(ZoneId.library(playerId), count)

    /**
     * Shuffle a player's library.
     */
    fun shuffleLibrary(playerId: EntityId): GameState =
        shuffleZone(ZoneId.library(playerId))

    /**
     * Shuffle a player's library with a specific random source.
     */
    fun shuffleLibrary(playerId: EntityId, random: kotlin.random.Random): GameState =
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
    fun addToHand(playerId: EntityId, entityId: EntityId): GameState =
        addToZone(entityId, ZoneId.hand(playerId))

    /**
     * Remove a card from a player's hand.
     */
    fun removeFromHand(playerId: EntityId, entityId: EntityId): GameState =
        removeFromZone(entityId, ZoneId.hand(playerId))

    /**
     * Draw a card (remove from library and add to hand).
     * Returns the drawn entity and updated state, or null if library is empty.
     */
    fun drawCard(playerId: EntityId): Pair<EntityId?, GameState> {
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
    fun drawCards(playerId: EntityId, count: Int): Pair<List<EntityId>, GameState> {
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
    fun addToGraveyard(playerId: EntityId, entityId: EntityId): GameState =
        addToZone(entityId, ZoneId.graveyard(playerId))

    /**
     * Remove a card from a player's graveyard.
     */
    fun removeFromGraveyard(playerId: EntityId, entityId: EntityId): GameState =
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
    fun addToBattlefield(entityId: EntityId): GameState =
        addToZone(entityId, ZoneId.BATTLEFIELD)

    /**
     * Remove an entity from the battlefield.
     */
    fun removeFromBattlefield(entityId: EntityId): GameState =
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
    fun addToStack(entityId: EntityId): GameState =
        addToZone(entityId, ZoneId.STACK)

    /**
     * Remove and return the top spell/ability from the stack (resolve).
     */
    fun popFromStack(): Pair<EntityId?, GameState> =
        removeTopFromZone(ZoneId.STACK)

    // ==========================================================================
    // Exile Operations
    // ==========================================================================

    /**
     * Add an entity to exile.
     */
    fun addToExile(entityId: EntityId): GameState =
        addToZone(entityId, ZoneId.EXILE)

    /**
     * Remove an entity from exile.
     */
    fun removeFromExile(entityId: EntityId): GameState =
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
    fun gainLife(playerId: EntityId, amount: Int): GameState {
        val life = getComponent<LifeComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(life.gainLife(amount)) }
    }

    /**
     * Lose life for a player.
     * Returns unchanged state if player doesn't exist.
     */
    fun loseLife(playerId: EntityId, amount: Int): GameState {
        val life = getComponent<LifeComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(life.loseLife(amount)) }
    }

    /**
     * Set a player's life to a specific value.
     * Returns unchanged state if player doesn't exist.
     */
    fun setLife(playerId: EntityId, amount: Int): GameState {
        val life = getComponent<LifeComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(life.setLife(amount)) }
    }

    /**
     * Add mana to a player's mana pool.
     * Returns unchanged state if player doesn't exist.
     */
    fun addMana(playerId: EntityId, color: Color, amount: Int = 1): GameState {
        val manaPool = getComponent<ManaPoolComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(manaPool.add(color, amount)) }
    }

    /**
     * Add colorless mana to a player's mana pool.
     * Returns unchanged state if player doesn't exist.
     */
    fun addColorlessMana(playerId: EntityId, amount: Int = 1): GameState {
        val manaPool = getComponent<ManaPoolComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(manaPool.addColorless(amount)) }
    }

    /**
     * Empty a player's mana pool.
     * Returns unchanged state if player doesn't exist.
     */
    fun emptyManaPool(playerId: EntityId): GameState {
        val manaPool = getComponent<ManaPoolComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(manaPool.empty()) }
    }

    /**
     * Add poison counters to a player.
     * Returns unchanged state if player doesn't exist.
     */
    fun addPoisonCounters(playerId: EntityId, amount: Int): GameState {
        val poison = getComponent<PoisonComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(poison.add(amount)) }
    }

    /**
     * Record that a player played a land this turn.
     * Returns unchanged state if player doesn't exist.
     */
    fun recordLandPlayed(playerId: EntityId): GameState {
        val lands = getComponent<LandsPlayedComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(lands.playLand()) }
    }

    /**
     * Reset lands played count (called at start of turn).
     * Returns unchanged state if player doesn't exist.
     */
    fun resetLandsPlayed(playerId: EntityId): GameState {
        val lands = getComponent<LandsPlayedComponent>(playerId) ?: return this
        return updateEntity(playerId) { it.with(lands.reset()) }
    }

    /**
     * Mark a player as having lost the game.
     */
    fun markPlayerLost(playerId: EntityId, reason: String): GameState =
        addComponent(playerId, LostGameComponent(reason))

    /**
     * Mark a player as having won the game.
     */
    fun markPlayerWon(playerId: EntityId): GameState =
        addComponent(playerId, WonGameComponent)

    companion object {
        /**
         * Create initial game state for a new game.
         *
         * @param players List of (EntityId, playerName) pairs
         */
        fun newGame(players: List<Pair<EntityId, String>>): GameState {
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

            return GameState(
                entities = entities,
                zones = zones,
                turnState = TurnState.newGame(playerOrder)
            )
        }

        /**
         * Create initial game state with simple string player names.
         */
        fun newGame(player1Name: String, player2Name: String): GameState {
            val player1 = EntityId.generate() to player1Name
            val player2 = EntityId.generate() to player2Name
            return newGame(listOf(player1, player2))
        }
    }
}
