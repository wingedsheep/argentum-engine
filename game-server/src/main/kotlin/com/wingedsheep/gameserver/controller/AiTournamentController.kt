package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.handler.LobbyHandler
import com.wingedsheep.engine.limited.BoosterGenerator
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Development-only endpoint to create a sealed tournament with AI-only players.
 *
 * After creation, open /tournament/{lobbyId} in the browser to spectate.
 * AI players will build decks, start matches, and play autonomously.
 *
 * Enable with: game.dev-endpoints.enabled=true
 */
@RestController
@RequestMapping("/api/dev/ai-tournament")
@ConditionalOnProperty(name = ["game.dev-endpoints.enabled"], havingValue = "true")
class AiTournamentController(
    private val lobbyHandler: LobbyHandler,
    private val boosterGenerator: BoosterGenerator
) {
    private val logger = LoggerFactory.getLogger(AiTournamentController::class.java)

    data class AiTournamentRequest(
        val setCodes: List<String>? = null,
        val playerCount: Int? = null,
        /** Optional per-player model overrides. Index 0 = player 1, index 1 = player 2, etc.
         *  Falls back to the server's configured model for any unspecified slots. */
        val models: List<String>? = null,
        /** Skip LLM deck building and use the fast heuristic builder instead. */
        val heuristicDeckbuilding: Boolean? = null
    )

    data class AiTournamentResponse(
        val lobbyId: String,
        val spectateUrl: String,
        val message: String
    )

    @PostMapping
    fun createAiTournament(
        @RequestBody request: AiTournamentRequest?
    ): ResponseEntity<AiTournamentResponse> {
        val setCodes = request?.setCodes?.ifEmpty { null }
            ?: boosterGenerator.availableSets.keys.toList().let { listOf(it.random()) }
        val playerCount = request?.playerCount?.coerceIn(2, 8) ?: 2

        return try {
            val lobbyId = lobbyHandler.createAiTournament(setCodes, playerCount, request?.models, request?.heuristicDeckbuilding)

            logger.info("AI tournament created via REST: lobbyId=$lobbyId, sets=$setCodes, players=$playerCount")

            ResponseEntity.ok(AiTournamentResponse(
                lobbyId = lobbyId,
                spectateUrl = "/tournament/$lobbyId",
                message = "AI tournament created. Open /tournament/$lobbyId to spectate. " +
                    "AI players are building decks and will start playing shortly."
            ))
        } catch (e: Exception) {
            logger.error("Failed to create AI tournament: ${e.message}", e)
            ResponseEntity.badRequest().body(AiTournamentResponse(
                lobbyId = "",
                spectateUrl = "",
                message = "Failed to create AI tournament: ${e.message}"
            ))
        }
    }

    @GetMapping("/sets")
    fun listAvailableSets(): ResponseEntity<List<SetInfo>> {
        val sets = boosterGenerator.availableSets.map { (code, config) ->
            SetInfo(code = code, name = config.setName)
        }.sortedBy { it.name }
        return ResponseEntity.ok(sets)
    }

    data class SetInfo(val code: String, val name: String)
}
