package com.wingedsheep.gameserver.websocket

import com.wingedsheep.gameserver.handler.ConnectionHandler
import com.wingedsheep.gameserver.handler.GamePlayHandler
import com.wingedsheep.gameserver.handler.LobbyHandler
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class GameWebSocketHandler(
    private val connectionHandler: ConnectionHandler,
    private val gamePlayHandler: GamePlayHandler,
    private val lobbyHandler: LobbyHandler,
    private val sender: MessageSender
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(GameWebSocketHandler::class.java)

    @PostConstruct
    fun wireCallbacks() {
        // Wire cross-handler callbacks to avoid circular dependencies
        gamePlayHandler.handleRoundCompleteCallback = { lobbyId -> lobbyHandler.handleRoundComplete(lobbyId) }
        gamePlayHandler.broadcastActiveMatchesCallback = { lobbyId -> lobbyHandler.broadcastActiveMatchesToWaitingPlayers(lobbyId) }
        gamePlayHandler.joinSealedGameCallback = { session, msg -> lobbyHandler.handleJoinSealedGame(session, msg) }
        gamePlayHandler.joinLobbyCallback = { session, msg -> lobbyHandler.handleJoinLobby(session, msg) }
        connectionHandler.handleGameOverCallback = { gameSession, reason -> gamePlayHandler.handleGameOver(gameSession, reason) }
        connectionHandler.handleRoundCompleteCallback = { lobbyId -> lobbyHandler.handleRoundComplete(lobbyId) }
        connectionHandler.broadcastStateUpdateCallback = { gameSession, events -> gamePlayHandler.broadcastStateUpdate(gameSession, events) }
        connectionHandler.sendActiveMatchesToPlayerCallback = { identity, wsSession -> lobbyHandler.sendActiveMatchesToPlayer(identity, wsSession) }
        connectionHandler.restoreSpectatingCallback = { identity, playerSession, wsSession, gameSessionId ->
            lobbyHandler.restoreSpectating(identity, playerSession, wsSession, gameSessionId)
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info("WebSocket connection established: ${session.id}")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            val clientMessage = sender.json.decodeFromString<ClientMessage>(message.payload)
            logger.debug("Received message from ${session.id}: $clientMessage")

            when (clientMessage) {
                is ClientMessage.Connect -> connectionHandler.handleConnect(session, clientMessage)

                is ClientMessage.CreateGame,
                is ClientMessage.JoinGame,
                is ClientMessage.SubmitAction,
                is ClientMessage.Concede,
                is ClientMessage.CancelGame,
                is ClientMessage.KeepHand,
                is ClientMessage.Mulligan,
                is ClientMessage.ChooseBottomCards,
                is ClientMessage.UpdateBlockerAssignments,
                is ClientMessage.SetFullControl -> gamePlayHandler.handle(session, clientMessage)

                is ClientMessage.CreateSealedGame,
                is ClientMessage.JoinSealedGame,
                is ClientMessage.SubmitSealedDeck,
                is ClientMessage.UnsubmitDeck,
                is ClientMessage.CreateTournamentLobby,
                is ClientMessage.JoinLobby,
                is ClientMessage.StartTournamentLobby,
                is ClientMessage.MakePick,
                is ClientMessage.LeaveLobby,
                is ClientMessage.StopLobby,
                is ClientMessage.UpdateLobbySettings -> lobbyHandler.handle(session, clientMessage)

                is ClientMessage.ReadyForNextRound -> {
                    lobbyHandler.handleReadyForNextRound(session)
                }

                is ClientMessage.SpectateGame,
                is ClientMessage.StopSpectating -> lobbyHandler.handle(session, clientMessage)
            }
        } catch (e: Exception) {
            logger.error("Error handling message from ${session.id}", e)
            sender.sendError(session, ErrorCode.INTERNAL_ERROR, "Failed to process message: ${e.message}")
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("WebSocket connection closed: ${session.id}, status: $status")
        connectionHandler.handleDisconnect(session)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        logger.error("Transport error for ${session.id}", exception)
        connectionHandler.handleDisconnect(session)
    }
}
