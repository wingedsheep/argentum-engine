package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.TournamentParticipantRow
import com.wingedsheep.gameserver.persistence.TournamentRepository
import com.wingedsheep.gameserver.persistence.TournamentRow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/** A finished tournament ready to be recorded for stats. */
data class RecordedTournament(
    val lobbyId: String,
    val name: String?,
    val format: String?,
    val gameMode: String?,
    val setCodes: String?,
    val playerCount: Int,
    val rounds: Int,
    val gamesPerMatch: Int,
    val winnerName: String?,
    val startedAt: Instant?,
    val endedAt: Instant,
    val participants: List<RecordedTournamentParticipant>,
)

/** One seat in a finished tournament. [userId] is null for guests and AI. */
data class RecordedTournamentParticipant(
    val userId: UUID?,
    val playerName: String,
    val isAi: Boolean,
    val placement: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
)

/**
 * Records finished tournaments for durable stats. The completion path calls this unconditionally;
 * which implementation is wired depends on whether accounts are enabled, mirroring [MatchResultSink].
 */
interface TournamentResultSink {
    fun record(tournament: RecordedTournament)
}

/** Default: accounts disabled — tournaments are not persisted. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpTournamentResultSink : TournamentResultSink {
    override fun record(tournament: RecordedTournament) = Unit
}

/** Accounts enabled: persist the tournament, but only when at least one seat is a human. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcTournamentResultSink(private val tournaments: TournamentRepository) : TournamentResultSink {
    private val logger = LoggerFactory.getLogger(JdbcTournamentResultSink::class.java)

    override fun record(tournament: RecordedTournament) {
        if (tournament.participants.none { !it.isAi }) return
        tournaments.save(
            TournamentRow(
                lobbyId = tournament.lobbyId,
                name = tournament.name,
                format = tournament.format,
                gameMode = tournament.gameMode,
                setCodes = tournament.setCodes,
                playerCount = tournament.playerCount,
                rounds = tournament.rounds,
                gamesPerMatch = tournament.gamesPerMatch,
                winnerName = tournament.winnerName,
                startedAt = tournament.startedAt,
                endedAt = tournament.endedAt,
                participants = tournament.participants.map {
                    TournamentParticipantRow(
                        userId = it.userId,
                        playerName = it.playerName,
                        isAi = it.isAi,
                        placement = it.placement,
                        wins = it.wins,
                        losses = it.losses,
                        draws = it.draws,
                    )
                }.toSet(),
            )
        )
        logger.debug("Recorded tournament {} for stats ({} seats)", tournament.lobbyId, tournament.participants.size)
    }
}
