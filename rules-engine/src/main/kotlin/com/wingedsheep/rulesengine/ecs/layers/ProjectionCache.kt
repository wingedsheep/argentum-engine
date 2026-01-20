package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import java.lang.ref.WeakReference

/**
 * A thread-safe cache for projected game object views.
 *
 * This cache dramatically improves performance when the same entities are
 * projected multiple times within a single game state context. Common scenarios:
 *
 * - LegalActionCalculator checking multiple creatures' keywords
 * - State-based actions checking toughness of all creatures
 * - Combat validation checking attacker/blocker properties
 * - Multiple consecutive queries during UI rendering
 *
 * ## Cache Invalidation
 *
 * The cache automatically invalidates when:
 * - A different GameState is passed (detected via reference equality)
 * - [invalidate] is called explicitly
 * - The cache grows beyond [maxSize]
 *
 * ## Thread Safety
 *
 * The cache uses synchronized access to the underlying maps. For high-concurrency
 * scenarios, consider using a thread-local cache instead.
 *
 * ## Memory Management
 *
 * The cache holds a WeakReference to the GameState to avoid memory leaks.
 * If the GameState is garbage collected, the cache automatically invalidates.
 *
 * ## Usage
 *
 * ```kotlin
 * val cache = ProjectionCache()
 *
 * // First query - projects entity and caches result
 * val view1 = cache.getView(state, entityId, modifierProvider)
 *
 * // Second query - returns cached result (no re-projection)
 * val view2 = cache.getView(state, entityId, modifierProvider)
 *
 * // Different state - invalidates cache and re-projects
 * val view3 = cache.getView(newState, entityId, modifierProvider)
 * ```
 *
 * @property maxSize Maximum number of entities to cache (default 500)
 */
class ProjectionCache(
    private val maxSize: Int = 500
) {
    /**
     * Weak reference to the GameState this cache was computed for.
     * When the state changes (different reference), the cache invalidates.
     */
    private var stateRef: WeakReference<GameState>? = null

    /**
     * Cached projector for the current state.
     */
    private var cachedProjector: StateProjector? = null

    /**
     * Cache of entity ID to projected view.
     * Using LinkedHashMap to support LRU eviction via access order.
     */
    private val viewCache = object : LinkedHashMap<EntityId, GameObjectView?>(
        16, 0.75f, true // accessOrder = true for LRU
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<EntityId, GameObjectView?>?): Boolean {
            return size > maxSize
        }
    }

    /**
     * Statistics for monitoring cache performance.
     */
    @Volatile
    var hits: Long = 0
        private set

    @Volatile
    var misses: Long = 0
        private set

    @Volatile
    var invalidations: Long = 0
        private set

    /**
     * Get the projected view for an entity, using cache if available.
     *
     * @param state The current game state
     * @param entityId The entity to project
     * @param modifierProvider Provider for collecting modifiers
     * @return The projected GameObjectView, or null if entity doesn't exist
     */
    @Synchronized
    fun getView(
        state: GameState,
        entityId: EntityId,
        modifierProvider: ModifierProvider? = null
    ): GameObjectView? {
        ensureStateMatch(state, modifierProvider)

        // Check cache
        if (viewCache.containsKey(entityId)) {
            hits++
            return viewCache[entityId]
        }

        // Cache miss - project and store
        misses++
        val projector = cachedProjector ?: return null
        val view = projector.getView(entityId)
        viewCache[entityId] = view
        return view
    }

    /**
     * Get views for multiple entities efficiently.
     *
     * @param state The current game state
     * @param entityIds The entities to project
     * @param modifierProvider Provider for collecting modifiers
     * @return Map of entity ID to projected view (null entries for non-existent entities)
     */
    @Synchronized
    fun getViews(
        state: GameState,
        entityIds: Collection<EntityId>,
        modifierProvider: ModifierProvider? = null
    ): Map<EntityId, GameObjectView?> {
        ensureStateMatch(state, modifierProvider)

        val projector = cachedProjector ?: return entityIds.associateWith { null }
        return entityIds.associateWith { entityId ->
            if (viewCache.containsKey(entityId)) {
                hits++
                viewCache[entityId]
            } else {
                misses++
                val view = projector.getView(entityId)
                viewCache[entityId] = view
                view
            }
        }
    }

    /**
     * Project all entities on the battlefield.
     * Results are cached for subsequent individual queries.
     */
    @Synchronized
    fun projectBattlefield(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): List<GameObjectView> {
        ensureStateMatch(state, modifierProvider)

        val projector = cachedProjector ?: return emptyList()
        val views = projector.projectBattlefield()

        // Cache all results
        for (view in views) {
            if (!viewCache.containsKey(view.entityId)) {
                misses++
                viewCache[view.entityId] = view
            } else {
                hits++
            }
        }

        return views
    }

    /**
     * Get the underlying projector, creating it if necessary.
     * Useful when you need to call projector methods directly.
     */
    @Synchronized
    fun getProjector(
        state: GameState,
        modifierProvider: ModifierProvider? = null
    ): StateProjector {
        ensureStateMatch(state, modifierProvider)
        return cachedProjector ?: StateProjector(state)
    }

    /**
     * Invalidate a specific entity's cached view.
     * Call this when you know a specific entity's projection is stale.
     */
    @Synchronized
    fun invalidateEntity(entityId: EntityId) {
        viewCache.remove(entityId)
    }

    /**
     * Invalidate the entire cache.
     * Call this when you know the game state has changed.
     */
    @Synchronized
    fun invalidate() {
        viewCache.clear()
        cachedProjector = null
        stateRef = null
        invalidations++
    }

    /**
     * Clear statistics counters.
     */
    @Synchronized
    fun resetStats() {
        hits = 0
        misses = 0
        invalidations = 0
    }

    /**
     * Get the cache hit rate as a percentage (0-100).
     */
    val hitRate: Double
        get() {
            val total = hits + misses
            return if (total == 0L) 0.0 else (hits.toDouble() / total) * 100
        }

    /**
     * Ensure the cache is valid for the given state.
     * Invalidates and rebuilds if the state has changed.
     */
    private fun ensureStateMatch(state: GameState, modifierProvider: ModifierProvider?) {
        val currentState = stateRef?.get()

        // If state reference changed (different GameState object), invalidate cache
        if (currentState !== state) {
            if (currentState != null) {
                invalidations++
            }
            viewCache.clear()
            stateRef = WeakReference(state)
            cachedProjector = StateProjector.forState(state, modifierProvider)
        }
    }

    /**
     * Get current cache size.
     */
    val size: Int
        @Synchronized
        get() = viewCache.size

    override fun toString(): String {
        return "ProjectionCache(size=$size, hits=$hits, misses=$misses, invalidations=$invalidations, hitRate=${"%.1f".format(hitRate)}%)"
    }

    companion object {
        /**
         * Thread-local cache for scenarios where each thread needs its own cache.
         * Useful for parallel processing of game states.
         */
        private val threadLocalCache = ThreadLocal.withInitial { ProjectionCache() }

        /**
         * Get a thread-local projection cache.
         * Each thread gets its own cache instance.
         */
        fun threadLocal(): ProjectionCache = threadLocalCache.get()

        /**
         * Create a cache optimized for a specific scenario.
         *
         * @param expectedEntities Expected number of entities to cache
         */
        fun sized(expectedEntities: Int): ProjectionCache {
            return ProjectionCache(maxSize = (expectedEntities * 1.5).toInt().coerceAtLeast(50))
        }
    }
}
