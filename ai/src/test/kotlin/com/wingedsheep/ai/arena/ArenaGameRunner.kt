package com.wingedsheep.ai.arena

import com.wingedsheep.ai.engine.safeFallbackAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.Deck

/**
 * Result of one arena game, recorded **by seat**, never by agent.
 *
 * Seat-indexed is the whole point: a pair plays the same game twice with the agents swapped, and
 * anything that reads "did agent A win" before the pair is assembled is how a seat bias sneaks in.
 */
data class ArenaGameOutcome(
    val pairId: Int,
    /** 0 = the agent that sat in seat 0 (on the play) played first. */
    val gameIndex: Int,
    val seat0Agent: String,
    val seat1Agent: String,
    val seed: Long,
    /** 0, 1, or null for a draw / unfinished game. */
    val winnerSeat: Int?,
    val turns: Int,
    val actions: Int,
    val durationMs: Long,
    val seat0Life: Int,
    val seat1Life: Int,
    val completed: Boolean,
    /** Why the game ended without a winner. Empty when it ended normally. */
    val drawReason: String,
    /** A thrown engine exception, if any — the arena doubles as a crash finder at scale. */
    val exception: String?,
    /**
     * Actions the AI proposed that the processor rejected, recovered by [safeFallbackAction].
     * Keyed by `"<ActionType>: <error>"` so the report can histogram them — this is the arena
     * doubling as a free bug finder at scale, and a rejected action is always a real defect
     * somewhere (the enumerator offered it, or the AI mangled it).
     */
    val illegalActions: Map<String, Int>,
    /** Set only when the runner was asked to record one. See [ArenaGameRunner.play]. */
    val actionStreamHash: String?,
)

/**
 * Plays a single head-to-head arena game: two [ArenaAgent]s, one deck each, one seed.
 *
 * A two-seat view of [TableGameRunner], which owns the actual game loop — so the head-to-head arena
 * and the multiplayer pod arena can never drift apart in their stuck detection, draw taxonomy or
 * illegal-action recovery. The two-seat [ArenaGameOutcome] stays because the paired-swap estimator,
 * the CSV and `FrozenBaselineTest` are all written against it and none of them mean anything at a
 * table with more than two seats.
 */
object ArenaGameRunner {

    fun play(
        registry: CardRegistry,
        seat0: ArenaAgent,
        seat1: ArenaAgent,
        seat0Deck: Deck,
        seat1Deck: Deck,
        seed: Long,
        pairId: Int,
        gameIndex: Int,
        maxTurns: Int = 50,
        /** Hash every action and decision into [ArenaGameOutcome.actionStreamHash]. Off by
         *  default: it costs a string per action, and only `FrozenBaselineTest` needs it. */
        recordActionStream: Boolean = false,
        featureCollector: ArenaFeatureCollector? = null,
    ): ArenaGameOutcome {
        val game = TableGameRunner.play(
            registry = registry,
            setup = TableSetup.HEADS_UP,
            agents = listOf(seat0, seat1),
            decks = listOf(seat0Deck, seat1Deck),
            seed = seed,
            groupId = pairId,
            rotation = gameIndex,
            maxTurns = maxTurns,
            recordActionStream = recordActionStream,
            featureCollector = featureCollector,
        )
        return ArenaGameOutcome(
            pairId = pairId,
            gameIndex = gameIndex,
            seat0Agent = seat0.name,
            seat1Agent = seat1.name,
            seed = seed,
            winnerSeat = game.winnerSeat,
            turns = game.turns,
            actions = game.actions,
            durationMs = game.durationMs,
            seat0Life = game.lifeBySeat[0],
            seat1Life = game.lifeBySeat[1],
            completed = game.completed,
            drawReason = game.drawReason,
            exception = game.exception,
            illegalActions = game.illegalActions,
            actionStreamHash = game.actionStreamHash,
        )
    }
}
