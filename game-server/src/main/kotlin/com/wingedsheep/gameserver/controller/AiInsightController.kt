package com.wingedsheep.gameserver.controller

import com.wingedsheep.ai.insight.AiDecisionInsight
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.gameserver.ai.AiHumanOverride
import com.wingedsheep.gameserver.ai.AiInsightEntry
import com.wingedsheep.gameserver.ai.AiInsightService
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Local testing mode over a game against the AI: browse the actions the AI weighed and how strongly
 * it preferred each, hold it at a decision and play a different option than the one it picked, and
 * export a position together with those ratings as AI-training input.
 *
 * Mounted only when `game.ai.insight-enabled` is set (see
 * [com.wingedsheep.gameserver.config.AiProperties.insightEnabled]). When it isn't, every route here
 * 404s, which is also how the client decides whether to offer the panel at all — no extra config
 * round-trip, and no way to leave the UI advertising a mode the server isn't running.
 *
 * Everything is keyed by **any seat's player id** rather than a game id, because the player id is
 * the only identifier the web client holds for its own game
 * ([com.wingedsheep.engine.view.ClientGameState.viewingPlayerId]).
 *
 * These responses carry the AI's *unmasked* view of the board — that is the entire point of the
 * export, and the reason this stays behind an opt-in local flag rather than shipping with the other
 * dev endpoints.
 *
 * - `GET /api/dev/ai-insight/{playerId}` → recent decisions, plus whether one is held right now
 * - `POST /api/dev/ai-insight/{playerId}/step?enabled=true` → hold the AI before each move
 * - `POST /api/dev/ai-insight/{playerId}/resume[?optionIndex=N]` → let it play, or play N instead
 * - `GET /api/dev/ai-insight/{playerId}/export[?decision=N]` → a decision + its board state
 * - `DELETE /api/dev/ai-insight/{playerId}` → drop the recorded history for this game
 */
@RestController
@RequestMapping("/api/dev/ai-insight")
@ConditionalOnProperty(name = ["game.ai.insight-enabled"], havingValue = "true")
class AiInsightController(private val service: AiInsightService) {

    @GetMapping("/{playerId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun list(
        @PathVariable playerId: String,
        @RequestParam(defaultValue = "60") limit: Int,
    ): ResponseEntity<String> {
        val seat = EntityId(playerId)
        // Doubles as the watchdog heartbeat: step mode only holds the AI while someone is watching.
        // Unconditional, and before the game lookup — a client that armed step mode on its very first
        // poll must count as present even though no decision has been recorded yet.
        service.markPolled(seat)
        val stepMode = service.isStepMode(seat)

        val gameSessionId = service.gameForPlayer(seat) ?: return json(
            AiInsightListResponse(gameSessionId = null, decisions = emptyList(), stepMode = stepMode)
        )

        val decisions = service.decisions(gameSessionId, limit.coerceIn(1, 200)).map { it.toDto() }
        val held = service.pending(gameSessionId)
        return json(
            AiInsightListResponse(
                gameSessionId = gameSessionId,
                decisions = decisions,
                stepMode = stepMode,
                pending = held?.let {
                    AiPendingApproval(
                        decisionId = it.decisionId,
                        proposedLabel = service.decision(gameSessionId, it.decisionId)
                            ?.insight?.chosenLabel ?: "its chosen move",
                    )
                },
            )
        )
    }

    /**
     * Turn "hold the AI before each move it actually weighed" on or off for this seat.
     *
     * Deliberately does *not* require the game to be known yet: arming step mode at the start of a
     * game, before the AI has recorded anything, is the normal case.
     */
    @PostMapping("/{playerId}/step")
    fun step(
        @PathVariable playerId: String,
        @RequestParam enabled: Boolean,
    ): ResponseEntity<Void> {
        val seat = EntityId(playerId)
        service.markPolled(seat)
        service.setStepMode(seat, enabled)
        return ResponseEntity.noContent().build()
    }

