package com.wingedsheep.gameserver.protocol

import com.wingedsheep.gameserver.dto.ClientEvent
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages sent from server to client.
 */
@Serializable
sealed interface ServerMessage {
    /**
     * Connection confirmed with assigned player ID.
     */
    @Serializable
    @SerialName("connected")
    data class Connected(val playerId: String, val token: String) : ServerMessage

    /**
     * Reconnection confirmed — client's previous session has been restored.
     */
    @Serializable
    @SerialName("reconnected")
    data class Reconnected(
        val playerId: String,
        val token: String,
        /** Current context: "lobby", "deckBuilding", "game", "tournament", or null */
        val context: String? = null,
        /** Session/lobby ID the player is currently in */
        val contextId: String? = null
    ) : ServerMessage

    /**
     * Game created successfully, waiting for opponent.
     */
    @Serializable
    @SerialName("gameCreated")
    data class GameCreated(val sessionId: String) : ServerMessage

    /**
     * Game is starting with both players connected.
     */
    @Serializable
    @SerialName("gameStarted")
    data class GameStarted(val opponentName: String) : ServerMessage

    /**
     * Game was cancelled before it started (by the creator).
     */
    @Serializable
    @SerialName("gameCancelled")
    data object GameCancelled : ServerMessage

    /**
     * Game state update after an action is executed.
     */
    @Serializable
    @SerialName("stateUpdate")
    data class StateUpdate(
        val state: ClientGameState,
        val events: List<ClientEvent>,
        val legalActions: List<LegalActionInfo>,
        /** Pending decision that requires player input (e.g., discard to hand size) */
        val pendingDecision: PendingDecision? = null
    ) : ServerMessage

    /**
     * Error response from the server.
     */
    @Serializable
    @SerialName("error")
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : ServerMessage

    /**
     * Game has ended.
     */
    @Serializable
    @SerialName("gameOver")
    data class GameOver(
        val winnerId: EntityId?,
        val reason: GameOverReason
    ) : ServerMessage

    /**
     * Basic card info for display in mulligan UI.
     */
    @Serializable
    data class MulliganCardInfo(
        val name: String,
        val imageUri: String? = null
    )

    /**
     * Mulligan decision required. Sent when player must choose to keep or mulligan.
     */
    @Serializable
    @SerialName("mulliganDecision")
    data class MulliganDecision(
        /** The player's current hand card IDs */
        val hand: List<EntityId>,
        /** How many times this player has mulliganed (0 = first look at opening hand) */
        val mulliganCount: Int,
        /** If keeping, how many cards must be put on bottom */
        val cardsToPutOnBottom: Int,
        /** Card info for display (entity ID -> card info) */
        val cards: Map<EntityId, MulliganCardInfo> = emptyMap()
    ) : ServerMessage

    /**
     * Player must choose cards to put on bottom of library after keeping a mulliganed hand.
     */
    @Serializable
    @SerialName("chooseBottomCards")
    data class ChooseBottomCards(
        /** The player's current hand card IDs */
        val hand: List<EntityId>,
        /** How many cards must be put on bottom */
        val cardsToPutOnBottom: Int
    ) : ServerMessage

    /**
     * Mulligan phase is complete and the game is starting.
     */
    @Serializable
    @SerialName("mulliganComplete")
    data class MulliganComplete(
        /** Player's final hand size after putting cards on bottom */
        val finalHandSize: Int
    ) : ServerMessage

    /**
     * Sent to a player when they have completed their mulligan but the opponent hasn't yet.
     */
    @Serializable
    @SerialName("waitingForOpponentMulligan")
    data object WaitingForOpponentMulligan : ServerMessage

    // =========================================================================
    // Sealed Draft Messages
    // =========================================================================

    /**
     * Card information for sealed deck building UI.
     */
    @Serializable
    data class SealedCardInfo(
        val name: String,
        val manaCost: String?,
        val typeLine: String,
        val rarity: String,
        val imageUri: String?,
        val power: Int? = null,
        val toughness: Int? = null,
        val oracleText: String? = null
    )

    /**
     * Sealed game created successfully, waiting for opponent.
     */
    @Serializable
    @SerialName("sealedGameCreated")
    data class SealedGameCreated(
        val sessionId: String,
        val setCode: String,
        val setName: String
    ) : ServerMessage

    /**
     * Sealed pool has been generated for the player.
     * Sent when both players have joined and pools are ready.
     */
    @Serializable
    @SerialName("sealedPoolGenerated")
    data class SealedPoolGenerated(
        /** Set code (e.g., "POR") */
        val setCode: String,
        /** Set name (e.g., "Portal") */
        val setName: String,
        /** 90 cards from 6 boosters */
        val cardPool: List<SealedCardInfo>,
        /** 5 basic land types available for deck building */
        val basicLands: List<SealedCardInfo>
    ) : ServerMessage

    /**
     * Opponent has submitted their sealed deck.
     */
    @Serializable
    @SerialName("opponentDeckSubmitted")
    data object OpponentDeckSubmitted : ServerMessage

    /**
     * Waiting for opponent to submit their deck.
     */
    @Serializable
    @SerialName("waitingForOpponent")
    data object WaitingForOpponent : ServerMessage

    /**
     * Deck submission was successful.
     */
    @Serializable
    @SerialName("deckSubmitted")
    data class DeckSubmitted(val deckSize: Int) : ServerMessage

    // =========================================================================
    // Sealed Lobby Messages
    // =========================================================================

