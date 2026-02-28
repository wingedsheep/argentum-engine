package com.wingedsheep.gameserver.lobby

import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.sealed.BoosterGenerator
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job

/**
 * Tournament format.
 */
enum class TournamentFormat {
    SEALED,
    DRAFT,
    WINSTON_DRAFT,
    GRID_DRAFT
}

/**
 * Grid draft row/column selection.
 */
enum class GridSelection {
    ROW_0, ROW_1, ROW_2, COL_0, COL_1, COL_2;

    /**
     * Get the indices into the 3x3 grid (row-major order) for this selection.
     */
    fun getIndices(): List<Int> = when (this) {
        ROW_0 -> listOf(0, 1, 2)
        ROW_1 -> listOf(3, 4, 5)
        ROW_2 -> listOf(6, 7, 8)
        COL_0 -> listOf(0, 3, 6)
        COL_1 -> listOf(1, 4, 7)
        COL_2 -> listOf(2, 5, 8)
    }
}

/**
 * Result of making a grid draft pick.
 */
sealed interface GridDraftResult {
    data class PickMade(val cards: List<CardDefinition>, val lastAction: String) : GridDraftResult
    data class GridComplete(val lastAction: String) : GridDraftResult
    data class DraftComplete(val lastAction: String) : GridDraftResult
    data class Error(val message: String) : GridDraftResult
}

/**
 * State machine for a tournament lobby.
 */
enum class LobbyState {
    WAITING_FOR_PLAYERS,
    DRAFTING,           // Draft only - picking cards
    DECK_BUILDING,
    TOURNAMENT_ACTIVE,
    TOURNAMENT_COMPLETE
}

/**
 * Direction to pass packs during draft.
 */
enum class PassDirection {
    LEFT,   // Pack 1 and 3
    RIGHT   // Pack 2
}

/**
 * Per-player state within a lobby.
 */
data class LobbyPlayerState(
    val identity: PlayerIdentity,
    /** For sealed: full pool. For draft: cards picked so far. */
    val cardPool: List<CardDefinition> = emptyList(),
    /** Draft only: current pack to pick from. */
    var currentPack: List<CardDefinition>? = null,
    /** Draft only: whether player has picked from current pack. */
    var hasPicked: Boolean = false,
    var submittedDeck: Map<String, Int>? = null
) {
    val hasSubmittedDeck: Boolean get() = submittedDeck != null
}

/**
 * Result of making a pick during draft.
 */
sealed interface PickResult {
    data class Success(
        val pickedCards: List<CardDefinition>,
        val totalPicked: Int,
        val waitingForPlayers: List<String>
    ) : PickResult

    data class Error(val message: String) : PickResult
}

/**
 * Result of taking a pile or skipping during Winston Draft.
 */
sealed interface WinstonActionResult {
    data class PileTaken(
        val cards: List<CardDefinition>,
        val lastAction: String
    ) : WinstonActionResult

    data class PileSkipped(
        val nextPileIndex: Int?,   // null if forced blind pick
        val lastAction: String
    ) : WinstonActionResult

    data class BlindPick(
        val card: CardDefinition,
        val lastAction: String
    ) : WinstonActionResult

    data class DraftComplete(
        val lastAction: String
    ) : WinstonActionResult

    data class Error(val message: String) : WinstonActionResult
}

/**
 * Multi-player tournament lobby supporting up to 8 players with host controls.
 * Supports Sealed, Draft, and Winston Draft formats.
 *
 * State machine:
 * WAITING_FOR_PLAYERS → [Sealed] DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
 * WAITING_FOR_PLAYERS → [Draft]  DRAFTING → DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
 */
