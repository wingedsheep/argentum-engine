package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.mtg.sets.definitions.portal.PortalSet
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import java.lang.management.ManagementFactory
import kotlin.math.roundToLong

/**
 * Microbenchmark: GameState clone/modify primitives, measured on both a freshly-initialized
 * game and a realistic mid-game position (populated battlefields, counters, damage, graveyards,
 * reduced life).
 *
 * Run with:
 *   ./gradlew :ai:test --tests "*.StateCloneBenchmark" -Dbenchmark=true
 *   ./gradlew :ai:test --tests "*.StateCloneBenchmark" -Dbenchmark=true -DbenchmarkIterations=2000000
 */
class StateCloneBenchmark : FunSpec({

    val iterations = System.getProperty("benchmarkIterations")?.toIntOrNull() ?: 1_000_000
    val warmup = (iterations / 10).coerceAtLeast(10_000)
    val benchmarkEnabled = System.getProperty("benchmark") == "true"

    test("state clone/modify microbenchmark").config(enabled = benchmarkEnabled) {
        val registry = CardRegistry().apply { register(PortalSet.allCards) }
        val initializer = GameInitializer(registry)

        // 60-card deck: 30 lands + 30 simple creatures (mostly 1- and 2-drops).
        val deck = Deck(buildList {
            repeat(15) { add("Mountain"); add("Plains") }
            repeat(8) { add("Raging Goblin"); add("Devoted Hero") }
            repeat(7) { add("Grizzly Bears"); add("Border Guard") }
        })

        val init = initializer.initializeGame(
            GameConfig(
                players = listOf(PlayerConfig("P1", deck), PlayerConfig("P2", deck)),
                skipMulligans = true, startingPlayerIndex = 0,
            )
        ).state

        val p1 = init.turnOrder[0]
        val p2 = init.turnOrder[1]
        val midGame = buildMidGameState(init, p1, p2)

        runBenchmarkSuite("INIT STATE (turn 1, empty battlefields)", init, p1, iterations, warmup)
        println()
        runBenchmarkSuite("MID-GAME STATE (turn 8, populated boards)", midGame, p1, iterations, warmup)
    }
})

/**
 * Build a realistic turn-8 position:
 *   - each player has 7 lands (4 tapped) and 5 creatures on the battlefield
 *   - 2 creatures per side have +1/+1 counters; 1 creature has marked damage
 *   - newest creature per side has summoning sickness + entered-this-turn
 *   - each player has 4 cards in graveyard
 *   - life totals reduced (P1: 15, P2: 11)
 *   - turn = 8, timestamp = 220, spells cast this turn = 3 (by active player)
 */
private fun buildMidGameState(initial: GameState, p1: EntityId, p2: EntityId): GameState {
    var s = initial
    for ((player, opponent) in listOf(p1 to p2, p2 to p1)) {
        val libraryKey = ZoneKey(player, Zone.LIBRARY)
        val battlefieldKey = ZoneKey(player, Zone.BATTLEFIELD)
        val graveyardKey = ZoneKey(player, Zone.GRAVEYARD)

        // Take the top 16 cards of the player's library and distribute them.
        val lib = s.getZone(libraryKey)
        val toBattlefield = lib.take(12)
        val toGraveyard = lib.drop(12).take(4)

        for (id in toBattlefield) s = s.moveToZone(id, libraryKey, battlefieldKey)
        for (id in toGraveyard) s = s.moveToZone(id, libraryKey, graveyardKey)

        // Tap the first 4 permanents, give +1/+1 counters to two, damage to one,
        // summoning sickness to the most recent arrival.
        val permanents = s.getZone(battlefieldKey)
        for (id in permanents.take(4)) {
            s = s.updateEntity(id) { it.with(TappedComponent) }
        }
        for (id in permanents.drop(7).take(2)) {
            s = s.updateEntity(id) {
                it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
            }
        }
        permanents.getOrNull(9)?.let { id ->
            s = s.updateEntity(id) { it.with(DamageComponent(2)) }
        }
        permanents.lastOrNull()?.let { id ->
            s = s.updateEntity(id) {
                it.with(SummoningSicknessComponent).with(EnteredThisTurnComponent)
            }
        }

        // Dip life totals.
        val targetLife = if (player == p1) 15 else 11
        s = s.updateEntity(player) { it.with(LifeTotalComponent(life = targetLife)) }
        @Suppress("UNUSED_VARIABLE") val unused = opponent
    }

    return s.copy(
        turnNumber = 8,
        timestamp = 220L,
        spellsCastThisTurn = 3,
        playerSpellsCastThisTurn = mapOf(p1 to 2, p2 to 1),
    )
}

