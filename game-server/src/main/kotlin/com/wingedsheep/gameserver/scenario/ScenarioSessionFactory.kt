package com.wingedsheep.gameserver.scenario

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.handler.GamePlayHandler
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Turns a built scenario state into a live [GameSession] and wires the opponent seat per
 * [ScenarioMode]. Shared by the dev and production scenario controllers so session creation
 * (state injection, stop overrides, identity pre-registration, AI / hotseat wiring) lives in
 * one place.
 */
@Service
class ScenarioSessionFactory(
    private val cardRegistry: CardRegistry,
    private val printingRegistry: PrintingRegistry,
    private val gameRepository: GameRepository,
    private val sessionRegistry: SessionRegistry,
    private val aiGameManager: AiGameManager,
    private val gamePlayHandler: GamePlayHandler,
) {
    private val logger = LoggerFactory.getLogger(ScenarioSessionFactory::class.java)
    private val stateTransformer = ClientStateTransformer(cardRegistry)

    /**
     * Create and persist a session for [build]. [player1Token]/[player2Token] pin specific
     * tokens (dev workflow uses stable "p1"/"p2"); when null a random token is generated
     * (production). [includeDevUrls] adds localhost open-URLs to the human-readable message.
     */
    fun createSession(
        build: ScenarioBuildResult,
        request: ScenarioRequest,
        player1Token: String? = null,
        player2Token: String? = null,
        includeDevUrls: Boolean = false,
    ): ScenarioResponse {
        val mode = request.effectiveMode
        val seats = request.seats()
        val seatNames = seats.map { it.first }
        val seatIds = build.playerIds
        val p1Name = seatNames[0]
        val p2Name = seatNames.getOrElse(1) { "Player2" }
        val player1Id = build.player1Id
        val player2Id = build.player2Id

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            stateTransformer = stateTransformer,
            printingRegistry = printingRegistry,
        )
        gameSession.injectStateForDevScenario(build.state)

        // Per-step stop overrides (prevents auto-pass at specified steps). The stop
        // fields are two-seat tooling; they keep applying to the first two seats.
        val p1Stops = request.player1StopAtSteps.orEmpty()
        val p1OppStops = request.player1OpponentStopAtSteps.orEmpty()
        if (p1Stops.isNotEmpty() || p1OppStops.isNotEmpty()) {
            gameSession.setStopOverrides(player1Id, p1Stops.toSet(), p1OppStops.toSet())
        }
        val p2Stops = request.player2StopAtSteps.orEmpty()
        val p2OppStops = request.player2OpponentStopAtSteps.orEmpty()
        if (p2Stops.isNotEmpty() || p2OppStops.isNotEmpty()) {
            gameSession.setStopOverrides(player2Id, p2Stops.toSet(), p2OppStops.toSet())
        }

        gameRepository.save(gameSession)

        fun preRegister(token: String, playerId: com.wingedsheep.sdk.model.EntityId, name: String): PlayerIdentity =
            PlayerIdentity(token = token, playerId = playerId, playerName = name)
                .apply { currentGameSessionId = gameSession.sessionId }
                .also { sessionRegistry.preRegisterIdentity(it) }

        return when (mode) {
            ScenarioMode.SELF -> {
                // One human connection controls EVERY seat (2-4 player pods). Pre-register
                // only player1's identity and route every seat's input authority to it.
                val token = player1Token ?: newToken()
                val identity = preRegister(token, player1Id, p1Name)
                gameSession.enableHotseat(player1Id)
                val roster = seatIds.mapIndexed { i, id ->
                    PlayerInfo(seatNames.getOrElse(i) { "Player${i + 1}" }, identity.token, id.value)
                }
                logger.info("Created hotseat scenario session ${gameSession.sessionId} (${seatIds.size} seats)")
                ScenarioResponse(
                    sessionId = gameSession.sessionId,
                    // Every PlayerInfo carries the same token: the one connection plays all seats.
                    player1 = roster[0],
                    player2 = roster.getOrElse(1) { roster[0] },
                    message = if (seatIds.size > 2)
                        "Hotseat scenario created — you control all ${seatIds.size} players."
                    else
                        "Hotseat scenario created — you control both players.",
                    mode = mode,
                    players = if (seatIds.size > 2) roster else null,
                )
            }

            ScenarioMode.AI -> {
                val aiSeat = request.aiPlayer ?: 2
                val humanSeat = if (aiSeat == 1) 2 else 1
                val humanToken = (if (humanSeat == 1) player1Token else player2Token) ?: newToken()
                val humanId = if (humanSeat == 1) player1Id else player2Id
                val humanName = if (humanSeat == 1) p1Name else p2Name
                preRegister(humanToken, humanId, humanName)

                val (aiSeatId, aiSeatName) = if (aiSeat == 1) player1Id to p1Name else player2Id to p2Name
                aiGameManager.wireAiForDevScenario(
                    gameSession = gameSession,
                    aiPlayerId = aiSeatId,
                    playerName = aiSeatName,
                    onActionReady = { id, action -> gamePlayHandler.handleAiAction(gameSession, id, action) },
                    onMulliganKeep = { id -> gamePlayHandler.handleAiMulliganKeep(gameSession, id) },
                    onMulliganTake = { id -> gamePlayHandler.handleAiMulliganTake(gameSession, id) },
                    onBottomCards = { id, cardIds -> gamePlayHandler.handleAiBottomCards(gameSession, id, cardIds) }
                )
                // Kick off the AI if it holds priority in the injected state.
                gamePlayHandler.broadcastStateUpdate(gameSession, emptyList())
                logger.info("Created vs-AI scenario session ${gameSession.sessionId} (AI is player $aiSeat)")

                val p1Token = if (aiSeat == 1) "(AI)" else humanToken
                val p2Token = if (aiSeat == 2) "(AI)" else humanToken
                val openMsg = if (includeDevUrls)
                    "Scenario created vs AI. Open http://localhost:5173/?token=$humanToken (you are Player $humanSeat)"
                else
                    "Scenario created vs AI — you are Player $humanSeat."
                ScenarioResponse(
                    sessionId = gameSession.sessionId,
                    player1 = PlayerInfo(p1Name, p1Token, player1Id.value),
                    player2 = PlayerInfo(p2Name, p2Token, player2Id.value),
                    message = openMsg,
                    mode = mode,
                )
            }

            ScenarioMode.TWO_PLAYER -> {
                val t1 = player1Token ?: newToken()
                val t2 = player2Token ?: newToken()
                preRegister(t1, player1Id, p1Name)
                preRegister(t2, player2Id, p2Name)
                logger.info("Created two-player scenario session ${gameSession.sessionId}")
                val openMsg = if (includeDevUrls)
                    "Scenario created. Open http://localhost:5173/?token=$t1 (Player 1) or http://localhost:5173/?token=$t2 (Player 2)"
                else
                    "Scenario created — share each token with a player."
                ScenarioResponse(
                    sessionId = gameSession.sessionId,
                    player1 = PlayerInfo(p1Name, t1, player1Id.value),
                    player2 = PlayerInfo(p2Name, t2, player2Id.value),
                    message = openMsg,
                    mode = mode,
                )
            }
        }
    }

    /**
     * Create a session by injecting a complete [GameState] verbatim — used by "share frame as
     * scenario" to reproduce the EXACT replay position (stack, targets, floating effects, mana,
     * counters, all trackers). Player seats/names are taken from the state's turn order. Supports
     * hotseat ([ScenarioMode.SELF], default) and [ScenarioMode.TWO_PLAYER].
     */
    fun createSessionFromState(state: GameState, mode: ScenarioMode): ScenarioResponse {
        val p1Id = state.turnOrder.getOrNull(0) ?: error("Snapshot has no players")
        val p2Id = state.turnOrder.getOrNull(1) ?: error("Snapshot needs two players")
        val p1Name = state.getEntity(p1Id)?.get<PlayerComponent>()?.name ?: "Player 1"
        val p2Name = state.getEntity(p2Id)?.get<PlayerComponent>()?.name ?: "Player 2"

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            stateTransformer = stateTransformer,
            printingRegistry = printingRegistry,
        )
        gameSession.injectStateForDevScenario(state)
        gameRepository.save(gameSession)

        fun preRegister(token: String, playerId: com.wingedsheep.sdk.model.EntityId, name: String) =
            PlayerIdentity(token = token, playerId = playerId, playerName = name)
                .apply { currentGameSessionId = gameSession.sessionId }
                .also { sessionRegistry.preRegisterIdentity(it) }

        return if (mode == ScenarioMode.TWO_PLAYER) {
            val t1 = newToken()
            val t2 = newToken()
            preRegister(t1, p1Id, p1Name)
            preRegister(t2, p2Id, p2Name)
            logger.info("Loaded snapshot session ${gameSession.sessionId} (two-player)")
            ScenarioResponse(
                sessionId = gameSession.sessionId,
                player1 = PlayerInfo(p1Name, t1, p1Id.value),
                player2 = PlayerInfo(p2Name, t2, p2Id.value),
                message = "Snapshot loaded — share each token with a player.",
                mode = ScenarioMode.TWO_PLAYER,
            )
        } else {
            // SELF / hotseat (default for snapshots): one connection controls both seats.
            val token = newToken()
            preRegister(token, p1Id, p1Name)
            gameSession.enableHotseat(p1Id)
            logger.info("Loaded snapshot session ${gameSession.sessionId} (hotseat)")
            ScenarioResponse(
                sessionId = gameSession.sessionId,
                player1 = PlayerInfo(p1Name, token, p1Id.value),
                player2 = PlayerInfo(p2Name, token, p2Id.value),
                message = "Snapshot loaded — you control both players.",
                mode = ScenarioMode.SELF,
            )
        }
    }

    private fun newToken(): String = UUID.randomUUID().toString()
}
