package com.wingedsheep.ai.arena

import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The arena's command-line entry point. Both tests are disabled unless explicitly switched on, so
 * a normal `:ai:test` run never pays for them.
 *
 * ```
 * just arena v0 blb-advisors 1000
 * just arena-gauntlet 200
 * just arena-pod ffa3 current v0-blind 150
 * just arena-report benchmarks/arena/<run-dir>     # summary of a partial / killed run
 * ```
 *
 * A head-to-head or pod run appends each finished pair / group to `results.csv` in its run directory
 * as it goes (see [ArenaResultsFile]), and a JVM shutdown (Ctrl-C, SIGTERM) prints and writes the
 * summary of what finished, so a run that is killed keeps its results.
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
    // Per-game wall-clock cap. On by default for these command-line runs (never for the harness
    // tests), because one runaway game must not hold a whole run hostage. 0 turns it off.
    val gameTimeout: Duration? = (System.getProperty("arenaGameTimeoutSec")?.toLongOrNull()
        ?: DEFAULT_GAME_TIMEOUT_SEC).takeIf { it > 0 }?.seconds

    val headToHead = System.getProperty("arena") == "true"
    val gauntlet = System.getProperty("arenaGauntlet") == "true"
    val pod = System.getProperty("arenaPod") == "true"
    val reportPath = System.getProperty("arenaReport")?.takeIf { it.isNotBlank() }

    test("arena: head to head").config(enabled = headToHead) {
        val agentA = ArenaAgents.resolve(requireProperty("arenaA"))
        val agentB = ArenaAgents.resolve(requireProperty("arenaB"))
        val config = ArenaConfig(agentA, agentB, games, seed, setCode, maxTurns, threads, featureOutput, gameTimeout)
        val dir = ArenaReport.runDir(agentA.name, agentB.name)

        println("=== ARENA: ${agentA.name} vs ${agentB.name} — ${config.pairs} pairs " +
            "(${config.pairs * 2} games) on $threads threads, $setCode, seed $seed, " +
            "game timeout ${gameTimeout?.let { "${it.inWholeSeconds}s" } ?: "off"} ===")
        val run = withResultsFile(ArenaResultsFile.open(dir, config)) { results ->
            val clock = TimeSource.Monotonic.markNow()
            Arena.run(config) { done, total, pair ->
                results.append(pair)
                val slowest = pair.games.maxOf { it.durationMs } / 1000.0
                val timedOut = pair.games.count { it.drawReason.startsWith(TableGameRunner.TIMEOUT_REASON) }
                println("  [$done/$total] pair ${pair.pairId}: ${pair.aWins}-${pair.bWins}-${pair.draws} " +
                    "(score ${fmt("%+.1f", pair.score)}) slowest game ${fmt("%.1f", slowest)}s, " +
                    "elapsed ${clock.elapsedNow().inWholeSeconds}s" +
                    if (timedOut > 0) "  <- $timedOut TIMEOUT" else "")
            }
        }

        println()
        print(ArenaReport.summary(run))
        println("Written to: ${ArenaReport.write(run, dir)}")
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
            gameTimeout = gameTimeout,
        )
        val dir = ArenaReport.podRunDir(table.id, agentA.name, agentB.name)

        println("=== POD ARENA (${table.id}): ${agentA.name} vs a field of ${agentB.name} — " +
            "${config.groups} groups (${config.games} games) on $threads threads, $setCode, seed $seed ===")
        println("    Parity for ${agentA.name} is a ${pct(config.nullShare)} win share, not 50%.")
        val run = withResultsFile(ArenaResultsFile.open(dir, config)) { results ->
            val clock = TimeSource.Monotonic.markNow()
            PodArena.run(config) { done, total, group ->
                results.append(group)
                val timedOut = group.games.count {
                    it.outcome.drawReason.startsWith(TableGameRunner.TIMEOUT_REASON)
                }
                println("  [$done/$total] group ${group.groupId}: ${group.aWins}/${group.games.size} " +
                    "(share ${pct(group.share)}), elapsed ${clock.elapsedNow().inWholeSeconds}s" +
                    if (timedOut > 0) "  <- $timedOut TIMEOUT" else "")
            }
        }

        println()
        print(ArenaReport.podSummary(run))
        println("Written to: ${ArenaReport.writePod(run, dir)}")
    }

    test("arena: report from a results file").config(enabled = reportPath != null) {
        val path = ArenaReport.resolveUserPath(requireNotNull(reportPath))
        print(ArenaResultsFile.report(path))
    }

    test("arena: gauntlet").config(enabled = gauntlet) {
        val agents = loadGauntlet()
        println("=== GAUNTLET: ${agents.joinToString(", ") { it.name }} — $games games per matchup ===")

        val runs = agents.indices.flatMap { i -> (i + 1 until agents.size).map { j -> agents[i] to agents[j] } }
            .map { (a, b) ->
                println("--- ${a.name} vs ${b.name} ---")
                Arena.run(ArenaConfig(a, b, games, seed, setCode, maxTurns, threads, gameTimeout = gameTimeout)).also {
                    print(ArenaReport.summary(it))
                    println()
                }
            }

        println()
        print(ArenaReport.gauntletSummary(runs, agents.map { it.name }))
        println("Written to: ${ArenaReport.writeGauntlet(runs, agents.map { it.name })}")
    }
})

/**
 * Default per-game wall-clock cap for command-line runs: ten minutes. A rollout-profile game is
 * ~50x a v0 game, but even those finish in well under a minute on a healthy machine, so this only
 * binds on a genuine runaway.
 */
private const val DEFAULT_GAME_TIMEOUT_SEC = 600L

/**
 * Run [block] with [results] open, printing where it lives first. A JVM shutdown before [block]
 * returns (Ctrl-C, SIGTERM, a killed Gradle) prints and writes `summary.partial.md` from whatever the
 * file holds — the finished pairs are already on disk, this just saves a reader the second command.
 */
private fun <T> withResultsFile(results: ArenaResultsFile, block: (ArenaResultsFile) -> T): T {
    println("    Results (appended per finished unit): ${results.results}")
    println("    Report a partial run with: just arena-report ${results.dir}")
    val hook = Thread {
        runCatching {
            val summary = ArenaResultsFile.report(results.dir)
            File(results.dir, "summary.partial.md").writeText("```\n$summary```\n")
            println()
            println("=== ARENA INTERRUPTED — summary of the finished units ===")
            print(summary)
            println("Written to: ${results.dir}/summary.partial.md")
        }.onFailure { System.err.println("Could not summarise the partial arena run: ${it.message}") }
    }
    Runtime.getRuntime().addShutdownHook(hook)
    try {
        return results.use(block)
    } finally {
        runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
    }
}

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
