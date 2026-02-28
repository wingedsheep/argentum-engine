package com.wingedsheep.gameserver.protocol

import com.wingedsheep.sdk.core.Step
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
     * Cancel a game that hasn't started yet (waiting for opponent).
     */
    @Serializable
    @SerialName("cancelGame")
    data object CancelGame : ClientMessage

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
     * Create a new sealed game with one or more sets.
     */
    @Serializable
    @SerialName("createSealedGame")
    data class CreateSealedGame(val setCodes: List<String>) : ClientMessage

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
    // Tournament Lobby Messages
    // =========================================================================

    /**
     * Create a new tournament lobby for up to 8 players.
     * Supports both SEALED and DRAFT formats.
     */
    @Serializable
    @SerialName("createTournamentLobby")
    data class CreateTournamentLobby(
        val setCodes: List<String>,
        val format: String = "SEALED",     // "SEALED" or "DRAFT"
        val boosterCount: Int = 6,         // Sealed: boosters in pool, Draft: packs per player
        val maxPlayers: Int = 8,
        val pickTimeSeconds: Int = 45      // Draft only
    ) : ClientMessage

    /**
     * Join an existing tournament lobby.
     */
    @Serializable
    @SerialName("joinLobby")
    data class JoinLobby(val lobbyId: String) : ClientMessage

    /**
     * Host starts the tournament lobby.
     * For Sealed: generates pools and begins deck building.
     * For Draft: distributes first packs and begins drafting.
     */
    @Serializable
    @SerialName("startTournamentLobby")
    data object StartTournamentLobby : ClientMessage

    /**
     * Make a pick during draft. Supports Pick 2 mode with multiple card names.
     */
    @Serializable
    @SerialName("makePick")
    data class MakePick(val cardNames: List<String>) : ClientMessage

    /**
     * Take the current pile during Winston Draft.
     */
    @Serializable
    @SerialName("winstonTakePile")
    data object WinstonTakePile : ClientMessage

    /**
     * Skip the current pile during Winston Draft.
     */
    @Serializable
    @SerialName("winstonSkipPile")
    data object WinstonSkipPile : ClientMessage

    /**
     * Leave the current lobby.
     */
    @Serializable
    @SerialName("leaveLobby")
    data object LeaveLobby : ClientMessage

    /**
     * Stop/disband the current lobby (host only).
     * All players will be removed and the lobby will be deleted.
     */
    @Serializable
    @SerialName("stopLobby")
    data object StopLobby : ClientMessage

    /**
     * Unsubmit a previously submitted deck to continue editing.
     * Only allowed while waiting for other players to submit.
     */
    @Serializable
    @SerialName("unsubmitDeck")
    data object UnsubmitDeck : ClientMessage

    /**
     * Update lobby settings (host only).
     */
    @Serializable
    @SerialName("updateLobbySettings")
    data class UpdateLobbySettings(
        val setCodes: List<String>? = null,
        val format: String? = null,           // "SEALED" or "DRAFT"
        val boosterCount: Int? = null,
        val maxPlayers: Int? = null,
        val gamesPerMatch: Int? = null,
        val pickTimeSeconds: Int? = null,     // Draft only
        val picksPerRound: Int? = null        // Draft only: 1 or 2
    ) : ClientMessage

    /**
     * Pick a row or column during grid draft.
     */
    @Serializable
    @SerialName("gridDraftPick")
    data class GridDraftPick(val selection: String) : ClientMessage

    // =========================================================================
    // Tournament Messages
    // =========================================================================

    /**
     * Player signals readiness for the next tournament round.
     */
    @Serializable
    @SerialName("readyForNextRound")
    data object ReadyForNextRound : ClientMessage

    /**
     * Add 1 minute to a disconnected tournament player's timer.
     */
    @Serializable
    @SerialName("addDisconnectTime")
    data class AddDisconnectTime(val playerId: String) : ClientMessage

    /**
     * Kick a disconnected tournament player (available after 2+ minutes of disconnect).
     * Immediately treats them as abandoned.
     */
    @Serializable
    @SerialName("kickPlayer")
    data class KickPlayer(val playerId: String) : ClientMessage

    /**
     * Start spectating a game in the current tournament.
     */
    @Serializable
    @SerialName("spectateGame")
    data class SpectateGame(val gameSessionId: String) : ClientMessage

    /**
     * Stop spectating and return to tournament overview.
     */
    @Serializable
    @SerialName("stopSpectating")
    data object StopSpectating : ClientMessage

    // =========================================================================
    // Combat UI Messages
    // =========================================================================

    /**
     * Update tentative blocker assignments during declare blockers phase.
     * Sent in real-time as the defending player assigns blockers.
     */
    @Serializable
    @SerialName("updateBlockerAssignments")
    data class UpdateBlockerAssignments(
        /** Map of blocker creature ID to attacker creature IDs */
        val assignments: Map<EntityId, List<EntityId>>
    ) : ClientMessage

    // =========================================================================
    // Game Settings Messages
    // =========================================================================

    /**
     * Toggle full control mode for the current game (backward compatibility).
     * When enabled, auto-pass is disabled and player receives priority at every possible point.
     */
    @Serializable
    @SerialName("setFullControl")
    data class SetFullControl(val enabled: Boolean) : ClientMessage

    /**
     * Set priority mode for the current game.
     * Values: "auto", "stops", "fullControl"
     */
    @Serializable
    @SerialName("setPriorityMode")
    data class SetPriorityMode(val mode: String) : ClientMessage

    /**
     * Set per-step stop overrides for the current game.
     * When a stop is set for a step, auto-pass will not skip that step (even if it normally would).
     */
    @Serializable
    @SerialName("setStopOverrides")
    data class SetStopOverrides(
        val myTurnStops: Set<Step>,
        val opponentTurnStops: Set<Step>
    ) : ClientMessage

    /**
     * Request to undo the last non-respondable action (e.g., play land, declare attackers).
     */
    @Serializable
    @SerialName("requestUndo")
    data object RequestUndo : ClientMessage
}
