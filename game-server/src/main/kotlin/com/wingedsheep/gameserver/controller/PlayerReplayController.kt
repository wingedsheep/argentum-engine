package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.replay.ReplayService
import com.wingedsheep.gameserver.replay.ReplaySummary
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.SessionRegistry
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Player-facing REST controller for browsing and replaying completed games.
 *
 * Auth: reads X-Player-Token header and looks up player identity in SessionRegistry.
 * Players can view replays of games they participated in, or any game from their tournament.
 */
@RestController
@RequestMapping("/api/replays")
class PlayerReplayController(
    private val replayService: ReplayService,
    private val sessionRegistry: SessionRegistry,
    private val lobbyRepository: LobbyRepository
) {

    @GetMapping
    fun listGames(
        @RequestHeader("X-Player-Token", required = false) token: String?
    ): ResponseEntity<Any> {
        val identity = resolveIdentity(token)
            ?: return ResponseEntity.status(401)
                .body(mapOf("error" to "Invalid or missing player token"))

        val summaries = replayService.recentForPlayer(identity.playerId.value).map { it.toGameSummary() }
        return ResponseEntity.ok(summaries)
    }

    @GetMapping("/tournament/{lobbyId}")
    fun listTournamentGames(
        @PathVariable lobbyId: String,
        @RequestHeader("X-Player-Token", required = false) token: String?
    ): ResponseEntity<Any> {
        val identity = resolveIdentity(token)
            ?: return ResponseEntity.status(401)
                .body(mapOf("error" to "Invalid or missing player token"))

        val tournament = lobbyRepository.findTournamentById(lobbyId)
            ?: return ResponseEntity.notFound().build()

        // Verify the player is a tournament participant
        if (identity.playerId !in tournament.playerIds) {
            return ResponseEntity.status(403)
                .body(mapOf("error" to "Not a tournament participant"))
        }

        val summaries = tournament.getCompletedGameSessionIds().mapNotNull { gameId ->
            replayService.summary(gameId)?.toGameSummary()
        }
        return ResponseEntity.ok(summaries)
    }

    @GetMapping("/{gameId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getReplay(
        @PathVariable gameId: String,
        @RequestHeader("X-Player-Token", required = false) token: String?,
        @RequestParam(required = false) lobbyId: String?
    ): ResponseEntity<Any> {
        val identity = resolveIdentity(token)
            ?: return ResponseEntity.status(401)
                .body(mapOf("error" to "Invalid or missing player token"))

        val stored = replayService.findStored(gameId)
            ?: return ResponseEntity.notFound().build()

        // Verify the player was a participant OR is in the same tournament
        val playerId = identity.playerId.value
        val isParticipant = stored.replay.players.any { it.playerId == playerId }
        val isTournamentMember = lobbyId?.let { lid ->
            val tournament = lobbyRepository.findTournamentById(lid)
            tournament != null &&
                identity.playerId in tournament.playerIds &&
                gameId in tournament.getCompletedGameSessionIds()
        } ?: false

        if (!isParticipant && !isTournamentMember) {
            return ResponseEntity.notFound().build()
        }

        val payload = replayService.viewerPayload(stored)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload.body)
    }

    private fun resolveIdentity(token: String?) =
        token?.takeIf { it.isNotBlank() }?.let { sessionRegistry.getIdentityByToken(it) }
}

/** Summary projection for a stored replay, as listed in the player replay browser. */
data class GameSummary(
    val gameId: String,
    val player1Name: String,
    val player2Name: String,
    val startedAt: String,
    val endedAt: String,
    val winnerName: String?,
    val snapshotCount: Int,
    val tournamentName: String? = null,
    val tournamentRound: Int? = null
)

fun ReplaySummary.toGameSummary() = GameSummary(
    gameId = gameId,
    player1Name = playerNames.getOrNull(0) ?: "",
    player2Name = playerNames.getOrNull(1) ?: "",
    startedAt = startedAt,
    endedAt = endedAt,
    winnerName = winnerName,
    snapshotCount = frameCount,
    tournamentName = tournamentName,
    tournamentRound = tournamentRound
)
