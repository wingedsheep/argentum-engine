package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * End-to-end determinism guarantees for the seeded RNG (Phases 1–5).
 *
 * The contract these tests pin down — and that the cross-engine parity harness, replays, and MCTS
 * rollouts all depend on — is: **a game seeded with the same value produces a byte-identical
 * [GameState]**, down to library order and entity ids. We assert this with structural `==` on the
 * whole state. `GameState.projectedState` is a non-constructor lazy `val`, so it is excluded from
 * the data class `equals`; the comparison covers every persisted field, including `rng`,
 * `nextEntityId`, `zones`, and `entities`.
 */
class ReproducibilityTest : FunSpec({

    fun registry(): CardRegistry = CardRegistry().apply { register(TestCards.all) }

    // A 40-card, two-type deck so shuffles are non-trivial — a same-order coincidence across two
    // different seeds is astronomically unlikely.
    fun config(seed: Long?) = GameConfig(
        players = listOf(
            PlayerConfig("Alice", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
            PlayerConfig("Bob", Deck.of("Forest" to 17, "Grizzly Bears" to 23)),
        ),
        skipMulligans = true,
        seed = seed,
    )

    fun libraryNames(state: com.wingedsheep.engine.state.GameState, playerIndex: Int): List<String> {
        val playerId = state.turnOrder[playerIndex]
        return state.getZone(ZoneKey(playerId, Zone.LIBRARY)).map {
            state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ?: "?"
        }
    }

    test("same seed produces a byte-identical game state") {
        val a = GameInitializer(registry()).initializeGame(config(seed = 12345L))
        val b = GameInitializer(registry()).initializeGame(config(seed = 12345L))

        // The whole state is structurally equal: turn order, every library's order, all entity ids,
        // the rng stream position, and the entity counter.
        a.state shouldBe b.state
        a.seed shouldBe 12345L
        b.seed shouldBe 12345L
    }

    test("different seeds produce different shuffles") {
        val a = GameInitializer(registry()).initializeGame(config(seed = 1L))
        val b = GameInitializer(registry()).initializeGame(config(seed = 2L))

        // Library order must differ (the whole point of seeding) ...
        a.state shouldNotBe b.state
        libraryNames(a.state, 0) shouldNotBe libraryNames(b.state, 0)

        // ... but the ids themselves are NOT a function of the seed — they're minted from a
        // deterministic counter, so both runs mint the same set. Only which card receives which
        // id follows the seed, so that an id never names its card (HiddenCardReferenceTest).
        a.state.entities.keys shouldBe b.state.entities.keys
        a.state.nextEntityId shouldBe b.state.nextEntityId
    }

    test("an unseeded game records a seed that reproduces it exactly") {
        // Live play passes seed = null; the initializer draws fresh entropy and records it.
        val live = GameInitializer(registry()).initializeGame(config(seed = null))

        // Replaying with the recorded seed reconstructs the identical state.
        val replay = GameInitializer(registry()).initializeGame(config(seed = live.seed))
        replay.state shouldBe live.state
    }

    test("two unseeded games differ (entropy seeding is actually random)") {
        val a = GameInitializer(registry()).initializeGame(config(seed = null))
        val b = GameInitializer(registry()).initializeGame(config(seed = null))
        // Different entropy seeds → different recorded seeds → different shuffles.
        a.seed shouldNotBe b.seed
        a.state shouldNotBe b.state
    }

    test("entity ids are deterministic, counter-based handles (not random uuids)") {
        val state = GameInitializer(registry()).initializeGame(config(seed = 7L)).state
        // Every minted id is of the form e<n>, and the counter equals the number of entities created.
        state.entities.keys.all { it.value.matches(Regex("e\\d+")) } shouldBe true
        state.nextEntityId shouldBe state.entities.size.toLong()
    }
})
