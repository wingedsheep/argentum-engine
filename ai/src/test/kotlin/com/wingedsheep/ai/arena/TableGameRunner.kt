package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.safeFallbackAction
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.AttackMode
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import java.security.MessageDigest
import kotlin.time.measureTime

/**
 * The shape of an arena table: how many seats, in what format, teamed how.
 *
 * A table is data, not a code path — exactly as [Format] is for the engine. [ArenaConfig] plays at
 * [HEADS_UP]; [PodArenaConfig] plays at one of the multiplayer tables.
 *
 * @property teams seat indices per team, or null when every seat is its own team. Members of a team
 *   must be **contiguous and equal-sized**: [PodArena] rotates the agent assignment by whole teams,
 *   which is only a clean rotation when the blocks line up.
 */
data class TableSetup(
    val id: String,
    val seats: Int,
    val format: Format = Format.Standard,
    val teams: List<List<Int>>? = null,
    val attackMode: AttackMode = AttackMode.MULTIPLE,
) {
    /** Team index per seat. Every seat is its own team when [teams] is null. */
    val teamOfSeat: List<Int> = (0 until seats).map { seat ->
        teams?.indexOfFirst { seat in it }?.takeIf { it >= 0 } ?: seat
    }

    val teamCount: Int get() = teamOfSeat.distinct().size

    /** Seats per team; the rotation step [PodArena] shifts an assignment by. */
    val seatsPerTeam: Int get() = seats / teamCount

    init {
        require(seats >= 2) { "A table needs at least two seats; $id has $seats." }
        require(seats % teamCount == 0) {
            "$id has $seats seats across $teamCount teams — teams must be equal-sized so the " +
                "seat rotation stays a rotation."
        }
    }

    companion object {
        /** The two-player table the head-to-head arena has always played at. */
        val HEADS_UP = TableSetup("1v1", seats = 2)

        /** Three-player free-for-all. The smallest table where "which opponent?" is a real question. */
        val FFA3 = TableSetup("ffa3", seats = 3)

        /** Four-player free-for-all. */
        val FFA4 = TableSetup("ffa4", seats = 4)

        /**
         * Two-Headed Giant (CR 810): two teams of two, shared life, shared turns. The table that
         * exercises the *teammate* half of the evaluator — a side whose board, hands and life pool
         * span two seats.
         */
        val TWO_HEADED_GIANT = TableSetup(
            id = "2hg",
            seats = 4,
            format = Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3)),
        )

        val all: List<TableSetup> = listOf(HEADS_UP, FFA3, FFA4, TWO_HEADED_GIANT)

        fun resolve(id: String): TableSetup = all.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException(
                "Unknown table \"$id\". Known tables: ${all.joinToString(", ") { it.id }}"
            )
    }
}

/**
 * One game at an arena table, recorded **by seat**, never by agent.
 *
 * Seat-indexed is the whole point: a rotation group plays the same game once per seat assignment,
 * and anything that reads "did agent A win" before the group is assembled is how a seat bias sneaks
 * in.
 */
data class TableGameOutcome(
    val groupId: Int,
    /** Which rotation of the agent assignment this game is: 0 is the base assignment. */
    val rotation: Int,
    val setup: TableSetup,
    /** Agent name per seat. */
    val seatAgents: List<String>,
    val seed: Long,
    /** Seat of the winning player, or null for a draw / unfinished game. */
    val winnerSeat: Int?,
    /** Team of the winning player. Every seat on it won (CR 810.8a). */
    val winnerTeam: Int?,
    val turns: Int,
    val actions: Int,
    val durationMs: Long,
    /** Life per seat, read through the team's pool where there is one (CR 810.9a). */
    val lifeBySeat: List<Int>,
    val completed: Boolean,
    /** Why the game ended without a winner. Empty when it ended normally. */
    val drawReason: String,
    /** A thrown engine exception, if any — the arena doubles as a crash finder at scale. */
    val exception: String?,
    /**
     * Actions the AI proposed that the processor rejected, recovered by [safeFallbackAction].
     * Keyed by `"<ActionType>: <error>"` so the report can histogram them.
     */
    val illegalActions: Map<String, Int>,
    /** Set only when the runner was asked to record one. */
    val actionStreamHash: String?,
) {
    /** Whether the seat at [seat] was on the winning team. */
    fun seatWon(seat: Int): Boolean = winnerTeam != null && setup.teamOfSeat[seat] == winnerTeam
}

/** Optional offline observer. The production/ordinary arena path passes null and allocates nothing. */
interface ArenaTrainingObserver {
    fun gameStarted(state: GameState, seats: List<EntityId>) {}
    fun quietRoot(state: GameState, actingPlayer: EntityId) {}
    fun action(action: GameAction) {}
    fun decision(playerId: EntityId, response: DecisionResponse) {}
}

