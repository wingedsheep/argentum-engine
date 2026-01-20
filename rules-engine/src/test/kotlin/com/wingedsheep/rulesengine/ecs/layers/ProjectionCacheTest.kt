package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests for ProjectionCache performance optimization.
 */
class ProjectionCacheTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("Basic caching") {
        test("caches entity views") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val cache = ProjectionCache()

            // First access - cache miss
            val view1 = cache.getView(state, creatureId)
            view1.shouldNotBeNull()
            view1.name shouldBe "Grizzly Bears"
            cache.misses shouldBe 1
            cache.hits shouldBe 0

            // Second access - cache hit
            val view2 = cache.getView(state, creatureId)
            view2.shouldNotBeNull()
            view2.name shouldBe "Grizzly Bears"
            cache.hits shouldBe 1
            cache.misses shouldBe 1

            // Third access - another cache hit
            cache.getView(state, creatureId)
            cache.hits shouldBe 2
        }

        test("invalidates cache when state changes") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val cache = ProjectionCache()

            // Cache a view
            cache.getView(state, creatureId)
            cache.misses shouldBe 1
            cache.invalidations shouldBe 0

            // Create a new state (simulating an action)
            val newState = state.copy(globalFlags = mapOf("changed" to "true"))

            // Access with new state - should invalidate and re-project
            cache.getView(newState, creatureId)
            cache.invalidations shouldBe 1
            cache.misses shouldBe 2 // New miss after invalidation
        }

        test("returns null for non-existent entities") {
            val state = newGame()
            val cache = ProjectionCache()

            val nonExistentId = EntityId.generate()
            val view = cache.getView(state, nonExistentId)
            view.shouldBeNull()
        }
    }

    context("Cache statistics") {
        test("tracks hit rate correctly") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val cache = ProjectionCache()

            // 1 miss, 9 hits = 90% hit rate
            cache.getView(state, creatureId) // miss
            repeat(9) {
                cache.getView(state, creatureId) // hits
            }

            cache.hitRate shouldBe 90.0
        }

        test("resetStats clears counters") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val cache = ProjectionCache()
            cache.getView(state, creatureId)
            cache.getView(state, creatureId)

            cache.resetStats()

            cache.hits shouldBe 0
            cache.misses shouldBe 0
        }
    }

    context("Bulk operations") {
        test("getViews caches all queried entities") {
            var state = newGame()

            // Create multiple creatures
            val creatureIds = (1..5).map { i ->
                val id = EntityId.generate()
                val (_, stateWithCreature) = state.createEntity(
                    id,
                    CardComponent(bearDef, player1Id),
                    ControllerComponent(player1Id)
                )
                state = stateWithCreature.addToZone(id, ZoneId.BATTLEFIELD)
                id
            }

            val cache = ProjectionCache()

            // Bulk query
            val views = cache.getViews(state, creatureIds)
            views.size shouldBe 5
            cache.misses shouldBe 5

            // Individual queries should hit cache
            for (id in creatureIds) {
                cache.getView(state, id)
            }
            cache.hits shouldBe 5
        }

        test("projectBattlefield caches all battlefield entities") {
            var state = newGame()

            // Create multiple creatures
            val creatureIds = (1..3).map { i ->
                val id = EntityId.generate()
                val (_, stateWithCreature) = state.createEntity(
                    id,
                    CardComponent(bearDef, player1Id),
                    ControllerComponent(player1Id)
                )
                state = stateWithCreature.addToZone(id, ZoneId.BATTLEFIELD)
                id
            }

            val cache = ProjectionCache()

            // Project battlefield
            val views = cache.projectBattlefield(state)
            views.size shouldBe 3

            // Individual queries should hit cache
            for (id in creatureIds) {
                cache.getView(state, id)
            }
            cache.hits shouldBe 3
        }
    }

    context("Cache management") {
        test("explicit invalidate clears cache") {
            var state = newGame()

            val creatureId = EntityId.generate()
            val (_, stateWithCreature) = state.createEntity(
                creatureId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithCreature.addToZone(creatureId, ZoneId.BATTLEFIELD)

            val cache = ProjectionCache()
            cache.getView(state, creatureId)
            cache.size shouldBe 1

            cache.invalidate()
            cache.size shouldBe 0
        }

        test("invalidateEntity removes specific entity") {
            var state = newGame()

            val creature1Id = EntityId.generate()
            val creature2Id = EntityId.generate()
            val (_, state1) = state.createEntity(
                creature1Id,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val (_, state2) = state1.createEntity(
                creature2Id,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = state2.addToZone(creature1Id, ZoneId.BATTLEFIELD)
                .addToZone(creature2Id, ZoneId.BATTLEFIELD)

            val cache = ProjectionCache()
            cache.getView(state, creature1Id)
            cache.getView(state, creature2Id)
            cache.size shouldBe 2

            cache.invalidateEntity(creature1Id)
            cache.size shouldBe 1

            // creature2 should still be cached
            val previousHits = cache.hits
            cache.getView(state, creature2Id)
            cache.hits shouldBe previousHits + 1 // One more hit
        }

        test("respects maxSize limit") {
            val state = newGame()
            val cache = ProjectionCache(maxSize = 3)

            // Create and cache 5 entities
            repeat(5) {
                val id = EntityId.generate()
                cache.getView(state, id) // Will return null but still touches cache
            }

            // Cache should not exceed maxSize
            // (Note: null entries still count)
            cache.size shouldBe 3
        }
    }

    context("Thread-local cache") {
        test("provides per-thread cache instance") {
            val cache1 = ProjectionCache.threadLocal()
            val cache2 = ProjectionCache.threadLocal()

            // Same thread should get same instance
            cache1 shouldBe cache2
        }
    }

    context("Performance comparison") {
        test("cached projection is faster than uncached for repeated access") {
            var state = newGame()

            // Create many creatures
            val creatureIds = (1..50).map { i ->
                val id = EntityId.generate()
                val (_, stateWithCreature) = state.createEntity(
                    id,
                    CardComponent(bearDef, player1Id),
                    ControllerComponent(player1Id)
                )
                state = stateWithCreature.addToZone(id, ZoneId.BATTLEFIELD)
                id
            }

            // Warm up
            val cache = ProjectionCache()
            for (id in creatureIds) {
                cache.getView(state, id)
            }

            // Time cached access (100 iterations)
            val cachedStart = System.nanoTime()
            repeat(100) {
                for (id in creatureIds) {
                    cache.getView(state, id)
                }
            }
            val cachedTime = System.nanoTime() - cachedStart

            // Time uncached access (100 iterations)
            val uncachedStart = System.nanoTime()
            repeat(100) {
                val projector = StateProjector(state)
                for (id in creatureIds) {
                    projector.getView(id)
                }
            }
            val uncachedTime = System.nanoTime() - uncachedStart

            // Cached should be significantly faster
            val speedup = uncachedTime.toDouble() / cachedTime.toDouble()
            println("Speedup: ${speedup}x (cached: ${cachedTime/1000000}ms, uncached: ${uncachedTime/1000000}ms)")

            // Expect at least 2x speedup with caching
            speedup shouldBeGreaterThan 2.0
        }
    }
})
