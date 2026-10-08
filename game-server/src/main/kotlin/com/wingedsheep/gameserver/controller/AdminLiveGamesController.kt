package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.activity.PlayerActivityResolver
import com.wingedsheep.gameserver.activity.PlayerActivityTracker
import com.wingedsheep.gameserver.auth.AdminAuthService
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.SessionRegistry
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Admin view of what the server is doing *right now*: every game session in memory (public or not,
 * AI or human) and every tournament lobby past its waiting room. Answers "is this a good moment for
 * maintenance?" and gives each game a session id the dashboard can spectate through the ordinary
 * `/?spectate=` deep link. Also lists every online player with what they're doing — playing,
 * drafting, waiting in a lobby, searching, editing decks — via [PlayerActivityResolver], and a short
 * feed of recent moves from [PlayerActivityTracker]. Read-only; auth through [AdminAuthService]. Unlike the stats endpoints it
 * needs no database, so it's mounted whether or not accounts are enabled.
 */
@RestController
@RequestMapping("/api/admin/live-games")
class AdminLiveGamesController(
    private val adminAuth: AdminAuthService,
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val sessionRegistry: SessionRegistry,
    private val activityResolver: PlayerActivityResolver,
    private val activityTracker: PlayerActivityTracker,
) {

    data class SeatDto(val name: String, val isAi: Boolean, val connected: Boolean, val life: Int?)

    data class LiveGameDto(
        val gameSessionId: String,
        val seats: List<SeatDto>,
        /** Same vocabulary as recorded match history: a LobbyGameMode name, QUICK_GAME, or CASUAL. */
        val gameMode: String,
        /** The tournament's TournamentFormat when in one, else the engine format. */
        val format: String,
        val setCode: String?,
        val ranked: Boolean,
        val publicSpectate: Boolean,
        /** The tournament lobby this game belongs to, when it's a tournament match. */
        val tournamentLobbyId: String?,
        val started: Boolean,
        val gameOver: Boolean,
        val turnNumber: Int?,
        val activePlayerName: String?,
        val step: String?,
        val startedAt: String?,
        val lastActionAt: String?,
        val spectatorCount: Int,
        /** At least one human seat with an open socket — the games a restart would interrupt. */
        val hasConnectedHuman: Boolean,
    )

    data class LiveLobbyDto(
        val lobbyId: String,
        val gameMode: String,
        val format: String,
        val state: String,
        val setNames: List<String>,
        val humanPlayers: Int,
        val connectedHumans: Int,
        val currentRound: Int?,
        val totalRounds: Int?,
    )

    data class OnlinePlayerDto(
        val name: String,
        val signedIn: Boolean,
        /** A [PlayerActivityResolver.Kind] name. */
        val activity: String,
        val detail: String,
        val gameSessionId: String?,
        /** Matchmaking queue being searched beside the main activity, if any. */
        val searching: String?,
        val searchingSince: String?,
        /** Last page the client reported (`home`, `deckbuilder`, …). */
        val page: String?,
        val pageSince: String?,
        /** Last message other than a page report or heartbeat — a click, a pick, a game action. */
        val lastInputAt: String?,
    )

    data class FeedEntryDto(val at: String, val playerName: String, val signedIn: Boolean, val kind: String, val text: String)

    data class LiveOverviewDto(
        val generatedAt: String,
        val onlinePlayers: Int,
        val games: List<LiveGameDto>,
        val lobbies: List<LiveLobbyDto>,
        val players: List<OnlinePlayerDto>,
        /** Recent player moves, newest first. In memory only — empty after a restart. */
        val feed: List<FeedEntryDto>,
    )

    @GetMapping
    fun overview(
        @RequestHeader("X-Admin-Password", required = false) password: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<Any> = adminAuth.guard(password, authorization) {
        val games = gameRepository.findAll().map { session ->
            val snapshot = session.adminSnapshot()
            val lobbyId = gameRepository.getLobbyForGame(session.sessionId)
            val tournament = lobbyId?.let { lobbyRepository.findLobbyById(it) }
            LiveGameDto(
                gameSessionId = session.sessionId,
                seats = snapshot.seats.map { SeatDto(it.name, it.isAi, it.connected, it.life) },
                gameMode = tournament?.gameMode?.name
                    ?: if (session.quickGameSetCode != null) "QUICK_GAME" else "CASUAL",
                format = tournament?.format?.name ?: session.engineFormat::class.simpleName ?: "Standard",
                setCode = session.quickGameSetCode,
                ranked = session.ranked,
                publicSpectate = session.publicSpectate,
                tournamentLobbyId = tournament?.lobbyId,
                started = snapshot.started,
                gameOver = snapshot.gameOver,
                turnNumber = snapshot.turnNumber,
                activePlayerName = snapshot.activePlayerName,
                step = snapshot.step?.name,
                startedAt = session.replayStartedAt?.toString(),
                lastActionAt = session.lastActionAt?.toString(),
                spectatorCount = session.getSpectators().size,
                hasConnectedHuman = snapshot.seats.any { !it.isAi && it.connected },
            )
        }.sortedWith(
            // Games a restart would hurt first, then the most recently active.
            compareByDescending<LiveGameDto> { it.hasConnectedHuman && !it.gameOver }
                .thenByDescending { it.lastActionAt ?: it.startedAt ?: "" }
        )

        val lobbies = lobbyRepository.findAllLobbies()
            .filter { it.state != LobbyState.WAITING_FOR_PLAYERS && it.state != LobbyState.TOURNAMENT_COMPLETE }
            .map { lobby ->
                val humans = lobby.players.values.map { it.identity }.filter { !it.isAi }
                val tournament = lobbyRepository.findTournamentById(lobby.lobbyId)
                LiveLobbyDto(
                    lobbyId = lobby.lobbyId,
                    gameMode = lobby.gameMode.name,
                    format = lobby.format.name,
                    state = lobby.state.name,
                    setNames = lobby.setNames,
                    humanPlayers = humans.size,
                    connectedHumans = humans.count { it.isConnected },
                    currentRound = tournament?.currentRound?.roundNumber,
                    totalRounds = tournament?.totalRounds,
                )
            }
            .sortedByDescending { it.connectedHumans }

        val identities = sessionRegistry.getAllIdentities()
        activityTracker.retainOnly(identities.mapTo(HashSet()) { it.token })
        val online = identities.filter { !it.isAi && it.webSocketSession?.isOpen == true }
        val players = online.map { identity ->
            val p = activityResolver.resolve(identity)
            OnlinePlayerDto(
                name = p.name,
                signedIn = p.signedIn,
                activity = p.activity.kind.name,
                detail = p.activity.detail,
                gameSessionId = p.activity.gameSessionId,
                searching = p.searching,
                searchingSince = p.searchingSince?.toString(),
                page = p.page,
                pageSince = p.pageSince?.toString(),
                lastInputAt = p.lastInputAt?.toString(),
            )
        }.sortedWith(compareBy<OnlinePlayerDto> { PlayerActivityResolver.Kind.valueOf(it.activity).ordinal }.thenBy { it.name.lowercase() })
        val feed = activityTracker.recentFeed().map { FeedEntryDto(it.at.toString(), it.playerName, it.signedIn, it.kind, it.text) }

        ResponseEntity.ok(
            LiveOverviewDto(
                generatedAt = Instant.now().toString(),
                onlinePlayers = online.size,
                games = games,
                lobbies = lobbies,
                players = players,
                feed = feed,
            )
        )
    }
}
