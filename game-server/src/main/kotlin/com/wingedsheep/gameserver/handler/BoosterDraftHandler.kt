package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.PickResult
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.session.PlayerIdentity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class BoosterDraftHandler(
    private val ctx: LobbySharedContext
) {
    private val logger = LoggerFactory.getLogger(BoosterDraftHandler::class.java)

    /** Callback invoked when draft completes — set by LobbyHandler to trigger AI deck building. */
    @Volatile var onDraftComplete: ((TournamentLobby) -> Unit)? = null

    fun handleMakePick(session: WebSocketSession, message: ClientMessage.MakePick) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            ctx.sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby")
            return
        }

        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            ctx.sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found")
            return
        }

        if (lobby.state != LobbyState.DRAFTING) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in drafting phase")
            return
        }

        val result = lobby.makePick(identity.playerId, message.cardNames)
        when (result) {
            is PickResult.Success -> {
                val pickedNames = result.pickedCards.map { it.name }
                logger.info("Player ${identity.playerName} picked ${pickedNames.joinToString(", ")} (${result.totalPicked} total)")

                ctx.sender.send(session, ServerMessage.DraftPickConfirmed(
                    cardNames = pickedNames,
                    totalPicked = result.totalPicked
                ))

                broadcastDraftPickMade(lobby, identity, result.waitingForPlayers)
                ctx.lobbyRepository.saveLobby(lobby)

                if (lobby.allPlayersPicked()) {
                    processDraftRound(lobby)
                }
            }
            is PickResult.Error -> {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, result.message)
            }
        }
    }

    fun processDraftRound(lobby: TournamentLobby) {
        lobby.pickTimerJob?.cancel()
        lobby.pickTimerJob = null

        val continuesDraft = lobby.passPacks()

        if (continuesDraft) {
            broadcastDraftPacks(lobby)
            ctx.lobbyRepository.saveLobby(lobby)
            startDraftTimer(lobby)
        } else {
            logger.info("Draft complete for lobby ${lobby.lobbyId}, transitioning to deck building")

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
            ctx.lobbyRepository.saveLobby(lobby)

            // Trigger AI deck building after draft completes
            onDraftComplete?.invoke(lobby)
        }
    }

    fun broadcastDraftPacks(lobby: TournamentLobby) {
        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            val pack = playerState.currentPack
            if (ws != null && ws.isOpen && pack != null) {
                ctx.sender.send(ws, ServerMessage.DraftPackReceived(
                    packNumber = lobby.currentPackNumber,
                    pickNumber = lobby.currentPickNumber,
                    cards = pack.map { cardToSealedCardInfo(it) },
                    timeRemainingSeconds = lobby.pickTimeSeconds,
                    passDirection = lobby.getPassDirection().name,
                    picksPerRound = minOf(lobby.picksPerRound, pack.size),
                    pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) }
                ))
            }
        }
    }

    private fun broadcastDraftPickMade(lobby: TournamentLobby, picker: PlayerIdentity, waitingForPlayers: List<String>) {
        val message = ServerMessage.DraftPickMade(
            playerId = picker.playerId.value,
            playerName = picker.playerName,
            waitingForPlayers = waitingForPlayers
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, message)
            }
        }
    }

    fun startDraftTimer(lobby: TournamentLobby) {
        lobby.pickTimeRemaining = lobby.pickTimeSeconds

        lobby.pickTimerJob = ctx.draftScope.launch {
            var remaining = lobby.pickTimeSeconds

            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                lobby.pickTimeRemaining = remaining

                ctx.broadcastTimerUpdate(lobby, remaining)
            }

            if (isActive && lobby.state == LobbyState.DRAFTING) {
                autoPickForSlowPlayers(lobby)
            }
        }
    }

    private fun autoPickForSlowPlayers(lobby: TournamentLobby) {
        val slowPlayers = lobby.getPlayersWaitingToPick()

        for (playerId in slowPlayers) {
            val result = lobby.autoPickFirstCards(playerId)
            if (result is PickResult.Success) {
                val playerState = lobby.players[playerId]
                val ws = playerState?.identity?.webSocketSession
                val pickedNames = result.pickedCards.map { it.name }
                if (ws != null && ws.isOpen) {
                    ctx.sender.send(ws, ServerMessage.DraftPickConfirmed(
                        cardNames = pickedNames,
                        totalPicked = result.totalPicked
                    ))
                }
                logger.info("Auto-picked ${pickedNames.joinToString(", ")} for player ${playerState?.identity?.playerName} (timeout)")
            }
        }

        if (lobby.allPlayersPicked()) {
            processDraftRound(lobby)
        }
    }

    /**
     * Send draft reconnection state to a reconnecting player.
     */
    fun sendDraftReconnectionState(session: WebSocketSession, lobby: TournamentLobby, identity: PlayerIdentity) {
        val playerState = lobby.players[identity.playerId]
        if (playerState?.currentPack != null) {
            ctx.sender.send(session, ServerMessage.DraftPackReceived(
                packNumber = lobby.currentPackNumber,
                pickNumber = lobby.currentPickNumber,
                cards = playerState.currentPack!!.map { cardToSealedCardInfo(it) },
                timeRemainingSeconds = lobby.pickTimeRemaining,
                passDirection = lobby.getPassDirection().name,
                picksPerRound = minOf(lobby.picksPerRound, playerState.currentPack!!.size),
                pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) }
            ))
        }
    }
}
