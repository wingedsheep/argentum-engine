package com.wingedsheep.gameserver.protocol

import com.wingedsheep.rulesengine.ecs.action.GameAction
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
    data class Connect(val playerName: String) : ClientMessage

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
    data class SubmitAction(val action: GameAction) : ClientMessage

    /**
     * Concede the current game.
     */
    @Serializable
    @SerialName("concede")
    data object Concede : ClientMessage
}
