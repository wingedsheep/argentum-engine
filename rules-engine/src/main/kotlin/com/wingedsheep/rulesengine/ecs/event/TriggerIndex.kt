package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ability.*
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent

/**
 * Event categories for trigger indexing.
 *
 * Each category corresponds to one or more GameEvent types that can fire triggers.
 * This allows O(1) lookup of which entities might have relevant triggers for a given event.
 */
enum class EventCategory {
    ENTER_BATTLEFIELD,      // EnteredBattlefield
    LEAVE_BATTLEFIELD,      // LeftBattlefield
    CREATURE_DIED,          // CreatureDied
    CARD_DRAWN,             // CardDrawn
    ATTACKER_DECLARED,      // AttackerDeclared
    BLOCKER_DECLARED,       // BlockerDeclared
    DAMAGE_TO_PLAYER,       // DamageDealtToPlayer
    DAMAGE_TO_CREATURE,     // DamageDealtToCreature
    UPKEEP,                 // UpkeepBegan
    END_STEP,               // EndStepBegan
    COMBAT_BEGAN,           // CombatBegan
    SPELL_CAST,             // SpellCast
    FIRST_MAIN_PHASE,       // FirstMainPhaseBegan
    TRANSFORMED,            // Transformed
    PERMANENT_TAPPED,       // PermanentTapped
    PERMANENT_UNTAPPED      // PermanentUntapped
}

/**
 * An index that maps event categories to entities with triggers for those events.
 *
 * This dramatically improves trigger detection performance by allowing O(1) lookup
 * of which entities might have relevant triggers for a given event, instead of
 * iterating over all battlefield entities for every event.
 *
 * ## Usage
 *
 * The index must be kept synchronized with the game state:
 * - Call [registerEntity] when a permanent enters the battlefield
 * - Call [unregisterEntity] when a permanent leaves the battlefield
 * - Or call [rebuild] to rebuild the entire index from scratch
 *
 * During trigger detection:
 * ```kotlin
 * val category = TriggerIndex.eventToCategory(event)
 * val relevantEntities = index.getEntitiesForCategory(category)
 * // Only iterate over relevantEntities instead of all battlefield entities
 * ```
 *
 * ## Thread Safety
 *
 * This class uses synchronized access for thread safety. For high-concurrency
 * scenarios, consider using a separate index per thread or a concurrent implementation.
 *
 * ## Memory
 *
 * The index stores entity references only, not full ability data. The memory
 * overhead is proportional to (number of entities) × (average triggers per entity).
 */