/**
 * Plays one game at an arbitrary [TableSetup]: N agents, one deck each, one seed.
 *
 * The single game loop for the whole arena — [ArenaGameRunner] is a two-seat wrapper over it, so a
 * head-to-head game and a pod game can never drift apart in their stuck detection, draw taxonomy or
 * illegal-action recovery.
 *
 * Built from `AdvisorBenchmark.playAdvisorGame`, not `AIBenchmark.playGame` — the latter
 * round-trips every action through `ClientStateTransformer.transform` + `LegalActionEnricher.enrich`
 * to satisfy the server's DTO interface, which is pure overhead for engine-vs-engine.
 */
object TableGameRunner {

    /**
     * Wedge detection: this many actions without the turn passing to another player.
     *
     * `GameState.turnNumber` counts player turns, so it is a sound clock here — it advances on every
     * turn regardless of seat count or who has been eliminated. (It was a *round* counter until the
     * turn-number fix, which froze outright once the opening seat was knocked out and made every
     * healthy three-way endgame read as wedged forever; this detector counted handovers itself to
     * dodge that.)
     *
     * 300 per player turn is deliberately looser than 300 per round (two player turns in a duel):
     * a false "stuck" silently discards a real result, which is worse than taking a few hundred
     * extra actions to notice a genuine wedge.
     */
    private const val STUCK_ACTIONS_PER_TURN = 300

    /**
     * Runaway backstop: no single game may cost more than this many actions.
     *
     * A duel is a few hundred actions, so this never binds there. A pod is a different animal —
     * three or four boards keep growing, the Strategist's per-decision cost grows with them, and a
     * late free-for-all can spend tens of thousands of actions failing to close a game out. That is
     * a legitimate result (nobody could win) but an illegitimate way to spend twenty minutes, so
     * runs that want a tighter leash pass their own value.
     */
    const val DEFAULT_MAX_ACTIONS = 20_000

