package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.MatchCardRow
import com.wingedsheep.gameserver.persistence.MatchParticipantRow
import com.wingedsheep.gameserver.persistence.MatchResultRepository
import com.wingedsheep.gameserver.persistence.MatchResultRow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/** A finished game ready to be recorded for stats. */
data class RecordedMatch(
    val gameId: String,
    val format: String?,
    val tournamentName: String?,
    /** Matchmaking context: a LobbyGameMode name, or QUICK_GAME / CASUAL for non-lobby games. */
    val gameMode: String?,
    /** Replay frame count at game-over — the activity measure behind the recording gate. */
    val frameCount: Int,
    /** GameState.turnNumber at game-over. */
    val turnCount: Int,
    val startedAt: Instant?,
    val endedAt: Instant,
    val participants: List<RecordedParticipant>,
)

/** One seat in a finished game. [userId] is null for guests and AI. */
data class RecordedParticipant(
    val userId: UUID?,
    val playerName: String,
    val won: Boolean,
    /** Deck color identity, canonical WUBRG order (e.g. "WU"); empty = colorless. Null if unknown. */
    val colors: String? = null,
    /** Comma-separated distinct set codes in the deck. Null if unknown. */
    val setCodes: String? = null,
    /** True for AI seats (which also have null userId). */
    val isAi: Boolean = false,
    /** Raw connecting IP, admin-only; never sent to clients. Null for AI / unknown. */
    val clientIp: String? = null,
    /** This seat's deck as card-name -> copies, for card-level stats. */
    val deckCards: Map<String, Int> = emptyMap(),
)

/**
 * Records finished games for durable stats. The game-over path calls this unconditionally; which
 * implementation is wired depends on whether accounts are enabled, so [GamePlayHandler] stays
 * decoupled from the persistence layer.
 */
interface MatchResultSink {
    fun record(match: RecordedMatch)
}

/** Default: accounts disabled — stats are not persisted. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpMatchResultSink : MatchResultSink {
    override fun record(match: RecordedMatch) = Unit
}

/**
 * Accounts enabled: persist the match, but only when at least one seat is a human (signed-in or
 * guest). AI-only games (e.g. the LLM tournament) would otherwise flood the table. Guest seats do not
 * contribute to any user's per-user stats (those filter by user_id) but do count toward the global
 * admin dashboard and geolocation.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcMatchResultSink(private val matchResults: MatchResultRepository) : MatchResultSink {
    private val logger = LoggerFactory.getLogger(JdbcMatchResultSink::class.java)

    override fun record(match: RecordedMatch) {
        if (match.participants.none { !it.isAi }) return
        matchResults.save(
            MatchResultRow(
                gameId = match.gameId,
                format = match.format,
                tournamentName = match.tournamentName,
                gameMode = match.gameMode,
                frameCount = match.frameCount,
                turnCount = match.turnCount,
                startedAt = match.startedAt,
                endedAt = match.endedAt,
                participants = match.participants.map {
                    MatchParticipantRow(
                        userId = it.userId,
                        playerName = it.playerName,
                        won = it.won,
                        colors = it.colors,
                        setCodes = it.setCodes,
                        isAi = it.isAi,
                        clientIp = it.clientIp,
                        cards = it.deckCards.map { (name, copies) ->
                            MatchCardRow(cardName = name, copies = copies)
                        }.toSet(),
                    )
                }.toSet(),
            )
        )
        logger.debug("Recorded match {} for stats ({} seats)", match.gameId, match.participants.size)
    }
}
