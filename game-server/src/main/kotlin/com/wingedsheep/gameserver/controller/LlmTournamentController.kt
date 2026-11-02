package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.tournament.llm.DeckbuildMode
import com.wingedsheep.gameserver.tournament.llm.LlmMatch
import com.wingedsheep.gameserver.tournament.llm.LlmTournament
import com.wingedsheep.gameserver.tournament.llm.LlmTournamentService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Dev-only endpoints driving an all-LLM single-elimination sealed tournament.
 *
 * Enable with: game.dev-endpoints.enabled=true (and game.ai.mode=llm + an OpenRouter key).
 * The web client exposes this at /llm-tournament (dev builds only).
 */
@RestController
@RequestMapping("/api/dev/llm-tournament")
@ConditionalOnProperty(name = ["game.dev-endpoints.enabled"], havingValue = "true")
class LlmTournamentController(
    private val service: LlmTournamentService,
    private val boosterGenerator: BoosterGenerator
) {
    private val logger = LoggerFactory.getLogger(LlmTournamentController::class.java)

    // ---- Requests / responses -------------------------------------------------

    data class CreateRequest(
        val modelIds: List<String> = emptyList(),
        val setCode: String = "",
        /** "draftsim" or "llm" */
        val deckbuildMode: String = "draftsim",
        val bestOf: Int = 3
    )

    data class SpeedRequest(val thinkingDelayMs: Long = 500)

    /** Token usage + dollar cost. costKnown=false means costUsd is a floor (provider didn't report cost). */
    data class CostView(
        val costUsd: Double,
        val totalTokens: Long,
        val calls: Int,
        val costKnown: Boolean
    )

    data class ParticipantView(
        val id: String,
        val modelId: String,
        val displayName: String,
        val seed: Int,
        val buildStatus: String,
        val deckSize: Int,
        val buildCost: CostView?,
        /** The built deck as cardName -> count (empty until the deck is built). */
        val deck: Map<String, Int>
    )

    data class FinishedGameView(
        val gameSessionId: String,
        val cost: CostView
    )

    data class MatchView(
        val id: String,
        val roundNumber: Int,
        val slot: Int,
        val player1Name: String?,
        val player2Name: String?,
        val player1ModelId: String?,
        val player2ModelId: String?,
        val player1GameWins: Int,
        val player2GameWins: Int,
        val winnerName: String?,
        val bye: Boolean,
        val complete: Boolean,
        val status: String,
        val currentGameSessionId: String?,
        val finishedGames: List<FinishedGameView>,
        val cost: CostView
    )

    data class RoundView(val roundNumber: Int, val matches: List<MatchView>)

    data class TournamentView(
        val id: String,
        val setCode: String,
        val deckbuildMode: String,
        val bestOf: Int,
        val status: String,
        val thinkingDelayMs: Long,
        val championName: String?,
        val participants: List<ParticipantView>,
        val rounds: List<RoundView>,
        /** Total LLM cost of games played so far. */
        val gameCost: CostView,
        /** Total LLM cost of building all decks (zero for heuristic). */
        val deckbuildCost: CostView,
        /** False when no OpenRouter key is configured — decks + play silently fall back to engine AI. */
        val llmAvailable: Boolean
    )

    /** Mirrors ServerMessage.AvailableSet so the dev page can reuse the shared SetPickerModal. */
    data class SetInfo(
        val code: String,
        val name: String,
        val partial: Boolean,
        /** Extension sets (bonus sheets) can't play alone; the single-set picker hides them. */
        val extensionSet: Boolean,
        val block: String?,
        val implementedCount: Int,
        val releaseDate: String?
    )

    // ---- Endpoints ------------------------------------------------------------

    @PostMapping
    fun create(@RequestBody request: CreateRequest): ResponseEntity<Any> {
        return try {
            val mode = when (request.deckbuildMode.lowercase()) {
                "llm" -> DeckbuildMode.LLM
                else -> DeckbuildMode.DRAFTSIM
            }
            val tournament = service.create(request.modelIds, request.setCode, mode, request.bestOf.coerceIn(1, 9))
            ResponseEntity.ok(toView(tournament))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid request")))
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Cannot create tournament")))
        } catch (e: Exception) {
            logger.error("Failed to create LLM tournament", e)
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Internal error")))
        }
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): ResponseEntity<Any> {
        val tournament = service.get(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toView(tournament))
    }

    @GetMapping
    fun list(): ResponseEntity<List<TournamentView>> =
        ResponseEntity.ok(service.all().map { toView(it) })

    @PostMapping("/{id}/start")
    fun start(@PathVariable id: String) = control(id) { service.start(it) }

    @PostMapping("/{id}/pause")
    fun pause(@PathVariable id: String) = control(id) { service.pause(it) }

    @PostMapping("/{id}/resume")
    fun resume(@PathVariable id: String) = control(id) { service.start(it) }

    @PostMapping("/{id}/step")
    fun step(@PathVariable id: String) = control(id) { service.step(it) }

    @PostMapping("/{id}/speed")
    fun speed(@PathVariable id: String, @RequestBody request: SpeedRequest) =
        control(id) { service.setSpeed(it, request.thinkingDelayMs) }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Any> {
        val tournament = service.get(id) ?: return ResponseEntity.notFound().build()
        service.delete(tournament)
        return ResponseEntity.ok(mapOf("deleted" to id))
    }

    @GetMapping("/sets")
    fun listSets(): ResponseEntity<List<SetInfo>> =
        ResponseEntity.ok(boosterGenerator.availableSets.values.map { config ->
            SetInfo(
                code = config.setCode,
                name = config.setName,
                partial = !config.fullyImplemented,
                extensionSet = config.extensionSet,
                block = config.block,
                implementedCount = config.distinctCardCount,
                releaseDate = config.releaseDate
            )
        })

    private fun control(id: String, action: (LlmTournament) -> Unit): ResponseEntity<Any> {
        val tournament = service.get(id) ?: return ResponseEntity.notFound().build()
        action(tournament)
        return ResponseEntity.ok(toView(tournament))
    }

    // ---- View mapping ---------------------------------------------------------

    private fun toView(t: LlmTournament): TournamentView {
        return TournamentView(
            id = t.id,
            setCode = t.setCode,
            deckbuildMode = t.deckbuildMode.name.lowercase(),
            bestOf = t.bestOf,
            status = t.status.name,
            thinkingDelayMs = t.thinkingDelayMs,
            championName = t.champion?.displayName,
            participants = t.participants.map { p ->
                ParticipantView(
                    id = p.id.value,
                    modelId = p.modelId,
                    displayName = p.displayName,
                    seed = p.seed,
                    buildStatus = p.buildStatus.name,
                    deckSize = p.deck.values.sum(),
                    buildCost = p.buildCost?.let { costView(it) },
                    deck = p.deck
                )
            },
            rounds = t.rounds.map { round ->
                RoundView(round.roundNumber, round.matches.map { matchView(t, it) })
            },
            gameCost = costView(t.totalGameCost()),
            deckbuildCost = costView(t.totalDeckbuildCost()),
            llmAvailable = service.llmAvailable()
        )
    }

    private fun costView(c: com.wingedsheep.gameserver.tournament.llm.LlmCost) =
        CostView(costUsd = c.costUsd, totalTokens = c.totalTokens, calls = c.calls, costKnown = c.costKnown)

    private fun matchView(t: LlmTournament, m: LlmMatch): MatchView {
        val status = when {
            m.isBye -> "bye"
            m.isComplete -> "done"
            m.currentGameSessionId != null -> "live"
            else -> "scheduled"
        }
        return MatchView(
            id = m.id,
            roundNumber = m.roundNumber,
            slot = m.slot,
            player1Name = t.participant(m.player1Id)?.displayName,
            player2Name = t.participant(m.player2Id)?.displayName,
            player1ModelId = t.participant(m.player1Id)?.modelId,
            player2ModelId = t.participant(m.player2Id)?.modelId,
            player1GameWins = m.player1GameWins,
            player2GameWins = m.player2GameWins,
            winnerName = t.participant(m.winnerId)?.displayName,
            bye = m.isBye,
            complete = m.isComplete,
            status = status,
            currentGameSessionId = m.currentGameSessionId,
            finishedGames = m.finishedGames.map { FinishedGameView(it.gameSessionId, costView(it.cost)) },
            cost = costView(m.finishedGames.fold(
                com.wingedsheep.gameserver.tournament.llm.LlmCost.ZERO
            ) { acc, g -> acc + g.cost })
        )
    }
}
