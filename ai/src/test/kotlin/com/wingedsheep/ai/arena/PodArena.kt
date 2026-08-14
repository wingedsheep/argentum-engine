package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.buildSeededSealedDeck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.MtgSet
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * A multiplayer arena run: one seat for [agentA], every other seat for [agentB].
 *
 * **This is "A against a field of B", not a symmetric head-to-head**, and it has to be read that
 * way. At a three-seat table the question is "can A out-play two copies of B", whose null
 * hypothesis is a **1/3** win share, not 50%. Splitting a pod evenly between two agents instead
 * would make the answer depend on who draws the kingmaker seat.
 *
 * The estimator is the multiplayer generalization of the head-to-head paired swap: a **rotation
 * group** plays the same decks and the same seed once per cyclic shift of the agent assignment, so
 * A occupies every team position exactly once per group and seat order cancels out.
 *
 * @param groups rotation groups to play. Each group is [TableSetup.teamCount] games.
 * @param seed the run seed. Decks, shuffles, turn order and every "at random" choice derive from it.
 */
data class PodArenaConfig(
    val agentA: ArenaAgent,
    val agentB: ArenaAgent,
    val table: TableSetup,
    val groups: Int,
    val seed: Long = ArenaConfig.DEFAULT_SEED,
    val setCode: String = "BLB",
    /**
     * Cap in turns **per seat**, so one unit here is [TableSetup.seats] player turns. Default 30
     * rather than the head-to-head 50: every seat's turn is a turn of real play, and 30 per seat is
     * already 90-120 of them at this table.
     */
    val maxTurns: Int = DEFAULT_MAX_TURNS,
    /** Per-game runaway backstop. See [TableGameRunner.DEFAULT_MAX_ACTIONS]. */
    val maxActions: Int = TableGameRunner.DEFAULT_MAX_ACTIONS,
    val threads: Int = Runtime.getRuntime().availableProcessors(),
) {
    val gamesPerGroup: Int get() = table.teamCount
    val games: Int get() = groups * gamesPerGroup

    /** A's win share if the two agents were equally strong: one team in [TableSetup.teamCount]. */
    val nullShare: Double get() = 1.0 / table.teamCount

    companion object {
        /**
         * 30 turns per seat, against the head-to-head arena's 50 — at [TableSetup.seats] seats
         * that is already 90-120 turns of real play.
         */
        const val DEFAULT_MAX_TURNS = 30
    }
}

/**
 * One pod game, tagged with the seat agent A sat in.
 *
 * The seat is carried rather than recovered from [TableGameOutcome.seatAgents], which is ambiguous
 * the moment A and B are the same agent — and the A-vs-A mirror is exactly the run that has to come
 * out at the null share.
 */
data class PodGame(val aSeat: Int, val outcome: TableGameOutcome) {
    val aWon: Boolean get() = outcome.seatWon(aSeat)
}

/**
 * One rotation group: the same decks and the same seed played once per cyclic shift of the agent
 * assignment, so agent A sits on each team exactly once.
 */
data class PodGroup(val groupId: Int, val games: List<PodGame>) {

    /** Games in this group A's team won. Draws and unfinished games count for nobody. */
    val aWins: Int = games.count { it.aWon }

    /** A's win share within the group. Compare against [PodArenaConfig.nullShare], not 50%. */
    val share: Double = if (games.isEmpty()) 0.0 else aWins.toDouble() / games.size
}

/** A completed pod run: the raw groups, the derived statistics, and the wall clock it took. */
data class PodArenaRun(
    val config: PodArenaConfig,
    val groups: List<PodGroup>,
    val stats: PodArenaStats,
    val wallClock: Duration,
)

object PodArena {

    fun run(
        config: PodArenaConfig,
        onProgress: (completed: Int, total: Int, group: PodGroup) -> Unit = { _, _, _ -> },
    ): PodArenaRun {
        val set = MtgSetCatalog.requireByCode(config.setCode)
        val registry = CardRegistry().apply {
            register(set.cards)
            register(set.basicLands)
        }

        val pool = Executors.newFixedThreadPool(config.threads)
        try {
            val completionService = ExecutorCompletionService<PodGroup>(pool)
            val finished = AtomicInteger(0)
            var groups: List<PodGroup>
            val wallClock = measureTime {
                // Submitted as whole groups, never as individual games: a partial run then always
                // holds complete rotations, so an interrupted pod arena is still unbiased.
                (1..config.groups).forEach { groupId ->
                    completionService.submit { playGroup(registry, set, config, groupId) }
                }
                groups = (1..config.groups).map {
                    completionService.take().get().also { group ->
                        onProgress(finished.incrementAndGet(), config.groups, group)
                    }
                }.sortedBy { it.groupId }
            }
            return PodArenaRun(
                config = config,
                groups = groups,
                stats = PodArenaStats.of(config, groups),
                wallClock = wallClock,
            )
        } finally {
            pool.shutdown()
        }
    }

    /**
     * One rotation group.
     *
     * Agent A starts on team 0 and shifts one whole team per rotation; every other seat is agent B.
     * The seed is per group, not per game, so every rotation shares the same libraries and the same
     * turn order — the only thing that changes is where A is sitting.
     */
    private fun playGroup(
        registry: CardRegistry,
        set: MtgSet,
        config: PodArenaConfig,
        groupId: Int,
    ): PodGroup {
        val groupSeed = mixSeed(config.seed, groupId.toLong())
        // One decklist for the whole table, as in the head-to-head arena: identical decks are the
        // lowest-variance design, and the seats still draw different shuffles of it.
        val deck = buildSeededSealedDeck(set.cards, Random(groupSeed))
        val table = config.table
        val decks = List(table.seats) { deck }

        val games = (0 until table.teamCount).map { rotation ->
            val aSeat = rotation * table.seatsPerTeam
            val agents = (0 until table.seats).map { seat ->
                if (seat == aSeat) config.agentA else config.agentB
            }
            PodGame(
                aSeat = aSeat,
                outcome = TableGameRunner.play(
                    registry, table, agents, decks,
                    seed = groupSeed, groupId = groupId, rotation = rotation,
                    maxTurns = config.maxTurns, maxActions = config.maxActions,
                ),
            )
        }
        return PodGroup(groupId, games)
    }
}
