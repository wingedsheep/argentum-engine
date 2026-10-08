package com.wingedsheep.gameserver.activity

import com.wingedsheep.gameserver.lobby.LobbyGameMode
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.QuickGameLobbyRepository
import com.wingedsheep.gameserver.matchmaking.MatchmakingService
import com.wingedsheep.gameserver.matchmaking.QueueKey
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.PlayerIdentity
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * What one online player is doing right now, for the admin Live games view. The server's own session
 * state answers most of it — a seat in a game, a draft pick, a lobby, a matchmaking search — and the
 * page the client last reported ([PlayerActivityTracker]) fills in the rest: the deckbuilder, the
 * help, a profile. Precedence follows what holds the player's attention: a game beats a lobby, a
 * lobby beats the page they happen to have open. A matchmaking search runs beside any of these, so
 * it is reported on its own.
 */
@Component
class PlayerActivityResolver(
    private val gameRepository: GameRepository,
    private val lobbyRepository: LobbyRepository,
    private val quickGameLobbies: QuickGameLobbyRepository,
    private val matchmaking: MatchmakingService,
    private val tracker: PlayerActivityTracker,
) {

    /** The coarse buckets the dashboard counts and colours by. */
    enum class Kind { PLAYING, SPECTATING, DRAFTING, BUILDING_LIMITED, TOURNAMENT, LOBBY, SEARCHING, DECKBUILDER, BROWSING, HOME }

    data class Activity(
        val kind: Kind,
        val detail: String,
        /** The game to spectate from the dashboard, when the player is seated in or watching one. */
        val gameSessionId: String? = null,
    )

    data class PlayerActivity(
        val name: String,
        val signedIn: Boolean,
        val activity: Activity,
        /** Matchmaking queue label while searching beside something else; null when not queued. */
        val searching: String?,
        val searchingSince: Instant?,
        val page: String?,
        val pageSince: Instant?,
        val lastInputAt: Instant?,
    )

    fun resolve(identity: PlayerIdentity): PlayerActivity {
        val page = tracker.pageOf(identity.token)
        val search = matchmaking.searchOf(identity.playerId)
        val searchLabel = search?.let { searchLabel(it.key) + if (it.matchFound) " · match found" else "" }
        val activity = gameActivity(identity)
            ?: spectating(identity)
            ?: tournamentLobby(identity)
            ?: quickGameLobby(identity)
            ?: searchLabel?.let { Activity(Kind.SEARCHING, "Searching for a match · $it") }
            ?: pageActivity(page?.page)
        return PlayerActivity(
            name = identity.playerName,
            signedIn = identity.userId != null,
            activity = activity,
            searching = searchLabel.takeIf { activity.kind != Kind.SEARCHING },
            searchingSince = search?.let { Instant.ofEpochMilli(it.joinedAt) },
            page = page?.page,
            pageSince = page?.since,
            lastInputAt = tracker.lastInputAt(identity.token),
        )
    }

    private fun gameActivity(identity: PlayerIdentity): Activity? {
        val session = identity.currentGameSessionId?.let { gameRepository.findById(it) } ?: return null
        val snapshot = session.adminSnapshot()
        if (snapshot.gameOver) return null
        val others = snapshot.seats.filter { it.name != identity.playerName }
            // Built-in AI names already read "[AI] …"; only an unmarked AI seat needs the tag.
            .joinToString(", ") { if (it.isAi && !it.name.startsWith("[AI]")) "${it.name} (AI)" else it.name }
        val where = when {
            !snapshot.started -> "mulligans"
            else -> "turn ${snapshot.turnNumber ?: "?"}"
        }
        val opponents = if (others.isEmpty()) "" else " vs $others"
        return Activity(Kind.PLAYING, "Playing$opponents · $where", session.sessionId)
    }

    private fun spectating(identity: PlayerIdentity): Activity? {
        val session = identity.currentSpectatingGameId?.let { gameRepository.findById(it) } ?: return null
        return Activity(Kind.SPECTATING, "Watching ${session.getPlayerNames().joinToString(" vs ")}", session.sessionId)
    }

    private fun tournamentLobby(identity: PlayerIdentity): Activity? {
        val lobby = identity.currentLobbyId?.let { lobbyRepository.findLobbyById(it) } ?: return null
        val seat = lobby.players[identity.playerId]
        val mode = if (lobby.gameMode == LobbyGameMode.TOURNAMENT) pretty(lobby.format.name) else "${pretty(lobby.format.name)} ${pretty(lobby.gameMode.name)}"
        val sets = lobby.setNames.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = " · ").orEmpty()
        return when (lobby.state) {
            LobbyState.WAITING_FOR_PLAYERS -> Activity(
                Kind.LOBBY,
                "${if (lobby.isHost(identity.playerId)) "Hosting" else "In"} a $mode lobby$sets · ${lobby.players.size} seated",
            )
            LobbyState.DRAFTING -> Activity(
                Kind.DRAFTING,
                "Drafting$sets · pack ${lobby.currentPackNumber}, ${seat?.cardPool?.size ?: 0} picked",
            )
            LobbyState.DECK_BUILDING -> Activity(
                Kind.BUILDING_LIMITED,
                if (seat?.hasSubmittedDeck == true) "Deck submitted, waiting for the pod$sets" else "Building a $mode deck$sets",
            )
            LobbyState.TOURNAMENT_ACTIVE -> {
                val round = lobbyRepository.findTournamentById(lobby.lobbyId)
                    ?.let { t -> t.currentRound?.roundNumber?.let { " · round $it of ${t.totalRounds}" } }
                    .orEmpty()
                Activity(Kind.TOURNAMENT, "Between matches in a $mode tournament$round")
            }
            LobbyState.TOURNAMENT_COMPLETE -> Activity(Kind.TOURNAMENT, "Viewing final standings ($mode)")
        }
    }

    private fun quickGameLobby(identity: PlayerIdentity): Activity? {
        val lobby = identity.currentQuickGameLobbyId?.let { quickGameLobbies.findById(it) } ?: return null
        val me = lobby.findPlayer(identity.playerId)
        val kind = when {
            lobby.momirBasic -> "Momir Basic"
            lobby.twoHeadedGiant -> "Two-Headed Giant"
            lobby.format != null -> lobby.format!!.displayName
            else -> "random deck"
        }
        val against = when {
            lobby.vsAi -> " vs the AI"
            lobby.isFull -> ""
            else -> " · waiting for an opponent"
        }
        val deck = when {
            me?.ready == true -> " · ready"
            me?.deckList != null -> " · deck chosen"
            else -> " · choosing a deck"
        }
        return Activity(Kind.LOBBY, "Quick game lobby ($kind)$against$deck")
    }

    private fun pageActivity(page: String?): Activity = when (page) {
        null, PlayerActivityTracker.HOME -> Activity(Kind.HOME, "On the home screen")
        "deckbuilder" -> Activity(Kind.DECKBUILDER, "Editing decks in the deckbuilder")
        else -> Activity(Kind.BROWSING, "Viewing ${PlayerActivityTracker.pageLabel(page)}")
    }

    private fun searchLabel(key: QueueKey): String =
        listOfNotNull(
            QueueKey.displayName(key.mode),
            key.format?.displayName,
            "ranked".takeIf { key.ranked },
        ).joinToString(" · ")

    private fun pretty(token: String): String =
        token.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}
