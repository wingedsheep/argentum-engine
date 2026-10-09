package com.wingedsheep.ai.arena

import java.io.File
import kotlin.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Renders arena results — to stdout while a run is in progress, and to
 * `benchmarks/arena/<timestamp>/` afterwards so a claim in a PR body has a file behind it.
 *
 * `results.csv` is one row per game (never per pair): the pair-level statistics are derived, and a
 * CSV that only carried them could not be re-analysed a different way later.
 */
object ArenaReport {

    private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    // ─────────────────────────────────────────────────────────────────────────
    // Head-to-head
    // ─────────────────────────────────────────────────────────────────────────

    fun summary(run: ArenaRun): String = summary(run.stats, meta(run))

    internal fun meta(run: ArenaRun) = ArenaRunMeta(
        run.config.setCode, run.config.seed, run.config.threads, run.wallClock,
        run.config.pairs, run.config.gameTimeout,
    )

    /** The summary from the statistics alone — what [ArenaResultsFile.report] renders a partial run with. */
    fun summary(s: ArenaStats, meta: ArenaRunMeta): String = buildString {
        appendLine("--- ARENA: ${s.agentA} vs ${s.agentB} ---")
        appendPartial(s.pairs, meta.plannedUnits, "pairs")
        appendLine("Games:        ${s.games} (${s.pairs} pairs), set=${meta.setCode}, seed=${meta.seed}")
        appendLine("Record:       ${s.aWins}W-${s.bWins}L-${s.draws}D for ${s.agentA}")
        appendLine()
        // Paired first: it is the estimator, and quoting the unpaired number first invites
        // reading the wrong interval.
        appendLine("Pair score:   ${fmt("%+.3f", s.meanPairScore)}  CI ${s.pairScoreCi}   (0 = parity)")
        appendLine("Pair win %:   ${pct(s.pairWinShare)}  CI [${pct(s.pairWinShareCi.low)}, ${pct(s.pairWinShareCi.high)}]  <- the merge gate")
        appendLine("Game score %: ${pct(s.gameScoreRate)}  Wilson [${pct(s.gameScoreCi.low)}, ${pct(s.gameScoreCi.high)}]  (unpaired, draws = 1/2)")
        appendLine()
        appendLine("Seat 0 wins:  ${s.seat0Wins} / ${s.games} (${pct(s.seat0WinRate)}) — first-player advantage, cancelled by pairing")
        appendLine("Completed:    ${s.completedGames} / ${s.games} (${pct(s.completionRate)})")
        appendTimeouts(s.timeouts, meta.gameTimeout)
        appendLine("Illegal acts: ${s.illegalActions.values.sum()} (actions the processor rejected — should be 0)")
        if (s.drawReasons.isNotEmpty()) {
            appendLine("Unfinished:   " + s.drawReasons.entries.joinToString(", ") { "${it.value}x ${it.key}" })
        }
        if (s.illegalActions.isNotEmpty()) {
            appendLine()
            appendLine("--- REJECTED AI ACTIONS (distinct) — each is a bug, not noise ---")
            s.illegalActions.forEach { (message, count) -> appendLine("  [${count}x] $message") }
        }
        if (s.exceptions.isNotEmpty()) {
            appendLine()
            appendLine("--- ENGINE EXCEPTIONS (distinct) ---")
            s.exceptions.forEach { (message, count) -> appendLine("  [${count}x] $message") }
        }
        appendLine()
        appendLine("Avg turns:    ${fmt("%.1f", s.meanTurns)}   avg actions: ${fmt("%.0f", s.meanActions)}   " +
            "avg game: ${fmt("%.0f", s.meanGameMs)}ms")
        appendLine("Wall clock:   ${meta.wallClock.inWholeSeconds}s on ${meta.threads} threads " +
            "(${fmt("%.1f", s.games * 1000.0 / meta.wallClock.inWholeMilliseconds.coerceAtLeast(1))} games/sec)")
        appendLine()
        appendLine(verdict(s))
    }