    /**
     * Release a held AI. Without [optionIndex] it plays its own pick; with one, that option's action
     * is submitted instead and the disagreement is stamped on the recorded decision.
     *
     * 409 rather than 404 when the option can't be played: the decision is real and the index is
     * real, but that row is one the engine already rejected, so there is nothing to submit.
     */
    @PostMapping("/{playerId}/resume")
    fun resume(
        @PathVariable playerId: String,
        @RequestParam(required = false) optionIndex: Int?,
    ): ResponseEntity<Void> {
        val seat = EntityId(playerId)
        service.markPolled(seat)
        val gameSessionId = service.gameForPlayer(seat) ?: return ResponseEntity.notFound().build()
        if (service.pending(gameSessionId) == null) return ResponseEntity.notFound().build()
        return if (service.resume(gameSessionId, optionIndex)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(409).build()
        }
    }

    /**
     * The export the panel's button produces: the board state the AI was looking at, paired with the
     * rating it gave every option and — when a human overrode it — the move played instead.
     *
     * `state` is a full [GameState] and so also round-trips through
     * `POST /api/scenarios/from-state`, which means a position that produced a bad rating can be
     * re-opened and replayed rather than only read about.
     */
    @GetMapping("/{playerId}/export", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun export(
        @PathVariable playerId: String,
        @RequestParam(required = false) decision: Long?,
    ): ResponseEntity<String> {
        val gameSessionId = service.gameForPlayer(EntityId(playerId))
            ?: return ResponseEntity.notFound().build()
        val entry = if (decision != null) {
            service.decision(gameSessionId, decision)
        } else {
            service.decisions(gameSessionId, 1).firstOrNull()
        } ?: return ResponseEntity.notFound().build()

        val payload = AiInsightExport(
            exportedAt = Instant.now().toString(),
            gameSessionId = gameSessionId,
            sample = AiInsightSample(
                id = entry.id,
                recordedAt = entry.recordedAt.toString(),
                insight = entry.insight,
                humanOverride = entry.humanOverride?.toDto(),
                state = entry.state,
            ),
        )
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"ai-insight-$gameSessionId-${entry.id}.json\"",
            )
            .body(persistenceJson.encodeToString(AiInsightExport.serializer(), payload))
    }

    @DeleteMapping("/{playerId}")
    fun clear(@PathVariable playerId: String): ResponseEntity<Void> {
        val gameSessionId = service.gameForPlayer(EntityId(playerId))
            ?: return ResponseEntity.notFound().build()
        service.clearGame(gameSessionId)
        return ResponseEntity.noContent().build()
    }

    private fun AiInsightEntry.toDto() =
        AiInsightDecision(id, recordedAt.toString(), insight, humanOverride?.toDto())

    private fun AiHumanOverride.toDto() = AiHumanOverrideDto(optionIndex, label)

    /**
     * kotlinx rather than Spring's Jackson converter: these payloads are built from engine types —
     * [EntityId] is a value class and [GameState] a large polymorphic tree — that only round-trip
     * correctly through the engine's own serializers module.
     */
    private inline fun <reified T> json(body: T): ResponseEntity<String> =
        ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(persistenceJson.encodeToString(body))
}

@Serializable
data class AiInsightListResponse(
    /** Null when no AI in this player's game has recorded a decision yet. */
    val gameSessionId: String?,
    /** Newest first. */
    val decisions: List<AiInsightDecision>,
    val stepMode: Boolean = false,
    /** Set while the AI is held waiting for a human to approve or replace its move. */
    val pending: AiPendingApproval? = null,
)

@Serializable
data class AiInsightDecision(
    val id: Long,
    val recordedAt: String,
    val insight: AiDecisionInsight,
    val humanOverride: AiHumanOverrideDto? = null,
)

@Serializable
data class AiPendingApproval(val decisionId: Long, val proposedLabel: String)

@Serializable
data class AiHumanOverrideDto(val optionIndex: Int, val label: String)

/**
 * Self-describing bundle: one board state, what the AI thought of every option in it, and the move
 * actually played if a human disagreed.
 */
@Serializable
data class AiInsightExport(
    val formatVersion: Int = 2,
    val exportedAt: String,
    val gameSessionId: String,
    val sample: AiInsightSample,
)

@Serializable
data class AiInsightSample(
    val id: Long,
    val recordedAt: String,
    val insight: AiDecisionInsight,
    /** What a human played instead, when they overrode the AI here. The label half of a training pair. */
    val humanOverride: AiHumanOverrideDto? = null,
    /** The AI's unmasked view of the position this decision was made from. */
    val state: GameState,
)
