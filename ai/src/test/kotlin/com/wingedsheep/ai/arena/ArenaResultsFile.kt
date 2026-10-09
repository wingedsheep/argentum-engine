package com.wingedsheep.ai.arena

import java.io.BufferedWriter
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * What a summary needs besides the statistics. Known in full for a live run, and recoverable from
 * the `run.properties` a run writes at startup — which is what lets a report be rebuilt from a run
 * that never finished.
 *
 * @param plannedUnits pairs (head-to-head) or rotation groups (pod) the run was asked for. A report
 *   with fewer finished units than this says PARTIAL in its header.
 */
data class ArenaRunMeta(
    val setCode: String,
    val seed: Long,
    val threads: Int,
    val wallClock: Duration,
    val plannedUnits: Int,
    val gameTimeout: Duration?,
)

/**
 * The incremental, recoverable half of an arena run.
 *
 * A run writes `run.properties` (who, where, what seed) and the header of `results.csv` before the
 * first game, then appends each pair (head-to-head) or rotation group (pod) the moment it completes,
 * flushed per unit. A run that is killed — or that never finishes because one game will not end —
 * therefore leaves every finished unit on disk, and [report] rebuilds the full summary from it.
 *
 * The unit is still the pair / the group: rows are written a whole unit at a time and the reader
 * drops any unit it cannot see completely (a torn last line), so a partial file always holds whole
 * pairs and stays an unbiased sample.
 *
 * Rows land in completion order; the final [ArenaReport.write] rewrites the file sorted by unit.
 */
class ArenaResultsFile private constructor(val dir: File, private val writer: BufferedWriter) : AutoCloseable {

    val results: File get() = File(dir, RESULTS)

    /** Append one finished pair and flush. Called from the collecting thread only. */
    @Synchronized
    fun append(pair: ArenaPair) = appendLines(ArenaReport.pairRows(pair))

    /** Append one finished rotation group and flush. */
    @Synchronized
    fun append(group: PodGroup) = appendLines(ArenaReport.groupRows(group))

    private fun appendLines(lines: List<String>) {
        // One write per unit, so a kill between units never leaves half a pair behind.
        writer.write(lines.joinToString("") { it + "\n" })
        writer.flush()
    }

    @Synchronized
    override fun close() = writer.close()

