package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.replay.GameHistoryRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.SessionRegistry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
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
    private val gameHistoryRepository: GameHistoryRepository,
    private val sessionRegistry: SessionRegistry,
    private val messageSender: MessageSender,
    private val lobbyRepository: LobbyRepository
) {

    @GetMapping
    fun listGames(
        @RequestHeader("X-Player-Token", required = false) token: String?
    ): ResponseEntity<Any> {
        val identity = resolveIdentity(token)
            ?: return ResponseEntity.status(401)
                .body(mapOf("error" to "Invalid or missing player token"))

        val playerId = identity.playerId.value
        val summaries = gameHistoryRepository.findByPlayerId(playerId).map { record ->
            GameSummary(
                gameId = record.gameId,
                player1Name = record.player1Name,
                player2Name = record.player2Name,
                startedAt = record.startedAt.toString(),
                endedAt = record.endedAt.toString(),
                winnerName = record.winnerName,
                snapshotCount = record.snapshots.size,
                tournamentName = record.tournamentName,
                tournamentRound = record.tournamentRound
            )
        }
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

        val gameSessionIds = tournament.getCompletedGameSessionIds()
        val summaries = gameSessionIds.mapNotNull { gameId ->
            gameHistoryRepository.findById(gameId)?.let { record ->
                GameSummary(
                    gameId = record.gameId,
                    player1Name = record.player1Name,
                    player2Name = record.player2Name,
                    startedAt = record.startedAt.toString(),
                    endedAt = record.endedAt.toString(),
                    winnerName = record.winnerName,
                    snapshotCount = record.snapshots.size
                )
            }
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

        val record = gameHistoryRepository.findById(gameId)
            ?: return ResponseEntity.notFound().build()

        // Verify the player was a participant OR is in the same tournament
        val playerId = identity.playerId.value
        val isParticipant = record.player1Id == playerId || record.player2Id == playerId
        val isTournamentMember = lobbyId?.let { lid ->
            val tournament = lobbyRepository.findTournamentById(lid)
            tournament != null &&
                identity.playerId in tournament.playerIds &&
                gameId in tournament.getCompletedGameSessionIds()
        } ?: false

        if (!isParticipant && !isTournamentMember) {
            return ResponseEntity.notFound().build()
        }

        val jsonString = messageSender.json.encodeToString(
            ListSerializer(ServerMessage.SpectatorStateUpdate.serializer()),
            record.snapshots
        )
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(jsonString)
    }

    private fun resolveIdentity(token: String?) =
        token?.takeIf { it.isNotBlank() }?.let { sessionRegistry.getIdentityByToken(it) }
}
