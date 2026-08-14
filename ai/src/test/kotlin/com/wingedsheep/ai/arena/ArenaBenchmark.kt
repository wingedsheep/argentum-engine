package com.wingedsheep.ai.arena

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path

/**
 * The arena's command-line entry point. Both tests are disabled unless explicitly switched on, so
 * a normal `:ai:test` run never pays for them.
 *
 * ```
 * just arena v0 blb-advisors 1000
 * just arena-gauntlet 200
 * just arena-pod ffa3 current v0-blind 150
 * ```
 *
 * How to read the output — and the promotion rule — is in `docs/ai/measurement.md`.
 */
class ArenaBenchmark : FunSpec({

    val games = System.getProperty("arenaGames")?.toIntOrNull() ?: 300
    val seed = System.getProperty("arenaSeed")?.toLongOrNull() ?: ArenaConfig.DEFAULT_SEED
    val setCode = System.getProperty("arenaSet") ?: "BLB"
    val maxTurns = System.getProperty("arenaMaxTurns")?.toIntOrNull() ?: 50
    val threads = System.getProperty("arenaThreads")?.toIntOrNull()
        ?: Runtime.getRuntime().availableProcessors()
    val featureOutput = System.getProperty("arenaEmitFeatures")?.let(Path::of)

    val headToHead = System.getProperty("arena") == "true"
    val gauntlet = System.getProperty("arenaGauntlet") == "true"
    val pod = System.getProperty("arenaPod") == "true"

    test("arena: head to head").config(enabled = headToHead) {
        val agentA = ArenaAgents.resolve(requireProperty("arenaA"))
        val agentB = ArenaAgents.resolve(requireProperty("arenaB"))
        val config = ArenaConfig(agentA, agentB, games, seed, setCode, maxTurns, threads, featureOutput)

        println("=== ARENA: ${agentA.name} vs ${agentB.name} — ${config.pairs} pairs " +
            "(${config.pairs * 2} games) on $threads threads, $setCode, seed $seed ===")
        val run = Arena.run(config) { done, total, pair ->
            if (done <= 5 || done % 25 == 0 || done == total) {
                println("  [$done/$total] pair ${pair.pairId}: ${pair.aWins}-${pair.bWins}-${pair.draws} " +
                    "(score ${fmt("%+.1f", pair.score)})")
            }
        }

        println()
        print(ArenaReport.summary(run))
        println("Written to: ${ArenaReport.write(run)}")
    }

    test("arena: multiplayer pod").config(enabled = pod) {
        val table = TableSetup.resolve(requireProperty("arenaTable"))
        val agentA = ArenaAgents.resolve(requireProperty("arenaA"))
        val agentB = ArenaAgents.resolve(requireProperty("arenaB"))
        // `games` is a game count everywhere else, so honour it as one here too and round up to a
        // whole number of rotation groups — a partial rotation is exactly the seat bias the
        // rotation exists to remove.
        val groups = ((games + table.teamCount - 1) / table.teamCount).coerceAtLeast(1)
        // Named, not positional: PodArenaConfig carries two int caps and a thread count, and
        // sliding one argument into the wrong slot produces a run that looks fine and measures
        // nothing (a `threads` value landing in `maxActions` truncates every game at 8 actions).
        val config = PodArenaConfig(
            agentA = agentA, agentB = agentB, table = table, groups = groups, seed = seed,
            setCode = setCode,
            // A pod round is several player turns, so the head-to-head default of 50 rounds is
            // 150-200 turns of play. Take the pod default unless the caller asked for a number.
            maxTurns = System.getProperty("arenaMaxTurns")?.toIntOrNull() ?: PodArenaConfig.DEFAULT_MAX_TURNS,
            threads = threads,
        )

        println("=== POD ARENA (${table.id}): ${agentA.name} vs a field of ${agentB.name} — " +
            "${config.groups} groups (${config.games} games) on $threads threads, $setCode, seed $seed ===")
        println("    Parity for ${agentA.name} is a ${pct(config.nullShare)} win share, not 50%.")
        val run = PodArena.run(config) { done, total, group ->
            if (done <= 5 || done % 25 == 0 || done == total) {
                println("  [$done/$total] group ${group.groupId}: ${group.aWins}/${group.games.size} " +
                    "(share ${pct(group.share)})")
            }
        }

        println()
        print(ArenaReport.podSummary(run))
        println("Written to: ${ArenaReport.writePod(run)}")
    }

    test("arena: gauntlet").config(enabled = gauntlet) {
        val agents = loadGauntlet()
        println("=== GAUNTLET: ${agents.joinToString(", ") { it.name }} — $games games per matchup ===")

        val runs = agents.indices.flatMap { i -> (i + 1 until agents.size).map { j -> agents[i] to agents[j] } }
            .map { (a, b) ->
                println("--- ${a.name} vs ${b.name} ---")
                Arena.run(ArenaConfig(a, b, games, seed, setCode, maxTurns, threads)).also {
                    print(ArenaReport.summary(it))
                    println()
                }
            }

        println()
        print(ArenaReport.gauntletSummary(runs, agents.map { it.name }))
        println("Written to: ${ArenaReport.writeGauntlet(runs, agents.map { it.name })}")
    }
})

@Serializable
private data class GauntletFile(val agents: List<String>)

/**
 * Gauntlet membership is a committed resource, not a command-line list: the whole point of a
 * gauntlet is that every version faces the *same* field, and a field you retype each run is not
 * the same field.
 */
internal fun loadGauntlet(): List<ArenaAgent> {
    val resource = ArenaBenchmark::class.java.getResourceAsStream("/arena/gauntlet.json")
        ?: error("Missing ai/src/test/resources/arena/gauntlet.json")
    // ignoreUnknownKeys so the file can carry a "_comment" explaining what a gauntlet is for.
    val json = Json { ignoreUnknownKeys = true }
    val names = resource.use { json.decodeFromString<GauntletFile>(it.readBytes().decodeToString()) }.agents
    require(names.size >= 2) { "A gauntlet needs at least two agents; gauntlet.json lists ${names.size}." }
    return names.map(ArenaAgents::resolve)
}

private fun requireProperty(name: String): String = System.getProperty(name)
    ?: error("-D$name is required. Known agents: ${ArenaAgents.names.joinToString(", ")}")
