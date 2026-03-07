package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.replay.GameHistoryRepository
import com.wingedsheep.gameserver.replay.GameReplayRecord
import com.wingedsheep.gameserver.replay.SpectatorReplayDelta
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin-only REST controller for browsing and replaying completed games.
 *
 * Auth: requires X-Admin-Password header matching the game.admin.password config.
 * Feature is disabled entirely if no password is configured.
 *
 * Snapshots are serialized with the same kotlinx.serialization Json instance used
 * by the WebSocket path (via MessageSender), so the client receives identical JSON.
 */
@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val gameHistoryRepository: GameHistoryRepository,
    private val gameProperties: GameProperties,
    private val messageSender: MessageSender
) {

    @GetMapping("/games")
    fun listGames(
        @RequestHeader("X-Admin-Password", required = false) password: String?
    ): ResponseEntity<Any> {
        val authError = checkAuth(password)
        if (authError != null) return authError

        val summaries = gameHistoryRepository.findAll().map { it.toSummary() }
        return ResponseEntity.ok(summaries)
    }

    @GetMapping("/games/{gameId}/replay", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getReplay(
        @PathVariable gameId: String,
        @RequestHeader("X-Admin-Password", required = false) password: String?
    ): ResponseEntity<Any> {
        val authError = checkAuth(password)
        if (authError != null) return authError

        val record = gameHistoryRepository.findById(gameId)
            ?: return ResponseEntity.notFound().build()

        val initialJson = messageSender.json.encodeToString(
            ServerMessage.SpectatorStateUpdate.serializer(),
            record.initialSnapshot
        )
        val deltasJson = messageSender.json.encodeToString(
            ListSerializer(SpectatorReplayDelta.serializer()),
            record.deltas
        )
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"initialSnapshot":$initialJson,"deltas":$deltasJson}""")
    }

    private fun checkAuth(password: String?): ResponseEntity<Any>? {
        val configuredPassword = gameProperties.admin.password
        if (configuredPassword.isBlank()) {
            return ResponseEntity.status(401)
                .body(mapOf("error" to "Admin feature is not configured"))
        }
        if (password != configuredPassword) {
            return ResponseEntity.status(401)
                .body(mapOf("error" to "Invalid admin password"))
        }
        return null
    }

    private fun GameReplayRecord.toSummary() = GameSummary(
        gameId = gameId,
        player1Name = player1Name,
        player2Name = player2Name,
        startedAt = startedAt.toString(),
        endedAt = endedAt.toString(),
        winnerName = winnerName,
        snapshotCount = frameCount,
        tournamentName = tournamentName,
        tournamentRound = tournamentRound
    )
}

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