    /** The promotion rule, stated as a sentence so a report cannot be misread as more than it is. */
    private fun verdict(s: ArenaStats): String = when {
        s.games < 100 ->
            "VERDICT: ${s.games} games is a smoke test, not evidence. The merge gate is 1,000."
        s.beatsOpponent ->
            "VERDICT: ${s.agentA} beats ${s.agentB} — lower CI bound ${pct(s.pairWinShareCi.low)} is above parity."
        s.pairWinShareCi.high < 0.5 ->
            "VERDICT: ${s.agentA} LOSES to ${s.agentB} — upper CI bound ${pct(s.pairWinShareCi.high)} is below parity."
        else ->
            "VERDICT: not distinguishable. CI [${pct(s.pairWinShareCi.low)}, ${pct(s.pairWinShareCi.high)}] " +
                "spans parity — this is not a demonstrated improvement."
    }

    const val PAIR_HEADER = "pair,game,seat0_agent,seat1_agent,seed,winner_seat,turns,actions,duration_ms," +
        "seat0_life,seat1_life,completed,illegal_actions,draw_reason,exception"

    /** The `results.csv` rows of one pair — one per game. */
    fun pairRows(pair: ArenaPair): List<String> = pair.games.map { game ->
        listOf(
            game.pairId, game.gameIndex, game.seat0Agent, game.seat1Agent, game.seed,
            game.winnerSeat ?: "", game.turns, game.actions, game.durationMs,
            game.seat0Life, game.seat1Life, game.completed, game.illegalActions.values.sum(),
            csv(game.drawReason), csv(game.exception ?: ""),
        ).joinToString(",")
    }

    /** The run directory for a head-to-head run: `benchmarks/arena/<timestamp>-<a>-vs-<b>/`. */
    fun runDir(agentA: String, agentB: String): File = outputDir("$agentA-vs-$agentB")

