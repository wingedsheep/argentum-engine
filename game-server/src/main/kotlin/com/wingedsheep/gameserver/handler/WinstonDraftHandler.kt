package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.lobby.WinstonActionResult
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.model.EntityId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class WinstonDraftHandler(
    private val ctx: LobbySharedContext
) {
    private val logger = LoggerFactory.getLogger(WinstonDraftHandler::class.java)

    /** Callback invoked when draft completes — set by LobbyHandler to trigger AI deck building. */
    @Volatile var onDraftComplete: ((TournamentLobby) -> Unit)? = null

    fun handleWinstonTakePile(session: WebSocketSession) {
        val (identity, lobby) = ctx.getIdentityAndLobby(session) ?: return

        if (lobby.format != TournamentFormat.WINSTON_DRAFT) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not a Winston Draft")
            return
        }

        synchronized(lobby.winstonLock) {
            val result = lobby.winstonTakePile(identity.playerId)
            handleWinstonResult(lobby, result, identity)
        }
    }

    fun handleWinstonSkipPile(session: WebSocketSession) {
        val (identity, lobby) = ctx.getIdentityAndLobby(session) ?: return

        if (lobby.format != TournamentFormat.WINSTON_DRAFT) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not a Winston Draft")
            return
        }

        synchronized(lobby.winstonLock) {
            val result = lobby.winstonSkipPile(identity.playerId)
            handleWinstonResult(lobby, result, identity)
        }
    }

    fun handleWinstonResult(lobby: TournamentLobby, result: WinstonActionResult, identity: PlayerIdentity) {
        when (result) {
            is WinstonActionResult.PileTaken,
            is WinstonActionResult.BlindPick,
            is WinstonActionResult.PileSkipped -> {
                val lastAction = when (result) {
                    is WinstonActionResult.PileTaken -> result.lastAction
                    is WinstonActionResult.BlindPick -> result.lastAction
                    is WinstonActionResult.PileSkipped -> result.lastAction
                }
                logger.info("Winston Draft [${lobby.lobbyId}]: $lastAction")

                when (result) {
                    is WinstonActionResult.PileTaken, is WinstonActionResult.BlindPick -> {
                        lobby.pickTimerJob?.cancel()
                        startWinstonTimer(lobby)
                    }
                    else -> {}
                }

                val pickedCards = when (result) {
                    is WinstonActionResult.PileTaken -> result.cards
                    is WinstonActionResult.BlindPick -> listOf(result.card)
                    else -> emptyList()
                }
                broadcastWinstonDraftState(lobby, lastAction, pickedCards, identity.playerId)
            }

            is WinstonActionResult.DraftComplete -> {
                logger.info("Winston Draft complete for lobby ${lobby.lobbyId}: ${result.lastAction}")
                lobby.pickTimerJob?.cancel()
                lobby.pickTimerJob = null

                val basicLandInfos = lobby.basicLands.values.map { cardToSealedCardInfo(it) }
                lobby.players.forEach { (_, playerState) ->
                    val poolInfos = playerState.cardPool.map { cardToSealedCardInfo(it) }
                    val ws = playerState.identity.webSocketSession
                    if (ws != null && ws.isOpen) {
                        ctx.sender.send(ws, ServerMessage.DraftComplete(
                            pickedCards = poolInfos,
                            basicLands = basicLandInfos
                        ))
                    }
                }

                ctx.broadcastLobbyUpdate(lobby)

                // Trigger AI deck building after draft completes
                onDraftComplete?.invoke(lobby)
            }

            is WinstonActionResult.Error -> {
                val ws = identity.webSocketSession
                if (ws != null) {
                    ctx.sender.sendError(ws, ErrorCode.INVALID_ACTION, result.message)
                }
            }
        }
    }

    fun broadcastWinstonDraftState(lobby: TournamentLobby, lastAction: String?, lastPickedCards: List<com.wingedsheep.sdk.model.CardDefinition> = emptyList(), lastPickerPlayerId: EntityId? = null) {
        val activePlayerId = lobby.getWinstonActivePlayerId() ?: return
        val activePlayerName = lobby.players[activePlayerId]?.identity?.playerName ?: "Unknown"
        val pileSizes = lobby.winstonPiles.map { it.size }
        val currentPileCards = lobby.winstonPiles[lobby.winstonCurrentPileIndex].map { cardToSealedCardInfo(it) }

        val seenByActive = lobby.winstonSeenCards.getOrPut(activePlayerId) { mutableSetOf() }
        for (card in lobby.winstonPiles[lobby.winstonCurrentPileIndex]) {
            seenByActive.add(card.name)
        }

        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession ?: return@forEach
            if (!ws.isOpen) return@forEach

            val isActivePlayer = playerId == activePlayerId
            val opponentId = lobby.getWinstonPlayerOrder().first { it != playerId }
            val opponentCards = lobby.players[opponentId]?.cardPool ?: emptyList()
            val seenByPlayer = lobby.winstonSeenCards[playerId] ?: emptySet()

            val knownOpponentCards = opponentCards.filter { it.name in seenByPlayer }.map { cardToSealedCardInfo(it) }
            val unknownOpponentCardCount = opponentCards.size - knownOpponentCards.size

            val lastPicked = if (lastPickerPlayerId != null && lastPickerPlayerId != playerId) {
                lastPickedCards.map { cardToSealedCardInfo(it) }
            } else emptyList()

            ctx.sender.send(ws, ServerMessage.WinstonDraftState(
                activePlayerName = activePlayerName,
                isYourTurn = isActivePlayer,
                currentPileIndex = lobby.winstonCurrentPileIndex,
                pileSizes = pileSizes,
                mainDeckRemaining = lobby.winstonMainDeck.size,
                currentPileCards = if (isActivePlayer) currentPileCards else null,
                pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) },
                totalPickedByOpponent = opponentCards.size,
                knownOpponentCards = knownOpponentCards,
                unknownOpponentCardCount = unknownOpponentCardCount,
                lastAction = lastAction,
                timeRemainingSeconds = lobby.pickTimeRemaining,
                lastPickedCards = lastPicked
            ))
        }
    }

    fun startWinstonTimer(lobby: TournamentLobby) {
        lobby.pickTimeRemaining = lobby.pickTimeSeconds
        lobby.pickTimerJob?.cancel()

        lobby.pickTimerJob = ctx.draftScope.launch {
            var remaining = lobby.pickTimeSeconds

            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                lobby.pickTimeRemaining = remaining

                ctx.broadcastTimerUpdate(lobby, remaining)
            }

            if (isActive && lobby.format == TournamentFormat.WINSTON_DRAFT) {
                synchronized(lobby.winstonLock) {
                    if (lobby.state == LobbyState.DRAFTING) {
                        val activePlayerId = lobby.getWinstonActivePlayerId()
                        if (activePlayerId != null) {
                            val result = lobby.winstonAutoPickForTimeout(activePlayerId)
                            val activeIdentity = lobby.players[activePlayerId]?.identity
                            if (activeIdentity != null) {
                                handleWinstonResult(lobby, result, activeIdentity)
                            }
                        }
                    }
                }
            }
        }
    }
}