    companion object {
        const val RESULTS = "results.csv"
        const val META = "run.properties"

        private const val KIND_HEAD_TO_HEAD = "head-to-head"
        private const val KIND_POD = "pod"

        /** Start a head-to-head results file in [dir]. */
        fun open(dir: File, config: ArenaConfig): ArenaResultsFile {
            writeMeta(dir, KIND_HEAD_TO_HEAD, config.agentA.name, config.agentB.name, null,
                config.setCode, config.seed, config.threads, config.pairs, config.gameTimeout)
            return start(dir, ArenaReport.PAIR_HEADER)
        }

        /** Start a pod results file in [dir]. */
        fun open(dir: File, config: PodArenaConfig): ArenaResultsFile {
            writeMeta(dir, KIND_POD, config.agentA.name, config.agentB.name, config.table.id,
                config.setCode, config.seed, config.threads, config.groups, config.gameTimeout)
            return start(dir, ArenaReport.GROUP_HEADER)
        }

        private fun start(dir: File, header: String): ArenaResultsFile {
            dir.mkdirs()
            val writer = File(dir, RESULTS).bufferedWriter()
            writer.write(header + "\n")
            writer.flush()
            return ArenaResultsFile(dir, writer)
        }

        private fun writeMeta(
            dir: File, kind: String, agentA: String, agentB: String, table: String?,
            setCode: String, seed: Long, threads: Int, planned: Int, gameTimeout: Duration?,
        ) {
            dir.mkdirs()
            val props = Properties().apply {
                setProperty("kind", kind)
                setProperty("agentA", agentA)
                setProperty("agentB", agentB)
                table?.let { setProperty("table", it) }
                setProperty("setCode", setCode)
                setProperty("seed", seed.toString())
                setProperty("threads", threads.toString())
                setProperty("planned", planned.toString())
                setProperty("gameTimeoutSec", gameTimeout?.inWholeSeconds?.toString() ?: "0")
                setProperty("startedAtEpochMs", System.currentTimeMillis().toString())
            }
            File(dir, META).outputStream().use { props.store(it, "arena run — see docs/ai/measurement.md") }
        }

        /**
         * Rebuild the summary of whatever a run in [path] finished. [path] is the run directory or
         * its `results.csv`. Works on a finished run too; on a partial one the header says PARTIAL.
         */
        fun report(path: File): String {
            val dir = if (path.isDirectory) path else path.absoluteFile.parentFile
            val props = Properties().apply {
                val meta = File(dir, META)
                require(meta.isFile) { "No $META next to the results in $dir — not an arena run directory." }
                meta.inputStream().use { load(it) }
            }
            val results = File(dir, RESULTS)
            val lines = results.readLines().filter { it.isNotBlank() }
            require(lines.isNotEmpty()) { "$results is empty." }

            val started = props.getProperty("startedAtEpochMs")?.toLongOrNull() ?: results.lastModified()
            val meta = ArenaRunMeta(
                setCode = props.getProperty("setCode"),
                seed = props.getProperty("seed").toLong(),
                threads = props.getProperty("threads").toInt(),
                wallClock = (results.lastModified() - started).coerceAtLeast(0).milliseconds,
                plannedUnits = props.getProperty("planned").toInt(),
                gameTimeout = props.getProperty("gameTimeoutSec")?.toLongOrNull()?.takeIf { it > 0 }?.seconds,
            )
            val agentA = props.getProperty("agentA")
            val agentB = props.getProperty("agentB")
            val rows = lines.drop(1)
            return when (props.getProperty("kind")) {
                KIND_POD -> {
                    val table = TableSetup.resolve(props.getProperty("table"))
                    val groups = parseGroups(rows, table)
                    ArenaReport.podSummary(PodArenaStats.of(agentA, agentB, table, groups), meta)
                }
                else -> ArenaReport.summary(ArenaStats.of(agentA, agentB, parsePairs(rows)), meta)
            }
        }

        /**
         * Head-to-head rows back into pairs. A pair missing a game, or any malformed row, is
         * dropped: a torn final write must never become half a pair in the sample.
         */
        internal fun parsePairs(rows: List<String>): List<ArenaPair> =
            rows.mapNotNull(::parseGame)
                .groupBy { it.pairId }
                .mapNotNull { (pairId, games) ->
                    val a = games.firstOrNull { it.gameIndex == 0 }
                    val b = games.firstOrNull { it.gameIndex == 1 }
                    if (a != null && b != null) ArenaPair(pairId, a, b) else null
                }
                .sortedBy { it.pairId }

        private fun parseGame(row: String): ArenaGameOutcome? = runCatching {
            val f = row.split(',')
            if (f.size != ArenaReport.PAIR_HEADER.split(',').size) return null
            ArenaGameOutcome(
                pairId = f[0].toInt(),
                gameIndex = f[1].toInt(),
                seat0Agent = f[2],
                seat1Agent = f[3],
                seed = f[4].toLong(),
                winnerSeat = f[5].toIntOrNull(),
                turns = f[6].toInt(),
                actions = f[7].toInt(),
                durationMs = f[8].toLong(),
                seat0Life = f[9].toInt(),
                seat1Life = f[10].toInt(),
                completed = f[11].toBooleanStrict(),
                illegalActions = illegalPlaceholder(f[12].toInt()),
                drawReason = f[13],
                exception = f[14].ifEmpty { null },
                actionStreamHash = null,
            )
        }.getOrNull()

        /** Pod rows back into rotation groups. A group missing any rotation is dropped. */
        internal fun parseGroups(rows: List<String>, table: TableSetup): List<PodGroup> =
            rows.mapNotNull { parsePodGame(it, table) }
                .groupBy { it.outcome.groupId }
                .mapNotNull { (groupId, games) ->
                    val byRotation = games.associateBy { it.outcome.rotation }
                    if ((0 until table.teamCount).all { it in byRotation }) {
                        PodGroup(groupId, (0 until table.teamCount).map { byRotation.getValue(it) })
                    } else null
                }
                .sortedBy { it.groupId }

        private fun parsePodGame(row: String, table: TableSetup): PodGame? = runCatching {
            val f = row.split(',')
            if (f.size != ArenaReport.GROUP_HEADER.split(',').size) return null
            PodGame(
                aSeat = f[3].toInt(),
                outcome = TableGameOutcome(
                    groupId = f[0].toInt(),
                    rotation = f[1].toInt(),
                    setup = table,
                    seatAgents = f[4].split('|'),
                    seed = f[5].toLong(),
                    winnerSeat = f[6].toIntOrNull(),
                    winnerTeam = f[7].toIntOrNull(),
                    turns = f[9].toInt(),
                    actions = f[10].toInt(),
                    durationMs = f[11].toLong(),
                    lifeBySeat = f[12].split('|').map { it.toInt() },
                    completed = f[13].toBooleanStrict(),
                    illegalActions = illegalPlaceholder(f[14].toInt()),
                    drawReason = f[15],
                    exception = f[16].ifEmpty { null },
                    actionStreamHash = null,
                ),
            )
        }.getOrNull()

        /** `results.csv` keeps only the count of rejected actions, not their messages. */
        private fun illegalPlaceholder(count: Int): Map<String, Int> =
            if (count > 0) mapOf("(message not kept in results.csv — rerun the pair to see it)" to count) else emptyMap()
    }
}