    /**
     * Info about a player in a lobby.
     */
    @Serializable
    data class LobbyPlayerInfo(
        val playerId: String,
        val playerName: String,
        val isHost: Boolean,
        val isConnected: Boolean,
        val deckSubmitted: Boolean = false
    )

    /**
     * Lobby settings.
     */
    @Serializable
    data class LobbySettings(
        val setCode: String,
        val setName: String,
        val boosterCount: Int,
        val maxPlayers: Int,
        val gamesPerMatch: Int = 1
    )

    /**
     * Sealed lobby created successfully.
     */
    @Serializable
    @SerialName("lobbyCreated")
    data class LobbyCreated(val lobbyId: String) : ServerMessage

    /**
     * Lobby state update — sent whenever lobby state changes.
     */
    @Serializable
    @SerialName("lobbyUpdate")
    data class LobbyUpdate(
        val lobbyId: String,
        val state: String,
        val players: List<LobbyPlayerInfo>,
        val settings: LobbySettings,
        val isHost: Boolean
    ) : ServerMessage

    // =========================================================================
    // Tournament Messages
    // =========================================================================

    /**
     * Player standing in the tournament.
     */
    @Serializable
    data class PlayerStandingInfo(
        val playerId: String,
        val playerName: String,
        val wins: Int,
        val losses: Int,
        val draws: Int,
        val points: Int,
        val isConnected: Boolean = true
    )

    /**
     * Match result info.
     */
    @Serializable
    data class MatchResultInfo(
        val player1Name: String,
        val player2Name: String,
        val winnerId: String?,
        val isDraw: Boolean = false,
        val isBye: Boolean = false
    )

    /**
     * Tournament has started.
     */
    @Serializable
    @SerialName("tournamentStarted")
    data class TournamentStarted(
        val lobbyId: String,
        val totalRounds: Int,
        val standings: List<PlayerStandingInfo>
    ) : ServerMessage

    /**
     * A tournament match is starting for this player.
     */
    @Serializable
    @SerialName("tournamentMatchStarting")
    data class TournamentMatchStarting(
        val lobbyId: String,
        val round: Int,
        val gameSessionId: String,
        val opponentName: String
    ) : ServerMessage

    /**
     * A tournament match is a bye for this player.
     */
    @Serializable
    @SerialName("tournamentBye")
    data class TournamentBye(
        val lobbyId: String,
        val round: Int
    ) : ServerMessage

    /**
     * A tournament round has completed.
     */
    @Serializable
    @SerialName("roundComplete")
    data class RoundComplete(
        val lobbyId: String,
        val round: Int,
        val results: List<MatchResultInfo>,
        val standings: List<PlayerStandingInfo>
    ) : ServerMessage

    /**
     * Tournament is complete with final standings.
     */
    @Serializable
    @SerialName("tournamentComplete")
    data class TournamentComplete(
        val lobbyId: String,
        val finalStandings: List<PlayerStandingInfo>
    ) : ServerMessage
}

/**
 * Information about a single target requirement for legal actions.
 * Includes valid targets so the client knows which entities can be selected.
 */
@Serializable
data class LegalActionTargetInfo(
    val index: Int,
    val description: String,
    val minTargets: Int,
    val maxTargets: Int,
    val validTargets: List<EntityId>
)

/**
 * Information about a legal action the player can take.
 */
@Serializable
data class LegalActionInfo(
    val actionType: String,
    val description: String,
    val action: GameAction,
    /** Valid target IDs if this action requires targeting */
    val validTargets: List<EntityId>? = null,
    /** Whether this action requires selecting targets before submission */
    val requiresTargets: Boolean = false,
    /** Maximum number of targets (default 1) */
    val targetCount: Int = 1,
    /** Minimum number of targets required (default = targetCount) */
    val minTargets: Int = targetCount,
    /** Description of the target requirement */
    val targetDescription: String? = null,
    /** Multiple target requirements for spells with multiple distinct targets */
    val targetRequirements: List<LegalActionTargetInfo>? = null,
    /** Valid attacker IDs for DeclareAttackers action */
    val validAttackers: List<EntityId>? = null,
    /** Valid blocker IDs for DeclareBlockers action */
    val validBlockers: List<EntityId>? = null,
    /** Whether this spell has X in its mana cost */
    val hasXCost: Boolean = false,
    /** Maximum X value the player can afford (null if not X cost spell) */
    val maxAffordableX: Int? = null,
    /** Minimum X value (usually 0) */
    val minX: Int = 0,
    /** Whether this is a mana ability (doesn't highlight card as playable) */
    val isManaAbility: Boolean = false,
    /** Additional cost info - sacrifice targets, etc. */
    val additionalCostInfo: AdditionalCostInfo? = null
)

/**
 * Information about additional costs for a spell.
 */
@Serializable
data class AdditionalCostInfo(
    /** Description of the additional cost */
    val description: String,
    /** Type of additional cost */
    val costType: String,
    /** Valid targets for sacrifice costs */
    val validSacrificeTargets: List<EntityId> = emptyList(),
    /** Number of permanents to sacrifice */
    val sacrificeCount: Int = 1
)

/**
 * Error codes for server error responses.
 */
@Serializable
enum class ErrorCode {
    NOT_CONNECTED,
    ALREADY_CONNECTED,
    GAME_NOT_FOUND,
    GAME_FULL,
    NOT_YOUR_TURN,
    INVALID_ACTION,
    INVALID_DECK,
    INTERNAL_ERROR
}

/**
 * Reasons why a game ended.
 */
@Serializable
enum class GameOverReason {
    LIFE_ZERO,
    DECK_OUT,
    CONCESSION,
    POISON_COUNTERS,
    DISCONNECTION
}
