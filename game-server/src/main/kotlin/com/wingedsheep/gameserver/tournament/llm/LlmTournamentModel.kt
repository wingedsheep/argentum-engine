package com.wingedsheep.gameserver.tournament.llm

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import java.util.concurrent.atomic.AtomicBoolean

/** Overall lifecycle of an LLM tournament. */
enum class LlmTournamentStatus {
    /** Generating sealed pools + building decks. */
    BUILDING,

    /** Pools/decks ready, bracket seeded, waiting for the user to press Start. */
    READY,

    /** Actively playing games (auto-launches the next game as each finishes). */
    RUNNING,

    /** Paused — the in-flight game finishes, but no new game auto-launches. */
    PAUSED,

    /** A single champion remains. */
    COMPLETE
}

/** Per-participant deckbuild progress (surfaced in the UI during [LlmTournamentStatus.BUILDING]). */
enum class ParticipantBuildStatus { PENDING, BUILDING_POOL, BUILDING_DECK, READY, FAILED }

enum class DeckbuildMode { DRAFTSIM, LLM }

/**
 * A tournament entrant: one OpenRouter model with its own sealed pool + built deck.
 */
class LlmParticipant(
    val id: EntityId,
    val modelId: String,
    val displayName: String,
    /** 1-based seed (input order); used to lay out the bracket. */
    val seed: Int
) {
    @Volatile var pool: List<CardDefinition> = emptyList()
    @Volatile var deck: Map<String, Int> = emptyMap()
    @Volatile var buildStatus: ParticipantBuildStatus = ParticipantBuildStatus.PENDING

    /** LLM cost of building this deck (null for heuristic / not yet built). */
    @Volatile var buildCost: LlmCost? = null
}

/** One finished game of a match, with the LLM cost it incurred (both players combined). */
class FinishedGame(
    val gameSessionId: String,
    val cost: LlmCost
)

/**
 * A best-of-N match between two participants (or a bye when [player2Id] is null).
 * Plays one game at a time; the first to ceil(bestOf/2) game wins takes the match.
 */
class LlmMatch(
    val id: String,
    val roundNumber: Int,
    /** Position within the round (0-based), used to derive next-round pairings. */
    val slot: Int,
    var player1Id: EntityId?,
    var player2Id: EntityId?
) {
    var player1GameWins: Int = 0
    var player2GameWins: Int = 0
    var winnerId: EntityId? = null
    var isComplete: Boolean = false

    /** The live game session id while a game is being played, else null. */
    @Volatile var currentGameSessionId: String? = null

    /** Finished games (replay targets + per-game cost), in play order. */
    val finishedGames: MutableList<FinishedGame> = mutableListOf()

    val isBye: Boolean get() = player1Id != null && player2Id == null

    /** True when both seats are filled (a real, playable game) and the match isn't done. */
    fun isPlayable(): Boolean = player1Id != null && player2Id != null && !isComplete
}

class LlmRound(
    val roundNumber: Int,
    val matches: List<LlmMatch>
) {
    val isComplete: Boolean get() = matches.all { it.isComplete }
}

/**
 * A single-elimination, best-of-N LLM tournament. All mutation is guarded by [lock] inside
 * [LlmTournamentService]; the [gameInFlight] gate guarantees at most one live game at a time.
 */
class LlmTournament(
    val id: String,
    val setCode: String,
    val deckbuildMode: DeckbuildMode,
    val bestOf: Int,
    val participants: List<LlmParticipant>
) {
    val lock = Any()
    val gameInFlight = AtomicBoolean(false)

    @Volatile var status: LlmTournamentStatus = LlmTournamentStatus.BUILDING

    /** Per-decision AI delay (ms); adjustable live via the pacing control. */
    @Volatile var thinkingDelayMs: Long = 500

    val rounds: MutableList<LlmRound> = mutableListOf()

    /** gameSessionId -> match, for routing game-over callbacks back to the bracket. */
    val gameToMatch: MutableMap<String, LlmMatch> = mutableMapOf()

    val winsNeeded: Int get() = bestOf / 2 + 1

    fun participant(id: EntityId?): LlmParticipant? = participants.firstOrNull { it.id == id }

    val currentRound: LlmRound? get() = rounds.lastOrNull()

    val champion: LlmParticipant?
        get() = if (status == LlmTournamentStatus.COMPLETE) participant(currentRound?.matches?.singleOrNull()?.winnerId) else null

    /** Total LLM cost so far across all played games (excludes deck building). */
    fun totalGameCost(): LlmCost =
        rounds.flatMap { it.matches }.flatMap { it.finishedGames }.fold(LlmCost.ZERO) { acc, g -> acc + g.cost }

    /** Total LLM cost of building every participant's deck. */
    fun totalDeckbuildCost(): LlmCost =
        participants.mapNotNull { it.buildCost }.fold(LlmCost.ZERO) { acc, c -> acc + c }
}