class TournamentLobby(
    val lobbyId: String = UUID.randomUUID().toString(),
    var setCodes: List<String>,
    var setNames: List<String>,
    private val boosterGenerator: BoosterGenerator,
    var format: TournamentFormat = TournamentFormat.SEALED,
    var boosterCount: Int = 6,        // Sealed: boosters in pool, Draft: packs per player (usually 3)
    var maxPlayers: Int = 8,
    var pickTimeSeconds: Int = 45,    // Draft only
    var picksPerRound: Int = 1,       // Draft only: cards to pick each round (1 or 2)
    var gamesPerMatch: Int = 1
) {

    /**
     * Update the sets for this lobby. Can only be changed while waiting for players.
     * Returns true if all sets were valid and changed, false otherwise.
     * Note: Empty list is NOT allowed via this method - handled by LobbyHandler.
     */
    fun updateSets(newSetCodes: List<String>): Boolean {
        if (state != LobbyState.WAITING_FOR_PLAYERS) return false
        if (newSetCodes.isEmpty()) return false

        // Validate all set codes first
        val configs = newSetCodes.map { code ->
            boosterGenerator.getSetConfig(code) ?: return false
        }

        setCodes = configs.map { it.setCode }
        setNames = configs.map { it.setName }
        return true
    }

    /** Players indexed by player ID */
    val players = ConcurrentHashMap<EntityId, LobbyPlayerState>()

    /** Tournament-level spectators (non-participants watching standings/matches) */
    val spectators = ConcurrentHashMap<EntityId, PlayerIdentity>()

    /** The host player ID (first player to create the lobby) */
    @Volatile
    var hostPlayerId: EntityId? = null
        private set

    @Volatile
    var state: LobbyState = LobbyState.WAITING_FOR_PLAYERS
        private set

    /** Basic lands available for deck building */
    val basicLands: Map<String, CardDefinition> by lazy {
        boosterGenerator.getBasicLands(setCodes)
    }

    /** Players who are ready for the next round */
    private val playersReadyForNextRound = mutableSetOf<EntityId>()

    /** Epoch counter incremented when ready state is cleared (prevents stale ready requests) */
    @Volatile
    var readyEpoch: Long = 0
        private set

    // =========================================================================
    // Draft-specific State
    // =========================================================================

    /** Current pack number (1, 2, or 3) */
    @Volatile
    var currentPackNumber: Int = 0
        private set

    /** Current pick number within the pack (1-15) */
    @Volatile
    var currentPickNumber: Int = 0
        private set

    /** Player order for pack passing (circular) */
    private var playerOrder: List<EntityId> = emptyList()

    /** Timer job for pick timeout */
    @Volatile
    var pickTimerJob: Job? = null

    /** Seconds remaining on current pick timer */
    @Volatile
    var pickTimeRemaining: Int = 0

    // =========================================================================
    // Winston Draft-specific State
    // =========================================================================

    /** The shared face-down main deck for Winston Draft */
    val winstonMainDeck: MutableList<CardDefinition> = mutableListOf()

    /** Three face-down piles for Winston Draft */
    val winstonPiles: Array<MutableList<CardDefinition>> = arrayOf(mutableListOf(), mutableListOf(), mutableListOf())

    /** Index of the active player in playerOrder (0 or 1) */
    @Volatile
    var winstonActivePlayerIndex: Int = 0

    /** Which pile (0-2) the active player is currently examining */
    @Volatile
    var winstonCurrentPileIndex: Int = 0

    /** Card names each player has seen (examined in piles but possibly skipped). playerId -> set of card names */
    val winstonSeenCards: MutableMap<EntityId, MutableSet<String>> = mutableMapOf()

    // =========================================================================
    // Grid Draft-specific State
    // =========================================================================

    /** Shared card pool for grid draft */
    val gridMainDeck: MutableList<CardDefinition> = mutableListOf()

    /** 3x3 grid, null = empty slot (9 elements, row-major order) */
    var gridCards: Array<CardDefinition?> = arrayOfNulls(9)
        private set

    /** Index into gridPlayerOrder for current active picker */
    @Volatile
    var gridActivePlayerIndex: Int = 0
        private set

    /** Player order for grid draft (rotates each grid) */
    var gridPlayerOrder: List<EntityId> = emptyList()
        private set

    /** How many picks have been made on the current grid */
    @Volatile
    var gridPicksThisGrid: Int = 0
        private set

    /** Which grid number we're on (1-based, for display) */
    @Volatile
    var gridNumber: Int = 0
        private set

    val isFull: Boolean get() = players.size >= maxPlayers
    val playerCount: Int get() = players.size

    /**
     * Get the current pack pass direction based on pack number.
     */
    fun getPassDirection(): PassDirection {
        return when (currentPackNumber) {
            1, 3 -> PassDirection.LEFT
            2 -> PassDirection.RIGHT
            else -> PassDirection.LEFT
        }
    }

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
     * Remove a player from the lobby (for disconnection).
     * During a tournament, the player state is kept so they can rejoin later.
     */
    fun removePlayer(playerId: EntityId) {
        val playerState = players[playerId] ?: return
        playerState.identity.currentLobbyId = null

        // Only fully remove player during WAITING_FOR_PLAYERS state
        // During tournament phases, keep the state so they can rejoin
        if (state == LobbyState.WAITING_FOR_PLAYERS) {
            players.remove(playerId)
            // Transfer host if the host left
            if (hostPlayerId == playerId) {
                hostPlayerId = players.keys.firstOrNull()
            }
        }
        // During tournament, the player state remains in `players` but their identity
        // is disconnected - they can rejoin using the lobby code
    }

    /**
     * Forcefully remove a player from the lobby (for explicit "Leave Tournament" action).
     * This permanently removes the player and they cannot rejoin.
     */
    fun forceRemovePlayer(playerId: EntityId) {
        val playerState = players.remove(playerId) ?: return
        playerState.identity.currentLobbyId = null

        // Transfer host if the host left
        if (hostPlayerId == playerId) {
            hostPlayerId = players.keys.firstOrNull()
        }
    }

    /**
     * Check if a player was in this tournament (can rejoin).
     */
    fun wasPlayerInTournament(playerId: EntityId): Boolean {
        return players.containsKey(playerId)
    }

    /**
     * Rejoin a player who was previously in this tournament.
     * Updates their identity to the new one while preserving their card pool and deck.
     */
    fun rejoinPlayer(identity: PlayerIdentity): Boolean {
        val existingState = players[identity.playerId] ?: return false

        // Update the player state with new identity but keep card pool and deck
        players[identity.playerId] = existingState.copy(
            identity = identity
        )
        identity.currentLobbyId = lobbyId
        return true
    }

    /**
     * Check if a player is the host.
     */
    fun isHost(playerId: EntityId): Boolean = hostPlayerId == playerId

    /**
     * Generate sealed pools for all players and transition to DECK_BUILDING.
     * Only the host can trigger this. Only valid for SEALED format.
     */
    fun startDeckBuilding(requestingPlayerId: EntityId): Boolean {
        if (!isHost(requestingPlayerId)) return false
        if (state != LobbyState.WAITING_FOR_PLAYERS) return false
        if (players.size < 2) return false
        if (format != TournamentFormat.SEALED) return false

        // Generate a shared distribution seed so all players get the same
        // set distribution (e.g., all get 3 Portal + 2 Onslaught boosters)
        val distributionSeed = System.currentTimeMillis()

        // Generate unique pools for each player (card contents differ, but set distribution is the same)
        players.forEach { (playerId, playerState) ->
            val pool = boosterGenerator.generateSealedPool(setCodes, boosterCount, distributionSeed)
            players[playerId] = playerState.copy(cardPool = pool)
        }

        state = LobbyState.DECK_BUILDING
        return true
    }

    /**
     * Start the draft phase. Only the host can trigger this.
     * Generates initial packs and distributes them to players.
     */
    fun startDraft(requestingPlayerId: EntityId): Boolean {
        if (!isHost(requestingPlayerId)) return false
        if (state != LobbyState.WAITING_FOR_PLAYERS) return false
        if (players.size < 2) return false
        if (format != TournamentFormat.DRAFT) return false

        // Set up player order (for pack passing)
        playerOrder = players.keys.toList().shuffled()

        // Initialize draft state
        currentPackNumber = 1
        currentPickNumber = 1

        // Generate and distribute first packs
        distributeNewPacks()

        state = LobbyState.DRAFTING
        return true
    }

    /**
     * Generate new packs for all players (called at start of each pack).
     */
    private fun distributeNewPacks() {
        players.forEach { (playerId, playerState) ->
            val newPack = boosterGenerator.generateBooster(setCodes)
            playerState.currentPack = newPack
            playerState.hasPicked = false
        }
    }

    /**
     * Make a pick during draft. Supports picking multiple cards (Pick 2 mode).
     */
    fun makePick(playerId: EntityId, cardNames: List<String>): PickResult {
        if (state != LobbyState.DRAFTING) {
            return PickResult.Error("Not in drafting phase")
        }

        val playerState = players[playerId]
            ?: return PickResult.Error("Player not in lobby")

        if (playerState.hasPicked) {
            return PickResult.Error("Already picked from current pack")
        }

        val currentPack = playerState.currentPack
            ?: return PickResult.Error("No pack available")

        // Validate we have the right number of picks
        val requiredPicks = minOf(picksPerRound, currentPack.size)
        if (cardNames.size != requiredPicks) {
            return PickResult.Error("Must pick exactly $requiredPicks card(s), got ${cardNames.size}")
        }

        // Validate all cards are in the pack
        val pickedCards = mutableListOf<CardDefinition>()
        for (cardName in cardNames) {
            val card = currentPack.find { it.name == cardName }
                ?: return PickResult.Error("Card not in pack: $cardName")
            if (pickedCards.any { it.name == cardName }) {
                return PickResult.Error("Cannot pick the same card twice: $cardName")
            }
            pickedCards.add(card)
        }

        // Pick the cards
        val newPool = playerState.cardPool + pickedCards
        val newPack = currentPack.filter { card -> cardNames.none { it == card.name } }

        // Update player state
        players[playerId] = playerState.copy(
            cardPool = newPool,
            currentPack = newPack
        )
        players[playerId]?.hasPicked = true

        // Check who is still waiting to pick
        val waitingPlayers = players.values
            .filter { !it.hasPicked }
            .map { it.identity.playerName }

        return PickResult.Success(
            pickedCards = pickedCards,
            totalPicked = newPool.size,
            waitingForPlayers = waitingPlayers
        )
    }

    /**
     * Check if all players have made their pick for the current round.
     */
    fun allPlayersPicked(): Boolean {
        return players.values.all { it.hasPicked }
    }

    /**
     * Pass packs to the next player and advance the pick.
     * Should be called after all players have picked.
     *
     * @return true if draft continues, false if pack is exhausted (need new pack or draft complete)
     */
    fun passPacks(): Boolean {
        if (!allPlayersPicked()) return false

        // Get remaining packs (after picks)
        val packsByPlayer = players.mapValues { (_, state) ->
            state.currentPack ?: emptyList()
        }

        // Check if packs are exhausted
        val anyCardsRemaining = packsByPlayer.values.any { it.isNotEmpty() }
        if (!anyCardsRemaining) {
            // Pack exhausted, check if we need another pack
            if (currentPackNumber < boosterCount) {
                // Start next pack
                currentPackNumber++
                currentPickNumber = 1
                distributeNewPacks()
                return true
            } else {
                // Draft complete, transition to deck building
                finishDraft()
                return false
            }
        }

        // Pass packs in the appropriate direction
        val direction = getPassDirection()
        val newPackAssignments = mutableMapOf<EntityId, List<CardDefinition>>()

        for (i in playerOrder.indices) {
            val currentPlayerId = playerOrder[i]
            val nextIndex = when (direction) {
                PassDirection.LEFT -> (i + 1) % playerOrder.size
                PassDirection.RIGHT -> (i - 1 + playerOrder.size) % playerOrder.size
            }
            val nextPlayerId = playerOrder[nextIndex]
            newPackAssignments[nextPlayerId] = packsByPlayer[currentPlayerId] ?: emptyList()
        }

        // Apply the new pack assignments
        newPackAssignments.forEach { (playerId, pack) ->
            players[playerId]?.currentPack = pack
            players[playerId]?.hasPicked = false
        }

        currentPickNumber += picksPerRound
        return true
    }

    /**
     * Finish the draft and transition to deck building.
     */
    private fun finishDraft() {
        // Clear pack state
        players.forEach { (_, state) ->
            state.currentPack = null
            state.hasPicked = false
        }

        state = LobbyState.DECK_BUILDING
    }

    /**
     * Auto-pick the first card(s) in a player's pack (used for timeout).
     * Picks up to picksPerRound cards.
     */
    fun autoPickFirstCards(playerId: EntityId): PickResult {
        val playerState = players[playerId]
            ?: return PickResult.Error("Player not found")

        val pack = playerState.currentPack
            ?: return PickResult.Error("No pack available")

        val cardsToPick = pack.take(minOf(picksPerRound, pack.size)).map { it.name }
        if (cardsToPick.isEmpty()) {
            return PickResult.Error("No cards available to pick")
        }

        return makePick(playerId, cardsToPick)
    }

    /**
     * Get the list of player IDs who haven't picked yet.
     */
    fun getPlayersWaitingToPick(): List<EntityId> {
        return players.filterValues { !it.hasPicked }.keys.toList()
    }

    // =========================================================================
    // Winston Draft Methods
    // =========================================================================

    /**
     * Start Winston Draft. Shuffles 6 boosters into a single pile,
     * creates 3 face-down piles of 1 card each.
     */
    fun startWinstonDraft(requestingPlayerId: EntityId): Boolean {
        if (!isHost(requestingPlayerId)) return false
        if (state != LobbyState.WAITING_FOR_PLAYERS) return false
        if (players.size != 2) return false
        if (format != TournamentFormat.WINSTON_DRAFT) return false

        // Set up player order (randomize who goes first)
        playerOrder = players.keys.toList().shuffled()

        // Generate boosters and shuffle into main deck
        val allCards = mutableListOf<CardDefinition>()
        repeat(boosterCount) {
            allCards.addAll(boosterGenerator.generateBooster(setCodes))
        }
        allCards.shuffle()

        winstonMainDeck.clear()
        winstonMainDeck.addAll(allCards)

        // Deal 1 card to each of 3 piles
        for (i in 0 until 3) {
            winstonPiles[i].clear()
            if (winstonMainDeck.isNotEmpty()) {
                winstonPiles[i].add(winstonMainDeck.removeFirst())
            }
        }

        winstonActivePlayerIndex = 0
        winstonCurrentPileIndex = 0

        state = LobbyState.DRAFTING
        return true
    }

    // =========================================================================
    // Grid Draft Methods
    // =========================================================================

    /**
     * Start the grid draft phase. Only the host can trigger this.
     * Generates a shared pool (6 boosters shuffled together) and deals the first grid.
     */
    fun startGridDraft(requestingPlayerId: EntityId): Boolean {
        if (!isHost(requestingPlayerId)) return false
        if (state != LobbyState.WAITING_FOR_PLAYERS) return false
        if (players.size < 2 || players.size > 3) return false
        if (format != TournamentFormat.GRID_DRAFT) return false

        // Generate pool: 6 boosters shuffled together
        val pool = mutableListOf<CardDefinition>()
        repeat(boosterCount) {
            pool.addAll(boosterGenerator.generateBooster(setCodes))
        }
        pool.shuffle()
        gridMainDeck.clear()
        gridMainDeck.addAll(pool)

        // Shuffle player order
        gridPlayerOrder = players.keys.toList().shuffled()
        gridActivePlayerIndex = 0
        gridPicksThisGrid = 0
        gridNumber = 0

        // Deal first grid
        dealGrid()

        state = LobbyState.DRAFTING
        return true
    }

    /**
     * Get the active player's ID for Winston Draft.
     */
    fun getWinstonActivePlayerId(): EntityId? {
        if (playerOrder.isEmpty()) return null
        return playerOrder[winstonActivePlayerIndex]
    }

    /**
     * Look at the current pile. Returns pile contents for the active player.
     */
    fun winstonLookAtPile(playerId: EntityId): List<CardDefinition>? {
        if (state != LobbyState.DRAFTING) return null
        if (format != TournamentFormat.WINSTON_DRAFT) return null
        if (getWinstonActivePlayerId() != playerId) return null

        val pile = winstonPiles[winstonCurrentPileIndex]
        return pile.toList()
    }

    /**
     * Take the current pile. Adds pile cards to player's pool,
     * replaces pile with 1 card from main deck if available.
     */
    fun winstonTakePile(playerId: EntityId): WinstonActionResult {
        if (state != LobbyState.DRAFTING) return WinstonActionResult.Error("Not in drafting phase")
        if (format != TournamentFormat.WINSTON_DRAFT) return WinstonActionResult.Error("Not Winston Draft")
        if (getWinstonActivePlayerId() != playerId) return WinstonActionResult.Error("Not your turn")

        val pile = winstonPiles[winstonCurrentPileIndex]
        if (pile.isEmpty()) return WinstonActionResult.Error("Pile is empty")

        val takenCards = pile.toList()
        val playerState = players[playerId] ?: return WinstonActionResult.Error("Player not found")
        val playerName = playerState.identity.playerName

        // Add cards to player's pool
        players[playerId] = playerState.copy(cardPool = playerState.cardPool + takenCards)

        // Clear pile and replenish with 1 card from main deck
        pile.clear()
        if (winstonMainDeck.isNotEmpty()) {
            pile.add(winstonMainDeck.removeFirst())
        }

        val lastAction = "$playerName took pile ${winstonCurrentPileIndex + 1} (${takenCards.size} card${if (takenCards.size != 1) "s" else ""})"

        // Advance to next player's turn
        advanceWinstonTurn()

        // Check if draft is complete
        if (isWinstonDraftComplete()) {
            finishDraft()
            return WinstonActionResult.DraftComplete(lastAction)
        }

        return WinstonActionResult.PileTaken(takenCards, lastAction)
    }

    /**
     * Skip the current pile. Adds 1 card from main deck to the pile,
     * then moves to next pile (or forced blind pick if pile 3 was skipped).
     */
    fun winstonSkipPile(playerId: EntityId): WinstonActionResult {
        if (state != LobbyState.DRAFTING) return WinstonActionResult.Error("Not in drafting phase")
        if (format != TournamentFormat.WINSTON_DRAFT) return WinstonActionResult.Error("Not Winston Draft")
        if (getWinstonActivePlayerId() != playerId) return WinstonActionResult.Error("Not your turn")

        val playerState = players[playerId] ?: return WinstonActionResult.Error("Player not found")
        val playerName = playerState.identity.playerName

        // Add a card from main deck to current pile
        if (winstonMainDeck.isNotEmpty()) {
            winstonPiles[winstonCurrentPileIndex].add(winstonMainDeck.removeFirst())
        }

        val skippedPileIndex = winstonCurrentPileIndex

        if (winstonCurrentPileIndex < 2) {
            // Move to next pile
            winstonCurrentPileIndex++
            val lastAction = "$playerName skipped pile ${skippedPileIndex + 1}"
            return WinstonActionResult.PileSkipped(winstonCurrentPileIndex, lastAction)
        } else {
            // Skipped pile 3 - forced blind pick from main deck
            if (winstonMainDeck.isNotEmpty()) {
                val blindCard = winstonMainDeck.removeFirst()
                players[playerId] = playerState.copy(cardPool = playerState.cardPool + blindCard)

                val lastAction = "$playerName skipped pile 3, took a card from the deck"

                // Advance to next player's turn
                advanceWinstonTurn()

                // Check if draft is complete
                if (isWinstonDraftComplete()) {
                    finishDraft()
                    return WinstonActionResult.DraftComplete(lastAction)
                }

                return WinstonActionResult.BlindPick(blindCard, lastAction)
            } else {
                // No cards left anywhere - draft is complete
                advanceWinstonTurn()
                finishDraft()
                return WinstonActionResult.DraftComplete("$playerName skipped pile 3, deck is empty")
            }
        }
    }

    /**
     * Advance to the next player's turn and reset pile index.
     */
    private fun advanceWinstonTurn() {
        winstonActivePlayerIndex = (winstonActivePlayerIndex + 1) % 2
        winstonCurrentPileIndex = 0
    }

    /**
     * Check if Winston Draft is complete (main deck empty and all piles empty).
     */
    private fun isWinstonDraftComplete(): Boolean {
        return winstonMainDeck.isEmpty() && winstonPiles.all { it.isEmpty() }
    }

    /**
     * Get the player order for Winston Draft.
     */
    fun getWinstonPlayerOrder(): List<EntityId> = playerOrder

    /**
     * Auto-pick for Winston Draft timeout: take the current pile.
     * If current pile is empty, take the blind card.
     */
    fun winstonAutoPickForTimeout(playerId: EntityId): WinstonActionResult {
        if (getWinstonActivePlayerId() != playerId) return WinstonActionResult.Error("Not your turn")

        val pile = winstonPiles[winstonCurrentPileIndex]
        return if (pile.isNotEmpty()) {
            winstonTakePile(playerId)
        } else if (winstonMainDeck.isNotEmpty()) {
            // Take blind card
            val playerState = players[playerId] ?: return WinstonActionResult.Error("Player not found")
            val blindCard = winstonMainDeck.removeFirst()
            players[playerId] = playerState.copy(cardPool = playerState.cardPool + blindCard)
            advanceWinstonTurn()
            if (isWinstonDraftComplete()) {
                finishDraft()
                WinstonActionResult.DraftComplete("${playerState.identity.playerName} auto-picked (timeout)")
            } else {
                WinstonActionResult.BlindPick(blindCard, "${playerState.identity.playerName} auto-picked (timeout)")
            }
        } else {
            finishDraft()
            WinstonActionResult.DraftComplete("Draft complete")
        }
    }

    /**
     * Deal a new 3x3 grid from the main deck.
     * If fewer than 9 cards remain, deals what's available.
     */
    fun dealGrid() {
        gridCards = arrayOfNulls(9)
        val cardsToDeal = minOf(9, gridMainDeck.size)
        for (i in 0 until cardsToDeal) {
            gridCards[i] = gridMainDeck.removeFirst()
        }
        gridPicksThisGrid = 0
        gridNumber++
    }

    /**
     * Pick a row or column from the grid.
     * Returns the cards picked and advances the turn.
     */
    fun gridPickRowOrColumn(playerId: EntityId, selection: GridSelection): GridDraftResult {
        if (state != LobbyState.DRAFTING) {
            return GridDraftResult.Error("Not in drafting phase")
        }
        if (format != TournamentFormat.GRID_DRAFT) {
            return GridDraftResult.Error("Not in grid draft format")
        }

        val expectedPlayer = gridPlayerOrder[gridActivePlayerIndex]
        if (playerId != expectedPlayer) {
            return GridDraftResult.Error("Not your turn")
        }

        // Get the cards at the selected indices
        val indices = selection.getIndices()
        val cards = indices.mapNotNull { gridCards[it] }
        if (cards.isEmpty()) {
            return GridDraftResult.Error("Selected row/column is empty")
        }

        // Remove picked cards from grid
        for (idx in indices) {
            gridCards[idx] = null
        }

        // Add cards to player's pool
        val playerState = players[playerId] ?: return GridDraftResult.Error("Player not found")
        players[playerId] = playerState.copy(cardPool = playerState.cardPool + cards)

        val playerName = playerState.identity.playerName
        val selectionLabel = selection.name.replace("_", " ").lowercase()
        val lastAction = "$playerName picked $selectionLabel (${cards.joinToString(", ") { it.name }})"

        gridPicksThisGrid++

        // Determine how many picks per grid based on player count
        // 2 players: 2 picks per grid (each player picks once)
        // 3 players: 2 picks per grid (2 of 3 players pick, rotate who sits out)
        val picksPerGrid = minOf(players.size, 2)

        if (gridPicksThisGrid >= picksPerGrid) {
            // Grid is exhausted — discard remaining cards and deal new grid
            // The last within-grid pick already advanced the index to the next player,
            // which is the correct starting picker for the new grid (rotates each grid).

            if (gridMainDeck.isEmpty()) {
                // Draft complete — remaining unpicked cards are discarded
                finishGridDraft()
                return GridDraftResult.DraftComplete(lastAction)
            }

            // Discard remaining cards and deal new grid
            dealGrid()
            return GridDraftResult.GridComplete(lastAction)
        } else {
            // Next player picks from the same grid
            gridActivePlayerIndex = (gridActivePlayerIndex + 1) % gridPlayerOrder.size
            return GridDraftResult.PickMade(cards, lastAction)
        }
    }

    /**
     * Get the active player for the current grid draft turn.
     */
    fun getGridActivePlayer(): EntityId? {
        return gridPlayerOrder.getOrNull(gridActivePlayerIndex)
    }

    /**
     * Get the available selections (non-empty rows/columns).
     */
    fun getAvailableGridSelections(): List<GridSelection> {
        return GridSelection.entries.filter { selection ->
            selection.getIndices().any { gridCards[it] != null }
        }
    }

    /**
     * Auto-pick the first available row/column for the active player (timeout).
     */
    fun autoGridPick(playerId: EntityId): GridDraftResult {
        val available = getAvailableGridSelections()
        if (available.isEmpty()) {
            return GridDraftResult.Error("No available selections")
        }
        return gridPickRowOrColumn(playerId, available.first())
    }

    /**
     * Check if the grid draft is complete.
     */
    fun isGridDraftComplete(): Boolean {
        return gridMainDeck.isEmpty() && gridCards.all { it == null }
    }

    /**
     * Finish the grid draft and transition to deck building.
     */
    private fun finishGridDraft() {
        gridCards = arrayOfNulls(9)
        gridMainDeck.clear()
        state = LobbyState.DECK_BUILDING
    }

    /**
     * Submit a deck for a player.
     */
    fun submitDeck(playerId: EntityId, deckList: Map<String, Int>): DeckSubmissionResult {
        // Allow submissions during DECK_BUILDING or TOURNAMENT_ACTIVE (before match starts)
        if (state != LobbyState.DECK_BUILDING && state != LobbyState.TOURNAMENT_ACTIVE) {
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
     * Unsubmit a previously submitted deck to allow editing again.
     * Allowed during deck building or tournament active (before match starts).
     * The caller should verify the player's match hasn't started yet.
     */
    fun unsubmitDeck(playerId: EntityId): Boolean {
        if (state != LobbyState.DECK_BUILDING && state != LobbyState.TOURNAMENT_ACTIVE) {
            return false
        }

        val playerState = players[playerId] ?: return false

        if (!playerState.hasSubmittedDeck) {
            return false // Nothing to unsubmit
        }

        // Clear deck and ready state
        players[playerId] = playerState.copy(submittedDeck = null)
        playersReadyForNextRound.remove(playerId)
        return true
    }

    /**
     * Check if all players have submitted decks.
     */
    fun allDecksSubmitted(): Boolean = players.values.all { it.hasSubmittedDeck }

    /**
     * Transition to tournament active state.
     * Used when all decks are submitted at once (legacy flow).
     */
    fun startTournament() {
        require(state == LobbyState.DECK_BUILDING) { "Not in deck building phase" }
        require(allDecksSubmitted()) { "Not all decks submitted" }
        state = LobbyState.TOURNAMENT_ACTIVE
    }

    /**
     * Activate tournament state for eager match starting.
     * Called when the first player submits their deck, allowing matches
     * to start as soon as both players in a match have submitted.
     */
    fun activateTournament() {
        if (state == LobbyState.DECK_BUILDING) {
            state = LobbyState.TOURNAMENT_ACTIVE
        }
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

        val availableSets = boosterGenerator.availableSets.values.map { config ->
            ServerMessage.AvailableSet(
                code = config.setCode,
                name = config.setName,
                incomplete = config.incomplete,
                block = config.block,
                implementedCount = config.cards.size,
                totalCount = config.totalSetSize
            )
        }

        return ServerMessage.LobbyUpdate(
            lobbyId = lobbyId,
            state = state.name,
            players = playerInfos,
            settings = ServerMessage.LobbySettings(
                setCodes = setCodes,
                setNames = setNames,
                availableSets = availableSets,
                format = format.name,
                boosterCount = boosterCount,
                maxPlayers = maxPlayers,
                pickTimeSeconds = pickTimeSeconds,
                picksPerRound = picksPerRound,
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

            // Enforce 4-copy limit
            if (count > 4) {
                return "Cannot have more than 4 copies of $cardName (have $count)"
            }
        }

        return null
    }

    sealed interface DeckSubmissionResult {
        data class Success(val allReady: Boolean) : DeckSubmissionResult
        data class Error(val message: String) : DeckSubmissionResult
    }

    // =========================================================================
    // Ready State Tracking (between tournament rounds)
    // =========================================================================

    /**
     * Mark a player as ready for the next round.
     * @return true if the player was newly marked ready, false if already ready
     */
    fun markPlayerReady(playerId: EntityId): Boolean {
        return playersReadyForNextRound.add(playerId)
    }

    /**
     * Clear the ready state for all players (called when starting a new round).
     */
    fun clearReadyState() {
        readyEpoch++
        playersReadyForNextRound.clear()
    }

    /**
     * Clear the ready state for a single player (called after their match starts).
     */
    fun clearPlayerReady(playerId: EntityId) {
        playersReadyForNextRound.remove(playerId)
    }

    /**
     * Get the set of player IDs who are ready for the next round.
     */
    fun getReadyPlayerIds(): Set<EntityId> = playersReadyForNextRound.toSet()

    /**
     * Check if all connected players are ready for the next round.
     */
    fun areAllPlayersReady(): Boolean {
        val connectedPlayers = players.values
            .filter { it.identity.isConnected }
            .map { it.identity.playerId }
        return connectedPlayers.isNotEmpty() && connectedPlayers.all { it in playersReadyForNextRound }
    }

    // =========================================================================
    // Persistence Support (for Redis caching)
    // =========================================================================

    /**
     * Restore lobby state from persistence.
     * Called when loading a lobby from Redis after server restart.
     *
     * Note: Players map should be populated before calling this.
     */
    internal fun restoreFromPersistence(
        state: LobbyState,
        hostPlayerId: EntityId?
    ) {
        this.state = state
        this.hostPlayerId = hostPlayerId
    }

    /**
     * Restore draft-specific state from persistence.
     */
    internal fun restoreDraftState(
        currentPackNumber: Int,
        currentPickNumber: Int,
        playerOrder: List<EntityId>
    ) {
        this.currentPackNumber = currentPackNumber
        this.currentPickNumber = currentPickNumber
        this.playerOrder = playerOrder
    }

    /**
     * Get player order for persistence.
     */
    fun getPlayerOrder(): List<EntityId> = playerOrder

    /**
     * Restore Winston Draft state from persistence.
     */
    internal fun restoreWinstonDraftState(
        mainDeck: List<CardDefinition>,
        piles: List<List<CardDefinition>>,
        activePlayerIndex: Int,
        currentPileIndex: Int,
        seenCards: Map<EntityId, MutableSet<String>> = emptyMap()
    ) {
        winstonMainDeck.clear()
        winstonMainDeck.addAll(mainDeck)
        for (i in 0 until 3) {
            winstonPiles[i].clear()
            if (i < piles.size) {
                winstonPiles[i].addAll(piles[i])
            }
        }
        winstonActivePlayerIndex = activePlayerIndex
        winstonCurrentPileIndex = currentPileIndex
        winstonSeenCards.clear()
        winstonSeenCards.putAll(seenCards)
    }

    /**
     * Associate a player identity with this lobby (for reconnection after restore).
     */
    fun associatePlayer(identity: PlayerIdentity, playerState: LobbyPlayerState) {
        players[identity.playerId] = playerState
        identity.currentLobbyId = lobbyId
    }

    // =========================================================================
    // Spectator Management
    // =========================================================================

    /**
     * Add a spectator to the tournament.
     */
    fun addSpectator(identity: PlayerIdentity) {
        spectators[identity.playerId] = identity
        identity.currentLobbyId = lobbyId
    }

    /**
     * Remove a spectator from the tournament.
     */
    fun removeSpectator(playerId: EntityId) {
        val identity = spectators.remove(playerId)
        if (identity != null) {
            identity.currentLobbyId = null
        }
    }

    /**
     * Check if a player is a spectator (not a participant).
     */
    fun isSpectator(playerId: EntityId): Boolean {
        return spectators.containsKey(playerId)
    }
}