    /**
     * Writes `results.csv` (sorted by pair, replacing the completion-order file the run appended
     * to) + `summary.md` into [dir].
     */
    fun write(run: ArenaRun, dir: File = runDir(run.stats.agentA, run.stats.agentB)): File {
        dir.mkdirs()
        File(dir, ArenaResultsFile.RESULTS).writeText(
            (listOf(PAIR_HEADER) + run.pairs.flatMap(::pairRows)).joinToString("") { it + "\n" }
        )
        File(dir, "summary.md").writeText("```\n${summary(run)}```\n")
        return dir
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multiplayer pod
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A pod run. Every number is quoted against the **null share** rather than 50%: one seat in a
     * field of N is a 1/N proposition, and reading a 34% pod result as "loses badly" is the single
     * most likely way to misread this report.
     */
    fun podSummary(run: PodArenaRun): String = podSummary(
        run.stats,
        ArenaRunMeta(
            run.config.setCode, run.config.seed, run.config.threads, run.wallClock,
            run.config.groups, run.config.gameTimeout,
        ),
    )

    /** The pod summary from the statistics alone — what [ArenaResultsFile.report] renders. */
    fun podSummary(s: PodArenaStats, meta: ArenaRunMeta): String = buildString {
        val gamesPerGroup = TableSetup.resolve(s.table).teamCount
        appendLine("--- POD ARENA (${s.table}): ${s.agentA} vs a field of ${s.agentB} ---")
        appendPartial(s.groups, meta.plannedUnits, "rotation groups")
        appendLine("Games:        ${s.games} (${s.groups} rotation groups x $gamesPerGroup), " +
            "set=${meta.setCode}, seed=${meta.seed}")
        appendLine("Record:       ${s.aWins} wins for ${s.agentA}, ${s.games - s.aWins - s.noWinner} " +
            "for the field, ${s.noWinner} with no winner")
        appendLine()
        appendLine("Win share:    ${pct(s.winShare)}  CI [${pct(s.winShareCi.low)}, ${pct(s.winShareCi.high)}]" +
            "   vs null ${pct(s.nullShare)}  <- the gate")
        appendLine("Decisive:     ${pct(s.decisiveWinShare)} of the ${s.games - s.noWinner} games that " +
            "produced a winner (a timeout is not a result for anyone)")
        appendLine()
        appendLine("Wins by team position: ${s.winsByTeamPosition.joinToString(", ")} " +
            "— turn-order advantage, cancelled by the rotation")
        appendLine("Completed:    ${s.completedGames} / ${s.games} (${pct(s.completionRate)})")
        appendTimeouts(s.timeouts, meta.gameTimeout)
        appendLine("Illegal acts: ${s.illegalActions.values.sum()} (actions the processor rejected)")
        if (s.drawReasons.isNotEmpty()) {
            appendLine("Unfinished:   " + s.drawReasons.entries.joinToString(", ") { "${it.value}x ${it.key}" })
        }
        if (s.illegalActions.isNotEmpty()) {
            appendLine()
            appendLine("--- REJECTED AI ACTIONS (distinct) — each is a bug, not noise ---")
            s.illegalActions.forEach { (message, count) -> appendLine("  [${count}x] $message") }
        }
        if (s.exceptions.isNotEmpty()) {
            appendLine()
            appendLine("--- ENGINE EXCEPTIONS (distinct) ---")
            s.exceptions.forEach { (message, count) -> appendLine("  [${count}x] $message") }
        }
        appendLine()
        appendLine("Avg turns:    ${fmt("%.1f", s.meanTurns)}   avg actions: ${fmt("%.0f", s.meanActions)}   " +
            "avg game: ${fmt("%.0f", s.meanGameMs)}ms")
        appendLine("Wall clock:   ${meta.wallClock.inWholeSeconds}s on ${meta.threads} threads")
        appendLine()
        appendLine(podVerdict(s))
    }

    private fun podVerdict(s: PodArenaStats): String = when {
        s.beatsField ->
            "VERDICT: ${s.agentA} beats a field of ${s.agentB} at ${s.table} — lower CI bound " +
                "${pct(s.winShareCi.low)} is above the ${pct(s.nullShare)} null."
        s.winShareCi.high < s.nullShare ->
            "VERDICT: ${s.agentA} LOSES to a field of ${s.agentB} at ${s.table} — upper CI bound " +
                "${pct(s.winShareCi.high)} is below the ${pct(s.nullShare)} null."
        else ->
            "VERDICT: not distinguishable. CI [${pct(s.winShareCi.low)}, ${pct(s.winShareCi.high)}] " +
                "spans the ${pct(s.nullShare)} null — this is not a demonstrated improvement."
    }

    const val GROUP_HEADER = "group,rotation,table,a_seat,seat_agents,seed,winner_seat,winner_team,a_won," +
        "turns,actions,duration_ms,life_by_seat,completed,illegal_actions,draw_reason,exception"

    /** The `results.csv` rows of one rotation group — one per game. */
    fun groupRows(group: PodGroup): List<String> = group.games.map { game ->
        val o = game.outcome
        listOf(
            o.groupId, o.rotation, o.setup.id, game.aSeat, o.seatAgents.joinToString("|"),
            o.seed, o.winnerSeat ?: "", o.winnerTeam ?: "", game.aWon,
            o.turns, o.actions, o.durationMs, o.lifeBySeat.joinToString("|"),
            o.completed, o.illegalActions.values.sum(),
            csv(o.drawReason), csv(o.exception ?: ""),
        ).joinToString(",")
    }

    /** The run directory for a pod run: `benchmarks/arena/<timestamp>-pod-<table>-<a>-vs-<b>/`. */
    fun podRunDir(table: String, agentA: String, agentB: String): File = outputDir("pod-$table-$agentA-vs-$agentB")

    /** Writes `results.csv` (sorted by group) + `summary.md` into [dir]. */
    fun writePod(run: PodArenaRun, dir: File = podRunDir(run.stats.table, run.stats.agentA, run.stats.agentB)): File {
        dir.mkdirs()
        File(dir, ArenaResultsFile.RESULTS).writeText(
            (listOf(GROUP_HEADER) + run.groups.flatMap(::groupRows)).joinToString("") { it + "\n" }
        )
        File(dir, "summary.md").writeText("```\n${podSummary(run)}```\n")
        return dir
    }

    /** A loud first line when the run did not finish — a partial report must never pass for a whole one. */
    private fun StringBuilder.appendPartial(finished: Int, planned: Int, unit: String) {
        if (finished < planned) {
            appendLine("PARTIAL RUN:  $finished of $planned $unit finished — every finished one is whole, " +
                "so this is an unbiased but smaller sample.")
        }
    }

    /**
     * Wall-clock timeouts, stated whenever the cap was on. They are draws for scoring, but unlike
     * the turn and action caps they depend on machine load: a timed-out game is not reproducible
     * from its seed, so a reader has to know how many there were.
     */
    private fun StringBuilder.appendTimeouts(timeouts: Int, cap: Duration?) {
        if (cap == null && timeouts == 0) return
        val limit = cap?.let { "${it.inWholeSeconds}s" } ?: "per-game"
        if (timeouts == 0) {
            appendLine("Timeouts:     0 (per-game wall-clock cap $limit)")
        } else {
            appendLine("TIMEOUTS:     $timeouts game(s) hit the $limit wall-clock cap — scored as draws, and " +
                "NOT reproducible from their seed (the cap depends on machine load)")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gauntlet
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The full pairwise matrix plus Bradley-Terry Elo.
     *
     * The matrix is the deliverable and the Elo is the convenience: a single rating cannot express
     * a rock-paper-scissors cycle, and MTG agents produce them routinely.
     */
    fun gauntletSummary(runs: List<ArenaRun>, agents: List<String>): String = buildString {
        val byPair = runs.associateBy { it.stats.agentA to it.stats.agentB }
        appendLine("--- GAUNTLET: ${agents.size} agents, ${runs.size} matchups ---")
        appendLine()
        val width = maxOf(12, agents.maxOf { it.length } + 1)
        append("".padEnd(width))
        agents.forEach { append(it.take(width - 1).padStart(width)) }
        appendLine()
        for (row in agents) {
            append(row.padEnd(width))
            for (column in agents) {
                val cell = when {
                    row == column -> "-"
                    else -> byPair[row to column]?.let { pct(it.stats.pairWinShare) }
                        ?: byPair[column to row]?.let { pct(1.0 - it.stats.pairWinShare) }
                        ?: "?"
                }
                append(cell.padStart(width))
            }
            appendLine()
        }
        appendLine()
        appendLine("Cell = row's pair win share vs column. 50% is parity.")
        appendLine()

        val matchups = runs.flatMap { run ->
            val s = run.stats
            listOf(
                Matchup(s.agentA, s.agentB, s.aWins + s.draws / 2.0, s.games),
                Matchup(s.agentB, s.agentA, s.bWins + s.draws / 2.0, s.games),
            )
        }
        val elo = BradleyTerry.elo(agents, matchups)
        appendLine("--- BRADLEY-TERRY ELO (report alongside the matrix, never instead of it) ---")
        elo.entries.sortedByDescending { it.value }.forEach { (agent, rating) ->
            appendLine("  ${agent.padEnd(width)} ${fmt("%.0f", rating)}")
        }
        appendLine()
        appendLine("Promotion rule: a new version must beat BOTH v0 and the version before it, and")
        appendLine("must not lose to any gauntlet member worse than 45%.")
    }

    fun writeGauntlet(runs: List<ArenaRun>, agents: List<String>): File {
        val dir = outputDir("gauntlet")
        File(dir, "matrix.md").writeText("```\n${gauntletSummary(runs, agents)}```\n")
        File(dir, "results.csv").writeText(buildString {
            appendLine("agent_a,agent_b,games,a_wins,b_wins,draws,pair_win_share,ci_low,ci_high")
            for (run in runs) {
                val s = run.stats
                appendLine("${s.agentA},${s.agentB},${s.games},${s.aWins},${s.bWins},${s.draws}," +
                    "${fmt("%.4f", s.pairWinShare)},${fmt("%.4f", s.pairWinShareCi.low)}," +
                    "${fmt("%.4f", s.pairWinShareCi.high)}")
            }
        })
        return dir
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun csv(value: String) = value.replace(',', ';').replace('\n', ' ')

    /**
     * Resolve a path a person typed: as given if it exists, else against the repo root (Gradle
     * runs tests from the module directory, so a repo-relative path would otherwise miss).
     */
    fun resolveUserPath(path: String): File {
        val file = File(path)
        return if (file.isAbsolute || file.exists()) file else File(repoRoot(), path)
    }

    private fun outputDir(label: String): File {
        val stamp = LocalDateTime.now().format(TIMESTAMP)
        return File(repoRoot(), "benchmarks/arena/$stamp-$label").apply { mkdirs() }
    }

    /**
     * Gradle runs tests with the *module* directory as the working directory, so results would
     * otherwise land in `ai/benchmarks/`. Walk up to the build root instead.
     */
    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        return File(System.getProperty("user.dir"))
    }
}
