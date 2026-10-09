package com.wingedsheep.gameserver.matchmaking

import com.wingedsheep.gameserver.ai.AiWebSocketSession
import com.wingedsheep.gameserver.handler.GamePlayHandler
import com.wingedsheep.gameserver.handler.LobbyHandler
import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.handler.QuickGameLobbyHandler
import com.wingedsheep.gameserver.lobby.QuickGameLobbyRepository
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.ranking.Elo
import com.wingedsheep.gameserver.ranking.Ranked
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.gameserver.social.BlockService
import com.wingedsheep.gameserver.social.Party
import com.wingedsheep.sdk.core.GameRules
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

/**
 * "Find opponent": lets players who don't know each other get a game. Owns a [MatchmakingQueue] and
 * connects it to the outside world — reads who is asking from the socket, looks up ranked ratings,
 * ticks the queue once a second, turns its events into [ServerMessage]s, and seats a confirmed pair:
 * a Jump In pair in a two-seat Jump In lobby ([LobbyHandler.createMatchmadeJumpInLobby]), every other
 * mode in a quick-game lobby ([QuickGameLobbyHandler.createMatchmadeLobby]). From there on it is an
 * ordinary game of that kind.
 *
 * Every queue call happens under [lock]; messages are sent and lobbies created after it is released.
 */
