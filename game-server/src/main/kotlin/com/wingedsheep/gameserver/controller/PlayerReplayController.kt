package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.replay.GameHistoryRepository
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
 * Players can only view replays of games they participated in.
 */
@RestController
@RequestMapping("/api/replays")
class PlayerReplayController(
    private val gameHistoryRepository: GameHistoryRepository,
    private val sessionRegistry: SessionRegistry,
    private val messageSender: MessageSender
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
                snapshotCount = record.snapshots.size
            )
        }
        return ResponseEntity.ok(summaries)
    }

    @GetMapping("/{gameId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getReplay(
        @PathVariable gameId: String,
        @RequestHeader("X-Player-Token", required = false) token: String?
    ): ResponseEntity<Any> {
        val identity = resolveIdentity(token)
            ?: return ResponseEntity.status(401)
                .body(mapOf("error" to "Invalid or missing player token"))

        val record = gameHistoryRepository.findById(gameId)
            ?: return ResponseEntity.notFound().build()

        // Verify the player was a participant
        val playerId = identity.playerId.value
        if (record.player1Id != playerId && record.player2Id != playerId) {
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
