package com.wingedsheep.gameserver.protocol

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.PrintingRef
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
    data class CreateGame(
        val deckList: Map<String, Int>,
        val vsAi: Boolean = false,
        val setCode: String? = null,
        /**
         * Optional ordered, per-card entries with pinned printings. When non-empty, this is
         * authoritative and [deckList] is ignored. When null/empty the legacy [deckList] path
         * is used (no per-card printing pinning).
         */
        val cardEntries: List<DeckEntryDTO>? = null,
    ) : ClientMessage

    /**
     * Join an existing game with a session ID and deck list.
     */
    @Serializable
    @SerialName("joinGame")
    data class JoinGame(
        val sessionId: String,
        val deckList: Map<String, Int>,
        /** Rich, optionally-pinned entries. See [CreateGame.cardEntries]. */
        val cardEntries: List<DeckEntryDTO>? = null,
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
     * Submit the built deck for a sealed game or a tournament lobby (Premade Decks format).
     * Deck list maps card name to count. The optional [commander] is honored only when the
     * tournament lobby's `deckFormat` is commander-shape (Commander / Brawl / Standard Brawl);
     * for other formats it's ignored. The commander card name MUST appear in [deckList] —
     * the validator and engine both rely on this invariant.
     */
    @Serializable
    @SerialName("submitSealedDeck")
    data class SubmitSealedDeck(
        val deckList: Map<String, Int>,
        val commander: String? = null,
        /** Rich, optionally-pinned entries. See [CreateGame.cardEntries]. */
        val cardEntries: List<DeckEntryDTO>? = null,
        /** Optional pinned printing for [commander]. Ignored when [commander] is null. */
        val commanderPrinting: PrintingRef? = null,
    ) : ClientMessage

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
        val pickTimeSeconds: Int = 45,     // Draft only
        val isPublic: Boolean = false,
        /** Master switch for in-app AI assistance (Suggest Pick / Auto-build). Defaults off. */
        val aiAssistEnabled: Boolean = false
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
     * Add an AI player to the current lobby (host only, sealed format only).
     */
    @Serializable
    @SerialName("addAiToLobby")
    data object AddAiToLobby : ClientMessage

    /**
     * Remove an AI player from the current lobby (host only, waiting state only).
     */
    @Serializable
    @SerialName("removeAiFromLobby")
    data class RemoveAiFromLobby(val playerId: String) : ClientMessage

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
        val format: String? = null,           // "SEALED" / "DRAFT" / "COMMANDER_DRAFT" / "COMMANDER_SEALED" / etc.
        val boosterCount: Int? = null,
        val boosterDistribution: Map<String, Int>? = null,  // Per-set booster counts
        val maxPlayers: Int? = null,
        val gamesPerMatch: Int? = null,
        val pickTimeSeconds: Int? = null,     // Draft only
        val picksPerRound: Int? = null,       // Draft only: 1 or 2
        val isPublic: Boolean? = null,
        /**
         * Deck-construction format restriction (Standard/Modern/Commander/...). String form of
         * [com.wingedsheep.sdk.core.DeckFormat]; "" or "NONE" clears the restriction.
         */
        val deckFormat: String? = null,
        /** Commander Draft/Sealed only — minimum deck size (default 60). */
        val deckSizeMin: Int? = null,
        /** Commander Draft/Sealed only — singleton toggle (default true = duplicates allowed). */
        val allowDuplicates: Boolean? = null,
        /** Commander Draft/Sealed only — "BRAWL" or "COMMANDER". */
        val commanderPreset: String? = null,
        /** Toggle Chaos boosters: each pack pulls from the union of selected sets. */
        val chaosBoosters: Boolean? = null,
        /**
         * Replace the host ban list — oracle card names excluded from generated boosters. The
         * full list is sent each time (not a delta); null leaves the current ban list unchanged.
         */
        val bannedCardNames: List<String>? = null,
        /** Master switch for in-app AI assistance (Suggest Pick / Auto-build). Null = unchanged. */
        val aiAssistEnabled: Boolean? = null,
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
     * Host adds extra rounds to a completed tournament.
     */
    @Serializable
    @SerialName("addExtraRound")
    data object AddExtraRound : ClientMessage

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
     * Update tentative attacker targets during declare attackers phase.
     * Sent in real-time as the attacking player assigns targets.
     */
    @Serializable
    @SerialName("updateAttackerTargets")
    data class UpdateAttackerTargets(
        /** Map of attacker creature ID to target entity ID (player or planeswalker) */
        val selectedAttackers: List<EntityId>,
        val attackerTargets: Map<EntityId, EntityId>
    ) : ClientMessage

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

    /**
     * Request a full state resync. Sent by the client when it detects it may have missed
     * messages (e.g., tab was backgrounded, or a gap in stateVersion sequence).
     */
    @Serializable
    @SerialName("requestResync")
    data object RequestResync : ClientMessage

    // =========================================================================
    // Quick Game Lobby Messages
    // =========================================================================

    /** Create a new quick-game lobby. If [vsAi] is true the server adds an AI opponent immediately. */
    @Serializable
    @SerialName("createQuickGameLobby")
    data class CreateQuickGameLobby(
        val vsAi: Boolean = false,
        val setCode: String? = null,
        val isPublic: Boolean = false,
        val format: com.wingedsheep.sdk.core.DeckFormat? = null
    ) : ClientMessage

    /** Join an existing quick-game lobby by its short code. */
    @Serializable
    @SerialName("joinQuickGameLobby")
    data class JoinQuickGameLobby(val lobbyId: String) : ClientMessage

    /** Leave the lobby; the host leaving closes it for everyone. */
    @Serializable
    @SerialName("leaveQuickGameLobby")
    data object LeaveQuickGameLobby : ClientMessage

    /**
     * Submit / replace this player's deck for the current lobby. An empty [deckList] means
     * "let the server pick a random sealed pool" (matches the current Quick Game default).
     */
    @Serializable
    @SerialName("submitQuickGameLobbyDeck")
    data class SubmitQuickGameLobbyDeck(
        val deckList: Map<String, Int>,
        /**
         * Optional commander card name for commander-shape lobby formats (Commander / Brawl /
         * Standard Brawl). Ignored for non-commander formats. The card name MUST appear in
         * [deckList] — the validator and engine both rely on this invariant.
         */
        val commander: String? = null,
        /** Rich, optionally-pinned entries. See [CreateGame.cardEntries]. */
        val cardEntries: List<DeckEntryDTO>? = null,
        /** Optional pinned printing for [commander]. Ignored when [commander] is null. */
        val commanderPrinting: PrintingRef? = null,
    ) : ClientMessage

    /** Toggle this player's ready flag. The server starts the game when both players are ready. */
    @Serializable
    @SerialName("setQuickGameLobbyReady")
    data class SetQuickGameLobbyReady(val ready: Boolean) : ClientMessage

    /**
     * Update the quick-game lobby's set code (used when a player has chosen the "Random" deck
     * tab — picks a sealed pool from this set). Pass null to mean "any set, server picks one".
     * Only the host (first non-AI player) is allowed to change this.
     */
    @Serializable
    @SerialName("setQuickGameLobbySetCode")
    data class SetQuickGameLobbySetCode(val setCode: String?) : ClientMessage

    /**
     * Toggle whether the quick-game lobby is publicly listed so other players can find it
     * without the invite code. Host-only; AI lobbies cannot be made public.
     */
    @Serializable
    @SerialName("setQuickGameLobbyPublic")
    data class SetQuickGameLobbyPublic(val isPublic: Boolean) : ClientMessage

    /**
     * Set the deck-format restriction for the lobby (host-only). Null = no restriction.
     * Re-validates every player's submitted deck and un-readies anyone who becomes invalid.
     */
    @Serializable
    @SerialName("setQuickGameLobbyFormat")
    data class SetQuickGameLobbyFormat(val format: com.wingedsheep.sdk.core.DeckFormat?) : ClientMessage
}