private fun runBenchmarkSuite(
    label: String,
    state: GameState,
    player: EntityId,
    iterations: Int,
    warmup: Int,
) {
    val handCards = state.getHand(player)
    val firstHandCard = handCards.first()
    val battlefield = state.getZone(player, Zone.BATTLEFIELD)
    val firstPermanent = battlefield.firstOrNull() ?: firstHandCard

    val handZone = ZoneKey(player, Zone.HAND)
    val graveyardZone = ZoneKey(player, Zone.GRAVEYARD)
    val battlefieldZone = ZoneKey(player, Zone.BATTLEFIELD)

    val componentsPerEntity = state.entities.values.map { it.all().size }
    val avgComponents = componentsPerEntity.average()
    val maxComponents = componentsPerEntity.maxOrNull() ?: 0
    val battlefieldSize = battlefield.size

    println("=== $label ===")
    println("Entities:   ${state.entities.size}  |  Zones: ${state.zones.size}  |  Battlefield (P1): $battlefieldSize")
    println("Components: avg ${"%.2f".format(avgComponents)}/entity, max $maxComponents")
    println("Iterations: $iterations (warmup $warmup)")
    println()
    println(String.format("%-48s %12s %14s %14s", "operation", "ns/op", "ops/sec", "bytes/op"))
    println("-".repeat(92))

    bench("copy() — no-op (baseline fork)", warmup, iterations) {
        state.copy()
    }
    bench("tick() — single-field copy", warmup, iterations) {
        state.tick()
    }
    bench("updateEntity + with(TappedComponent) [hand]", warmup, iterations) {
        state.updateEntity(firstHandCard) { it.with(TappedComponent) }
    }
    bench("updateEntity + with(TappedComponent) [permanent]", warmup, iterations) {
        state.updateEntity(firstPermanent) { it.with(TappedComponent) }
    }
    bench("moveToZone (hand → graveyard)", warmup, iterations) {
        state.moveToZone(firstHandCard, handZone, graveyardZone)
    }
    bench("moveToZone (battlefield → graveyard)", warmup, iterations) {
        state.moveToZone(firstPermanent, battlefieldZone, graveyardZone)
    }
    val chainIters = iterations / 4
    bench("chain: untap all P1 permanents (${battlefieldSize}×)", warmup / 4, chainIters) {
        var cur = state
        for (id in battlefield) cur = cur.updateEntity(id) { it.without<TappedComponent>() }
        cur
    }
}

private fun bench(name: String, warmup: Int, iterations: Int, op: () -> Any?) {
    var sink: Any? = null
    repeat(warmup) { sink = op() }

    val threadBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    val tid = Thread.currentThread().id

    val allocBefore = threadBean.getThreadAllocatedBytes(tid)
    val t0 = System.nanoTime()
    repeat(iterations) { sink = op() }
    val t1 = System.nanoTime()
    val allocAfter = threadBean.getThreadAllocatedBytes(tid)

    if (sink == null) error("sink null — compiler may have elided the operation")

    val nsPerOp = (t1 - t0).toDouble() / iterations
    val bytesPerOp = (allocAfter - allocBefore).toDouble() / iterations
    val opsPerSec = (1_000_000_000.0 / nsPerOp).roundToLong()

    println(String.format("%-48s %10.0f ns %14d %12.0f B", name, nsPerOp, opsPerSec, bytesPerOp))
}
