package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.replay.GameHistoryRepository
import com.wingedsheep.gameserver.replay.SpectatorReplayDelta
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Public (unauthenticated) REST controller for viewing game replays via shareable links.
 * Anyone with the game ID can view the replay — replays only contain spectator-view data
 * (no hidden information like hands).
 */
@RestController
@RequestMapping("/api/public/replays")
class PublicReplayController(
    private val gameHistoryRepository: GameHistoryRepository,
    private val messageSender: MessageSender
) {

    @GetMapping("/{gameId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getReplay(@PathVariable gameId: String): ResponseEntity<Any> {
        val record = gameHistoryRepository.findById(gameId)
            ?: return ResponseEntity.notFound().build()

        val initialSnapshotJson = messageSender.json.encodeToString(
            ServerMessage.SpectatorStateUpdate.serializer(),
            record.initialSnapshot
        )
        val deltasJson = messageSender.json.encodeToString(
            ListSerializer(SpectatorReplayDelta.serializer()),
            record.deltas
        )

        val response = PublicReplayResponse(
            gameId = record.gameId,
            player1Name = record.player1Name,
            player2Name = record.player2Name,
            winnerName = record.winnerName,
            startedAt = record.startedAt.toString(),
            endedAt = record.endedAt.toString(),
            snapshotCount = record.frameCount
        )

        // Build combined JSON with metadata + initial snapshot + deltas
        // We manually compose because snapshots use kotlinx.serialization
        val metadataJson = messageSender.json.encodeToString(response)
        val combinedJson = """{"metadata":$metadataJson,"initialSnapshot":$initialSnapshotJson,"deltas":$deltasJson}"""

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(combinedJson)
    }
}

@Serializable
data class PublicReplayResponse(
    val gameId: String,
    val player1Name: String,
    val player2Name: String,
    val winnerName: String?,
    val startedAt: String,
    val endedAt: String,
    val snapshotCount: Int
)
