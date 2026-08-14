package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.gameserver.replay.ReplayFidelity
import com.wingedsheep.gameserver.replay.ReplayService
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

    @GetMapping("/{gameId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getReplay(@PathVariable gameId: String): ResponseEntity<Any> {
        val stored = replayService.findStored(gameId)
            ?: return ResponseEntity.notFound().build()
        val payload = replayService.viewerPayload(stored)
            ?: return ResponseEntity.notFound().build()
        val replay = stored.replay

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
