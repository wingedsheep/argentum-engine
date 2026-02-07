package com.wingedsheep.gameserver.controller

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
        val format: String
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
                format = lobby.format.name
            )
        )
    }
}