class TriggerIndex(
    private val abilityRegistry: AbilityRegistry
) {
    /**
     * Map from event category to set of entities with triggers for that category.
     */
    private val index = mutableMapOf<EventCategory, MutableSet<EntityId>>()

    /**
     * Reverse index: entity ID to its registered categories.
     * Used for efficient unregistration.
     */
    private val entityCategories = mutableMapOf<EntityId, MutableSet<EventCategory>>()

    /**
     * Statistics for monitoring index performance.
     */
    @Volatile
    var registrations: Long = 0
        private set

    @Volatile
    var unregistrations: Long = 0
        private set

    @Volatile
    var lookups: Long = 0
        private set

    /**
     * Register an entity's triggers in the index.
     *
     * Call this when a permanent enters the battlefield.
     *
     * @param entityId The entity that entered
     * @param definition The card definition (to look up triggered abilities)
     */
    @Synchronized
    fun registerEntity(entityId: EntityId, definition: CardDefinition) {
        val abilities = abilityRegistry.getTriggeredAbilities(entityId, definition)
        if (abilities.isEmpty()) return

        val categories = mutableSetOf<EventCategory>()

        for (ability in abilities) {
            val category = triggerToCategory(ability.trigger)
            if (category != null) {
                index.getOrPut(category) { mutableSetOf() }.add(entityId)
                categories.add(category)
            }
        }

        if (categories.isNotEmpty()) {
            entityCategories[entityId] = categories
            registrations++
        }
    }

    /**
     * Unregister an entity from the index.
     *
     * Call this when a permanent leaves the battlefield.
     *
     * @param entityId The entity that left
     */
    @Synchronized
    fun unregisterEntity(entityId: EntityId) {
        val categories = entityCategories.remove(entityId) ?: return

        for (category in categories) {
            index[category]?.remove(entityId)
        }
        unregistrations++
    }

    /**
     * Get all entities that might have triggers for the given event category.
     *
     * @param category The event category to look up
     * @return Set of entity IDs with potentially relevant triggers
     */
    @Synchronized
    fun getEntitiesForCategory(category: EventCategory?): Set<EntityId> {
        lookups++
        if (category == null) return emptySet()
        return index[category]?.toSet() ?: emptySet()
    }

    /**
     * Get all entities that might have triggers for the given event.
     *
     * @param event The game event
     * @return Set of entity IDs with potentially relevant triggers
     */
    @Synchronized
    fun getEntitiesForEvent(event: GameEvent): Set<EntityId> {
        val category = eventToCategory(event) ?: return emptySet()
        return getEntitiesForCategory(category)
    }

    /**
     * Rebuild the entire index from the current game state.
     *
     * This is useful when the index might be out of sync, or during initialization.
     *
     * @param state The current game state
     */
    @Synchronized
    fun rebuild(state: GameState) {
        clear()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            registerEntity(entityId, cardComponent.definition)
        }
    }

    /**
     * Clear the entire index.
     */
    @Synchronized
    fun clear() {
        index.clear()
        entityCategories.clear()
    }

    /**
     * Reset statistics counters.
     */
    @Synchronized
    fun resetStats() {
        registrations = 0
        unregistrations = 0
        lookups = 0
    }

    /**
     * Get the current index size (total entity-category mappings).
     */
    val size: Int
        @Synchronized
        get() = index.values.sumOf { it.size }

    /**
     * Get the number of indexed entities.
     */
    val entityCount: Int
        @Synchronized
        get() = entityCategories.size

    override fun toString(): String {
        return "TriggerIndex(entities=$entityCount, mappings=$size, registrations=$registrations, unregistrations=$unregistrations, lookups=$lookups)"
    }

    companion object {
        /**
         * Map a Trigger type to its corresponding EventCategory.
         *
         * @param trigger The trigger to categorize
         * @return The category, or null if the trigger doesn't map to a standard event
         */
        fun triggerToCategory(trigger: Trigger): EventCategory? {
            return when (trigger) {
                is OnEnterBattlefield -> EventCategory.ENTER_BATTLEFIELD
                is OnOtherCreatureEnters -> EventCategory.ENTER_BATTLEFIELD
                is OnLeavesBattlefield -> EventCategory.LEAVE_BATTLEFIELD
                is OnDeath -> EventCategory.CREATURE_DIED
                is OnOtherCreatureWithSubtypeDies -> EventCategory.CREATURE_DIED
                is OnDraw -> EventCategory.CARD_DRAWN
                is OnAttack -> EventCategory.ATTACKER_DECLARED
                is OnYouAttack -> EventCategory.COMBAT_BEGAN
                is OnBlock -> EventCategory.BLOCKER_DECLARED
                is OnDealsDamage -> null // Can be either player or creature damage
                is OnUpkeep -> EventCategory.UPKEEP
                is OnEndStep -> EventCategory.END_STEP
                is OnBeginCombat -> EventCategory.COMBAT_BEGAN
                is OnDamageReceived -> EventCategory.DAMAGE_TO_CREATURE
                is OnSpellCast -> EventCategory.SPELL_CAST
                is OnFirstMainPhase -> EventCategory.FIRST_MAIN_PHASE
                is OnTransform -> EventCategory.TRANSFORMED
                is OnBecomesTapped -> EventCategory.PERMANENT_TAPPED
                is OnBecomesUntapped -> EventCategory.PERMANENT_UNTAPPED
            }
        }

        /**
         * Map a GameEvent to its corresponding EventCategory.
         *
         * @param event The event to categorize
         * @return The category, or null if the event doesn't trigger abilities
         */
        fun eventToCategory(event: GameEvent): EventCategory? {
            return when (event) {
                is GameEvent.EnteredBattlefield -> EventCategory.ENTER_BATTLEFIELD
                is GameEvent.LeftBattlefield -> EventCategory.LEAVE_BATTLEFIELD
                is GameEvent.CreatureDied -> EventCategory.CREATURE_DIED
                is GameEvent.CardDrawn -> EventCategory.CARD_DRAWN
                is GameEvent.AttackerDeclared -> EventCategory.ATTACKER_DECLARED
                is GameEvent.BlockerDeclared -> EventCategory.BLOCKER_DECLARED
                is GameEvent.DamageDealtToPlayer -> EventCategory.DAMAGE_TO_PLAYER
                is GameEvent.DamageDealtToCreature -> EventCategory.DAMAGE_TO_CREATURE
                is GameEvent.UpkeepBegan -> EventCategory.UPKEEP
                is GameEvent.EndStepBegan -> EventCategory.END_STEP
                is GameEvent.CombatBegan -> EventCategory.COMBAT_BEGAN
                is GameEvent.SpellCast -> EventCategory.SPELL_CAST
                is GameEvent.FirstMainPhaseBegan -> EventCategory.FIRST_MAIN_PHASE
                is GameEvent.Transformed -> EventCategory.TRANSFORMED
                is GameEvent.PermanentTapped -> EventCategory.PERMANENT_TAPPED
                is GameEvent.PermanentUntapped -> EventCategory.PERMANENT_UNTAPPED
                // Events that don't typically trigger abilities
                is GameEvent.CardDiscarded,
                is GameEvent.CardExiled,
                is GameEvent.ReturnedToHand,
                is GameEvent.CombatEnded,
                is GameEvent.LifeGained,
                is GameEvent.LifeLost,
                is GameEvent.CountersAdded,
                is GameEvent.CountersRemoved,
                is GameEvent.PlayerLost,
                is GameEvent.GameEnded -> null
            }
        }

        /**
         * Create an index pre-populated from the current game state.
         */
        fun forState(state: GameState, abilityRegistry: AbilityRegistry): TriggerIndex {
            val index = TriggerIndex(abilityRegistry)
            index.rebuild(state)
            return index
        }
    }
}

/**
 * Indexed entry containing entity and controller for efficient trigger checking.
 */
data class IndexedEntity(
    val entityId: EntityId,
    val controllerId: EntityId,
    val definition: CardDefinition
)
