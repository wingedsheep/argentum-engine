package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.gameserver.replay.CompactReplay
import com.wingedsheep.gameserver.replay.ReplayFidelity
import com.wingedsheep.gameserver.replay.ReplayFile
import com.wingedsheep.gameserver.replay.ReplayFileException
import com.wingedsheep.gameserver.replay.ReplayService
import com.wingedsheep.gameserver.replay.ReplayStatus
import com.wingedsheep.gameserver.replay.ReplayViewerPayload
import jakarta.servlet.http.HttpServletRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Semaphore

private const val MAX_CONCURRENT_UPLOADS = 2

/**
 * Public (unauthenticated) REST controller for viewing game replays via shareable links.
 * Anyone with the game ID can view the replay — replays only contain spectator-view data
 * (no hidden information like hands). The unguessable game id is the share token.
 *
 * The body comes from [ReplayService.viewerPayload], which re-simulates the compact record when that
 * still reproduces the game faithfully and falls back to the frames archived at record time when it
 * doesn't. The metadata says which happened, so the viewer can be honest about it.
 */
@RestController
@RequestMapping("/api/public/replays")
class PublicReplayController(
    private val replayService: ReplayService,
    private val messageSender: MessageSender
) {
    /** Concurrent upload re-simulations; past this an upload is refused rather than queued. */
    private val uploadPermits = Semaphore(MAX_CONCURRENT_UPLOADS)

    @GetMapping("/{gameId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getReplay(@PathVariable gameId: String): ResponseEntity<Any> {
        val stored = replayService.findStored(gameId)
            ?: return ResponseEntity.notFound().build()
        val payload = replayService.viewerPayload(stored)
            ?: return ResponseEntity.notFound().build()
        return viewerResponse(stored.replay, payload)
    }

    /**
     * The game's compact record ([CompactReplay]) as a downloadable JSON file — the input stream,
     * seed, decks and pinned cards, which [uploadReplay] (on this or any other server) re-simulates
     * back into the full game.
     *
     * Finished games only. The record carries the RNG seed and every decklist, which together give
     * away every hand and library order, so handing it out mid-game would let a player read their
     * opponent's hidden information.
     */
    @GetMapping("/{gameId}/export", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun exportReplay(@PathVariable gameId: String): ResponseEntity<String> {
        val stored = replayService.findStored(gameId)
            ?.takeIf { it.status == ReplayStatus.FINISHED }
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(ReplayFile.fileName(gameId)).build().toString(),
            )
            .body(ReplayFile.export(stored.replay))
    }

    /**
     * Watch a replay file — an [exportReplay] download, plain or gzipped — without storing it. The
     * body is the raw file; the response has the same shape as [getReplay].
     *
     * Re-simulating is the expensive part of serving any replay, and here the input is whatever the
     * uploader sent, so [ReplayFile] caps its size before anything runs and [uploadPermits] caps how
     * many run at once.
     */
    @PostMapping("/upload", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun uploadReplay(request: HttpServletRequest): ResponseEntity<Any> {
        if (!uploadPermits.tryAcquire()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(mapOf("error" to "The server is busy replaying other uploads — try again in a moment."))
        }
        try {
            val replay = try {
                ReplayFile.read(request.inputStream)
            } catch (e: ReplayFileException) {
                return ResponseEntity.badRequest().body(mapOf("error" to e.message))
            }
            val payload = replayService.viewerPayloadForUpload(replay)
                ?: return ResponseEntity.unprocessableEntity()
                    .body(mapOf("error" to "This replay could not be re-simulated on the current version."))
            return viewerResponse(replay, payload)
        } finally {
            uploadPermits.release()
        }
    }

    private fun viewerResponse(replay: CompactReplay, payload: ReplayViewerPayload): ResponseEntity<Any> {
        val response = PublicReplayResponse(
            gameId = replay.gameId,
            player1Name = replay.players.getOrNull(0)?.name ?: "",
            player2Name = replay.players.getOrNull(1)?.name ?: "",
            winnerName = replay.winnerName,
            startedAt = replay.startedAt,
            endedAt = replay.endedAt,
            snapshotCount = payload.frameCount,
            fidelity = payload.fidelity.name,
            degradedReason = payload.degradedReason,
            stateReproducible = payload.stateReproducible,
        )

        // Manually composed: the frames are already-serialized JSON (freshly rendered, or read
        // straight out of the archive), so splicing beats decode-and-re-encode.
        val metadataJson = messageSender.json.encodeToString(response)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"metadata":$metadataJson,${payload.bodyFields()}}""")
    }

    /**
     * The full, unmasked game state for a single replay frame, used by "share frame as
     * scenario" to reproduce the EXACT position (stack, targets, floating effects, mana, …).
     * Re-simulated from the compact record up to [frame]. Served separately from [getReplay] so
     * normal (masked) replay viewing never receives hidden information — only an explicit share
     * does. The game is finished, so revealing the full state of a snapshot is intended.
     *
     * 404s when the record no longer re-simulates: a shared scenario has to be a real position, and
     * archived frames are pictures of a game, not a game state.
     */
    @GetMapping("/{gameId}/frames/{frame}/full-state", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getFrameFullState(
        @PathVariable gameId: String,
        @PathVariable frame: Int,
    ): ResponseEntity<String> {
        val state = replayService.reconstructStateAt(gameId, frame)
            ?: return ResponseEntity.notFound().build()
        // persistenceJson has allowStructuredMapKeys (GameState.zones is keyed by ZoneKey).
        val json = persistenceJson.encodeToString(GameState.serializer(), state)
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json)
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
    val snapshotCount: Int,
    /** [ReplayFidelity] name — EXACT, UNVERIFIED, or DIVERGED. */
    val fidelity: String = ReplayFidelity.UNVERIFIED.name,
    /** Player-facing explanation, set when the replay isn't a faithful re-simulation. */
    val degradedReason: String? = null,
    /** Whether "share frame as scenario" can work for this replay. */
    val stateReproducible: Boolean = true,
)
