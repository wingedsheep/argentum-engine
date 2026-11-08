package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tournaments")
class TournamentController(
    private val lobbyRepository: LobbyRepository,
    private val gameRepository: GameRepository,
) {

    data class TournamentStatusDTO(
        val exists: Boolean,
        val state: String,
        val playerCount: Int,
        val format: String,
        val maxPlayers: Int,
        val setNames: List<String>,
        val isPublic: Boolean
    )

    data class PublicTournamentDTO(
        val lobbyId: String,
        val state: String,
        val playerCount: Int,
        val maxPlayers: Int,
        val format: String,
        val setNames: List<String>,
        val boosterCount: Int,
        val gamesPerMatch: Int,
        /** Games per match: 1, or 3 for a best-of-three. */
        val bestOf: Int = 1,
        val deckFormat: String? = null,
        val hostName: String? = null,
        /** The host's account avatar id, or null for a guest — drawn beside "Hosted by". */
        val hostAvatar: String? = null,
        /** Seated human players, host first. AI seats are counted in [playerCount] and [aiPlayerCount]. */
        val playerNames: List<String> = emptyList(),
        /** How many of [playerCount]'s seats are held by AI players, so the list can tell them apart from people. */
        val aiPlayerCount: Int = 0,
        /** TOURNAMENT (bracket), FREE_FOR_ALL, TWO_HEADED_GIANT or TEAM_VS_TEAM. */
        val gameMode: String = "TOURNAMENT",
        val ranked: Boolean = false,
        /** Name of the cube the packs come from, when it isn't a set. */
        val cubeName: String? = null,
        /** Seconds the lobby has been open, measured on the server so client clock skew can't distort it. */
        val openSeconds: Long = 0,
    )

    data class LiveTournamentMatchDTO(
        val gameSessionId: String,
        val lobbyId: String,
        val round: Int,
        val player1Name: String,
        val player2Name: String,
        val player1Life: Int,
        val player2Life: Int,
    )

    @GetMapping("/{lobbyId}/status")
    fun getStatus(@PathVariable lobbyId: String): ResponseEntity<TournamentStatusDTO> {
        val lobby = lobbyRepository.findLobbyById(lobbyId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            TournamentStatusDTO(
                exists = true,
                state = lobby.state.name,
                playerCount = lobby.playerCount,
                format = lobby.format.name,
                maxPlayers = lobby.maxPlayers,
                setNames = lobby.setNames,
                isPublic = lobby.isPublic
            )
        )
    }

    @GetMapping("/public")
    fun listPublic(): ResponseEntity<List<PublicTournamentDTO>> {
        val now = System.currentTimeMillis()
        val publicLobbies = lobbyRepository.findAllLobbies()
            .filter { lobby ->
                lobby.isPublic &&
                    lobby.state == LobbyState.WAITING_FOR_PLAYERS &&
                    !lobby.isFull
            }
            .sortedBy { it.lobbyId }
            .map { lobby ->
                val host = lobby.hostPlayerId?.let { lobby.players[it]?.identity }
                val humans = lobby.players.values.map { it.identity }.filter { !it.isAi }
                PublicTournamentDTO(
                    lobbyId = lobby.lobbyId,
                    state = lobby.state.name,
                    playerCount = lobby.playerCount,
                    maxPlayers = lobby.maxPlayers,
                    format = lobby.format.name,
                    setNames = lobby.setNames,
                    boosterCount = lobby.boosterCount,
                    gamesPerMatch = lobby.gamesPerMatch,
                    bestOf = lobby.bestOf,
                    deckFormat = lobby.deckFormat?.name,
                    hostName = host?.playerName,
                    hostAvatar = host?.avatar,
                    playerNames = humans.sortedBy { if (it.playerId == host?.playerId) 0 else 1 }.map { it.playerName },
                    aiPlayerCount = lobby.playerCount - humans.size,
                    gameMode = lobby.gameMode.name,
                    ranked = lobby.ranked,
                    cubeName = lobby.cube?.name,
                    openSeconds = (now - lobby.createdAt).coerceAtLeast(0) / 1000,
                )
            }

        return ResponseEntity.ok(publicLobbies)
    }

    /**
     * In-progress matches in public tournaments. Powers the Live Games section on the landing
     * page so anonymous visitors can drop in as a spectator. Each row is a single match (one
     * tournament can produce many concurrent matches).
     */
    @GetMapping("/live")
    fun listLive(): ResponseEntity<List<LiveTournamentMatchDTO>> {
        val live = lobbyRepository.findAllLobbies()
            .filter { it.isPublic && it.state == LobbyState.TOURNAMENT_ACTIVE }
            .flatMap { lobby ->
                val tournament = lobbyRepository.findTournamentById(lobby.lobbyId) ?: return@flatMap emptyList()
                val round = tournament.currentRound?.roundNumber ?: 0
                tournament.getAllInProgressMatches().mapNotNull { match ->
                    val gameSessionId = match.gameSessionId ?: return@mapNotNull null
                    val session = gameRepository.findById(gameSessionId) ?: return@mapNotNull null
                    if (session.isGameOver()) return@mapNotNull null
                    // Tournament matches are 2-player; the live tile shows the two seats.
                    val names = session.getPlayerNames()
                    val life = session.getLifeTotals()
                    if (names.size < 2 || life.size < 2) return@mapNotNull null
                    LiveTournamentMatchDTO(
                        gameSessionId = gameSessionId,
                        lobbyId = lobby.lobbyId,
                        round = round,
                        player1Name = names[0],
                        player2Name = names[1],
                        player1Life = life[0],
                        player2Life = life[1],
                    )
                }
            }
            .sortedBy { it.gameSessionId }
        return ResponseEntity.ok(live)
    }
}
