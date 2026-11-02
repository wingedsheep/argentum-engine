package com.wingedsheep.gameserver.tournament.llm

import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.handler.GamePlayHandler
import com.wingedsheep.sdk.model.EntityId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Dev-only orchestrator for an all-LLM, single-elimination, best-of-N sealed tournament.
 *
 * Reuses the existing leaf primitives — [AiGameManager.createAiIdentity]/`wireAiForGame`,
 * [GamePlayHandler.createAndStartAiVsAiGame], [BoosterGenerator], the replay stack — without
 * touching the production round-robin tournament-lobby flow. It owns its own bracket model and
 * a pacing gate ([LlmTournament.gameInFlight]) so at most one game runs at a time and the user
 * controls when each game starts.
 *
 * Pacing is at game boundaries: you can't cleanly freeze a half-resolved engine game, so "pause"
 * means "stop after the current game finishes."
 */
@Service
class LlmTournamentService(
    private val gamePlayHandler: GamePlayHandler,
    private val aiGameManager: AiGameManager,
    private val boosterGenerator: BoosterGenerator,
    private val deckBuildService: LlmSealedDeckBuildService,
    private val gameProperties: GameProperties,
    private val costTracker: LlmCostTracker
) {
    private val logger = LoggerFactory.getLogger(LlmTournamentService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val tournaments = ConcurrentHashMap<String, LlmTournament>()

    fun get(id: String): LlmTournament? = tournaments[id]
    fun all(): List<LlmTournament> = tournaments.values.toList()

    /**
     * Whether the LLM is actually reachable. When false, both deck building and in-game play fall
     * back to the built-in engine AI no matter what the user picks — the UI surfaces this.
     */
    fun llmAvailable(): Boolean =
        gameProperties.ai.enabled && gameProperties.ai.effectiveApiKey.isNotBlank()

    // =========================================================================
    // Creation + build
    // =========================================================================

    fun create(modelIds: List<String>, setCode: String, deckbuildMode: DeckbuildMode, bestOf: Int = 3): LlmTournament {
        require(modelIds.size >= 2) { "A tournament needs at least 2 models" }
        require(aiGameManager.isEnabled) {
            "AI is not enabled. Set game.ai.enabled=true and provide an OpenRouter API key (game.ai.mode=llm)."
        }
        require(boosterGenerator.availableSets.containsKey(setCode)) { "Unknown set code: $setCode" }

        val participants = modelIds.mapIndexed { index, modelId ->
            val identity = aiGameManager.createAiIdentity(modelOverride = modelId)
            LlmParticipant(
                id = identity.playerId,
                modelId = modelId,
                displayName = identity.playerName,
                seed = index + 1
            )
        }

        val tournament = LlmTournament(
            id = "llmt-${UUID.randomUUID().toString().take(8)}",
            setCode = setCode,
            deckbuildMode = deckbuildMode,
            bestOf = bestOf,
            participants = participants
        )
        tournament.thinkingDelayMs = gameProperties.ai.thinkingDelayMs
        tournaments[tournament.id] = tournament

        seedFirstRound(tournament)
        scope.launch { buildAll(tournament) }

        logger.info("Created LLM tournament {} ({} models, set={}, mode={}, bestOf={})",
            tournament.id, modelIds.size, setCode, deckbuildMode, bestOf)
        return tournament
    }

    private suspend fun buildAll(tournament: LlmTournament) {
        val useLlm = tournament.deckbuildMode == DeckbuildMode.LLM
        try {
            tournament.participants.map { participant ->
                scope.async {
                    try {
                        participant.buildStatus = ParticipantBuildStatus.BUILDING_POOL
                        participant.pool = boosterGenerator.generateSealedPool(tournament.setCode, boosterCount = 6)
                        participant.buildStatus = ParticipantBuildStatus.BUILDING_DECK
                        val bucket = "build:${participant.id.value}"
                        val deck = deckBuildService.build(
                            pool = participant.pool,
                            setCodes = listOf(tournament.setCode),
                            useLlm = useLlm,
                            modelOverride = participant.modelId,
                            usageSink = { usage -> costTracker.record(bucket, usage) }
                        )
                        participant.deck = deck
                        participant.buildCost = costTracker.snapshot(bucket).takeIf { it.calls > 0 }
                        costTracker.clear(bucket)
                        participant.buildStatus =
                            if (deck.values.sum() >= 40) ParticipantBuildStatus.READY else ParticipantBuildStatus.FAILED
                    } catch (e: Exception) {
                        logger.error("Deck build failed for {} ({}): {}", participant.displayName, participant.modelId, e.message, e)
                        participant.buildStatus = ParticipantBuildStatus.FAILED
                    }
                }
            }.awaitAll()
        } finally {
            synchronized(tournament.lock) {
                if (tournament.status == LlmTournamentStatus.BUILDING) {
                    tournament.status = LlmTournamentStatus.READY
                }
            }
            logger.info("LLM tournament {} decks built — ready", tournament.id)
        }
    }

    // =========================================================================
    // Bracket
    // =========================================================================

    private fun seedFirstRound(tournament: LlmTournament) {
        synchronized(tournament.lock) {
            val n = tournament.participants.size
            val size = nextPowerOfTwo(n)
            val seedOrder = bracketSeedOrder(size) // 1-based seed numbers in bracket slot order
            val slots = seedOrder.map { seedNum -> tournament.participants.getOrNull(seedNum - 1) }

            val matches = (0 until size step 2).mapIndexed { slotIndex, i ->
                val p1 = slots[i]
                val p2 = slots[i + 1]
                LlmMatch(
                    id = "m-${UUID.randomUUID().toString().take(6)}",
                    roundNumber = 1,
                    slot = slotIndex,
                    player1Id = p1?.id,
                    player2Id = p2?.id
                ).also { resolveByeIfNeeded(it) }
            }
            tournament.rounds.add(LlmRound(1, matches))
            advanceBracketIfRoundComplete(tournament)
        }
    }

    /** A match with one empty seat is an auto-win for the present player. */
    private fun resolveByeIfNeeded(match: LlmMatch) {
        val p1 = match.player1Id
        val p2 = match.player2Id
        when {
            p1 != null && p2 == null -> { match.winnerId = p1; match.isComplete = true }
            p1 == null && p2 != null -> { match.player1Id = p2; match.player2Id = null; match.winnerId = p2; match.isComplete = true }
            p1 == null && p2 == null -> { match.isComplete = true } // empty match (shouldn't happen for n>=2)
        }
    }

    /** While the current round is fully decided, build the next round from its winners. */
    private fun advanceBracketIfRoundComplete(tournament: LlmTournament) {
        while (true) {
            val current = tournament.currentRound ?: return
            if (!current.isComplete) return
            if (current.matches.size <= 1) {
                tournament.status = LlmTournamentStatus.COMPLETE
                logger.info("LLM tournament {} complete — champion {}",
                    tournament.id, tournament.participant(current.matches.firstOrNull()?.winnerId)?.displayName)
                return
            }
            val nextMatches = (current.matches.indices step 2).mapIndexed { slotIndex, i ->
                LlmMatch(
                    id = "m-${UUID.randomUUID().toString().take(6)}",
                    roundNumber = current.roundNumber + 1,
                    slot = slotIndex,
                    player1Id = current.matches[i].winnerId,
                    player2Id = current.matches[i + 1].winnerId
                ).also { resolveByeIfNeeded(it) }
            }
            tournament.rounds.add(LlmRound(current.roundNumber + 1, nextMatches))
        }
    }

    private fun findNextPlayableMatch(tournament: LlmTournament): LlmMatch? {
        advanceBracketIfRoundComplete(tournament)
        return tournament.currentRound?.matches?.firstOrNull { it.isPlayable() }
    }

    // =========================================================================
    // Pacing controls
    // =========================================================================

    fun start(tournament: LlmTournament) {
        synchronized(tournament.lock) {
            if (tournament.status != LlmTournamentStatus.READY && tournament.status != LlmTournamentStatus.PAUSED) return
            tournament.status = LlmTournamentStatus.RUNNING
        }
        scope.launch { tryLaunchNextGame(tournament, force = false) }
    }

    fun pause(tournament: LlmTournament) {
        synchronized(tournament.lock) {
            if (tournament.status == LlmTournamentStatus.RUNNING) tournament.status = LlmTournamentStatus.PAUSED
        }
    }

    /** Play exactly one more game, even when paused (does not change RUNNING/PAUSED). */
    fun step(tournament: LlmTournament) {
        val canStep = tournament.status == LlmTournamentStatus.READY || tournament.status == LlmTournamentStatus.PAUSED
        if (!canStep) return
        scope.launch { tryLaunchNextGame(tournament, force = true) }
    }

    fun setSpeed(tournament: LlmTournament, thinkingDelayMs: Long) {
        tournament.thinkingDelayMs = thinkingDelayMs.coerceIn(0, 60_000)
        // Apply to the currently live match's AI players so the change takes effect immediately.
        synchronized(tournament.lock) {
            tournament.currentRound?.matches?.firstOrNull { it.currentGameSessionId != null }?.let { live ->
                listOfNotNull(live.player1Id, live.player2Id).forEach {
                    aiGameManager.setThinkingDelay(it, tournament.thinkingDelayMs)
                }
            }
        }
    }

    fun delete(tournament: LlmTournament) {
        tournaments.remove(tournament.id)
    }

    private fun tryLaunchNextGame(tournament: LlmTournament, force: Boolean): Boolean {
        synchronized(tournament.lock) {
            if (tournament.status == LlmTournamentStatus.COMPLETE || tournament.status == LlmTournamentStatus.BUILDING) return false
            if (!force && tournament.status != LlmTournamentStatus.RUNNING) return false
            if (!tournament.gameInFlight.compareAndSet(false, true)) return false // a game is already live

            val match = findNextPlayableMatch(tournament)
            if (match == null) {
                tournament.gameInFlight.set(false)
                return false
            }
            val p1 = tournament.participant(match.player1Id)
            val p2 = tournament.participant(match.player2Id)
            if (p1 == null || p2 == null) {
                tournament.gameInFlight.set(false)
                return false
            }

            val gameId = gamePlayHandler.createAndStartAiVsAiGame(
                player1Id = p1.id,
                player1Deck = p1.deck,
                player2Id = p2.id,
                player2Deck = p2.deck,
                setCode = tournament.setCode,
                thinkingDelayMs = tournament.thinkingDelayMs
            )
            match.currentGameSessionId = gameId
            tournament.gameToMatch[gameId] = match
            logger.info("LLM tournament {}: launched game {} for match {} (round {})",
                tournament.id, gameId, match.id, match.roundNumber)
            return true
        }
    }

    // =========================================================================
    // Game-over callback (wired in GameWebSocketHandler)
    // =========================================================================

    fun onGameComplete(gameSessionId: String, winnerId: EntityId?, winnerLifeRemaining: Int) {
        val tournament = tournaments.values.firstOrNull { it.gameToMatch.containsKey(gameSessionId) } ?: return
        synchronized(tournament.lock) {
            val match = tournament.gameToMatch[gameSessionId] ?: return
            tournament.gameInFlight.set(false)
            match.currentGameSessionId = null
            val gameCost = costTracker.snapshot(gameSessionId)
            costTracker.clear(gameSessionId)
            match.finishedGames.add(FinishedGame(gameSessionId, gameCost))

            when (winnerId) {
                match.player1Id -> match.player1GameWins++
                match.player2Id -> match.player2GameWins++
                else -> {} // draw / unknown — counts for neither
            }

            when {
                match.player1GameWins >= tournament.winsNeeded -> { match.winnerId = match.player1Id; match.isComplete = true }
                match.player2GameWins >= tournament.winsNeeded -> { match.winnerId = match.player2Id; match.isComplete = true }
                match.finishedGames.size >= tournament.bestOf -> {
                    // Out of games (e.g. draws); decide on the game tally, player1 wins ties.
                    match.winnerId = if (match.player1GameWins >= match.player2GameWins) match.player1Id else match.player2Id
                    match.isComplete = true
                }
            }

            if (match.isComplete) {
                logger.info("LLM tournament {}: match {} complete, winner {}",
                    tournament.id, match.id, tournament.participant(match.winnerId)?.displayName)
                advanceBracketIfRoundComplete(tournament)
            }
        }

        if (tournament.status == LlmTournamentStatus.RUNNING) {
            scope.launch { tryLaunchNextGame(tournament, force = false) }
        }
    }

    // =========================================================================
    // Bracket math
    // =========================================================================

    private fun nextPowerOfTwo(n: Int): Int {
        var p = 1
        while (p < n) p *= 2
        return p
    }

    /**
     * Standard single-elimination seeding order for [size] (a power of two): returns the 1-based
     * seed number for each bracket slot, so top seeds meet byes/low seeds first. e.g. size 4 -> [1,4,2,3].
     */
    private fun bracketSeedOrder(size: Int): List<Int> {
        var seeds = listOf(1)
        while (seeds.size < size) {
            val roundSize = seeds.size * 2
            val next = ArrayList<Int>(roundSize)
            for (s in seeds) {
                next.add(s)
                next.add(roundSize + 1 - s)
            }
            seeds = next
        }
        return seeds
    }
}
