package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.protocol.DeckRequestConverter
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.tournament.AiMatchSimulator
import com.wingedsheep.gameserver.tournament.TournamentManager
import com.wingedsheep.gameserver.tournament.TournamentMatch
import com.wingedsheep.gameserver.tournament.TournamentRound
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.deck.EasterEggDeckInjector
import com.wingedsheep.engine.limited.BoosterGenerator
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

@Component
class TournamentMatchHandler(
    private val ctx: LobbySharedContext,
    private val spectatingHandler: SpectatingHandler,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry,
    private val tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry,
    private val gamePlayHandler: GamePlayHandler,
    private val gameProperties: GameProperties,
    private val gameRepository: GameRepository,
    private val aiGameManager: AiGameManager,
    private val aiMatchSimulator: AiMatchSimulator,
    private val tournamentResultSink: com.wingedsheep.gameserver.stats.TournamentResultSink
) {
    private val logger = LoggerFactory.getLogger(TournamentMatchHandler::class.java)

    /** One in-flight [startReadyMatches] trampoline. Thread-confined: the sweep never leaves its thread. */
    private class Sweep(val lobbyId: String) {
        var again: Boolean = false
    }

    private val activeSweep = ThreadLocal<Sweep?>()

    fun handleReadyForNextRound(session: WebSocketSession) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in a lobby")
            return
        }

        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not found")
            return
        }

        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId)
        if (tournament == null || tournament.isComplete) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament not active")
            return
        }

        // Spectators can't ready up
        if (lobby.isSpectator(identity.playerId)) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Spectators cannot ready up")
            return
        }

        // Read the epoch before queueing on the lock. If the ready set is wiped while we wait — the
        // tournament being resumed for extra rounds is the live case — this request was aimed at a
        // bracket that no longer exists, and honouring it would ready the player for a round they
        // haven't seen. A round *completing* no longer clears readies, so it no longer lands here.
        val epochBeforeLock = lobby.readyEpoch

        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            if (lobby.readyEpoch != epochBeforeLock) return

            // A ready click means "I dismissed the game-over overlay and want the next game". A player
            // whose match is still running can't have done that, so this is a duplicate or a click that
            // raced their own match starting. Banking it would consent to the *following* match while
            // they are mid-game, and the sweep would launch that game the moment this one ends.
            if (tournament.hasActiveMatch(identity.playerId)) {
                logger.debug(
                    "Ignoring ready from ${identity.playerName} in tournament $lobbyId: match still in progress"
                )
                return
            }

            if (!prepareRoundsIfNeeded(lobby, tournament)) {
                completeTournament(lobbyId)
                return
            }

            val wasNewlyReady = lobby.markPlayerReady(identity.playerId)
            if (!wasNewlyReady) {
                return
            }

            logger.info("Player ${identity.playerName} ready for next round in tournament $lobbyId")

            broadcastReadyStatus(lobby, identity)
            ctx.lobbyRepository.saveLobby(lobby)

            resolveByesForPlayer(lobby, tournament, identity)
            startReadyMatches(lobby, tournament)
        }
    }

    fun handleMatchResult(
        lobbyId: String,
        gameSessionId: String,
        winnerId: EntityId?,
        winnerLifeRemaining: Int
    ) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
            tournament.reportMatchResult(gameSessionId, winnerId, winnerLifeRemaining)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)

            val resultRound = tournament.getRoundForMatch(gameSessionId)
            val match = resultRound?.matches?.find { it.gameSessionId == gameSessionId }
            afterMatchResult(lobbyId, resultRound, match)
        }
    }

    /**
     * Everything a decided match sets in motion, once the result itself is recorded: announce it,
     * persist the standings, re-advertise the active matches, close the round if this was its last
     * game, and sweep for whatever that just unblocked.
     *
     * Shared by the played path ([handleMatchResult]) and the simulated one ([simulateMatch]) so a
     * simulated result moves the bracket in exactly the same way a played one does — the only thing
     * that differs about it is that no game was run. Assumes the per-lobby round lock is held.
     */
    private fun afterMatchResult(lobbyId: String, resultRound: TournamentRound?, match: TournamentMatch?) {
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        if (resultRound != null && match != null) announceMatchComplete(lobbyId, resultRound, match)
        // Persist the freshly-updated standings so profiles/dashboard show live results.
        recordTournamentProgress(lobbyId)
        spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobbyId)

        // Test round completion *before* touching ready state: `autoReadyAiPlayers` prepares the
        // next round when the current one is done, which would make `isRoundComplete()` report on
        // the fresh round and swallow this round's `RoundComplete` broadcast entirely. The
        // round-complete path readies the AI itself, so either branch ends with a start sweep.
        //
        // Only a result *from* the current round can close it. Matches from later rounds run
        // concurrently with it (eager starting), and letting one of those answer for the current
        // round would re-report a round already closed — announcing stale results and clearing
        // every player's game-session pointer, including seats mid-game.
        val closesCurrentRound = resultRound != null &&
                resultRound.roundNumber == tournament.currentRound?.roundNumber
        if (closesCurrentRound && tournament.isRoundComplete()) {
            doHandleRoundComplete(lobbyId)
        } else {
            val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
            if (lobby != null) {
                // Ready the AI, but not the human: they must click "Ready for Next Round" after
                // dismissing the game-over overlay, so the next game can't start underneath it.
                // The sweep inside also retries pairs this result just unblocked.
                autoReadyAiPlayers(lobby, tournament, autoReadyHumansVsAi = false)
                ctx.lobbyRepository.saveLobby(lobby)
            }
        }

        // `MatchComplete` / `RoundComplete` make the client drop its ready list, and the readies
        // that survive a round boundary would otherwise vanish from the lobby's counter. Re-state
        // the authoritative set once everything above has settled.
        ctx.lobbyRepository.findLobbyById(lobbyId)?.let { broadcastReadyStatus(it) }
    }

    fun handleAbandon(lobbyId: String, playerId: EntityId) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
            tournament.recordAbandon(playerId)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)

            // Drop the departed player's ready flag. Nothing else clears it now that a round boundary
            // doesn't, and a leaver left in the set inflates the lobby's "n/m ready" for good.
            ctx.lobbyRepository.findLobbyById(lobbyId)?.let { lobby ->
                lobby.clearPlayerReady(playerId)
                ctx.lobbyRepository.saveLobby(lobby)
            }

            // Freeze the standings as they stand after the forfeit; the row keeps these if it later
            // flips to ABANDONED on teardown.
            recordTournamentProgress(lobbyId)

            spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobbyId)

            if (tournament.isRoundComplete()) {
                doHandleRoundComplete(lobbyId)
            } else {
                // A forfeit completes every match the abandoner had left, in every round — the same
                // "an earlier game finished" event a real result is, so the pairs it frees need the
                // same sweep, or they wait on an unrelated result to come along.
                val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
                if (lobby != null) {
                    startReadyMatches(lobby, tournament)
                    broadcastReadyStatus(lobby)
                    ctx.lobbyRepository.saveLobby(lobby)
                }
            }
        }
    }

    fun handleAddExtraRound(session: WebSocketSession) {
        val token = ctx.sessionRegistry.getTokenByWsId(session.id)
        val identity = token?.let { ctx.sessionRegistry.getIdentityByToken(it) }
        if (identity == null) {
            ctx.sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected")
            return
        }

        val lobbyId = identity.currentLobbyId
        if (lobbyId == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Not in a lobby")
            return
        }

        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId)
        if (lobby == null) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Lobby not found")
            return
        }

        if (!lobby.isHost(identity.playerId)) {
            ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can add extra rounds")
            return
        }

        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            if (lobby.state != com.wingedsheep.gameserver.lobby.LobbyState.TOURNAMENT_COMPLETE) {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament is not complete")
                return
            }

            val tournament = ctx.lobbyRepository.findTournamentById(lobbyId)
            if (tournament == null) {
                ctx.sender.sendError(session, ErrorCode.INVALID_ACTION, "Tournament not found")
                return
            }

            tournament.addExtraRound()
            lobby.resumeTournament()

            logger.info("Host ${identity.playerName} added extra rounds to tournament $lobbyId (now ${tournament.totalRounds} total)")

            val connectedIds = lobby.players.values
                .filter { it.identity.isConnected }
                .map { it.identity.playerId }
                .toSet()

            val standings = tournament.getStandingsInfo(connectedIds)
            val nextMatchups = tournament.peekNextRoundMatchups()

            lobby.players.forEach { (playerId, playerState) ->
                val ws = playerState.identity.webSocketSession
                if (ws != null && ws.isOpen) {
                    val opponentId = nextMatchups[playerId]
                    val opponentName = opponentId?.let { lobby.players[it]?.identity?.playerName }
                    val hasBye = nextMatchups.containsKey(playerId) && opponentId == null

                    ctx.sender.send(ws, ServerMessage.TournamentResumed(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = standings,
                        nextOpponentName = opponentName,
                        nextRoundHasBye = hasBye
                    ))
                }
            }

            lobby.spectators.forEach { (_, spectatorIdentity) ->
                val ws = spectatorIdentity.webSocketSession
                if (ws != null && ws.isOpen) {
                    ctx.sender.send(ws, ServerMessage.TournamentResumed(
                        lobbyId = lobbyId,
                        totalRounds = tournament.totalRounds,
                        standings = standings
                    ))
                }
            }

            autoReadyAiPlayers(lobby, tournament)

            ctx.lobbyRepository.saveLobby(lobby)
            ctx.lobbyRepository.saveTournament(lobbyId, tournament)
        }
    }

    fun handleRoundComplete(lobbyId: String) {
        val lock = ctx.roundLocks.computeIfAbsent(lobbyId) { Any() }
        synchronized(lock) {
            doHandleRoundComplete(lobbyId)
        }
    }

    private fun doHandleRoundComplete(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
        val round = tournament.currentRound ?: return

        logger.info("Round ${round.roundNumber} complete for tournament $lobbyId")

        // Deliberately NOT clearing ready state here. A ready flag is consumed the moment the player's
        // match starts, so whoever is still ready at a round boundary never had it consumed: an early
        // finisher who dismissed the game-over overlay and clicked Ready while the round was finishing,
        // a player who sat out a BYE, or an AI. Wiping them discarded a deliberate Ready action and
        // forced a second click; none of them has a game-over overlay a new game could start under.

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        lobby.players.forEach { (playerId, playerState) ->
            playerState.identity.currentGameSessionId = null

            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                val nextMatch = tournament.getNextMatchForPlayer(playerId)
                val nextOpponentName: String?
                val hasBye: Boolean

                if (nextMatch != null) {
                    val (nextRound, nm) = nextMatch
                    val isNextRound = nextRound.roundNumber == round.roundNumber + 1
                    val opponentId = if (nm.player1Id == playerId) nm.player2Id else nm.player1Id
                    nextOpponentName = if (isNextRound && !nm.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
                    hasBye = nm.isBye
                } else {
                    nextOpponentName = null
                    hasBye = false
                }

                val roundComplete = ServerMessage.RoundComplete(
                    lobbyId = lobbyId,
                    round = round.roundNumber,
                    results = tournament.getCurrentRoundResults(),
                    standings = tournament.getStandingsInfo(connectedIds),
                    nextOpponentName = nextOpponentName,
                    nextRoundHasBye = hasBye,
                    isTournamentComplete = tournament.isComplete
                )
                ctx.sender.send(ws, roundComplete)
            }
        }

        for ((_, spectatorIdentity) in lobby.spectators) {
            val ws = spectatorIdentity.webSocketSession ?: continue
            if (!ws.isOpen) continue

            val roundComplete = ServerMessage.RoundComplete(
                lobbyId = lobbyId,
                round = round.roundNumber,
                results = tournament.getCurrentRoundResults(),
                standings = tournament.getStandingsInfo(connectedIds),
                isTournamentComplete = tournament.isComplete
            )
            ctx.sender.send(ws, roundComplete)
        }

        if (!tournament.isComplete) {
            // Open the next round now the closing one has been reported. This used to happen as a side
            // effect of `autoReadyAiPlayers` — reachable only because the ready-clearing above made its
            // AI guard true — so it has to be explicit here, and it has to come after the broadcast:
            // the messages above describe the round that just ended.
            prepareRoundsIfNeeded(lobby, tournament)

            // Ready the AI for the next round, but not the human: they must click "Ready for Next
            // Round" after dismissing the game-over overlay, so the next game can't start underneath it.
            autoReadyAiPlayers(lobby, tournament, autoReadyHumansVsAi = false)
        }

        ctx.lobbyRepository.saveLobby(lobby)
        ctx.lobbyRepository.saveTournament(lobbyId, tournament)

        if (tournament.isComplete) {
            completeTournament(lobbyId)
        }
    }

    /**
     * Tell the two seats of a decided match how the bracket now stands.
     *
     * Keyed by the match rather than by a game session id: a simulated match never had a session, and
     * it still has to report itself the same way a played one does.
     */
    private fun announceMatchComplete(lobbyId: String, completedRound: TournamentRound, match: TournamentMatch) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val matchPlayerIds = listOfNotNull(match.player1Id, match.player2Id)

        for (playerId in matchPlayerIds) {
            val playerState = lobby.players[playerId] ?: continue
            val ws = playerState.identity.webSocketSession ?: continue
            if (!ws.isOpen) continue

            val nextMatch = tournament.getNextMatchForPlayer(playerId)
            val nextOpponentName: String?
            val hasBye: Boolean

            if (nextMatch != null) {
                val (nextRound, nm) = nextMatch
                val isCurrentRound = nextRound.roundNumber == (tournament.currentRound?.roundNumber ?: -1)
                val opponentId = if (nm.player1Id == playerId) nm.player2Id else nm.player1Id
                nextOpponentName = if (isCurrentRound && !nm.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
                hasBye = nm.isBye
            } else {
                nextOpponentName = null
                hasBye = false
            }

            ctx.sender.send(ws, ServerMessage.MatchComplete(
                lobbyId = lobbyId,
                round = completedRound.roundNumber,
                results = tournament.getRoundResults(completedRound),
                standings = tournament.getStandingsInfo(connectedIds),
                nextOpponentName = nextOpponentName,
                nextRoundHasBye = hasBye,
                isTournamentComplete = tournament.isComplete,
                roundComplete = completedRound.isComplete
            ))
        }

        ctx.lobbyRepository.saveTournament(lobbyId, tournament)
    }

    /**
     * Advance the bracket until the current round is one that still has games to play, announcing the
     * BYEs each newly opened round auto-completes.
     *
     * Returns false when the schedule ran out — the caller finishes the tournament.
     *
     * `currentRound` is what `isRoundComplete`, spectating and the next-opponent fields all read, so
     * advancing is not bookkeeping: leave it pointing at a finished round and the round-complete path
     * fires again on the next result. Loops because an all-BYE round is complete the moment it opens.
     */
    private fun prepareRoundsIfNeeded(lobby: TournamentLobby, tournament: TournamentManager): Boolean {
        while (tournament.currentRound.let { it == null || it.isComplete }) {
            val round = tournament.startNextRound() ?: return false

            for (match in round.matches) {
                if (!match.isBye || !match.isComplete) continue
                val byePlayerState = lobby.players[match.player1Id] ?: continue
                val byeWs = byePlayerState.identity.webSocketSession
                if (byeWs != null && byeWs.isOpen) {
                    ctx.sender.send(byeWs, ServerMessage.TournamentBye(
                        lobbyId = lobby.lobbyId,
                        round = round.roundNumber
                    ))
                    spectatingHandler.sendActiveMatchesToPlayer(byePlayerState.identity, byeWs)
                }
            }

            ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
            logger.info("Prepared round ${round.roundNumber} for tournament ${lobby.lobbyId}")
        }
        return true
    }

    /**
     * Complete and announce every BYE sitting between this player and their next real match. A bye
     * has no opponent to wait for, so it resolves the moment the player is ready for it.
     */
    fun resolveByesForPlayer(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity
    ) {
        while (true) {
            val (round, match) = tournament.getNextMatchForPlayer(identity.playerId) ?: return
            if (!match.isBye) return

            match.isComplete = true
            val ws = identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, ServerMessage.TournamentBye(
                    lobbyId = lobby.lobbyId,
                    round = round.roundNumber
                ))
                spectatingHandler.sendActiveMatchesToPlayer(identity, ws)
            }
            ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
        }
    }

    /**
     * Launch every match both of whose seats are ready — see [TournamentManager.startableMatches].
     *
     * This is a sweep over the whole ready set, not a lookup for one player, because a ready flag is
     * consumed only when a match actually starts: a pair refused by the earlier-round guard stays
     * ready, and `markPlayerReady` will never transition false→true for them a second time. Nothing
     * else would ever reconsider them, so every caller that changes the ready set *or* completes a
     * match runs this pass.
     *
     * Re-entrant by trampoline rather than by recursion. Reporting a result runs the sweep again (a
     * finished match unblocks other pairs), and with simulated AI matches that result comes back
     * *inside* this call — so an all-AI bracket resolves as one cascade and per-match recursion would
     * nest one level per match, hundreds deep on a long schedule. A nested call therefore asks the
     * outermost sweep for another pass and returns; the loop below re-queries the live ready set, so
     * the deferred pass sees everything the nested one would have.
     */
    fun startReadyMatches(lobby: TournamentLobby, tournament: TournamentManager) {
        val enclosing = activeSweep.get()
        if (enclosing != null && enclosing.lobbyId == lobby.lobbyId) {
            enclosing.again = true
            return
        }

        val sweep = Sweep(lobby.lobbyId)
        activeSweep.set(sweep)
        try {
            do {
                sweep.again = false
                sweepReadyMatches(lobby, tournament)
            } while (sweep.again)
        } finally {
            activeSweep.set(enclosing)
        }
    }

    /** One pass of [startReadyMatches]; never call directly — the trampoline owns the repetition. */
    private fun sweepReadyMatches(lobby: TournamentLobby, tournament: TournamentManager) {
        forfeitUnseatedMatches(lobby, tournament)

        var startedAny = false
        for ((round, match) in tournament.startableMatches(lobby.getReadyPlayerIds())) {
            val player2Id = match.player2Id ?: continue

            // The list above was snapshotted before anything in this loop ran, and simulating a match
            // decides it on the spot — which re-enters this sweep through the result path (to open the
            // next round, to re-ready the AI). A later entry can therefore already be decided or
            // launched by the time we reach it; acting on it again would run the pair twice.
            if (match.isComplete || match.gameSessionId != null) continue

            if (shouldSimulate(lobby, match, player2Id)) {
                simulateMatch(lobby, tournament, round, match, player2Id)
                startedAny = true
                continue
            }

            logger.info(
                "Both players ready, starting round ${round.roundNumber} match: " +
                    "${lobby.players[match.player1Id]?.identity?.playerName} vs " +
                    "${lobby.players[player2Id]?.identity?.playerName}"
            )
            if (startSingleMatch(lobby, tournament, round, match)) {
                lobby.clearPlayerReady(match.player1Id)
                lobby.clearPlayerReady(player2Id)
                startedAny = true
            }
        }
        // The starts above consumed ready flags; push the new authoritative set so the lobby's
        // "n/m ready" counter doesn't keep counting players who are already in a game.
        if (startedAny) broadcastReadyStatus(lobby)
    }

    /**
     * Forfeit every match still owed by a bracket seat the lobby no longer has, and open the round
     * that clears.
     *
     * Removal paths forfeit as they go (see `LobbyHandler.forfeitBracketMatchesIfSeatGone`), so this
     * is the backstop rather than the first line: it also repairs a bracket that drifted before that
     * existed, or through a path nobody has thought of yet. It sits at the top of [startReadyMatches]
     * because that is the one pass every readiness change and every match result already runs through
     * — see [TournamentManager.unseatedPlayersWithMatchesLeft] for what a ghost seat does to the rest
     * of the bracket if nothing forfeits it.
     *
     * Closing the round here is not optional: the forfeits can decide the open round outright, and if
     * this is the pass that unstuck a bracket with nothing left to play, no later result is coming to
     * notice.
     */
    private fun forfeitUnseatedMatches(lobby: TournamentLobby, tournament: TournamentManager) {
        val unseated = tournament.unseatedPlayersWithMatchesLeft(lobby.players.keys)
        if (unseated.isEmpty()) return

        for (playerId in unseated) {
            logger.warn(
                "Tournament {}: bracket seat {} is no longer in the lobby; forfeiting its remaining matches",
                lobby.lobbyId,
                playerId.value,
            )
            tournament.recordAbandon(playerId)
        }
        ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
        recordTournamentProgress(lobby.lobbyId)

        // Report it the way a real last result would: [doHandleRoundComplete] broadcasts the standings,
        // advances the bracket, and completes the tournament if nothing is left. It re-enters this
        // sweep, which is why the forfeits above come first — the second pass finds nothing and stops.
        if (tournament.isRoundComplete()) {
            doHandleRoundComplete(lobby.lobbyId)
        }
    }

    /**
     * Whether this match can be decided without playing it: the server has
     * `game.tournament.simulate-ai-matches` on, and [AiMatchSimulator.shouldSimulate] agrees on the
     * shape of the bracket.
     *
     * The cost this avoids is the point: a round-robin with one human and N AI seats schedules O(N²)
     * matches and the human is in only N of them, so nearly every game the server runs is two AI
     * controllers grinding out a result that reaches nobody but the standings table. See
     * [AiMatchSimulator] for what "decide" means (a coin flip, deliberately).
     */
    private fun shouldSimulate(lobby: TournamentLobby, match: TournamentMatch, player2Id: EntityId): Boolean =
        gameProperties.tournament.simulateAiMatches &&
            aiMatchSimulator.shouldSimulate(
                bracketSeats = lobby.players.keys,
                aiSeats = aiSeats(lobby),
                player1Id = match.player1Id,
                player2Id = player2Id,
            )

    /**
     * The lobby's AI seats. A seat counts as AI if either the live AI registry or its persisted
     * identity says so. The two can disagree for a window after a restart — the registry is rebuilt by
     * rehydration, the identity flag survives in the lobby — and an AI seat the registry hasn't
     * re-wired yet is exactly the seat whose game would hang, so treating it as AI is also the safe
     * reading.
     */
    private fun aiSeats(lobby: TournamentLobby): Set<EntityId> =
        lobby.players.keys.filterTo(mutableSetOf()) { playerId ->
            aiGameManager.isAiPlayer(playerId) || lobby.players[playerId]?.identity?.isAi == true
        }

    /**
     * Decide an AI-vs-AI match without running a game, and report it exactly as a played one reports.
     *
     * Consuming both ready flags mirrors a real start: a flag is spent the moment the seat is put in a
     * match, and [afterMatchResult] re-readies the AI for whatever comes next. Nothing else about the
     * bracket knows the difference — the match keeps no `gameSessionId`, so it never shows up as
     * something to spectate and has no replay, which is what [TournamentMatch.isSimulated] tells the
     * standings to say.
     */
    private fun simulateMatch(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        round: TournamentRound,
        match: TournamentMatch,
        player2Id: EntityId,
    ) {
        val winnerId = aiMatchSimulator.pickWinner(match.player1Id, player2Id)
        logger.info(
            "Simulating round {} AI match in tournament {}: {} vs {} — winner {}",
            round.roundNumber,
            lobby.lobbyId,
            lobby.players[match.player1Id]?.identity?.playerName,
            lobby.players[player2Id]?.identity?.playerName,
            lobby.players[winnerId]?.identity?.playerName,
        )

        tournament.reportSimulatedResult(match, winnerId)
        lobby.clearPlayerReady(match.player1Id)
        lobby.clearPlayerReady(player2Id)
        ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
        ctx.lobbyRepository.saveLobby(lobby)

        afterMatchResult(lobby.lobbyId, round, match)
    }

    fun startSingleMatch(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        round: TournamentRound,
        match: TournamentMatch
    ): Boolean {
        // A BYE has no game to start; [TournamentManager.startableMatches] already filters them out.
        val player2Id = match.player2Id ?: return false

        // Both refusals below used to be bare `?: return false`. They cost us a production stall: the
        // sweep re-picked the same unstartable match on every pass, refused it without a word, and left
        // the pair incomplete forever — so the only symptom in the log was silence. Say which seat and
        // why; the two causes want opposite handling.
        val player1State = lobby.players[match.player1Id]
        val player2State = lobby.players[player2Id]
        if (player1State == null || player2State == null) {
            // Structural, and [forfeitUnseatedMatches] should already have forfeited it: reaching here
            // means a seat left between that reconcile and this call.
            logger.warn(
                "Tournament {}: cannot start round {} match — no lobby seat for {}",
                lobby.lobbyId,
                round.roundNumber,
                listOfNotNull(
                    match.player1Id.value.takeIf { player1State == null },
                    player2Id.value.takeIf { player2State == null },
                ).joinToString(", "),
            )
            return false
        }

        val submittedDeck1 = lobby.getSubmittedDeck(match.player1Id)
        val submittedDeck2 = lobby.getSubmittedDeck(player2Id)
        if (submittedDeck1 == null || submittedDeck2 == null) {
            // Transient — an AI seat whose deck build hasn't landed yet, most likely. Deliberately not
            // forfeited: the next sweep starts the match once the deck arrives.
            logger.warn(
                "Tournament {}: cannot start round {} match yet — no submitted deck for {}",
                lobby.lobbyId,
                round.roundNumber,
                listOfNotNull(
                    player1State.identity.playerName.takeIf { submittedDeck1 == null },
                    player2State.identity.playerName.takeIf { submittedDeck2 == null },
                ).joinToString(", "),
            )
            return false
        }

        val baseDeck1 = BoosterGenerator.withBasicLandArt(submittedDeck1, lobby.basicLands)
        val baseDeck2 = BoosterGenerator.withBasicLandArt(submittedDeck2, lobby.basicLands)
        val deckPrintings1 = player1State.cardPool + lobby.basicLands.values
        val deckPrintings2 = player2State.cardPool + lobby.basicLands.values
        val deck1WithEgg = EasterEggDeckInjector.maybeInjectEasterEggs(
            player1State.identity.playerName, baseDeck1, gameProperties.easterEggs.enabled
        )
        val deck2WithEgg = EasterEggDeckInjector.maybeInjectEasterEggs(
            player2State.identity.playerName, baseDeck2, gameProperties.easterEggs.enabled
        )

        // Commander rules come off the lobby's Rules axis — one field, whether the decks were
        // brought, sealed or drafted. The deck list on the wire / in the lobby includes one copy of
        // the commander; the engine expects `Deck.cards` (= library) without it, so strip one copy
        // here. Mirrors QuickGameLobbyHandler.startGame.
        val usesCommanders = lobby.usesCommanderRules
        val commander1 = if (usesCommanders) player1State.commander else null
        val commander2 = if (usesCommanders) player2State.commander else null
        // The deck-submit path rejects commander submissions that don't designate a commander, but
        // defend in depth: if we somehow reach match start without one, refuse to launch the match
        // instead of crashing in GameInitializer.
        if (usesCommanders && (commander1 == null || commander2 == null)) {
            val missing = listOfNotNull(
                player1State.identity.playerName.takeIf { commander1 == null },
                player2State.identity.playerName.takeIf { commander2 == null },
            ).joinToString(", ")
            logger.warn("Tournament ${lobby.lobbyId}: cannot start Commander match — missing commander for $missing")
            return false
        }
        // A partner pair (CR 702.124) seats both commanders; the second only rides beside a first.
        val commanders1 = listOfNotNull(commander1, player1State.secondCommander?.takeIf { commander1 != null })
        val commanders2 = listOfNotNull(commander2, player2State.secondCommander?.takeIf { commander2 != null })
        val unpinnedDeck1 = DeckRequestConverter.stripCommanders(deck1WithEgg, commanders1)
        val unpinnedDeck2 = DeckRequestConverter.stripCommanders(deck2WithEgg, commanders2)
        val deck1 = BoosterGenerator.withCardArt(unpinnedDeck1, deckPrintings1)
        val deck2 = BoosterGenerator.withCardArt(unpinnedDeck2, deckPrintings2)

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled,
            debugMode = gameProperties.debugMode,
            printingRegistry = printingRegistry,
            tokenArtRegistry = tokenArtRegistry,
        )
        if (usesCommanders) {
            // Where the deck came from decides the shape: a pool-built deck reads life total /
            // commander damage / deck size from the lobby's preset (BRAWL = 60/25/16, COMMANDER =
            // 60/30/21). Every match here is 1v1, so TournamentLobby.effectiveCommanderPreset
            // resolves to the host's choice — it is read rather than commanderPreset so the preset
            // rule lives in exactly one place. A brought deck is paper Commander (100/40/21).
            gameSession.engineFormat =
                if (lobby.format == com.wingedsheep.gameserver.lobby.TournamentFormat.PREMADE_DECKS) {
                    com.wingedsheep.sdk.core.Format.Commander()
                } else {
                    lobby.effectiveCommanderPreset.toFormat().copy(deckSize = lobby.deckSizeMin)
                }
        }
        // Ranked tournaments adjust both players' ELO per 1v1 match. Only TOURNAMENT-mode lobbies are
        // ranked-eligible and the start gate rejects AI/guest seats, so a ranked match here is always
        // two signed-in humans; the game-over path re-checks before applying any rating change.
        if (lobby.ranked) {
            gameSession.ranked = true
            gameSession.rankedMode = com.wingedsheep.gameserver.ranking.Ranked
                .modeForTournament(lobby.rules, lobby.format)
        }
        val ps1 = player1State.identity.toPlayerSession()
        val ps2 = player2State.identity.toPlayerSession()

        gameSession.addPlayer(
            ps1, deck1, commanderCardNames = commanders1,
            sideboard = BoosterGenerator.withCardArt(
                lobby.getSubmittedSideboard(match.player1Id), deckPrintings1,
            ),
            companion = lobby.getSubmittedCompanion(match.player1Id),
        )
        gameSession.addPlayer(
            ps2, deck2, commanderCardNames = commanders2,
            sideboard = BoosterGenerator.withCardArt(
                lobby.getSubmittedSideboard(match.player2Id), deckPrintings2,
            ),
            companion = lobby.getSubmittedCompanion(match.player2Id),
        )

        // Carry isAi / aiModelOverride from each identity so a server restart can rehydrate and
        // re-wire an AI seat. Omitting these persisted the AI as isAi=false, so on recovery it was
        // treated as a human, never re-wired, and the match froze with the AI unable to act.
        gameSession.setPlayerPersistenceInfo(
            ps1.playerId, ps1.playerName, player1State.identity.token,
            isAi = player1State.identity.isAi, aiModelOverride = player1State.identity.aiModelOverride
        )
        gameSession.setPlayerPersistenceInfo(
            ps2.playerId, ps2.playerName, player2State.identity.token,
            isAi = player2State.identity.isAi, aiModelOverride = player2State.identity.aiModelOverride
        )

        gameRepository.save(gameSession)
        gameRepository.linkToLobby(gameSession.sessionId, lobby.lobbyId)
        match.gameSessionId = gameSession.sessionId
        ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)

        ctx.cleanUpSpectatingState(player1State.identity)
        ctx.cleanUpSpectatingState(player2State.identity)

        player1State.identity.currentGameSessionId = gameSession.sessionId
        player2State.identity.currentGameSessionId = gameSession.sessionId

        val ws1 = player1State.identity.webSocketSession
        val ws2 = player2State.identity.webSocketSession
        if (ws1 != null) {
            ctx.sessionRegistry.getPlayerSession(ws1.id)?.currentGameSessionId = gameSession.sessionId
        }
        if (ws2 != null) {
            ctx.sessionRegistry.getPlayerSession(ws2.id)?.currentGameSessionId = gameSession.sessionId
        }

        if (ws1 != null && ws1.isOpen) {
            ctx.sender.send(ws1, ServerMessage.TournamentMatchStarting(
                lobbyId = lobby.lobbyId,
                round = round.roundNumber,
                gameSessionId = gameSession.sessionId,
                opponentName = player2State.identity.playerName
            ))
        }
        if (ws2 != null && ws2.isOpen) {
            ctx.sender.send(ws2, ServerMessage.TournamentMatchStarting(
                lobbyId = lobby.lobbyId,
                round = round.roundNumber,
                gameSessionId = gameSession.sessionId,
                opponentName = player1State.identity.playerName
            ))
        }

        for (ps in listOf(ps1, ps2)) {
            if (aiGameManager.isAiPlayer(ps.playerId)) {
                aiGameManager.wireAiForGame(
                    gameSession = gameSession,
                    aiPlayerId = ps.playerId,
                    deckList = lobby.getSubmittedDeck(ps.playerId),
                    onActionReady = { aiPlayerId, action, interactionEpoch ->
                        gamePlayHandler.handleAiAction(gameSession, aiPlayerId, action, interactionEpoch)
                    },
                    onMulliganKeep = { aiPlayerId ->
                        gamePlayHandler.handleAiMulliganKeep(gameSession, aiPlayerId)
                    },
                    onMulliganTake = { aiPlayerId ->
                        gamePlayHandler.handleAiMulliganTake(gameSession, aiPlayerId)
                    },
                    onBottomCards = { aiPlayerId, cardIds ->
                        gamePlayHandler.handleAiBottomCards(gameSession, aiPlayerId, cardIds)
                    }
                )
                val aiIdentity = lobby.players[ps.playerId]?.identity
                if (aiIdentity != null) {
                    val newWs = aiIdentity.webSocketSession
                    if (newWs != null) {
                        val newPs = PlayerSession(
                            webSocketSession = newWs,
                            playerId = ps.playerId,
                            playerName = ps.playerName,
                            currentGameSessionId = gameSession.sessionId
                        )
                        gameSession.replacePlayerSession(ps.playerId, newPs)
                    }
                }
            }
        }

        gamePlayHandler.startGame(gameSession)

        spectatingHandler.broadcastActiveMatchesToWaitingPlayers(lobby.lobbyId)
        return true
    }

    fun ensureTournamentCreated(lobby: TournamentLobby): TournamentManager {
        val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
        return synchronized(lock) {
            val existing = ctx.lobbyRepository.findTournamentById(lobby.lobbyId)
            if (existing != null) return@synchronized existing

            logger.info("Creating tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players (early creation for matchups)")

            val players = lobby.players.values
                .map { ps -> ps.identity.playerId to ps.identity.playerName }
                .sortedBy { it.first.value }

            val tournament = TournamentManager(lobby.lobbyId, players, lobby.gamesPerMatch)
            ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
            recordTournamentStarted(lobby)

            tournament
        }
    }

    fun sendTournamentStartedToPlayer(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        identity: PlayerIdentity,
        wsOverride: WebSocketSession? = null
    ) {
        val ws = wsOverride ?: identity.webSocketSession ?: return

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val nextMatch = tournament.getNextMatchForPlayer(identity.playerId)
        val nextOpponentName: String?
        val hasBye: Boolean

        if (nextMatch != null) {
            val (nextRound, match) = nextMatch
            val isCurrentRound = tournament.currentRound?.let { nextRound.roundNumber == it.roundNumber } ?: true
            val opponentId = if (match.player1Id == identity.playerId) match.player2Id else match.player1Id
            nextOpponentName = if (isCurrentRound && !match.isBye) opponentId?.let { lobby.players[it]?.identity?.playerName } else null
            hasBye = match.isBye
        } else {
            val firstRoundMatchups = tournament.peekNextRoundMatchups()
            val nextOpponentId = firstRoundMatchups[identity.playerId]
            nextOpponentName = nextOpponentId?.let { lobby.players[it]?.identity?.playerName }
            hasBye = firstRoundMatchups.containsKey(identity.playerId) && nextOpponentId == null
        }

        ctx.sender.send(ws, ServerMessage.TournamentStarted(
            lobbyId = lobby.lobbyId,
            totalRounds = tournament.totalRounds,
            standings = tournament.getStandingsInfo(connectedIds),
            nextOpponentName = nextOpponentName,
            nextRoundHasBye = hasBye
        ))

        val readyPlayerIds = lobby.getReadyPlayerIds()
        if (readyPlayerIds.isNotEmpty()) {
            // A snapshot for this one player, not news about them — hence no playerId.
            ctx.sender.send(ws, ServerMessage.PlayerReadyForRound(
                lobbyId = lobby.lobbyId,
                readyPlayerIds = readyPlayerIds.map { it.value },
                totalConnectedPlayers = connectedIds.size
            ))
        }
    }

    fun startTournament(lobby: TournamentLobby) {
        logger.info("Starting tournament for lobby ${lobby.lobbyId} with ${lobby.playerCount} players")
        lobby.startTournament()

        val players = lobby.players.values
            .map { ps -> ps.identity.playerId to ps.identity.playerName }
            .sortedBy { it.first.value }

        val tournament = TournamentManager(lobby.lobbyId, players, lobby.gamesPerMatch)
        ctx.lobbyRepository.saveTournament(lobby.lobbyId, tournament)
        recordTournamentStarted(lobby)

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val firstRoundMatchups = tournament.peekNextRoundMatchups()

        lobby.players.forEach { (playerId, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                val nextOpponentId = firstRoundMatchups[playerId]
                val nextOpponentName = if (nextOpponentId != null) {
                    lobby.players[nextOpponentId]?.identity?.playerName
                } else {
                    null
                }
                val hasBye = firstRoundMatchups.containsKey(playerId) && nextOpponentId == null

                ctx.sender.send(ws, ServerMessage.TournamentStarted(
                    lobbyId = lobby.lobbyId,
                    totalRounds = tournament.totalRounds,
                    standings = tournament.getStandingsInfo(connectedIds),
                    nextOpponentName = nextOpponentName,
                    nextRoundHasBye = hasBye
                ))
            }
        }
    }

    /**
     * @param autoReadyHumansVsAi when true, a human whose next opponent is AI is auto-readied (and the
     *   match started) so a solo-vs-AI tournament doesn't require a manual ready click to begin. This is
     *   intentionally `false` after a game/round finishes: the human must first dismiss the game-over
     *   overlay and click "Ready for Next Round" in the lobby, so the next game can't start underneath it.
     */
    fun autoReadyAiPlayers(lobby: TournamentLobby, tournament: TournamentManager, autoReadyHumansVsAi: Boolean = true) {
        val lock = ctx.roundLocks.computeIfAbsent(lobby.lobbyId) { Any() }
        synchronized(lock) {
            autoReadyAiPlayersLocked(lobby, tournament, autoReadyHumansVsAi)
        }
    }

    private fun autoReadyAiPlayersLocked(lobby: TournamentLobby, tournament: TournamentManager, autoReadyHumansVsAi: Boolean) {
        // Any AI seat with a submitted deck that isn't ready yet. When every AI is already ready there
        // is nothing to mark — but that must NOT short-circuit the [startReadyMatches] sweep at the
        // bottom: a pair blocked by an earlier-round game stays ready, and re-examining the whole ready
        // set is the only thing that starts it once the blocker lands.
        val hasAiPlayersToReady = lobby.players.any { (playerId, ps) ->
            aiGameManager.isAiPlayer(playerId) && ps.hasSubmittedDeck && playerId !in lobby.getReadyPlayerIds()
        }

        // Open a round once everyone has a deck, so there are matches to find: the first one, or the
        // one an extra rotation just appended past a finished bracket. Deliberately outside the
        // `hasAiPlayersToReady` guard, which was only ever true here because `doHandleRoundComplete`
        // had just wiped the ready set — it doesn't, now, and the advance can't hang off that.
        if (lobby.allDecksSubmitted()) {
            prepareRoundsIfNeeded(lobby, tournament)
        }

        if (hasAiPlayersToReady) {
            for ((playerId, playerState) in lobby.players) {
                if (!aiGameManager.isAiPlayer(playerId)) continue
                if (!playerState.hasSubmittedDeck) continue

                if (lobby.markPlayerReady(playerId)) {
                    logger.info("AI ${playerState.identity.playerName} auto-ready for next round")
                    // Broadcast the AI's flip too. The human branches below do; without this the other
                    // clients' ready indicators stay stale until something else happens to broadcast.
                    broadcastReadyStatus(lobby, playerState.identity)
                    resolveByesForPlayer(lobby, tournament, playerState.identity)
                }
            }
        }

        // Auto-ready human players whose next opponent is AI (no reason to wait).
        // Skipped after a game/round ends: the human must dismiss the game-over overlay and click
        // "Ready for Next Round" first, otherwise the next game would start under the overlay.
        if (autoReadyHumansVsAi) {
            for ((playerId, playerState) in lobby.players) {
                if (aiGameManager.isAiPlayer(playerId)) continue
                if (!playerState.hasSubmittedDeck) continue
                if (playerId in lobby.getReadyPlayerIds()) continue

                val nextMatch = tournament.getNextMatchForPlayer(playerId) ?: continue
                val (nextRound, match) = nextMatch
                val opponentId = if (match.player1Id == playerId) match.player2Id else match.player1Id
                if (opponentId == null || !aiGameManager.isAiPlayer(opponentId)) continue

                if (tournament.hasIncompleteMatchBefore(playerId, nextRound.roundNumber)) continue

                lobby.markPlayerReady(playerId)
                logger.info("Auto-readied ${playerState.identity.playerName} (opponent is AI)")
                broadcastReadyStatus(lobby, playerState.identity)
                resolveByesForPlayer(lobby, tournament, playerState.identity)
            }
        }

        startReadyMatches(lobby, tournament)
    }

    /**
     * Push the authoritative ready set to every connected player. [identity] names the player whose
     * flag just went up, when there is one; a plain snapshot — after match starts consumed flags —
     * passes null and only carries the set.
     */
    fun broadcastReadyStatus(lobby: TournamentLobby, identity: PlayerIdentity? = null) {
        val connectedPlayers = lobby.players.values.filter { it.identity.isConnected }
        val readyPlayerIds = lobby.getReadyPlayerIds().map { it.value }

        val readyMessage = ServerMessage.PlayerReadyForRound(
            lobbyId = lobby.lobbyId,
            playerId = identity?.playerId?.value,
            playerName = identity?.playerName,
            readyPlayerIds = readyPlayerIds,
            totalConnectedPlayers = connectedPlayers.size
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, readyMessage)
            }
        }
    }

    fun completeTournament(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return

        logger.info("Tournament complete for lobby $lobbyId")
        lobby.completeTournament()
        ctx.lobbyRepository.saveLobby(lobby)
        ctx.lobbyRepository.saveTournament(lobbyId, tournament)

        val connectedIds = lobby.players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
            .toSet()

        val message = ServerMessage.TournamentComplete(
            lobbyId = lobbyId,
            finalStandings = tournament.getStandingsInfo(connectedIds)
        )

        lobby.players.forEach { (_, playerState) ->
            val ws = playerState.identity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, message)
            }
        }

        lobby.spectators.forEach { (_, spectatorIdentity) ->
            val ws = spectatorIdentity.webSocketSession
            if (ws != null && ws.isOpen) {
                ctx.sender.send(ws, message)
            }
        }

        // Record the finished tournament for durable stats. No-op unless accounts are enabled, and
        // only when at least one seat is a human (AI-only / LLM tournaments use a separate path). This
        // upserts the in-progress row recorded when the bracket went live, flipping it to COMPLETED.
        tournamentResultSink.recordCompleted(
            buildRecordedTournament(lobby, tournament, endedAt = java.time.Instant.now())
        )
    }

    /**
     * Persist the current standings of a still-running tournament, so player profiles and the admin
     * dashboard show live results (wins/losses/draws and provisional placement) rather than the zeroed
     * seed written at start. A no-op unless accounts are enabled with a human seat, and the sink only
     * touches rows still marked IN_PROGRESS. Called after each match result and after an abandon; an
     * abandoned tournament then keeps these last-known standings when its row flips to ABANDONED.
     */
    private fun recordTournamentProgress(lobbyId: String) {
        val lobby = ctx.lobbyRepository.findLobbyById(lobbyId) ?: return
        val tournament = ctx.lobbyRepository.findTournamentById(lobbyId) ?: return
        tournamentResultSink.recordProgress(buildRecordedTournament(lobby, tournament, endedAt = null))
    }

    /**
     * Build a [RecordedTournament][com.wingedsheep.gameserver.stats.RecordedTournament] snapshot from the
     * live tournament's ranked standings. [endedAt] is null while in progress and set on completion; the
     * winner is only named once the tournament has ended.
     */
    private fun buildRecordedTournament(
        lobby: TournamentLobby,
        tournament: TournamentManager,
        endedAt: java.time.Instant?,
    ): com.wingedsheep.gameserver.stats.RecordedTournament {
        val standings = tournament.getRankedStandings()
        return com.wingedsheep.gameserver.stats.RecordedTournament(
            lobbyId = lobby.lobbyId,
            name = tournamentDisplayName(lobby),
            format = lobby.format.name,
            gameMode = lobby.gameMode.name,
            setCodes = lobby.setCodes.joinToString(","),
            playerCount = lobby.playerCount,
            rounds = tournament.getRoundsForPersistence().size,
            gamesPerMatch = lobby.gamesPerMatch,
            winnerName = if (endedAt != null) standings.firstOrNull { it.rank == 1 }?.standing?.playerName else null,
            startedAt = null,
            endedAt = endedAt,
            participants = standings.map { rs ->
                val identity = lobby.players[rs.standing.playerId]?.identity
                com.wingedsheep.gameserver.stats.RecordedTournamentParticipant(
                    userId = identity?.userId,
                    playerName = rs.standing.playerName,
                    isAi = identity?.isAi == true,
                    placement = rs.rank,
                    wins = rs.standing.wins,
                    losses = rs.standing.losses,
                    draws = rs.standing.draws,
                )
            },
        )
    }

    /** Human-readable tournament name, e.g. "Bloomburrow / Duskmourn Sealed". */
    private fun tournamentDisplayName(lobby: TournamentLobby): String =
        lobby.setNames.joinToString(" / ") + " " +
            lobby.format.name.lowercase().replaceFirstChar { it.uppercase() }

    /**
     * Record that a tournament has gone live, so it shows in the admin dashboard and player profiles
     * while it is still being played. Idempotent per lobby (the sink skips if a row already exists) and
     * a no-op unless accounts are enabled with at least one human seat. Placements are recorded as 0
     * (unknown) until [completeTournament] fills in the final standings.
     */
    private fun recordTournamentStarted(lobby: TournamentLobby) {
        tournamentResultSink.recordStarted(
            com.wingedsheep.gameserver.stats.RecordedTournament(
                lobbyId = lobby.lobbyId,
                name = tournamentDisplayName(lobby),
                format = lobby.format.name,
                gameMode = lobby.gameMode.name,
                setCodes = lobby.setCodes.joinToString(","),
                playerCount = lobby.playerCount,
                rounds = 0,
                gamesPerMatch = lobby.gamesPerMatch,
                winnerName = null,
                startedAt = java.time.Instant.now(),
                endedAt = null,
                participants = lobby.players.values.map { ps ->
                    com.wingedsheep.gameserver.stats.RecordedTournamentParticipant(
                        userId = ps.identity.userId,
                        playerName = ps.identity.playerName,
                        isAi = ps.identity.isAi,
                        placement = 0,
                        wins = 0,
                        losses = 0,
                        draws = 0,
                    )
                },
            )
        )
    }
}
