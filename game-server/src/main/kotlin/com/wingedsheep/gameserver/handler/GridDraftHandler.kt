package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.GridDraftResult
import com.wingedsheep.gameserver.lobby.GridSelection
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.sdk.model.EntityId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class GridDraftHandler(
    private val ctx: LobbySharedContext
) {
    private val logger = LoggerFactory.getLogger(GridDraftHandler::class.java)

    /** Callback invoked when draft completes — set by LobbyHandler to trigger AI deck building. */
    @Volatile var onDraftComplete: ((TournamentLobby) -> Unit)? = null

    fun handleGridDraftPick(session: WebSocketSession, message: ClientMessage.GridDraftPick) {
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

        val selection = try {
            GridSelection.valueOf(message.selection)
        } catch (e: IllegalArgumentException) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Invalid selection: ${message.selection}")
            return
        }

        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            if (lobby.state != LobbyState.DRAFTING || lobby.format != TournamentFormat.GRID_DRAFT) {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in grid draft phase")
                return
            }

            val result = lobby.gridPickRowOrColumn(identity.playerId, selection)
            processGridDraftResult(lobby, result, identity.playerId)
        }
    }

    fun processGridDraftResult(lobby: TournamentLobby, result: GridDraftResult, pickerId: EntityId) {
        when (result) {
            is GridDraftResult.PickMade -> {
                logger.info("Grid draft: ${result.lastAction}")
                lobby.pickTimerJob?.cancel()
                broadcastGridDraftState(lobby, result.lastAction, result.cards, pickerId)
                startGridDraftTimer(lobby)
                ctx.lobbyRepository.saveLobby(lobby)
            }
            is GridDraftResult.GridComplete -> {
                logger.info("Grid draft: ${result.lastAction} - new grid dealt")
                lobby.pickTimerJob?.cancel()
                broadcastGridDraftState(lobby, result.lastAction, result.cards, pickerId)
                startGridDraftTimer(lobby)
                ctx.lobbyRepository.saveLobby(lobby)
            }
            is GridDraftResult.DraftComplete -> {
                if (lobby.isGridDraftComplete()) {
                    logger.info("Grid draft complete for lobby ${lobby.lobbyId}")
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
                } else {
                    logger.info("Grid draft: group complete, other group(s) still active in lobby ${lobby.lobbyId}")
                    lobby.pickTimerJob?.cancel()
                    broadcastGridDraftState(lobby, result.lastAction, result.cards, pickerId)
                    startGridDraftTimer(lobby)
                }
                ctx.lobbyRepository.saveLobby(lobby)
            }
            is GridDraftResult.Error -> {
                logger.warn("Grid draft pick failed: ${result.message}")
            }
        }
    }

    fun broadcastGridDraftState(lobby: TournamentLobby, lastAction: String?, lastPickedCards: List<com.wingedsheep.sdk.model.CardDefinition> = emptyList(), lastPickerPlayerId: EntityId? = null) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                val group = lobby.getGroupForPlayer(playerId)
                    ?: lobby.gridGroups.firstOrNull()
                    ?: return@forEach

                val gridInfos = group.gridCards.map { card ->
                    card?.let { cardToSealedCardInfo(it) }
                }
                val availableSelections = lobby.getAvailableGridSelections(group).map { it.name }
                val activePlayer = lobby.getGridActivePlayer(group)
                val activePlayerName = lobby.players[activePlayer]?.identity?.playerName ?: "Unknown"
                val playerOrderNames = group.playerOrder.map { id ->
                    lobby.players[id]?.identity?.playerName ?: "Unknown"
                }

                val pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) }

                val sameGroupPlayerIds = group.playerOrder.toSet()
                val otherPickCounts = lobby.players
                    .filter { it.key != playerId && it.key in sameGroupPlayerIds }
                    .map { (_, ps) -> ps.identity.playerName to ps.cardPool.size }
                    .toMap()
                val otherPickedCards = lobby.players
                    .filter { it.key != playerId && it.key in sameGroupPlayerIds }
                    .map { (_, ps) -> ps.identity.playerName to ps.cardPool.map { cardToSealedCardInfo(it) } }
                    .toMap()

                val lastPicked = if (lastPickerPlayerId == null || lastPickerPlayerId in sameGroupPlayerIds) {
                    lastPickedCards.map { cardToSealedCardInfo(it) }
                } else {
                    emptyList()
                }

                ctx.sender.send(ws, ServerMessage.GridDraftState(
                    grid = gridInfos,
                    activePlayerName = activePlayerName,
                    isYourTurn = playerId == activePlayer,
                    mainDeckRemaining = group.mainDeck.size,
                    pickedCards = pickedCards,
                    totalPickedByOthers = otherPickCounts,
                    pickedCardsByOthers = otherPickedCards,
                    lastAction = if (lastPickerPlayerId == null || lastPickerPlayerId in sameGroupPlayerIds) lastAction else null,
                    timeRemainingSeconds = lobby.pickTimeSeconds,
                    availableSelections = if (playerId == activePlayer) availableSelections else emptyList(),
                    playerOrder = playerOrderNames,
                    currentPickerIndex = group.activePlayerIndex,
                    gridNumber = group.gridNumber,
                    lastPickedCards = lastPicked
                ))
            }
        }
    }

    fun startGridDraftTimer(lobby: TournamentLobby) {
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

            if (isActive && lobby.state == LobbyState.DRAFTING && lobby.format == TournamentFormat.GRID_DRAFT) {
                val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
                synchronized(lock) {
                    if (lobby.state != LobbyState.DRAFTING) return@synchronized

                    val activePlayers = lobby.getAllGridActivePlayers()
                    for ((_, activePlayer) in activePlayers) {
                        val result = lobby.autoGridPick(activePlayer)
                        processGridDraftResult(lobby, result, activePlayer)
                        if (lobby.isGridDraftComplete()) break
                    }
                }
            }
        }
    }

    fun gridDraftDefaultBoosters(playerCount: Int): Int = when {
        playerCount >= 4 -> 22
        playerCount >= 3 -> 15
        else -> 11
    }
}
