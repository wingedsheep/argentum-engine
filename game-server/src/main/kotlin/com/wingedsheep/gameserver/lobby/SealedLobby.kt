package com.wingedsheep.gameserver.lobby

import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * State machine for a multi-player sealed lobby.
 */
enum class LobbyState {
    WAITING_FOR_PLAYERS,
    DECK_BUILDING,
    TOURNAMENT_ACTIVE,
    TOURNAMENT_COMPLETE
}

/**
 * Per-player state within a lobby.
 */
data class LobbyPlayerState(
    val identity: PlayerIdentity,
    val cardPool: List<CardDefinition> = emptyList(),
    var submittedDeck: Map<String, Int>? = null
) {
    val hasSubmittedDeck: Boolean get() = submittedDeck != null
}

/**
 * Multi-player sealed lobby supporting up to 8 players with host controls.
 *
 * State machine:
 * WAITING_FOR_PLAYERS → DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
 */
class SealedLobby(
    val lobbyId: String = UUID.randomUUID().toString(),
    val setCode: String,
    val setName: String,
    var boosterCount: Int = 6,
    var maxPlayers: Int = 8,
    var gamesPerMatch: Int = 1
) {
    private val boosterGenerator = BoosterGenerator()

    /** Players indexed by player ID */
    val players = ConcurrentHashMap<EntityId, LobbyPlayerState>()

    /** The host player ID (first player to create the lobby) */
    @Volatile
    var hostPlayerId: EntityId? = null
        private set

    @Volatile
    var state: LobbyState = LobbyState.WAITING_FOR_PLAYERS
        private set

    /** Basic lands available for deck building */
    val basicLands: Map<String, CardDefinition> by lazy {
        boosterGenerator.getBasicLands(setCode)
    }

    val isFull: Boolean get() = players.size >= maxPlayers
    val playerCount: Int get() = players.size

    /**
     * Add a player to the lobby. First player becomes host.
     */
    fun addPlayer(identity: PlayerIdentity): EntityId {
        require(state == LobbyState.WAITING_FOR_PLAYERS) { "Lobby not accepting players" }
        require(!isFull) { "Lobby is full" }

        val playerId = identity.playerId
        players[playerId] = LobbyPlayerState(identity = identity)
        identity.currentLobbyId = lobbyId

        if (hostPlayerId == null) {
            hostPlayerId = playerId
        }

        return playerId
    }

    /**
     * Remove a player from the lobby.
     */
    fun removePlayer(playerId: EntityId) {
        val playerState = players.remove(playerId) ?: return
        playerState.identity.currentLobbyId = null

        // Transfer host if the host left
        if (hostPlayerId == playerId) {
            hostPlayerId = players.keys.firstOrNull()
        }
    }

    /**
     * Check if a player is the host.
     */
    fun isHost(playerId: EntityId): Boolean = hostPlayerId == playerId

    /**
     * Generate sealed pools for all players and transition to DECK_BUILDING.
     * Only the host can trigger this.
     */
    fun startDeckBuilding(requestingPlayerId: EntityId): Boolean {
        if (!isHost(requestingPlayerId)) return false
        if (state != LobbyState.WAITING_FOR_PLAYERS) return false
        if (players.size < 2) return false

        // Generate unique pools for each player
        players.forEach { (playerId, playerState) ->
            val pool = boosterGenerator.generateSealedPool(setCode, boosterCount)
            players[playerId] = playerState.copy(cardPool = pool)
        }

        state = LobbyState.DECK_BUILDING
        return true
    }

    /**
     * Submit a deck for a player.
     */
    fun submitDeck(playerId: EntityId, deckList: Map<String, Int>): DeckSubmissionResult {
        if (state != LobbyState.DECK_BUILDING) {
            return DeckSubmissionResult.Error("Not in deck building phase")
        }

        val playerState = players[playerId]
            ?: return DeckSubmissionResult.Error("Player not in lobby")

        if (playerState.hasSubmittedDeck) {
            return DeckSubmissionResult.Error("Deck already submitted")
        }

        // Validate deck
        val validationResult = validateDeck(playerState.cardPool, deckList)
        if (validationResult != null) {
            return DeckSubmissionResult.Error(validationResult)
        }

        players[playerId] = playerState.copy(submittedDeck = deckList)

        return DeckSubmissionResult.Success(allReady = allDecksSubmitted())
    }

    /**
     * Check if all players have submitted decks.
     */
    fun allDecksSubmitted(): Boolean = players.values.all { it.hasSubmittedDeck }

    /**
     * Transition to tournament active state.
     */
    fun startTournament() {
        require(state == LobbyState.DECK_BUILDING) { "Not in deck building phase" }
        require(allDecksSubmitted()) { "Not all decks submitted" }
        state = LobbyState.TOURNAMENT_ACTIVE
    }

    /**
     * Mark tournament as complete.
     */
    fun completeTournament() {
        state = LobbyState.TOURNAMENT_COMPLETE
    }

    /**
     * Get the submitted deck for a player.
     */
    fun getSubmittedDeck(playerId: EntityId): Map<String, Int>? {
        return players[playerId]?.submittedDeck
    }

    /**
     * Build a lobby update message for a specific player.
     */
    fun buildLobbyUpdate(forPlayerId: EntityId): ServerMessage.LobbyUpdate {
        val playerInfos = players.values.map { ps ->
            ServerMessage.LobbyPlayerInfo(
                playerId = ps.identity.playerId.value,
                playerName = ps.identity.playerName,
                isHost = isHost(ps.identity.playerId),
                isConnected = ps.identity.isConnected,
                deckSubmitted = ps.hasSubmittedDeck
            )
        }

        return ServerMessage.LobbyUpdate(
            lobbyId = lobbyId,
            state = state.name,
            players = playerInfos,
            settings = ServerMessage.LobbySettings(
                setCode = setCode,
                setName = setName,
                boosterCount = boosterCount,
                maxPlayers = maxPlayers,
                gamesPerMatch = gamesPerMatch
            ),
            isHost = isHost(forPlayerId)
        )
    }

    private fun validateDeck(pool: List<CardDefinition>, deckList: Map<String, Int>): String? {
        val totalCards = deckList.values.sum()
        if (totalCards < 40) {
            return "Deck must have at least 40 cards (has $totalCards)"
        }

        val poolCounts = pool
            .filterNot { it.typeLine.isBasicLand }
            .groupingBy { it.name }
            .eachCount()

        val basicLandNames = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

        for ((cardName, count) in deckList) {
            if (count <= 0) continue
            if (cardName in basicLandNames) continue

            val availableInPool = poolCounts[cardName] ?: 0
            if (availableInPool == 0) {
                return "Card not in pool: $cardName"
            }
            if (count > availableInPool) {
                return "Not enough copies of $cardName in pool (have $availableInPool, trying to use $count)"
            }
        }

        return null
    }

    sealed interface DeckSubmissionResult {
        data class Success(val allReady: Boolean) : DeckSubmissionResult
        data class Error(val message: String) : DeckSubmissionResult
    }
}
