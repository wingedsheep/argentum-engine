package com.wingedsheep.gameserver.stats

import com.wingedsheep.gameserver.persistence.TournamentParticipantRow
import com.wingedsheep.gameserver.persistence.TournamentRepository
import com.wingedsheep.gameserver.persistence.TournamentRow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/** Lifecycle state of a recorded tournament. Mirrors the `status` column on the `tournaments` table. */
enum class TournamentStatus { IN_PROGRESS, COMPLETED, ABANDONED }

/** A tournament ready to be recorded for stats — at start (partial) or on completion (final). */
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
    /** Null while the tournament is still in progress; set when it completes. */
    val endedAt: Instant?,
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
 * Records tournaments for durable stats across their whole lifecycle, so the admin dashboard and
 * player profiles see in-progress and abandoned tournaments, not only finished ones. All three calls
 * are safe no-ops when accounts are disabled (mirroring [MatchResultSink]); the JDBC implementation is
 * keyed on lobby id so start/complete/abandon upsert a single row.
 */
interface TournamentResultSink {
    /** Record that a tournament has gone live. Idempotent per lobby; skipped when no seat is human. */
    fun recordStarted(tournament: RecordedTournament)

    /**
     * Refresh an in-progress tournament's standings (wins/losses/draws and current placement) so
     * player profiles and the admin dashboard reflect results as they happen, not only at the end.
     * No-op unless an IN_PROGRESS row already exists for the lobby; never resurrects a
     * completed/abandoned tournament.
     */
    fun recordProgress(tournament: RecordedTournament)

    /** Record a tournament's final result, flipping it to COMPLETED. Upserts the in-progress row. */
    fun recordCompleted(tournament: RecordedTournament)

    /** Mark a torn-down tournament as ABANDONED. No-op unless an in-progress row exists for the lobby. */
    fun recordAbandoned(lobbyId: String)
}

/** Default: accounts disabled — tournaments are not persisted. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpTournamentResultSink : TournamentResultSink {
    override fun recordStarted(tournament: RecordedTournament) = Unit
    override fun recordProgress(tournament: RecordedTournament) = Unit
    override fun recordCompleted(tournament: RecordedTournament) = Unit
    override fun recordAbandoned(lobbyId: String) = Unit
}

/** Accounts enabled: persist the tournament lifecycle, but only when at least one seat is a human. */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcTournamentResultSink(private val tournaments: TournamentRepository) : TournamentResultSink {
    private val logger = LoggerFactory.getLogger(JdbcTournamentResultSink::class.java)

    override fun recordStarted(tournament: RecordedTournament) {
        if (tournament.participants.none { !it.isAi }) return
        // Idempotent: startTournament and the eager-creation path may both fire for one lobby.
        if (tournaments.findFirstByLobbyIdOrderByIdDesc(tournament.lobbyId) != null) return
        tournaments.save(tournament.toRow(TournamentStatus.IN_PROGRESS))
        logger.debug("Recorded tournament {} as in progress ({} seats)", tournament.lobbyId, tournament.participants.size)
    }

    override fun recordProgress(tournament: RecordedTournament) {
        if (tournament.participants.none { !it.isAi }) return
        val existing = tournaments.findFirstByLobbyIdOrderByIdDesc(tournament.lobbyId) ?: return
        // Only refresh a live tournament; never overwrite a completed or abandoned one.
        if (existing.status != TournamentStatus.IN_PROGRESS.name) return
        tournaments.save(
            tournament.toRow(
                status = TournamentStatus.IN_PROGRESS,
                id = existing.id,
                // Keep the start time captured when the bracket went live.
                startedAt = existing.startedAt,
            )
        )
        logger.debug("Refreshed in-progress standings for tournament {} ({} seats)", tournament.lobbyId, tournament.participants.size)
    }

    override fun recordCompleted(tournament: RecordedTournament) {
        if (tournament.participants.none { !it.isAi }) return
        val existing = tournaments.findFirstByLobbyIdOrderByIdDesc(tournament.lobbyId)
        tournaments.save(
            tournament.toRow(
                status = TournamentStatus.COMPLETED,
                id = existing?.id,
                // Keep the start time captured when the bracket went live.
                startedAt = tournament.startedAt ?: existing?.startedAt,
            )
        )
        logger.debug("Recorded tournament {} as completed ({} seats)", tournament.lobbyId, tournament.participants.size)
    }

    override fun recordAbandoned(lobbyId: String) {
        val existing = tournaments.findFirstByLobbyIdOrderByIdDesc(lobbyId) ?: return
        if (existing.status != TournamentStatus.IN_PROGRESS.name) return
        tournaments.save(existing.copy(status = TournamentStatus.ABANDONED.name, endedAt = Instant.now()))
        logger.debug("Recorded tournament {} as abandoned", lobbyId)
    }

    private fun RecordedTournament.toRow(
        status: TournamentStatus,
        id: Long? = null,
        startedAt: Instant? = this.startedAt,
    ) = TournamentRow(
        id = id,
        lobbyId = lobbyId,
        name = name,
        format = format,
        gameMode = gameMode,
        setCodes = setCodes,
        playerCount = playerCount,
        rounds = rounds,
        gamesPerMatch = gamesPerMatch,
        winnerName = winnerName,
        status = status.name,
        startedAt = startedAt,
        endedAt = endedAt,
        participants = participants.map {
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
}
