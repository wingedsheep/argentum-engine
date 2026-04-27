package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.repository.LobbyRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tournaments")
class TournamentController(
    private val lobbyRepository: LobbyRepository
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
        val gamesPerMatch: Int
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
                    gamesPerMatch = lobby.gamesPerMatch
                )
            }

        return ResponseEntity.ok(publicLobbies)
    }
}
