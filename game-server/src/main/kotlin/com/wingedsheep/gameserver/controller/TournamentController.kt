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
        val deckFormat: String? = null
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
        val publicLobbies = lobbyRepository.findAllLobbies()
            .filter { lobby ->
                lobby.isPublic &&
                    lobby.state == LobbyState.WAITING_FOR_PLAYERS &&
                    !lobby.isFull
            }
            .sortedBy { it.lobbyId }
            .map { lobby ->
                PublicTournamentDTO(
                    lobbyId = lobby.lobbyId,
                    state = lobby.state.name,
                    playerCount = lobby.playerCount,
                    maxPlayers = lobby.maxPlayers,
                    format = lobby.format.name,
                    setNames = lobby.setNames,
                    boosterCount = lobby.boosterCount,
                    gamesPerMatch = lobby.gamesPerMatch,
                    deckFormat = lobby.deckFormat?.name
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
