package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.handler.ConnectionHandler.Companion.cardToSealedCardInfo
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.PickResult
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.model.EntityId
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

        synchronized(lobby.draftLock) {
            val result = lobby.makePick(identity.playerId, message.cardNames)
            when (result) {
                is PickResult.Success -> {
                    processPickResult(lobby, identity.playerId, identity, result)
                }
                is PickResult.Error -> {
                    ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, result.message)
                }
            }
        }
    }

    /**
     * Shared post-pick processing for both human and AI picks.
     * Sends confirmations, broadcasts pick status, delivers new packs to recipients,
     * and handles round/draft completion.
     *
     * MUST be called inside synchronized(lobby.draftLock).
     */
    fun processPickResult(
        lobby: TournamentLobby,
        pickerId: EntityId,
        pickerIdentity: PlayerIdentity,
        result: PickResult.Success
    ) {
        val pickedNames = result.pickedCards.map { it.name }
        logger.info("Player ${pickerIdentity.playerName} picked ${pickedNames.joinToString(", ")} (${result.totalPicked} total)")

        // Send pick confirmation to the picker
        val pickerWs = pickerIdentity.webSocketSession
        if (pickerWs != null && pickerWs.isOpen) {
            ctx.sender.send(pickerWs, ServerMessage.DraftPickConfirmed(
                cardNames = pickedNames,
                totalPicked = result.totalPicked
            ))
        }

        // Broadcast pick made with pack counts to all players
        broadcastDraftPickMade(lobby, pickerIdentity, result.playerPackCounts)

        if (result.draftComplete) {
            // Draft finished — cancel all timers and send completion messages
            lobby.cancelAllPlayerTimers()

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
            onDraftComplete?.invoke(lobby)
            return
        }

        if (result.newPackRound) {
            // New pack round — cancel all timers, broadcast fresh packs, start per-player timers
            lobby.cancelAllPlayerTimers()
            broadcastDraftPacks(lobby)
            startAllPlayerTimers(lobby)
            ctx.lobbyRepository.saveLobby(lobby)
            return
        }

        // Cancel picker's timer (they just picked)
        cancelPlayerTimer(lobby, pickerId)

        // Send new pack to recipient if they were idle and got one directly, start their timer
        if (result.recipientGotNewPack && result.passedToPlayerId != null) {
            sendPackToPlayer(lobby, result.passedToPlayerId)
            startPlayerTimer(lobby, result.passedToPlayerId)
        }

        // Send next queued pack to the picker if they dequeued one, start their timer
        if (result.pickerGotNewPack) {
            sendPackToPlayer(lobby, pickerId)
            startPlayerTimer(lobby, pickerId)
        }

        ctx.lobbyRepository.saveLobby(lobby)
    }

    /**
     * Send a DraftPackReceived message to a specific player for their current pack.
     */
    private fun sendPackToPlayer(lobby: TournamentLobby, playerId: EntityId) {
        val playerState = lobby.players[playerId] ?: return
        val pack = playerState.currentPack ?: return
        val ws = playerState.identity.webSocketSession ?: return
        if (!ws.isOpen) return

        // Derive pick number from this player's pool growth since the round started —
        // booster sizes vary by strategy (15-card classic, 13-card Play Booster, 20-card commander).
        val pickNumber = (playerState.cardPool.size - playerState.poolSizeAtRoundStart) / lobby.picksPerRound + 1
        val timeRemaining = if (lobby.hasPickTimer) lobby.playerTimeRemaining[playerId] ?: lobby.pickTimeSeconds else null

        ctx.sender.send(ws, ServerMessage.DraftPackReceived(
            packNumber = lobby.currentPackNumber,
            pickNumber = pickNumber,
            cards = pack.map { cardToSealedCardInfo(it) },
            timeRemainingSeconds = timeRemaining,
            passDirection = lobby.getPassDirection().name,
            picksPerRound = minOf(lobby.picksPerRound, pack.size),
            pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) },
            queuedPacks = playerState.packQueue.size,
            podSize = lobby.players.size
        ))
    }

    fun broadcastDraftPacks(lobby: TournamentLobby) {
        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            val pack = playerState.currentPack
            if (ws != null && ws.isOpen && pack != null) {
                ctx.sender.send(ws, ServerMessage.DraftPackReceived(
                    packNumber = lobby.currentPackNumber,
                    pickNumber = 1,  // Fresh pack round always starts at pick 1
                    cards = pack.map { cardToSealedCardInfo(it) },
                    timeRemainingSeconds = lobby.pickTimeSeconds.takeIf { lobby.hasPickTimer },
                    passDirection = lobby.getPassDirection().name,
                    picksPerRound = minOf(lobby.picksPerRound, pack.size),
                    pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) },
                    queuedPacks = playerState.packQueue.size,
                    podSize = lobby.players.size
                ))
            }
        }
    }

    private fun broadcastDraftPickMade(lobby: TournamentLobby, picker: PlayerIdentity, playerPackCounts: Map<String, Int>) {
        val message = ServerMessage.DraftPickMade(
            playerId = picker.playerId.value,
            playerName = picker.playerName,
            playerPackCounts = playerPackCounts
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, message)
            }
        }
    }

    /**
     * Start per-player timers for all players who have a pack.
     */
    fun startAllPlayerTimers(lobby: TournamentLobby) {
        lobby.cancelAllPlayerTimers()
        for ((playerId, playerState) in lobby.players) {
            if (playerState.currentPack != null) {
                startPlayerTimer(lobby, playerId)
            }
        }
    }

    /**
     * Start a per-player pick timer. When it expires, auto-pick for this player only.
     */
    private fun startPlayerTimer(lobby: TournamentLobby, playerId: EntityId) {
        cancelPlayerTimer(lobby, playerId)
        // No time limit: never start a job, so the pick is never auto-made for the player.
        if (!lobby.hasPickTimer) return
        lobby.playerTimeRemaining[playerId] = lobby.pickTimeSeconds

        val job = ctx.draftScope.launch {
            var remaining = lobby.pickTimeSeconds

            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                lobby.playerTimeRemaining[playerId] = remaining

                ctx.sendTimerUpdateToPlayer(lobby, playerId, remaining)
            }

            if (isActive && lobby.state == LobbyState.DRAFTING) {
                autoPickForPlayer(lobby, playerId)
            }
        }
        lobby.playerTimerJobs[playerId] = job
    }

    private fun cancelPlayerTimer(lobby: TournamentLobby, playerId: EntityId) {
        lobby.playerTimerJobs.remove(playerId)?.cancel()
        lobby.playerTimeRemaining.remove(playerId)
    }

    private fun autoPickForPlayer(lobby: TournamentLobby, playerId: EntityId) {
        synchronized(lobby.draftLock) {
            val playerState = lobby.players[playerId] ?: return
            if (playerState.currentPack == null) return
            val identity = playerState.identity
            val result = lobby.autoPickFirstCards(playerId)
            if (result is PickResult.Success) {
                logger.info("Auto-picked ${result.pickedCards.map { it.name }.joinToString(", ")} for player ${identity.playerName} (timeout)")
                processPickResult(lobby, playerId, identity, result)
            }
        }
    }

    /**
     * Send draft reconnection state to a reconnecting player.
     */
    fun sendDraftReconnectionState(session: WebSocketSession, lobby: TournamentLobby, identity: PlayerIdentity) {
        val playerState = lobby.players[identity.playerId] ?: return
        val pack = playerState.currentPack

        // Always send DraftPackReceived so the client creates draftState.
        // If the player has no current pack (waiting), send an empty pack.
        val pickNumber = if (pack != null) {
            (playerState.cardPool.size - playerState.poolSizeAtRoundStart) / lobby.picksPerRound + 1
        } else {
            1
        }

        ctx.sender.send(session, ServerMessage.DraftPackReceived(
            packNumber = lobby.currentPackNumber,
            pickNumber = pickNumber,
            cards = pack?.map { cardToSealedCardInfo(it) } ?: emptyList(),
            timeRemainingSeconds = if (lobby.hasPickTimer) lobby.playerTimeRemaining[identity.playerId] ?: lobby.pickTimeSeconds else null,
            passDirection = lobby.getPassDirection().name,
            picksPerRound = if (pack != null) minOf(lobby.picksPerRound, pack.size) else lobby.picksPerRound,
            pickedCards = playerState.cardPool.map { cardToSealedCardInfo(it) },
            queuedPacks = playerState.packQueue.size,
            podSize = lobby.players.size
        ))

        // Also send current pack counts so reconnected player sees the draft table state
        val packCounts = lobby.getPlayerPackCounts()
        if (packCounts.isNotEmpty()) {
            ctx.sender.send(session, ServerMessage.DraftPickMade(
                playerId = identity.playerId.value,
                playerName = identity.playerName,
                playerPackCounts = packCounts
            ))
        }
    }
}
