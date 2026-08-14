package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * Proves the multiplayer pod arena measures what it claims to, before anyone reads a win share off
 * it. The head-to-head analogue is [ArenaHarnessTest].
 *
 * Three claims:
 *
 * 1. **The mirror lands on the null share, exactly.** With the same agent in every seat, the games
 *    in a rotation group are the same game rotated, so exactly one of them is won by the seat A
 *    happens to occupy. Any deviation is a real defect — a seat leak, a seed leak, or
 *    nondeterminism in the AI.
 * 2. **A pod game is a real game.** It reaches a natural finish, and neither the AI nor the
 *    enumerator produces a rejected action along the way.
 * 3. **The harness discriminates.** The evaluating agent beats a field of the zero-weight control at
 *    a table where, before Phase 3, it could not see most of the board.
 *
 * Portal, not Bloomburrow: simpler cards run faster, and this test asserts structure, not strength.
 */
class PodArenaHarnessTest : FunSpec({

    // Every run here is capped by actions rather than by turns. A late pod position is expensive —
    // three or four growing boards, and the Strategist simulates every candidate against all of
    // them — so an uncapped mirror spends minutes per game proving something a few hundred actions
    // already proved. `just arena-pod` is where full-length pod games get played.
    fun config(table: TableSetup, a: String, b: String, groups: Int, maxActions: Int = 800) = PodArenaConfig(
        agentA = ArenaAgents.resolve(a),
        agentB = ArenaAgents.resolve(b),
        table = table,
        groups = groups,
        setCode = "POR",
        maxTurns = 30,
        maxActions = maxActions,
    )

    /**
     * With the same agent in every seat the games of a rotation group are literally the same game
     * with A's label moved around the table, so a decisive group gives A exactly one win and an
     * unfinished one gives A none. That makes the *decisive* share exactly the null share and the
     * raw share exactly `null x completion rate` — no interval, no variance.
     */
    fun assertMirror(run: PodArenaRun, winnerOf: (TableGameOutcome) -> Int?) {
        run.groups.forEach { group ->
            withClue("group ${group.groupId} — same agent in every seat, one seed") {
                group.games.map { winnerOf(it.outcome) }.distinct().size shouldBe 1
                group.aWins shouldBe if (winnerOf(group.games.first().outcome) != null) 1 else 0
            }
        }
        // Conditional because a capped-short run can be entirely undecided, and 0 wins out of 0
        // decisive games is not evidence of anything either way.
        if (run.stats.games > run.stats.noWinner) {
            withClue("${run.stats.noWinner} of ${run.stats.games} games produced no winner") {
                run.stats.decisiveWinShare shouldBe run.stats.nullShare
            }
        }
    }

    test("a v0 mirror in a three-player pod lands exactly on the 1/3 null share") {
        assertMirror(PodArena.run(config(TableSetup.FFA3, "v0", "v0", groups = 3))) { it.winnerSeat }
    }

    test("a v0 mirror in Two-Headed Giant lands exactly on the 1/2 null share") {
        assertMirror(PodArena.run(config(TableSetup.TWO_HEADED_GIANT, "v0", "v0", groups = 2))) { it.winnerTeam }
    }

    // ── One real game per table ──────────────────────────────────────────────
    //
    // The arena is a free crash finder at scale, and multiplayer is the least-exercised engine path
    // there is. One game per table, not a whole rotation group: in a mirror the games of a group
    // are the same game relabelled, so the extra rotations cost wall clock and assert nothing new.
    // One test per table so each gets its own hang-guard budget and a failure names the table.

    val set = MtgSetCatalog.requireByCode("POR")
    val podRegistry = CardRegistry().apply { register(set.cards); register(set.basicLands) }

    fun soloGame(table: TableSetup, maxTurns: Int): TableGameOutcome {
        val seed = mixSeed(ArenaConfig.DEFAULT_SEED, 1L)
        val deck = buildSeededSealedDeck(set.cards, Random(seed))
        val v0 = ArenaAgents.resolve("v0")
        return TableGameRunner.play(
            podRegistry, table, List(table.seats) { v0 }, List(table.seats) { deck },
            seed = seed, groupId = 1, rotation = 0, maxTurns = maxTurns,
        )
    }

    /**
     * Clean means three things, and the third is the one that caught a real defect: reaching the
     * cap is fine, **wedging is not**. Every non-cap draw reason — `stuck`, `error`, `noProgress`,
     * `exception` — is a game the harness could not carry to its end.
     */
    fun TableGameOutcome.shouldRunClean() {
        withClue("${setup.id}: $illegalActions") { illegalActions.keys.shouldBeEmpty() }
        withClue("${setup.id}: $exception") { exception shouldBe null }
        withClue("${setup.id} wedged: $drawReason") {
            val hitACap = listOf("maxTurns", "maxActions").any { drawReason.startsWith(it) }
            (drawReason.isEmpty() || hitACap) shouldBe true
        }
    }

    test("a three-player pod plays to a real finish, clean") {
        val game = soloGame(TableSetup.FFA3, maxTurns = 30)
        game.shouldRunClean()
        withClue("did not finish: ${game.drawReason}") { game.completed shouldBe true }
    }

    test("a four-player pod plays past its first elimination to a real finish, clean") {
        // The path that matters here is the endgame after a seat is knocked out. `turnNumber` used
        // to be a round counter that only advanced for `turnOrder.first()` — and turnOrder keeps
        // eliminated players — so it froze the moment the opening seat died and a harness measuring
        // progress by it declared a healthy three-way endgame wedged. This test is that regression
        // net: it failed with `stuck` before the fix.
        val game = soloGame(TableSetup.FFA4, maxTurns = 30)
        game.shouldRunClean()
        withClue("did not finish: ${game.drawReason}") { game.completed shouldBe true }
    }

    test("a Two-Headed Giant game runs clean") {
        // Capped short rather than played out: 30 shared life across two boards makes 2HG the
        // slowest table by a distance, and the assertion that matters — nothing wedged, nothing
        // was rejected — does not need the last twenty rounds. `just arena-pod 2hg` plays them.
        soloGame(TableSetup.TWO_HEADED_GIANT, maxTurns = 12).shouldRunClean()
    }

    test("the pod arena discriminates: an evaluating agent beats a field of the blind control") {
        // `v0-blind` zeroes every evaluation weight, so it can never prefer an action to passing.
        // Before Phase 3 the real agent was blind to most of this table too; if this ever stops
        // separating, the pod arena has become a coin flip and nothing measured on it means
        // anything.
        // Room to actually finish: a blind field folds fast, so these games are cheap even
        // uncapped, and a truncated game is a win for nobody.
        val run = PodArena.run(config(TableSetup.FFA3, "v0", "v0-blind", groups = 6, maxActions = 4000))
        withClue("v0 won ${run.stats.aWins}/${run.stats.games} against two blind seats") {
            run.stats.winShare shouldBeGreaterThan run.stats.nullShare
        }
    }

    test("every table's teams partition its seats into equal, contiguous blocks") {
        // TableSetup.init enforces it; this pins the committed tables so a future one cannot be
        // added in a shape the rotation would silently mis-handle.
        TableSetup.all.forEach { table ->
            withClue(table.id) {
                table.teamOfSeat.size shouldBe table.seats
                table.seats % table.teamCount shouldBe 0
                table.teamOfSeat shouldBe (0 until table.seats).map { it / table.seatsPerTeam }
            }
        }
    }
})
