package com.wingedsheep.gameserver.protocol

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.GameAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages sent from client to server.
 */
@Serializable
sealed interface ClientMessage {
    /**
     * Connect to the server with a player name.
     */
    @Serializable
    @SerialName("connect")
    data class Connect(val playerName: String, val token: String? = null) : ClientMessage

    /**
     * Create a new game with a deck list.
     * Deck list maps card name to count.
     */
    @Serializable
    @SerialName("createGame")
    data class CreateGame(val deckList: Map<String, Int>) : ClientMessage

    /**
     * Join an existing game with a session ID and deck list.
     */
    @Serializable
    @SerialName("joinGame")
    data class JoinGame(
        val sessionId: String,
        val deckList: Map<String, Int>
    ) : ClientMessage

    /**
     * Submit a game action for execution.
     */
    @Serializable
    @SerialName("submitAction")
    data class SubmitAction(val action: GameAction, val messageId: String? = null) : ClientMessage

    /**
     * Concede the current game.
     */
    @Serializable
    @SerialName("concede")
    data object Concede : ClientMessage

    /**
     * Keep the current opening hand (end mulligan phase for this player).
     */
    @Serializable
    @SerialName("keepHand")
    data object KeepHand : ClientMessage

    /**
     * Mulligan: shuffle hand back and draw a new hand.
     */
    @Serializable
    @SerialName("mulligan")
    data object Mulligan : ClientMessage

    /**
     * Choose which cards to put on the bottom of the library after keeping a mulliganed hand.
     * The number of cards must equal the mulligan count (number of mulligans taken).
     */
    @Serializable
    @SerialName("chooseBottomCards")
    data class ChooseBottomCards(val cardIds: List<EntityId>) : ClientMessage

    // =========================================================================
    // Sealed Draft Messages
    // =========================================================================

    /**
     * Create a new sealed game with a specific set.
     */
    @Serializable
    @SerialName("createSealedGame")
    data class CreateSealedGame(val setCode: String) : ClientMessage

    /**
     * Join an existing sealed game session.
     */
    @Serializable
    @SerialName("joinSealedGame")
    data class JoinSealedGame(val sessionId: String) : ClientMessage

    /**
     * Submit the built deck for a sealed game.
     * Deck list maps card name to count.
     */
    @Serializable
    @SerialName("submitSealedDeck")
    data class SubmitSealedDeck(val deckList: Map<String, Int>) : ClientMessage

    // =========================================================================
    // Sealed Lobby Messages
    // =========================================================================

    /**
     * Create a new sealed lobby for up to 8 players.
     */
    @Serializable
    @SerialName("createSealedLobby")
    data class CreateSealedLobby(
        val setCode: String,
        val boosterCount: Int = 6,
        val maxPlayers: Int = 8
    ) : ClientMessage

    /**
     * Join an existing sealed lobby.
     */
    @Serializable
    @SerialName("joinLobby")
    data class JoinLobby(val lobbyId: String) : ClientMessage

    /**
     * Host starts the sealed lobby (generates pools, begins deck building).
     */
    @Serializable
    @SerialName("startSealedLobby")
    data object StartSealedLobby : ClientMessage

    /**
     * Leave the current lobby.
     */
    @Serializable
    @SerialName("leaveLobby")
    data object LeaveLobby : ClientMessage

    /**
     * Update lobby settings (host only).
     */
    @Serializable
    @SerialName("updateLobbySettings")
    data class UpdateLobbySettings(val boosterCount: Int? = null, val maxPlayers: Int? = null, val gamesPerMatch: Int? = null) : ClientMessage

    // =========================================================================
    // Tournament Messages
    // =========================================================================

    /**
     * Player signals readiness for the next tournament round.
     */
    @Serializable
    @SerialName("readyForNextRound")
    data object ReadyForNextRound : ClientMessage
}