    fun play(
        registry: CardRegistry,
        setup: TableSetup,
        agents: List<ArenaAgent>,
        decks: List<Deck>,
        seed: Long,
        groupId: Int,
        rotation: Int,
        /** Cap in turns *per seat* — a duel gets `2 * maxTurns` player turns, a four-pod `4 *`. */
        maxTurns: Int = 50,
        maxActions: Int = DEFAULT_MAX_ACTIONS,
        /** Hash every action and decision into [TableGameOutcome.actionStreamHash]. Off by
         *  default: it costs a string per action, and only `FrozenBaselineTest` needs it. */
        recordActionStream: Boolean = false,
        featureCollector: ArenaFeatureCollector? = null,
        trainingObserver: ArenaTrainingObserver? = null,
    ): TableGameOutcome {
        require(agents.size == setup.seats && decks.size == setup.seats) {
            "${setup.id} has ${setup.seats} seats but got ${agents.size} agents / ${decks.size} decks."
        }
        val processor = ActionProcessor(registry)
        val enumerator = LegalActionEnumerator.create(registry)
        val initializer = GameInitializer(registry)

        val init = initializer.initializeGame(
            GameConfig(
                players = decks.mapIndexed { seat, deck -> PlayerConfig("Seat$seat", deck) },
                // Mulligans are skipped so a rerun at the same seed is the same game. That puts
                // mulligan quality out of test — schedule a separate mulligan A/B rather than
                // pretending this measures it.
                skipMulligans = true,
                startingPlayerIndex = 0,
                seed = seed,
                format = setup.format,
                teams = setup.teams,
                attackMode = setup.attackMode,
            )
        )

        // startingPlayerIndex = 0 keeps turnOrder aligned with the configured player order, so
        // seat index and turnOrder index are the same thing.
        val seatIds = init.state.turnOrder
        val decklistsByPlayer = seatIds.mapIndexed { seat, id ->
            val deck = decks[seat]
            val names = if (deck.cardEntries.isNotEmpty()) deck.cardEntries.map { it.name } else deck.cards
            val allNames = names + listOfNotNull(deck.commander)
            id to allNames.groupingBy { it }.eachCount()
        }.toMap()
        val players = seatIds.mapIndexed { seat, id ->
            agents[seat].createPlayer(registry, id, decklistsByPlayer)
        }
        val bySeat = seatIds.withIndex().associate { (seat, id) -> id to seat }
        fun seatOf(playerId: EntityId) = bySeat[playerId] ?: -1
        fun aiFor(playerId: EntityId) = players[seatOf(playerId)]

        var state: GameState = init.state
        trainingObserver?.gameStarted(state, seatIds)
        var actionCount = 0
        val illegalActions = mutableMapOf<String, Int>()
        var lastActivePlayer: EntityId? = null
        var lastProgressAction = 0
        var drawReason = ""
        var exception: String? = null
        // `maxTurns` is a cap in turns *per seat*, so a duel and a four-player pod get the same
        // number of turns each and the head-to-head numbers keep their historical meaning.
        // `GameState.turnNumber` counts player turns, hence the multiply.
        val maxPlayerTurns = maxTurns * setup.seats
        val stream = if (recordActionStream) MessageDigest.getInstance("SHA-256") else null
        fun record(entry: String) = stream?.update(entry.toByteArray(Charsets.UTF_8))
        val featureGame = featureCollector?.newGame(
            "$groupId-$rotation-$seed",
            seatIds.mapIndexed { seat, id -> id to agents[seat].name }.toMap(),
        )

        val duration = measureTime {
            try {
                while (!state.gameOver && state.turnNumber < maxPlayerTurns &&
                    actionCount < maxActions
                ) {
                    if (actionCount - lastProgressAction > STUCK_ACTIONS_PER_TURN) {
                        drawReason = "stuck(turn=${state.turnNumber},step=${state.step.name})"
                        break
                    }
                    if (state.activePlayerId != lastActivePlayer) {
                        lastActivePlayer = state.activePlayerId
                        lastProgressAction = actionCount
                    }

                    val decision = state.pendingDecision
                    if (decision != null) {
                        actionCount++
                        val response = aiFor(decision.playerId).respondToDecision(state, decision)
                        trainingObserver?.decision(decision.playerId, response)
                        record("D$actionCount|${seatOf(decision.playerId)}|${decision::class.simpleName}|$response\n")
                        val r = processor.process(state, SubmitDecision(decision.playerId, response)).result
                        if (r.error != null) {
                            drawReason = "decisionError(${r.error})"
                            break
                        }
                        state = r.state
                        continue
                    }

                    val priorityPlayer = state.priorityPlayerId
                    if (priorityPlayer == null) {
                        drawReason = "noPriority(turn=${state.turnNumber})"
                        break
                    }

                    // A quiet position has no unresolved prompt and an empty stack. Sampling at
                    // this boundary avoids teaching the evaluator transient resolution states.
                    if (state.stack.isEmpty()) {
                        featureGame?.observe(state, priorityPlayer)
                        trainingObserver?.quietRoot(state, priorityPlayer)
                    }

                    actionCount++
                    val action = aiFor(priorityPlayer).chooseAction(state)
                    trainingObserver?.action(action)
                    record("A$actionCount|${seatOf(priorityPlayer)}|${state.step.name}|$action\n")
                    val r = processor.process(state, action).result
                    val next = if (r.error != null) {
                        val subjectId = when (action) {
                            is CastSpell -> action.cardId
                            is ActivateAbility -> action.sourceId
                            else -> null
                        }
                        val subject = subjectId?.let { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name }
                        val key = "${action::class.simpleName}${subject?.let { "[$it]" }.orEmpty()}: ${r.error}"
                        illegalActions[key] = (illegalActions[key] ?: 0) + 1
                        val fallback = processor
                            .process(state, safeFallbackAction(state, priorityPlayer, enumerator))
                            .result
                        if (fallback.error != null) {
                            drawReason = "error(${r.error}; fallback: ${fallback.error})"
                            null
                        } else fallback.state
                    } else r.state

                    if (next == null) break
                    if (next === state) {
                        drawReason = "noProgress(turn=${state.turnNumber},step=${state.step.name})"
                        break
                    }
                    state = next
                }
                if (!state.gameOver && drawReason.isEmpty()) {
                    drawReason = when {
                        actionCount >= maxActions -> "maxActions($maxActions)"
                        else -> "maxTurns($maxTurns)"
                    }
                }
            } catch (e: Throwable) {
                exception = "${e::class.simpleName}: ${e.message}"
                if (drawReason.isEmpty()) drawReason = "exception"
            }
        }

        val lifeBySeat = seatIds.map { state.lifeTotal(it) }
        // `winnerId` names one representative of the winning team (GameEndCheck), so the team is
        // the authoritative answer and the seat is the representative's.
        val winnerSeat = if (state.gameOver) state.winnerId?.let { bySeat[it] } else null
        val winnerTeam = winnerSeat?.let { setup.teamOfSeat[it] }
        featureGame?.finish(if (state.gameOver) state.winnerId else null)
        record("END|turns=${state.turnNumber}|winner=$winnerSeat|life=${lifeBySeat.joinToString("/")}\n")

        return TableGameOutcome(
            groupId = groupId,
            rotation = rotation,
            setup = setup,
            seatAgents = agents.map { it.name },
            seed = seed,
            winnerSeat = winnerSeat,
            winnerTeam = winnerTeam,
            turns = state.turnNumber,
            actions = actionCount,
            durationMs = duration.inWholeMilliseconds,
            lifeBySeat = lifeBySeat,
            completed = state.gameOver,
            drawReason = drawReason,
            exception = exception,
            illegalActions = illegalActions.toMap(),
            actionStreamHash = stream?.digest()?.joinToString("") { "%02x".format(it) }?.take(16),
        )
    }
}