@Component
class MatchmakingService(
    private val sessionRegistry: SessionRegistry,
    private val sender: MessageSender,
    private val quickGameLobbyHandler: QuickGameLobbyHandler,
    private val lobbyHandler: LobbyHandler,
    private val quickGameLobbies: QuickGameLobbyRepository,
    private val tournamentLobbies: LobbyRepository,
    private val gameRepository: GameRepository,
    private val ratingLookup: RatingLookup,
    private val gamePlayHandler: GamePlayHandler,
    private val blocks: BlockService,
) {
    private val logger = LoggerFactory.getLogger(MatchmakingService::class.java)
    private val queue = MatchmakingQueue()
    private val lock = Any()
    private var lastBroadcastCounts: Map<QueueKey, Int> = emptyMap()

    fun handle(session: WebSocketSession, message: ClientMessage) {
        val identity = sessionRegistry.getIdentityByWsId(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        when (message) {
            is ClientMessage.JoinMatchmaking -> when (val key = QueueKey.of(message.mode, message.format, message.ranked)) {
                is QueueKey.Result.Valid -> join(session, identity, key.key)
                is QueueKey.Result.Invalid -> sender.sendError(session, ErrorCode.INVALID_ACTION, key.reason)
            }
            is ClientMessage.LeaveMatchmaking -> dispatch(synchronized(lock) { queue.leave(identity.playerId) })
            is ClientMessage.RespondToMatch -> dispatch(
                synchronized(lock) { queue.respond(identity.playerId, message.matchId, message.accept) },
            )
            else -> {}
        }
    }

    /** Searching players per queue, for the home screen's first paint (later changes are pushed). */
    fun queueCounts(): List<ServerMessage.MatchmakingQueueCount> =
        toCounts(synchronized(lock) { queue.counts() })

    /** Where [playerId] stands in matchmaking, for the admin Live overview; null when not queued. */
    fun searchOf(playerId: EntityId): Search? = synchronized(lock) {
        val match = queue.pendingMatchOf(playerId)
        val entry = queue.entryOf(playerId) ?: match?.players?.firstOrNull { it.playerId == playerId }
        entry?.let { Search(it.key, it.joinedAt, matchFound = match != null) }
    }

    data class Search(val key: QueueKey, val joinedAt: Long, val matchFound: Boolean)

    @Scheduled(fixedDelay = TICK_MS, initialDelay = TICK_MS)
    fun tick() {
        val available = sessionRegistry.getAllIdentities()
            .filter { !it.isAi && it.isConnected && !isBusyElsewhere(it) }
            .mapTo(HashSet()) { it.playerId }
        val blocked = { a: QueueEntry, b: QueueEntry ->
            blocks.blockedEitherWay(Party(a.playerId, a.userId), Party(b.playerId, b.userId))
        }
        dispatch(synchronized(lock) { queue.tick(System.currentTimeMillis(), isAvailable = { it in available }, isBlocked = blocked) })
    }

    private fun join(session: WebSocketSession, identity: PlayerIdentity, key: QueueKey) {
        val userId = identity.userId
        if (key.ranked && userId == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Sign in to play ranked")
            return
        }
        if (isBusyElsewhere(identity)) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Leave your current lobby or game before searching")
            return
        }
        val rating = if (key.ranked && userId != null) {
            // Only Random deck and Constructed have ranked queues, and both are quick games.
            ratingLookup.ratingOf(userId, Ranked.modeForQuickGame(GameRules.inferred(false, key.format), key.format, false))
        } else {
            Elo.STARTING_RATING
        }
        val events = synchronized(lock) {
            if (queue.pendingMatchOf(identity.playerId) != null) {
                null
            } else {
                // Re-sending the same queue keeps your place; switching queues starts the clock over.
                val joinedAt = queue.entryOf(identity.playerId)?.takeIf { it.key == key }?.joinedAt
                    ?: System.currentTimeMillis()
                queue.join(QueueEntry(identity.playerId, userId, identity.playerName, key, rating, joinedAt))
            }
        }
        if (events == null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Answer the match you were offered first")
            return
        }
        dispatch(events)
    }

    /**
     * In a lobby or an unfinished game — somewhere a match would pull them out of. Practice against
     * the AI doesn't count: that is how a player passes the time in the queue, and a match found
     * mid-game ends it ([releaseFromPractice]).
     */
    private fun isBusyElsewhere(identity: PlayerIdentity): Boolean {
        val quickLobby = identity.currentQuickGameLobbyId?.let { quickGameLobbies.findById(it) }
        if (quickLobby != null && !quickLobby.vsAi) return true
        if (identity.currentLobbyId?.let { tournamentLobbies.findLobbyById(it) } != null) return true
        val game = identity.currentGameSessionId?.let { gameRepository.findById(it) } ?: return false
        return !game.isGameOver() && !isPractice(game)
    }

    /** A game outside any lobby with exactly one human seat: you against the AI. */
    private fun isPractice(game: GameSession): Boolean =
        gameRepository.getLobbyForGame(game.sessionId) == null &&
            game.getPlayers().count { it.webSocketSession !is AiWebSocketSession } == 1

    /** Close [identity]'s AI lobby or concede their AI game, quietly, so the matched lobby can open. */
    private fun releaseFromPractice(identity: PlayerIdentity) {
        quickGameLobbyHandler.dissolvePracticeLobby(identity.playerId)
        val game = identity.currentGameSessionId?.let { gameRepository.findById(it) } ?: return
        if (!game.isGameOver() && isPractice(game)) gamePlayHandler.leavePracticeGame(game, identity.playerId)
    }

    private fun dispatch(events: List<MatchmakingEvent>) {
        for (event in events) {
            when (event) {
                is MatchmakingEvent.Searching -> sendTo(
                    event.entry.playerId,
                    ServerMessage.MatchmakingStatus(
                        searching = true,
                        mode = event.entry.key.mode,
                        format = event.entry.key.format,
                        ranked = event.entry.key.ranked,
                        searchingSince = event.entry.joinedAt,
                        notice = event.notice,
                    ),
                )
                is MatchmakingEvent.Idle ->
                    sendTo(event.playerId, ServerMessage.MatchmakingStatus(searching = false, notice = event.notice))
                is MatchmakingEvent.Found -> event.match.players.forEach { sendMatchFound(event.match, it.playerId) }
                is MatchmakingEvent.Accepted -> sendMatchFound(event.match, event.playerId)
                is MatchmakingEvent.Confirmed -> seat(event.match)
            }
        }
        broadcastCountsIfChanged()
    }

    private fun sendMatchFound(match: PendingMatch, playerId: EntityId) {
        val opponent = match.opponentOf(playerId)
        val key = opponent.key
        sendTo(
            playerId,
            ServerMessage.MatchFound(
                matchId = match.matchId,
                opponentName = opponent.playerName,
                mode = key.mode,
                format = key.format,
                ranked = key.ranked,
                opponentRating = if (key.ranked) opponent.rating.toInt() else null,
                acceptWindowMs = (match.expiresAt - System.currentTimeMillis()).coerceAtLeast(0),
                youAccepted = playerId in match.accepted,
            ),
        )
    }

    /**
     * Both accepted: make their lobby. A player who vanished or got busy in the accept window is
     * dropped, and the other goes back in the queue at their original place.
     */
    private fun seat(match: PendingMatch) {
        val identities = match.players.associate { entry ->
            entry.playerId to sessionRegistry.getAllIdentities().firstOrNull { it.playerId == entry.playerId }
        }
        val unavailable = match.players.filter { entry ->
            val identity = identities[entry.playerId]
            identity == null || !identity.isConnected || isBusyElsewhere(identity)
        }
        if (unavailable.isEmpty()) identities.values.filterNotNull().forEach(::releaseFromPractice)
        val key = match.first.key
        val seated = unavailable.isEmpty() && when (key.mode) {
            MatchmakingMode.JUMP_IN -> lobbyHandler.createMatchmadeJumpInLobby(match.players.map { identities.getValue(it.playerId)!! })
            else -> quickGameLobbyHandler.createMatchmadeLobby(
                players = match.players.map { it.playerId to (identities[it.playerId]?.playerName ?: it.playerName) },
                key = key,
            )
        }
        if (seated) {
            match.players.forEach { sendTo(it.playerId, ServerMessage.MatchmakingStatus(searching = false, matched = true)) }
            return
        }
        logger.info("Match ${match.matchId}: could not seat ${unavailable.map { it.playerName }}; requeueing the rest")
        val requeue = match.players.filterNot { it in unavailable }
        val events = synchronized(lock) {
            requeue.flatMap { queue.requeue(it, "Your opponent is no longer available — back in the queue") }
        }
        unavailable.forEach { sendTo(it.playerId, ServerMessage.MatchmakingStatus(searching = false)) }
        dispatch(events)
    }

    private fun sendTo(playerId: EntityId, message: ServerMessage) {
        val ws = sessionRegistry.getAllIdentities().firstOrNull { it.playerId == playerId }?.webSocketSession
        if (ws != null && ws.isOpen) sender.send(ws, message)
    }

    private fun broadcastCountsIfChanged() {
        val counts = synchronized(lock) {
            val current = queue.counts()
            if (current == lastBroadcastCounts) return
            lastBroadcastCounts = current
            current
        }
        val message = ServerMessage.MatchmakingQueues(toCounts(counts))
        sessionRegistry.getAllIdentities().forEach { identity ->
            if (identity.isAi) return@forEach
            val ws = identity.webSocketSession
            if (ws != null && ws.isOpen) sender.send(ws, message)
        }
    }

    private fun toCounts(counts: Map<QueueKey, Int>) = counts.map { (key, n) ->
        ServerMessage.MatchmakingQueueCount(mode = key.mode, format = key.format, ranked = key.ranked, searching = n)
    }

    companion object {
        const val TICK_MS: Long = 1_000
    }
}
