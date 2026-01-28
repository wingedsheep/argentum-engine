package com.wingedsheep.gameserver.repository

import com.wingedsheep.gameserver.lobby.SealedLobby
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.tournament.TournamentManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

interface LobbyRepository {
    fun saveLobby(lobby: SealedLobby)
    fun findLobbyById(lobbyId: String): SealedLobby?
    fun removeLobby(lobbyId: String): SealedLobby?
    fun findAllLobbies(): Collection<SealedLobby>

    fun saveSealedSession(session: SealedSession)
    fun findSealedSessionById(sessionId: String): SealedSession?
    fun removeSealedSession(sessionId: String): SealedSession?

    fun saveTournament(lobbyId: String, tournament: TournamentManager)
    fun findTournamentById(lobbyId: String): TournamentManager?
    fun removeTournament(lobbyId: String): TournamentManager?
}

@Component
@ConditionalOnProperty(name = ["cache.redis.enabled"], havingValue = "false", matchIfMissing = true)
class InMemoryLobbyRepository : LobbyRepository {

    private val sealedLobbies = ConcurrentHashMap<String, SealedLobby>()
    private val sealedSessions = ConcurrentHashMap<String, SealedSession>()
    private val tournaments = ConcurrentHashMap<String, TournamentManager>()

    override fun saveLobby(lobby: SealedLobby) {
        sealedLobbies[lobby.lobbyId] = lobby
    }

    override fun findLobbyById(lobbyId: String): SealedLobby? = sealedLobbies[lobbyId]

    override fun removeLobby(lobbyId: String): SealedLobby? = sealedLobbies.remove(lobbyId)

    override fun findAllLobbies(): Collection<SealedLobby> = sealedLobbies.values

    override fun saveSealedSession(session: SealedSession) {
        sealedSessions[session.sessionId] = session
    }

    override fun findSealedSessionById(sessionId: String): SealedSession? = sealedSessions[sessionId]

    override fun removeSealedSession(sessionId: String): SealedSession? = sealedSessions.remove(sessionId)

    override fun saveTournament(lobbyId: String, tournament: TournamentManager) {
        tournaments[lobbyId] = tournament
    }

    override fun findTournamentById(lobbyId: String): TournamentManager? = tournaments[lobbyId]

    override fun removeTournament(lobbyId: String): TournamentManager? = tournaments.remove(lobbyId)
}
