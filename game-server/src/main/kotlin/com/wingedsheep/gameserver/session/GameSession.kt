package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.view.ClientEvent
import com.wingedsheep.engine.view.ClientEventTransformer
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.view.StateDiffCalculator
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.priority.AutoPassManager
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardEntry
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

private val logger = LoggerFactory.getLogger(GameSession::class.java)

/**
 * Represents an active game session between two players.
 *
 * This session acts as a thin wrapper around the engine's ActionProcessor.
 * The engine handles all game logic including mulligan state tracking.
 */
class GameSession(
    val sessionId: String = UUID.randomUUID().toString(),
    private val services: EngineServices,
    private val stateTransformer: ClientStateTransformer = ClientStateTransformer(services.cardRegistry),
    private val useHandSmoother: Boolean = false,
    /**
     * Number of seats this session fills before it is [isReady] to start. Defaults to 2 (the
     * quick-game / sealed / tournament-match case, unchanged). Free-for-All lobbies (Phase 4)
     * pass 3–4. The engine, sessions, and DTOs are seat-count agnostic; this is the only knob.
     */
    val maxPlayers: Int = 2,
) {
    /** Backward-compatible constructor: wraps a CardRegistry in EngineServices. */
    constructor(
        sessionId: String = UUID.randomUUID().toString(),
        cardRegistry: CardRegistry,
        stateTransformer: ClientStateTransformer = ClientStateTransformer(cardRegistry),
        useHandSmoother: Boolean = false,
        debugMode: Boolean = false,
        printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry? = null,
        maxPlayers: Int = 2,
        tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry? = null,
    ) : this(sessionId, EngineServices(cardRegistry, printingRegistry, tokenArtRegistry), if (debugMode) ClientStateTransformer(cardRegistry, debugMode = true) else stateTransformer, useHandSmoother, maxPlayers)

    private val cardRegistry: CardRegistry get() = services.cardRegistry
    // Lock for synchronizing state modifications to prevent lost updates
    private val stateLock = Any()

    @Volatile
    private var gameState: GameState? = null
        set(value) {
            field = value
            if (value != null) recordEliminations(value)
        }

    /**
     * Players in the order they lost the game (first eliminated first). Maintained by the
     * [gameState] setter — the single chokepoint every state mutation flows through — by diffing
     * the engine's `PlayerLostComponent` markers against what's already recorded. Drives
     * Free-for-All standings (placement = reverse elimination order).
     */
    private val eliminationOrder = CopyOnWriteArrayList<EntityId>()

    private fun recordEliminations(state: GameState) {
        for (playerId in state.turnOrder) {
            if (playerId in eliminationOrder) continue
            if (state.getEntity(playerId)?.has<PlayerLostComponent>() == true) {
                eliminationOrder.add(playerId)
            }
        }
    }

    /** Players in the order they lost (first eliminated first). Empty while everyone is alive. */
    fun getEliminationOrder(): List<EntityId> = eliminationOrder.toList()

    /** Player seats that have not been eliminated yet. */
    fun getActivePlayerIds(): List<EntityId> = synchronized(stateLock) {
        val state = gameState ?: return@synchronized emptyList()
        players.keys.mapNotNull { playerId ->
            playerId.takeUnless { state.getEntity(it)?.has<PlayerLostComponent>() == true }
        }
    }

    /**
     * Seats already sent their personal [ServerMessage.PlayerEliminated]. A seat is told exactly
     * once, however it died — conceding, damage, decking out — so its client shows the "you're out,
     * the table plays on" overlay a single time.
     */
    private val eliminationNotified = ConcurrentHashMap.newKeySet<EntityId>()

    /** Eliminated seats that haven't been told yet, in elimination order. */
    fun unnotifiedEliminations(): List<EntityId> = eliminationOrder.filter { it !in eliminationNotified }

    /** Record that [playerId] has been told they're out, so it isn't told again. */
    fun markEliminationNotified(playerId: EntityId) {
        eliminationNotified.add(playerId)
    }

    /**
     * Why [playerId] is out of the game, in client terms. Falls back to [GameOverReason.LIFE_ZERO]
     * for a seat with no loss marker — same fallback [getGameOverReason] uses.
     */
    fun getEliminationReason(playerId: EntityId): GameOverReason =
        gameOverReasonFor(gameState?.getEntity(playerId)?.get<PlayerLostComponent>()?.reason)

    /** Checkpoint for undoing the last non-respondable action (e.g., play land, declare attackers) */
    @Volatile
    private var undoCheckpoint: GameState? = null

    /** The player who owns the current undo checkpoint */
    @Volatile
    private var undoCheckpointOwner: EntityId? = null

    /** State saved when the active player passes priority in precombat main, used to undo combat entry */
    @Volatile
    private var preCombatState: GameState? = null

    /**
     * [recordedActions] size at each undo checkpoint, kept in lockstep with [undoCheckpoint] /
     * [preCombatState]. When a player undoes, the replay log must roll back to exactly the actions
     * that produced the restored checkpoint state, or reconstruction would diverge.
     */
    @Volatile
    private var undoCheckpointActionCount: Int? = null
    @Volatile
    private var preCombatActionCount: Int? = null
    /** Set code used for quick game deck generation (so joining player uses the same set) */
    @Volatile
    var quickGameSetCode: String? = null

    /**
     * When true, this game is listed as a Live Game on the landing page so anonymous visitors can
     * spectate it. Set by the lobby/quick-game handler at start; tournament games derive this from
     * the parent tournament lobby's `isPublic` flag instead.
     */
    @Volatile
    var publicSpectate: Boolean = false

    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, List<String>>()
    /**
     * Per-player sideboard card names ("outside the game", CR 100.4). Flat list, one entry per
     * copy. Empty for almost every game. Seeds [com.wingedsheep.sdk.core.Zone.SIDEBOARD] at game
     * start so wish effects (Burning Wish, …) can fetch from it.
     */
    private val sideboards = mutableMapOf<EntityId, List<String>>()
    /** Per-player commander card name for commander-shape formats. Null = no commander. */
    private val commanderCardNames = mutableMapOf<EntityId, String>()
    /**
     * Active spectators, keyed by player id. Keyed by *identity* rather than held as a set of
     * sessions because a reconnect (refresh, or leaving and coming back) arrives on a brand-new
     * [PlayerSession]: a set would then hold both the dead and the live socket and the same person
     * would show up twice in the players' "N watching" list.
     */
    private val spectators = ConcurrentHashMap<EntityId, PlayerSession>()

    /**
     * Format the engine should run this game under. Set by the lobby / quick-game / tournament
     * handler before [startGame]. Null = use [com.wingedsheep.sdk.core.Format.Standard]. Stored on
     * the session (not GameInitializer) so reconnects / persistence don't need to thread it.
     */
    @Volatile
    var engineFormat: com.wingedsheep.sdk.core.Format = com.wingedsheep.sdk.core.Format.Standard

    /**
     * Which opponents creatures may attack (CR 802 / 803). Set by the Free-for-All lobby handler
     * before [startGame]; [com.wingedsheep.sdk.core.AttackMode.MULTIPLE] for two-player and
     * tournament games, where it has no effect. Stored on the session for the same reasons as
     * [engineFormat].
     */
    @Volatile
    var attackMode: com.wingedsheep.sdk.core.AttackMode = com.wingedsheep.sdk.core.AttackMode.MULTIPLE

    /**
     * Team partitioning for team variants (Two-Headed Giant — CR 810), as lists of seat indices
     * into the join/turn order; each entry is one team. Set by the lobby / scenario handler before
     * [startGame] and forwarded to [GameConfig.teams], which stamps [TeamComponent] on each player.
     * Null = no teams (every seat plays alone), so 2-player / Free-for-All games are unchanged.
     * Stored on the session for the same reasons as [engineFormat].
     */
    @Volatile
    var teams: List<List<Int>>? = null

    /**
     * True when this is a ranked 1v1 game between two signed-in accounts — its result adjusts both
     * players' ELO for [rankedMode]. Set by the quick-game / tournament handler before [startGame],
     * only after eligibility is confirmed (1v1, every seat a logged-in human). False — the default —
     * for every casual, guest, AI, or multiplayer game. Stored on the session for the same reasons as
     * [engineFormat]: the originating lobby may be gone by the time the game ends.
     */
    @Volatile
    var ranked: Boolean = false

    /** Which ranked queue this game counts toward when [ranked]; null otherwise. */
    @Volatile
    var rankedMode: com.wingedsheep.gameserver.ranking.RankedMode? = null

    /** Player info for persistence (playerId -> (playerName, token)) */
    private val playerPersistenceInfo = mutableMapOf<EntityId, PlayerPersistenceInfo>()

    data class PlayerPersistenceInfo(
        val playerName: String,
        val token: String,
        val isAi: Boolean = false,
        val aiModelOverride: String? = null
    )

    /**
     * Runaway/wedge backstop for this game — see [GameStallGuard]. Lives on the session because
     * every applied action funnels through [recordAction], which is the only place that can see
     * "the game moved" for every path at once.
     */
    private var stallGuard = GameStallGuard()

    /** Where [appendToReplayLog] stops recording. Mutable only for the test seam below. */
    private var replayActionCap =
        com.wingedsheep.gameserver.replay.ReplayRecordingPolicy.MAX_RECORDED_ACTIONS

    private val actionProcessor = ActionProcessor(services)
    private val gameInitializer = GameInitializer(cardRegistry, services.printingRegistry)
    private val autoPassManager = AutoPassManager(cardRegistry)
    private val spectatorStateBuilder = SpectatorStateBuilder(cardRegistry, stateTransformer)
    private val decisionEnricher = DecisionEnricher(cardRegistry)
    private val legalActionEnumerator = LegalActionEnumerator(
        cardRegistry, services.manaSolver, services.costCalculator,
        services.predicateEvaluator, services.conditionEvaluator, services.turnManager
    )
    private val legalActionEnricher = LegalActionEnricher(services.manaSolver, cardRegistry)

    /** Tracks the last processed messageId per player for idempotency */
    private val lastProcessedMessageId = java.util.concurrent.ConcurrentHashMap<EntityId, String>()

    /** Accumulated game log per player (player-specific due to masking) */
    private val gameLogs = java.util.concurrent.ConcurrentHashMap<EntityId, MutableList<ClientEvent>>()

    /** Per-player priority mode setting (AUTO = smart auto-pass, STOPS = stop on opponent stack + combat damage, FULL_CONTROL = never auto-pass) */
    private val priorityModes = java.util.concurrent.ConcurrentHashMap<EntityId, PriorityMode>()
    private val stopOverrides = java.util.concurrent.ConcurrentHashMap<EntityId, StopOverrideSettings>()

    // Compact replay recording — see [com.wingedsheep.gameserver.replay.CompactReplay]. Instead of
    // storing a masked snapshot, a per-frame delta, and a full unmasked GameState for every frame,
    // we record only the reproducible inputs: the [replaySetup] (seed + decks + seat ids, captured
    // at [startGame]) and the ordered [recordedActions] applied to the game. The full spectator
    // stream is re-derived on demand by ReplayReconstructor.
    @Volatile
    private var replaySetup: com.wingedsheep.gameserver.replay.ReplaySetup? = null
    private val recordedActions = CopyOnWriteArrayList<GameAction>()
    // Set once the recording has hit [ReplayRecordingPolicy.MAX_RECORDED_ACTIONS] and been frozen.
    // Sticky, and stored with the record — see [recordAction].
    @Volatile
    private var replayTruncated = false
    // Persistent-yield mutations applied out-of-band of [recordedActions]. Captured in turn order so
    // the reconstructor can re-apply each at the action position it was set (see [CompactReplay.yields]).
    private val recordedYields = CopyOnWriteArrayList<com.wingedsheep.gameserver.replay.ReplayYieldEntry>()
    // Sparse position fingerprints, so a later reconstruction can tell "this is the game that was
    // played" from "this is a game the current engine produces from the same inputs".
    private val recordedCheckpoints =
        CopyOnWriteArrayList<com.wingedsheep.gameserver.replay.ReplayCheckpoint>()
    // Archived card definitions for this game, computed lazily on first use — see [getPinnedCards].
    @Volatile
    private var pinnedCards: List<String>? = null
    var replayStartedAt: Instant? = null
        private set

    /** Per-player cache of last sent ClientGameState for delta computation */
    private val lastSentState = java.util.concurrent.ConcurrentHashMap<EntityId, ClientGameState>()

    /** Monotonically increasing version counter, included in every state update so clients can detect missed messages */
    private val stateVersions = java.util.concurrent.ConcurrentHashMap<EntityId, Long>()

    data class StopOverrideSettings(
        val myTurnStops: Set<Step> = emptySet(),
        val opponentTurnStops: Set<Step> = emptySet()
    )

    enum class PriorityMode {
        AUTO,
        STOPS,
        FULL_CONTROL
    }

    /**
     * All seated players in join order (which is also turn order once the game starts). This is
     * the single N-player accessor; broadcasts iterate it. Note the underlying map is a
     * [LinkedHashMap], so iteration order is stable.
     */
    fun getPlayers(): List<PlayerSession> = players.values.toList()

    val isFull: Boolean get() = players.size >= maxPlayers
    val isReady: Boolean get() = players.size == maxPlayers && deckLists.size == maxPlayers
    val isStarted: Boolean get() = gameState != null

    /**
     * Check if we're in mulligan phase by looking at engine's mulligan state.
     */
    val isMulliganPhase: Boolean
        get() {
            val state = gameState ?: return false
            return state.turnOrder.any { playerId ->
                val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
                mullState != null && !mullState.hasKept
            }
        }

    /**
     * Check if all mulligans are complete.
     */
    val allMulligansComplete: Boolean
        get() {
            val state = gameState ?: return false
            return state.turnOrder.all { playerId ->
                val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
                mullState?.hasKept == true && mullState.cardsToBottom == 0
            }
        }

    /**
     * Add a player to this game session.
     * Returns the assigned EntityId for this player.
     */
    fun addPlayer(
        playerSession: PlayerSession,
        deckList: Map<String, Int>,
        commanderCardName: String? = null,
        sideboard: Map<String, Int> = emptyMap(),
    ): EntityId {
        require(!isFull) { "Game session is full" }

        val playerId = playerSession.playerId
        players[playerId] = playerSession

        // Convert deck list map to flat list of card names
        val cards = deckList.flatMap { (cardName, count) ->
            List(count) { cardName }
        }
        deckLists[playerId] = cards
        sideboards[playerId] = sideboard.flatMap { (cardName, count) -> List(count) { cardName } }
        if (commanderCardName != null) {
            commanderCardNames[playerId] = commanderCardName
        } else {
            commanderCardNames.remove(playerId)
        }
        playerSession.currentGameSessionId = sessionId

        return playerId
    }

    /**
     * Un-seat a player: they give up their chair in this session.
     *
     * Before the game starts that also releases their submitted deck and sideboard — the seat is free
     * for someone else. Once the game is under way the decklist stops being live state and becomes
     * part of the historical record (match history reads every seat's deck at game over, and the AI is
     * re-wired from it after a restart), so it is kept. That distinction matters because "un-seat then
     * re-seat" is a tempting way to swap in a reconnecting player's new socket — see [associatePlayer],
     * which does that in one step without disturbing the seat at all.
     */
    fun removePlayer(playerId: EntityId) {
        players[playerId]?.currentGameSessionId = null
        players.remove(playerId)
        if (!isStarted) {
            deckLists.remove(playerId)
            sideboards.remove(playerId)
        }
    }

    /**
     * Get every other seated player's ID (all opponents of [playerId]). In a 2-player game this
     * is a single-element list. Order follows seating/turn order. In Two-Headed Giant (CR 810)
     * a teammate is not an opponent, so the player's whole team is excluded; the engine's
     * [GameState.teamOf] is the single source of truth, and degrades to a singleton in non-team
     * games so this is unchanged there. Before the game has started (no state yet) every other
     * seat is treated as an opponent.
     */
    fun getOpponentIds(playerId: EntityId): List<EntityId> {
        val team = gameState?.teamOf(playerId)?.toHashSet() ?: setOf(playerId)
        return players.keys.filter { it !in team }
    }

    /**
     * Get the player session for a player ID.
     */
    fun getPlayerSession(playerId: EntityId): PlayerSession? = players[playerId]

    /**
     * Replace a player's session (e.g., when wiring a new AI WebSocket session for a tournament match).
     */
    fun replacePlayerSession(playerId: EntityId, newSession: PlayerSession) {
        if (players.containsKey(playerId)) {
            players[playerId] = newSession
        }
    }

    // =========================================================================
    // Spectator Management
    // =========================================================================

    /**
     * Add a spectator to this game session, replacing any earlier session that same spectator
     * was watching on (see [spectators]).
     */
    fun addSpectator(spectator: PlayerSession) {
        spectators[spectator.playerId] = spectator
    }

    /**
     * Remove a spectator from this game session. Only drops the entry while it still points at
     * [spectator]'s socket, so a late cleanup for a socket the spectator has already replaced by
     * reconnecting can't evict their live session.
     */
    fun removeSpectator(spectator: PlayerSession) {
        spectators.remove(spectator.playerId, spectator)
    }

    /**
     * Get all spectators with a live socket. Closed sockets are filtered out rather than counted:
     * a spectator who closed their tab is not watching, even before [pruneDisconnectedSpectators]
     * gets around to dropping them.
     */
    fun getSpectators(): Set<PlayerSession> =
        spectators.values.filterTo(mutableSetOf()) { it.isConnected }

    /**
     * Drop spectators whose socket has closed, returning true when anything was removed so the
     * caller can refresh the players' spectator badge. Closing a tab produces no StopSpectating
     * message, so without this sweep those entries would live as long as the game session.
     */
    fun pruneDisconnectedSpectators(): Boolean =
        spectators.values.removeAll { !it.isConnected }

    /**
     * The spectator badge shown to the seated players: how many people are watching, and who.
     */
    fun spectatorCountMessage(): ServerMessage.SpectatorCountChanged {
        val current = getSpectators()
        return ServerMessage.SpectatorCountChanged(
            gameSessionId = sessionId,
            count = current.size,
            spectatorNames = current.map { it.playerName }
        )
    }

    /**
     * Player names in seat order, for spectator display.
     */
    fun getPlayerNames(): List<String> = getPlayers().map { it.playerName }

    /**
     * Current life totals in seat order, for spectator display. Routed through
     * [GameState.lifeTotal] so a Two-Headed Giant team's shared total (CR 810.9a) shows the
     * same value for both teammates; in non-team games this is the player's own total.
     */
    fun getLifeTotals(): List<Int> {
        val state = gameState ?: return emptyList()
        return getPlayers().map { player ->
            if (state.getEntity(player.playerId)?.get<LifeTotalComponent>() != null) {
                state.lifeTotal(player.playerId)
            } else 20
        }
    }

    /**
     * The N-player seat roster (turn order). [seatIndex] is the player's index in the engine's
     * turn order once the game has started, falling back to join order before then. [viewerId],
     * when given, flags that recipient's own seat ([PlayerSeatInfo.isYou]); spectators pass null.
     */
    fun seatInfos(viewerId: EntityId? = null): List<ServerMessage.PlayerSeatInfo> {
        val state = gameState
        val turnOrder = state?.turnOrder
        val seated = getPlayers()
        return seated.mapIndexed { joinIndex, player ->
            val seatIndex = turnOrder?.indexOf(player.playerId)?.takeIf { it >= 0 } ?: joinIndex
            ServerMessage.PlayerSeatInfo(
                playerId = player.playerId.value,
                name = player.playerName,
                seatIndex = seatIndex,
                isYou = viewerId != null && player.playerId == viewerId,
                isAi = playerPersistenceInfo[player.playerId]?.isAi == true,
                // Team membership for Two-Headed Giant (CR 810); null in non-team games. Prefer the
                // engine's stamped TeamComponent, but fall back to the configured [teams] partition
                // (join-order indices) so the roster carries teamIndex even before startGame() runs
                // (the pod sends the seat roster before the game state is initialized).
                teamIndex = state?.getEntity(player.playerId)
                    ?.get<com.wingedsheep.engine.state.components.identity.TeamComponent>()
                    ?.teamIndex
                    ?: teams?.indexOfFirst { joinIndex in it }?.takeIf { it >= 0 },
                // 2HG pools life per team (CR 810.4); Team vs. Team keeps per-player life (CR 808.5).
                // Prefer the running game's format (set for scenario/hotseat pods too), falling back
                // to the configured format before the game state exists.
                teamSharedLife = (state?.format ?: engineFormat).sharesTeamLife,
                teamSharedTurns = (state?.format ?: engineFormat).sharesTeamTurns,
            )
        }.sortedBy { it.seatIndex }
    }

    fun buildSpectatorState(): ServerMessage.SpectatorStateUpdate? {
        val state = gameState ?: return null
        val seated = getPlayers()
        if (seated.size < 2) return null
        val seats = seated.map { SpectatorSeat(it.playerId, it.playerName) }
        return spectatorStateBuilder.buildState(state, seats, seatInfos(), sessionId)
    }

    /**
     * Start the game. Both players must have joined with deck lists.
     * Initializes the game with the new engine - mulligan phase is handled by the engine.
     */
    fun startGame(): GameState {
        require(isReady) { "Game session not ready - need $maxPlayers players with deck lists" }

        val playerConfigs = players.map { (playerId, session) ->
            PlayerConfig(
                name = session.playerName,
                deck = Deck(
                    cards = deckLists[playerId]!!,
                    cardEntries = deckLists[playerId]!!.map(::cardEntryFromIdentifier),
                    sideboard = sideboards[playerId].orEmpty().map(::cardEntryFromIdentifier),
                ),
                playerId = playerId,  // Pass existing player ID to the engine
                commanderCardName = commanderCardNames[playerId],
            )
        }

        val config = GameConfig(
            players = playerConfigs,
            useHandSmoother = useHandSmoother,
            format = engineFormat,
            attackMode = attackMode,
            // Team partitioning for Two-Headed Giant (CR 810); null for non-team games. The seat
            // indices line up with playerConfigs, which preserves the join/turn order of `players`.
            teams = teams,
        )

        val result = gameInitializer.initializeGame(config)
        gameState = result.state
        replayStartedAt = Instant.now()
        // Capture everything needed to reconstruct this game later, including the seed the engine
        // actually used (so the shuffle / turn order / coin flips replay identically) and the seat
        // roster (now that gameState exists, seatInfos() reflects the real turn order).
        replaySetup = com.wingedsheep.gameserver.replay.ReplaySetup(
            seed = result.seed,
            format = engineFormat,
            attackMode = attackMode,
            startingHandSize = config.startingHandSize,
            skipMulligans = config.skipMulligans,
            useHandSmoother = config.useHandSmoother,
            handSmootherCandidates = config.handSmootherCandidates,
            startingPlayerIndex = config.startingPlayerIndex,
            teams = config.teams,
            players = playerConfigs.map { pc ->
                com.wingedsheep.gameserver.replay.ReplayPlayerSetup(
                    playerId = pc.playerId!!.value,
                    name = pc.name,
                    deck = pc.deck,
                    startingLife = pc.startingLife,
                    commanderCardName = pc.commanderCardName,
                )
            },
            seatRoster = seatInfos(),
        )
        return result.state
    }

    private fun cardEntryFromIdentifier(identifier: String): CardEntry {
        val separator = identifier.lastIndexOf('#')
        if (separator < 0) return CardEntry(identifier)
        val coordinates = identifier.substring(separator + 1)
        val dash = coordinates.indexOf('-')
        if (dash <= 0 || dash == coordinates.lastIndex) return CardEntry(identifier)
        return CardEntry(
            name = identifier.substring(0, separator),
            printing = com.wingedsheep.sdk.model.PrintingRef(
                setCode = coordinates.substring(0, dash),
                collectorNumber = coordinates.substring(dash + 1),
            ),
        )
    }

    /**
     * Get the mulligan count for a player.
     */
    fun getMulliganCount(playerId: EntityId): Int {
        val state = gameState ?: return 0
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return mullState?.mulligansTaken ?: 0
    }

    /**
     * Check if a player has completed their mulligan.
     */
    fun hasMulliganComplete(playerId: EntityId): Boolean {
        val state = gameState ?: return false
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return mullState?.hasKept == true && mullState.cardsToBottom == 0
    }

    /**
     * Check if a player is awaiting bottom card selection.
     */
    fun isAwaitingBottomCards(playerId: EntityId): Boolean {
        val state = gameState ?: return false
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return mullState?.hasKept == true && mullState.cardsToBottom > 0
    }

    /**
     * Get the number of cards player needs to put on bottom.
     */
    fun getCardsToBottom(playerId: EntityId): Int {
        val state = gameState ?: return 0
        val mullState = state.getEntity(playerId)?.get<MulliganStateComponent>()
        return if (mullState?.hasKept == true) mullState.cardsToBottom else 0
    }

    /**
     * Get the player's current hand for mulligan decisions.
     */
    fun getHand(playerId: EntityId): List<EntityId> {
        val state = gameState ?: return emptyList()
        return state.getHand(playerId)
    }

    /**
     * Player chooses to keep their current hand.
     * Routes through the engine's action processor.
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun keepHand(playerId: EntityId): MulliganActionResult = synchronized(stateLock) {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = KeepHand(playerId)
        val result = actionProcessor.process(state, action).result

        val error = result.error
        if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            recordAction(action)
            val mullState = result.state.getEntity(playerId)?.get<MulliganStateComponent>()
            if (mullState?.cardsToBottom ?: 0 > 0) {
                MulliganActionResult.NeedsBottomCards(mullState!!.cardsToBottom)
            } else {
                MulliganActionResult.Success
            }
        }
    }

    /**
     * Player chooses to mulligan - shuffle hand and draw a new hand.
     * Routes through the engine's action processor.
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun takeMulligan(playerId: EntityId): MulliganActionResult = synchronized(stateLock) {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = TakeMulligan(playerId)
        val result = actionProcessor.process(state, action).result

        val error = result.error
        if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            recordAction(action)
            MulliganActionResult.Success
        }
    }

    /**
     * Player chooses which cards to put on the bottom of their library.
     * Routes through the engine's action processor.
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     */
    fun chooseBottomCards(playerId: EntityId, cardIds: List<EntityId>): MulliganActionResult = synchronized(stateLock) {
        val state = gameState ?: return MulliganActionResult.Failure("Game not started")

        val action = BottomCards(playerId, cardIds)
        val result = actionProcessor.process(state, action).result

        val error = result.error
        if (error != null) {
            MulliganActionResult.Failure(error)
        } else {
            gameState = result.state
            recordAction(action)
            MulliganActionResult.Success
        }
    }

    /**
     * Get the mulligan decision message for a player.
     */
    fun getMulliganDecision(playerId: EntityId): ServerMessage.MulliganDecision {
        val hand = getHand(playerId)
        val count = getMulliganCount(playerId)
        val state = gameState
        val cards = mulliganCardInfo(state, hand)
        val isOnThePlay = gameState?.activePlayerId == playerId
        // Cards bottomed if this player keeps now. Reads the component's free-mulligan-aware
        // cardsToBottom (CR 800.6) rather than the raw mulligan count, so a multiplayer first
        // mulligan correctly shows "bottom 0".
        val cardsToPutOnBottom = state?.getEntity(playerId)
            ?.get<MulliganStateComponent>()?.cardsToBottom ?: count
        return ServerMessage.MulliganDecision(
            hand = hand,
            mulliganCount = count,
            cardsToPutOnBottom = cardsToPutOnBottom,
            cards = cards,
            isOnThePlay = isOnThePlay
        )
    }

    /**
     * Get the choose bottom cards message for a player.
     */
    fun getChooseBottomCardsMessage(playerId: EntityId): ServerMessage.ChooseBottomCards? {
        val count = getCardsToBottom(playerId)
        if (count == 0) return null
        val hand = getHand(playerId)
        val state = gameState
        return ServerMessage.ChooseBottomCards(
            hand = hand,
            cardsToPutOnBottom = count,
            cards = mulliganCardInfo(state, hand)
        )
    }

    /**
     * Build the per-card display info the mulligan screens render.
     *
     * The art comes off the entity's own [CardComponent.imageUri], which [CardEntityFactory]
     * stamps from the printing the player actually put in their deck. Re-deriving it from the
     * canonical [CardDefinition] metadata instead (as this used to) shows the *original* printing's
     * art for every reprint, so the mulligan hand didn't match the same cards once they were in
     * play. The definition lookup remains only as a fallback for entities with no image stamped.
     */
    private fun mulliganCardInfo(
        state: GameState?,
        hand: List<EntityId>
    ): Map<EntityId, ServerMessage.MulliganCardInfo> {
        if (state == null) return emptyMap()
        return hand.associateWith { entityId ->
            val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
            val imageUri = cardComponent?.imageUri
                ?: cardComponent?.cardDefinitionId?.let { defId ->
                    cardRegistry.getCard(defId)?.metadata?.imageUri
                }
            ServerMessage.MulliganCardInfo(
                name = cardComponent?.name ?: "Unknown",
                imageUri = imageUri,
                manaCost = cardComponent?.manaCost?.toString(),
                typeLine = cardComponent?.typeLine?.toString(),
                power = cardComponent?.baseStats?.basePower,
                toughness = cardComponent?.baseStats?.baseToughness,
                oracleText = cardComponent?.oracleText?.takeIf { it.isNotBlank() }
            )
        }
    }

    sealed interface MulliganActionResult {
        data object Success : MulliganActionResult
        data class NeedsBottomCards(val count: Int) : MulliganActionResult
        data class Failure(val reason: String) : MulliganActionResult
    }

    /**
     * Execute a game action.
     *
     * Routes the action through the engine's ActionProcessor.
     * Synchronized to prevent lost updates when multiple players act simultaneously.
     *
     * Undo checkpoint management follows the engine's [UndoCheckpointAction] policy —
     * the engine decides what to do with checkpoints, the server just executes it.
     */
    fun executeAction(playerId: EntityId, action: GameAction, messageId: String? = null): ActionResult = synchronized(stateLock) {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        // Seat authorization: a seat may submit actions tagged with its own playerId, or
        // act on behalf of a player whose turn it currently controls (Mindslaver-style).
        // The action.playerId always represents the in-game actor (whose mana, cards,
        // and spell-controllership this action is); the controller is just the input
        // device. Concede is excluded — the affected player can always concede regardless
        // of who's controlling them.
        val actionPlayerId = action.playerId
        if (action !is Concede && playerId != actionPlayerId && state.actorFor(actionPlayerId) != playerId) {
            return ActionResult.Failure("Not authorized to submit actions for player $actionPlayerId")
        }

        // Idempotency check: if this messageId was already processed, skip
        if (messageId != null) {
            val lastId = lastProcessedMessageId[playerId]
            if (lastId == messageId) {
                return ActionResult.Failure("Duplicate message")
            }
        }

        // If the opponent takes a substantive action, invalidate the undo checkpoint —
        // the opponent has seen the game state after the undoable action and made a decision
        // based on it. A bare `PassPriority` is benign (no information revealed, no real
        // decision), so it preserves the checkpoint; the engine's [UndoPolicyComputer] already
        // returns PRESERVE for it. Keeping the checkpoint through opponent passes is what lets
        // the active player undo back to precombat main when they auto-passed into
        // declare-attackers by accident.
        if (undoCheckpoint != null && undoCheckpointOwner != null
            && playerId != undoCheckpointOwner
            && action !is PassPriority) {
            clearCheckpoint()
        }

        val (result, undoPolicy) = actionProcessor.process(state, action)

        val error = result.error
        if (error != null) {
            return ActionResult.Failure(error)
        }

        // Apply the engine's undo policy
        applyUndoPolicy(undoPolicy, action, state, playerId)

        gameState = result.state
        recordAction(action)
        if (messageId != null) lastProcessedMessageId[playerId] = messageId
        val pendingDecision = result.pendingDecision
        return if (pendingDecision != null) {
            ActionResult.PausedForDecision(result.state, pendingDecision, result.events)
        } else {
            ActionResult.Success(result.state, result.events)
        }
    }

    /**
     * Handle player concession.
     * Synchronized to prevent lost updates.
     */
    fun playerConcedes(playerId: EntityId): GameState? = synchronized(stateLock) {
        val state = gameState ?: return null
        val action = Concede(playerId)
        val result = actionProcessor.process(state, action).result

        gameState = result.state
        if (result.error == null) recordAction(action)
        result.state
    }

    /**
     * Get the client game state for a specific player.
     */
    fun getClientState(playerId: EntityId): ClientGameState? {
        val state = gameState ?: return null
        return stateTransformer.transform(state, playerId)
    }

    fun getLegalActions(playerId: EntityId): List<LegalActionInfo> {
        val state = gameState ?: return emptyList()

        // CR 605.3a — while a rule or effect is asking this seat for a mana payment (ward, "you may
        // pay {B}", an attack tax) they hold no priority, but they may still activate mana
        // abilities. Offer exactly those: the pre-computed source menu on the decision only covers
        // {T}-shaped abilities, so without this a cost payable only with, say, Ashnod's Altar is
        // unreachable. See [ManaPaymentWindow].
        ManaPaymentWindow.openFor(state, playerId)?.let { window ->
            val manaActions = legalActionEnumerator.enumerateManaAbilities(state, window.playerId)
            return legalActionEnricher.enrich(manaActions, state, window.playerId)
        }

        val priorityPlayer = state.priorityPlayerId ?: return emptyList()
        // The seat this connection may act as right now. Normally the priority player, or — when
        // their turn is hijacked — whoever this connection is the actor for. Legal actions are
        // enumerated for the *acting* seat, since it is that seat's mana, cards and turn.
        //
        // CR 805.5: under shared team turns the whole of the baton holder's team may act, so a
        // teammate's connection gets its own actions instead of nothing — that is what lets you
        // answer what your partner just did without first waiting for the baton to reach you.
        // The baton holder is tried first so a hotseat client (the actor for every seat) keeps
        // driving exactly the seat the UI is focused on. Outside a shared-turns format
        // [GameState.priorityTeam] is the singleton baton holder and this is the old expression.
        val actingSeat = (listOf(priorityPlayer) + state.priorityTeam.filter { it != priorityPlayer })
            .firstOrNull { state.actorFor(it) == playerId }
            ?: return emptyList()
        if (state.pendingDecision != null) return emptyList()
        val engineActions = legalActionEnumerator.enumerate(state, actingSeat)
        return legalActionEnricher.enrich(engineActions, state, actingSeat)
    }


    /**
     * Create a state update message for a player.
     * Returns either a full [ServerMessage.StateUpdate] (first update or after reconnect)
     * or a [ServerMessage.StateDeltaUpdate] (subsequent updates with only changes).
     */
    fun createStateUpdate(playerId: EntityId, events: List<GameEvent>): ServerMessage? {
        val state = gameState ?: return null
        val clientState = getClientState(playerId) ?: return null
        val legalActions = getLegalActions(playerId)

        // Transform raw engine events to client events
        val clientEvents = ClientEventTransformer.transform(events, playerId)

        // Accumulate into persistent game log (filter noisy events)
        val logEntries = clientEvents.filter { it !is ClientEvent.PermanentTapped && it !is ClientEvent.PermanentUntapped && it !is ClientEvent.ManaAdded }
        val playerLog = gameLogs.getOrPut(playerId) { mutableListOf() }
        playerLog.addAll(logEntries)

        // Include pending decision only for the player who needs to make it — i.e. the
        // actor for the affected player. During a hijacked turn this routes the
        // decision to the controller, not the affected player.
        // Enrich with imageUri from card registry since engine doesn't have access to metadata
        val pendingDecision = state.pendingDecision?.takeIf { state.actorFor(it.playerId) == playerId }?.let {
            decisionEnricher.enrich(it, state, playerId)
        }

        // Calculate next stop point for the Pass button (only if player has priority,
        // or is the actor for whoever has priority during a hijacked turn).
        val playerOverrides = getStopOverrides(playerId)
        val playerMode = getPriorityMode(playerId)
        // "Can this connection act in the current priority window?" — the baton holder's seat, or
        // (CR 805.5) any seat on the baton holder's team under shared team turns.
        val isActorForPriority = state.priorityTeam.any { state.actorFor(it) == playerId }
        val nextStopPoint = if (isActorForPriority && playerMode != PriorityMode.FULL_CONTROL) {
            // The same notion of "meaningful" the stop decision itself uses — otherwise the
            // button can promise a stop (say, at the opponent's end step for a spell we can't
            // actually pay for) that never arrives.
            val hasMeaningfulActions = autoPassManager.getMeaningfulActions(legalActions).isNotEmpty()
            autoPassManager.getNextStopPoint(state, playerId, hasMeaningfulActions, myTurnStops = playerOverrides.myTurnStops, opponentTurnStops = playerOverrides.opponentTurnStops, stopsMode = playerMode == PriorityMode.STOPS)
        } else {
            null
        }

        // Include opponent decision status for the player who is NOT driving this
        // decision — i.e. when their seat is not the actor for the affected player.
        val opponentDecisionStatus = state.pendingDecision?.takeIf { state.actorFor(it.playerId) != playerId }?.let {
            decisionEnricher.createOpponentDecisionStatus(it, state, playerId)
        }

        val stateWithLog = clientState.copy(gameLog = playerLog.toList())
        val stopOverrideInfo = if (playerOverrides.myTurnStops.isNotEmpty() || playerOverrides.opponentTurnStops.isNotEmpty()) {
            ServerMessage.StopOverrideInfo(playerOverrides.myTurnStops, playerOverrides.opponentTurnStops)
        } else {
            null
        }
        val priorityModeStr = when (playerMode) {
            PriorityMode.AUTO -> "auto"
            PriorityMode.STOPS -> "stops"
            PriorityMode.FULL_CONTROL -> "fullControl"
        }

        // Check if we have a previous state for delta computation
        val previous = lastSentState[playerId]
        lastSentState[playerId] = stateWithLog
        val version = stateVersions.merge(playerId, 1L) { old, inc -> old + inc }!!

        if (previous != null) {
            // Compute delta and send smaller message
            val delta = StateDiffCalculator.computeDelta(previous, stateWithLog)
            return ServerMessage.StateDeltaUpdate(delta, clientEvents, legalActions, pendingDecision, nextStopPoint, opponentDecisionStatus, stopOverrideInfo, isUndoAvailable(playerId), priorityModeStr, version)
        }

        // First update — send full state
        return ServerMessage.StateUpdate(stateWithLog, clientEvents, legalActions, pendingDecision, nextStopPoint, opponentDecisionStatus, stopOverrideInfo, isUndoAvailable(playerId), priorityModeStr, version)
    }

    /**
     * Clear the last sent state for a player, forcing the next update to be a full state.
     * Called on reconnect to ensure the client gets a complete state.
     */
    fun clearLastSentState(playerId: EntityId) {
        lastSentState.remove(playerId)
        stateVersions.remove(playerId)
    }

    // =========================================================================
    // Priority Mode Settings
    // =========================================================================

    /**
     * Set priority mode for a player.
     * AUTO = Arena-style smart auto-passing
     * STOPS = Stop on opponent stack items + combat damage
     * FULL_CONTROL = Never auto-pass
     */
    fun setPriorityMode(playerId: EntityId, mode: PriorityMode) {
        priorityModes[playerId] = mode
        logger.info("Player $playerId set priority mode to $mode")
    }

    /**
     * Get priority mode for a player.
     */
    fun getPriorityMode(playerId: EntityId): PriorityMode {
        return priorityModes[playerId] ?: PriorityMode.AUTO
    }

    /**
     * Set full control mode for a player (backward compatibility).
     * When enabled, auto-pass is disabled and the player receives priority at every possible point.
     */
    fun setFullControl(playerId: EntityId, enabled: Boolean) {
        setPriorityMode(playerId, if (enabled) PriorityMode.FULL_CONTROL else PriorityMode.AUTO)
    }

    /**
     * Check if a player has full control mode enabled.
     */
    fun isFullControlEnabled(playerId: EntityId): Boolean {
        return getPriorityMode(playerId) == PriorityMode.FULL_CONTROL
    }

    // =========================================================================
    // Stop Override Settings
    // =========================================================================

    /**
     * Set per-step stop overrides for a player.
     * When a stop is set for a step, auto-pass will not skip that step.
     */
    fun setStopOverrides(playerId: EntityId, myTurnStops: Set<Step>, opponentTurnStops: Set<Step>) {
        stopOverrides[playerId] = StopOverrideSettings(myTurnStops, opponentTurnStops)
        logger.info("Player $playerId set stop overrides: myTurn=$myTurnStops, opponentTurn=$opponentTurnStops")
    }

    /**
     * Get per-step stop overrides for a player.
     */
    fun getStopOverrides(playerId: EntityId): StopOverrideSettings {
        return stopOverrides[playerId] ?: StopOverrideSettings()
    }

    // =========================================================================
    // Persistent Yields (MTGO right-click yields — backlog §C)
    // =========================================================================
    //
    // Yields live on the immutable GameState (not a session-side map), so the pure engine can consult
    // auto-answers during resolution. Because the engine *consumes* them while resolving (it silently
    // auto-answers an optional trigger's may-question, emitting no GameAction), they are NOT captured
    // by the [recordedActions] stream — so we also record every yield mutation against the current
    // action count ([recordedYields]) and re-apply it during reconstruction. Without this, any game in
    // which a player set an "always answer" yield re-simulates differently and the replay truncates at
    // the first auto-answered trigger. Mutations are pure GameState transforms applied under the lock.

    /** Set a persistent yield for [playerId] against [identity]. */
    fun setAbilityYield(
        playerId: EntityId,
        identity: com.wingedsheep.sdk.scripting.AbilityIdentity,
        kind: com.wingedsheep.engine.state.YieldKind
    ) = synchronized(stateLock) {
        gameState = gameState?.withYield(playerId, identity, kind)
        recordYield(com.wingedsheep.gameserver.replay.ReplayYieldOp.SET, playerId, identity, kind)
    }

    /** Revoke every yield [playerId] holds against [identity]. */
    fun clearAbilityYield(
        playerId: EntityId,
        identity: com.wingedsheep.sdk.scripting.AbilityIdentity
    ) = synchronized(stateLock) {
        gameState = gameState?.withoutYield(playerId, identity)
        recordYield(com.wingedsheep.gameserver.replay.ReplayYieldOp.CLEAR_ABILITY, playerId, identity, null)
    }

    /** Drop all of [playerId]'s yields. */
    fun clearAllYields(playerId: EntityId) = synchronized(stateLock) {
        gameState = gameState?.withoutYields(playerId)
        recordYield(com.wingedsheep.gameserver.replay.ReplayYieldOp.CLEAR_ALL, playerId, null, null)
    }

    // =========================================================================
    // Auto-Pass Management
    // =========================================================================

    /**
     * Check if the player with priority should automatically pass.
     * Returns the player ID that should auto-pass, or null if no auto-pass should occur.
     *
     * This implements Arena-style smart priority passing.
     */
    fun getAutoPassPlayer(): EntityId? = synchronized(stateLock) {
        val state = gameState ?: return null

        // Can't auto-pass if game is over
        if (state.gameOver) return null

        // Nobody may pass priority while the game is waiting on a decision. This used to be
        // implicit — getLegalActions returned nothing during a decision — but a mana-payment
        // window (CR 605.3a) now legitimately offers mana abilities, so state it outright.
        if (state.pendingDecision != null) return null

        // Get the player with priority
        val priorityPlayer = state.priorityPlayerId ?: return null

        // The actor is whoever is actually clicking — normally the priority player, or
        // the controller during a hijacked turn. Auto-pass settings track per-seat,
        // so consult the actor's preferences and the actor's legal-actions view.
        val actorPlayer = state.actorFor(priorityPlayer)

        // Check if player has full control enabled - never auto-pass
        val playerMode = getPriorityMode(actorPlayer)
        if (playerMode == PriorityMode.FULL_CONTROL) {
            return null
        }

        // Get legal actions for the actor (returns priority player's actions)
        val legalActions = getLegalActions(actorPlayer)

        // Check if they should auto-pass
        val overrides = getStopOverrides(actorPlayer)

        // Check for legal activated abilities from non-battlefield zones (e.g., graveyard).
        // These are often step-locked (like Undead Gladiator's upkeep-only ability) and the
        // player should always get a chance to use them rather than auto-passing through.
        val hasNonBattlefieldAbility = legalActions.any { actionInfo ->
            actionInfo.actionType == "ActivateAbility" &&
                !actionInfo.isManaAbility &&
                (actionInfo.action as? ActivateAbility)?.let { action ->
                    !state.getBattlefield().contains(action.sourceId)
                } ?: false
        }

        val effectiveOverrides = if (hasNonBattlefieldAbility) {
            val isMyTurn = state.isActiveTurnFor(priorityPlayer)
            if (isMyTurn) {
                overrides.copy(myTurnStops = overrides.myTurnStops + state.step)
            } else {
                overrides.copy(opponentTurnStops = overrides.opponentTurnStops + state.step)
            }
        } else {
            overrides
        }

        return if (autoPassManager.shouldAutoPass(state, priorityPlayer, legalActions, effectiveOverrides.myTurnStops, effectiveOverrides.opponentTurnStops, stopsMode = playerMode == PriorityMode.STOPS)) {
            priorityPlayer
        } else {
            null
        }
    }

    /**
     * Check if undo is available for a player.
     * Returns true if a checkpoint exists and the player is the priority player of the checkpoint state.
     */
    fun isUndoAvailable(playerId: EntityId): Boolean {
        val checkpoint = undoCheckpoint ?: return false
        return checkpoint.priorityPlayerId == playerId
    }

    /**
     * Execute an undo, restoring the game state to the checkpoint.
     * Only the player who took the undoable action can undo.
     */
    fun executeUndo(playerId: EntityId): ActionResult = synchronized(stateLock) {
        val checkpoint = undoCheckpoint ?: return ActionResult.Failure("No undo available")

        if (checkpoint.priorityPlayerId != playerId) {
            return ActionResult.Failure("Not your action to undo")
        }

        gameState = checkpoint
        // Roll the replay log back to the actions that produced the restored state, so a later
        // reconstruction replays exactly this history. Yields recorded after the rollback point are
        // dropped too — they were set against actions that no longer exist.
        undoCheckpointActionCount?.let { target ->
            while (recordedActions.size > target) recordedActions.removeAt(recordedActions.size - 1)
            recordedYields.removeIf { it.afterActionCount > target }
            recordedCheckpoints.removeIf { it.afterActionCount > target }
        }
        clearCheckpoint()
        logger.info("Player $playerId undid their last action")
        ActionResult.Success(checkpoint, emptyList())
    }

    /**
     * Apply the engine's undo checkpoint policy.
     * The engine decides what to do with the checkpoint based on game rules;
     * the server just follows the policy mechanically.
     */
    private fun applyUndoPolicy(
        policy: UndoCheckpointAction,
        action: GameAction,
        preActionState: GameState,
        playerId: EntityId
    ) {
        // [recordedActions] size right now == the number of actions that produced [preActionState]
        // (the action currently being processed has not been recorded yet — that happens after the
        // policy is applied). Capturing it alongside each checkpoint lets [executeUndo] truncate the
        // replay log back to the restored state.
        val currentActionCount = recordedActions.size
        when (policy) {
            UndoCheckpointAction.SET_CHECKPOINT -> {
                // For DeclareAttackers, use the pre-combat state so undo goes back to main phase
                if (action is DeclareAttackers && preCombatState != null) {
                    undoCheckpoint = preCombatState
                    undoCheckpointActionCount = preCombatActionCount
                } else {
                    undoCheckpoint = preActionState
                    undoCheckpointActionCount = currentActionCount
                }
                undoCheckpointOwner = playerId
            }
            UndoCheckpointAction.SET_PRECOMBAT_CHECKPOINT -> {
                preCombatState = preActionState
                preCombatActionCount = currentActionCount
                undoCheckpoint = preActionState
                undoCheckpointActionCount = currentActionCount
                undoCheckpointOwner = playerId
            }
            UndoCheckpointAction.SET_IF_NO_EXISTING_CHECKPOINT -> {
                if (undoCheckpoint == null) {
                    undoCheckpoint = preActionState
                    undoCheckpointActionCount = currentActionCount
                    undoCheckpointOwner = playerId
                }
            }
            UndoCheckpointAction.PRESERVE -> { /* no change */ }
            UndoCheckpointAction.CLEAR -> clearCheckpoint()
        }
    }

    /**
     * Clear all undo checkpoint state.
     */
    private fun clearCheckpoint() {
        undoCheckpoint = null
        undoCheckpointOwner = null
        preCombatState = null
        undoCheckpointActionCount = null
        preCombatActionCount = null
    }

    /**
     * Execute auto-pass for a player.
     * Returns the result of the PassPriority action.
     */
    fun executeAutoPass(playerId: EntityId): ActionResult = synchronized(stateLock) {
        val state = gameState ?: return ActionResult.Failure("Game not started")

        // Verify this player has priority
        if (state.priorityPlayerId != playerId) {
            return ActionResult.Failure("Player does not have priority")
        }

        // During combat declaration steps, submit an empty declaration instead of PassPriority.
        // The engine requires declarations before allowing priority to pass.
        // Turn ownership is team-wide in a shared team turn (CR 805.10a) — the same gate the
        // engine's PassPriorityHandler / DeclareBlockersHandler use — so the active player's
        // teammate isn't handed a DeclareBlockers (or a bare pass) the engine will refuse.
        val action: GameAction = when {
            state.step == Step.DECLARE_ATTACKERS && state.isActiveTurnFor(playerId) &&
                state.getEntity(playerId)?.get<AttackersDeclaredThisCombatComponent>() == null ->
                DeclareAttackers(playerId, emptyMap())

            state.step == Step.DECLARE_BLOCKERS && !state.isActiveTurnFor(playerId) &&
                state.getEntity(playerId)?.get<BlockersDeclaredThisCombatComponent>() == null ->
                DeclareBlockers(playerId, emptyMap())

            else -> PassPriority(playerId)
        }
        val (result, undoPolicy) = actionProcessor.process(state, action)

        val error = result.error
        if (error != null) {
            return ActionResult.Failure(error)
        }

        // Same rule as executeAction: a bare PassPriority from the opponent is benign and
        // preserves the checkpoint. The auto-pass path may instead submit an empty
        // DeclareAttackers/DeclareBlockers — those are real combat declarations and do clear.
        if (undoCheckpoint != null && undoCheckpointOwner != null
            && playerId != undoCheckpointOwner
            && action !is PassPriority) {
            clearCheckpoint()
        }

        // Apply the engine's undo policy
        applyUndoPolicy(undoPolicy, action, state, playerId)

        gameState = result.state
        recordAction(action)
        val pendingDecision = result.pendingDecision
        return if (pendingDecision != null) {
            ActionResult.PausedForDecision(result.state, pendingDecision, result.events)
        } else {
            ActionResult.Success(result.state, result.events)
        }
    }

    /**
     * Check if the game is over.
     */
    fun isGameOver(): Boolean = gameState?.gameOver == true

    /**
     * Get the winner ID if the game is over. In a team game this is a *representative* of the
     * winning team (the engine records one seat); use [getWinnerIds] for everyone who won.
     */
    fun getWinnerId(): EntityId? = gameState?.winnerId

    /**
     * Every seat that won: the winner's whole still-in team (CR 810.8a — a team wins together), or
     * just the winner outside a team game. Empty for a draw or an unfinished game. Anything that
     * labels a seat as having won or lost — the GameOver message, match history, standings — has
     * to read this rather than compare against the single [getWinnerId], or the winning team's
     * other head is told they lost.
     */
    fun getWinnerIds(): List<EntityId> {
        val state = gameState ?: return emptyList()
        val winner = state.winnerId ?: return emptyList()
        return state.teamOf(winner).filter {
            state.getEntity(it)?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() != true
        }
    }

    /**
     * Determine the reason for game over.
     */
    fun getGameOverReason(): GameOverReason? {
        val state = gameState ?: return null
        if (!state.gameOver) return null

        // If no winner, it's a draw (both players lost simultaneously)
        if (state.winnerId == null) {
            return GameOverReason.DRAW
        }

        // Find why the losing side lost. In Two-Headed Giant a whole team is defeated, so prefer a
        // meaningful cause over the propagated TEAM_DEFEATED marker (CR 810.8a) — mirrors the
        // engine's GameEndCheck reason derivation. (Team-aware winner display is Phase 6.)
        val lostReasons = state.turnOrder
            .mapNotNull { state.getEntity(it)?.get<PlayerLostComponent>()?.reason }
        val reason = lostReasons.firstOrNull { it != LossReason.TEAM_DEFEATED }
            ?: lostReasons.firstOrNull()

        return gameOverReasonFor(reason)
    }

    /** Engine loss reason → the client-facing reason code. */
    private fun gameOverReasonFor(reason: LossReason?): GameOverReason = when (reason) {
        LossReason.LIFE_ZERO -> GameOverReason.LIFE_ZERO
        LossReason.EMPTY_LIBRARY -> GameOverReason.DECK_OUT
        LossReason.POISON_COUNTERS -> GameOverReason.POISON_COUNTERS
        LossReason.CONCESSION -> GameOverReason.CONCESSION
        LossReason.CARD_EFFECT -> GameOverReason.CARD_EFFECT
        LossReason.COMMANDER_DAMAGE -> GameOverReason.COMMANDER_DAMAGE
        LossReason.TEAM_DEFEATED -> GameOverReason.CARD_EFFECT
        null -> GameOverReason.LIFE_ZERO // Fallback
    }


    sealed interface ActionResult {
        data class Success(
            val state: GameState,
            val events: List<GameEvent>
        ) : ActionResult

        data class Failure(val reason: String) : ActionResult

        data class PausedForDecision(
            val state: GameState,
            val decision: PendingDecision,
            val events: List<GameEvent>
        ) : ActionResult
    }

    // =========================================================================
    // Replay Recording
    // =========================================================================

    /**
     * The one thing every applied action passes through: append it to the compact replay log, and
     * ask the [stallGuard] whether this game is still going anywhere.
     *
     * Both halves are bounded on purpose, and for different reasons — see [appendToReplayLog] and
     * [enforceProgress].
     */
    private fun recordAction(action: GameAction) {
        appendToReplayLog(action)
        enforceProgress()
    }

    /**
     * Append to the replay log, up to [ReplayRecordingPolicy.MAX_RECORDED_ACTIONS] actions.
     *
     * A replay costs ~7 stored bytes per action, so length is not what makes a runaway game
     * expensive — this list is. It is a [CopyOnWriteArrayList] (read by the flusher off the game
     * thread, appended under the state lock), so every append copies the whole array: the recording
     * costs O(n²) element copies over a game, which is nothing at the few hundred actions a real
     * game takes and ruinous at six figures. On top of that the flusher re-encodes the entire log
     * every few seconds for as long as the game lasts.
     *
     * So the recording gives up rather than the game: past the cap the log is frozen and marked
     * [replayTruncated], which is the same "keep the honest shorter prefix" outcome a lost flush
     * already produces (see [restoreReplayRecording]) and which the viewer reports as a partial
     * recording. Freezing is permanent for the session — an undo may shorten the log afterwards
     * (leaving a valid, shorter prefix) but nothing may extend it again, or the record would have a
     * hole in the middle and reconstruct a game nobody played.
     */
    private fun appendToReplayLog(action: GameAction) {
        if (replayTruncated) return
        if (recordedActions.size >= replayActionCap) {
            replayTruncated = true
            logger.warn(
                "Replay recording for $sessionId hit $replayActionCap actions — freezing the log; " +
                    "the game continues but its replay stops here"
            )
            return
        }
        recordedActions.add(action)
        stampCheckpointIfDue()
    }

    /**
     * End the game as a draw when it has stopped making progress — see [GameStallGuard] for the two
     * shapes this catches and CR 104.4b for why a draw is the right verdict.
     *
     * The draw is expressed the way the engine expresses one (`gameOver` with no winner), so every
     * caller's existing `isGameOver()` check finalizes the match through the normal game-over path:
     * players and spectators are notified, the replay is saved, the lobby callback fires, the
     * session is cleaned up. Nothing needs to know this particular game over was our idea except
     * [stallMessage], which explains it to the players.
     */
    private fun enforceProgress() {
        val state = gameState ?: return
        if (state.gameOver) return
        val stall = stallGuard.onActionApplied(state) ?: return
        logger.error(
            "Game $sessionId is not making progress (${stall.code}) — ending it as a draw. " +
                "This is a backstop for a loop the AI's own guard missed; the replay is worth reading."
        )
        gameState = state.copy(gameOver = true, winnerId = null)
    }

    /**
     * Why this game was abandoned, for the game-over overlay, or null for a game that ended on its
     * own terms. Preferred over the engine's stock reason text because "Draw" alone reads like a
     * rules outcome the players caused.
     */
    fun stallMessage(): String? = stallGuard.stall?.playerMessage

    /**
     * Record that [playerId]'s action was rejected and no fallback could be applied either, and
     * report whether that seat has run out of moves it will make — see
     * [GameStallGuard.onActionRejected]. Nothing reached the engine, so this is invisible to
     * [recordAction] and has to be reported by the caller that saw the rejection.
     */
    fun noteActionRejected(playerId: EntityId): Boolean = synchronized(stateLock) {
        stallGuard.onActionRejected(playerId)
    }

    /**
     * Every N actions, fingerprint the live position and keep it with the recording.
     *
     * The input log alone can't tell a faithful re-simulation from a drifted one — both just apply
     * actions. These stamps are what makes the difference detectable later, when the engine has
     * moved on and "the actions still applied" no longer implies "the same game came out". Costs one
     * short SHA-256 per 20 actions; see [com.wingedsheep.gameserver.replay.ReplayFingerprint].
     */
    private fun stampCheckpointIfDue() {
        if (replaySetup == null) return
        val count = recordedActions.size
        if (count % com.wingedsheep.gameserver.replay.ReplayRecordingPolicy.CHECKPOINT_EVERY_ACTIONS != 0) return
        val state = gameState ?: return
        recordedCheckpoints.add(
            com.wingedsheep.gameserver.replay.ReplayCheckpoint(
                afterActionCount = count,
                fingerprint = com.wingedsheep.gameserver.replay.ReplayFingerprint.of(state),
            )
        )
    }

    /**
     * Capture a persistent-yield mutation against the current action count, but only while the game is
     * being recorded for replay (a [replaySetup] exists). Injected dev-scenario/hotseat sessions aren't
     * replayable, so their yields needn't be recorded.
     */
    private fun recordYield(
        op: com.wingedsheep.gameserver.replay.ReplayYieldOp,
        playerId: EntityId,
        identity: com.wingedsheep.sdk.scripting.AbilityIdentity?,
        kind: com.wingedsheep.engine.state.YieldKind?,
    ) {
        if (replaySetup == null) return
        recordedYields.add(
            com.wingedsheep.gameserver.replay.ReplayYieldEntry(
                afterActionCount = recordedActions.size,
                playerId = playerId.value,
                op = op,
                identity = identity,
                kind = kind,
            )
        )
    }

    /**
     * The reproducible setup captured at [startGame], or null for games whose state was injected
     * directly (dev scenarios / hotseat) and therefore can't be re-simulated from inputs.
     */
    fun getReplaySetup(): com.wingedsheep.gameserver.replay.ReplaySetup? = replaySetup

    /** The ordered input stream applied to this game. */
    fun getRecordedActions(): List<GameAction> = recordedActions.toList()

    /** The persistent-yield mutations applied to this game, in order, for replay reconstruction. */
    fun getReplayYields(): List<com.wingedsheep.gameserver.replay.ReplayYieldEntry> = recordedYields.toList()

    /**
     * Whether this game's recording was frozen before the game ended (see [appendToReplayLog]), so
     * the stored record is an honest prefix rather than the whole game.
     */
    fun isReplayTruncated(): Boolean = replayTruncated

    /** Sparse position fingerprints taken while this game was played. */
    fun getReplayCheckpoints(): List<com.wingedsheep.gameserver.replay.ReplayCheckpoint> =
        recordedCheckpoints.toList()

    /**
     * The compiled definitions of every card in this game's decks, archived with the replay so it
     * re-simulates against the card code it actually ran on rather than whatever the corpus looks
     * like when someone watches it — see [com.wingedsheep.gameserver.replay.ReplayCardPin].
     *
     * Computed once, on first use (the first flush or game over) rather than at [startGame], so the
     * serialization cost lands off the game-start path and never at all for games that end before
     * they're worth recording.
     *
     * Serialization happens *outside* [stateLock]: it reads only the setup's decklists, which never
     * change once the game has started, and it is tens of milliseconds of JSON per game — long
     * enough that holding the game's lock for it would visibly stall play on the first flush. Two
     * callers racing here both capture and one result is published; wasted work, never wrong.
     */
    fun getPinnedCards(): List<String> {
        pinnedCards?.let { return it }
        val setup = getReplaySetup() ?: return emptyList()
        val captured = com.wingedsheep.gameserver.replay.ReplayCardPin.capture(cardRegistry, setup)
        return synchronized(stateLock) { pinnedCards ?: captured.also { pinnedCards = it } }
    }

    /**
     * One consistent read of everything the flusher needs, so the recording it stores and the
     * fingerprint it stores describe the *same* position — see
     * [com.wingedsheep.gameserver.replay.ReplayRecordingSnapshot] for why sampling them separately
     * is unsound. Null for sessions that aren't being recorded, or aren't started yet.
     */
    internal fun replayRecordingSnapshot(): com.wingedsheep.gameserver.replay.ReplayRecordingSnapshot? =
        synchronized(stateLock) {
            val setup = replaySetup ?: return null
            val state = gameState ?: return null
            com.wingedsheep.gameserver.replay.ReplayRecordingSnapshot(
                setup = setup,
                actions = recordedActions.toList(),
                yields = recordedYields.toList(),
                checkpoints = recordedCheckpoints.toList(),
                fingerprint = com.wingedsheep.gameserver.replay.ReplayFingerprint.of(state),
                startedAt = replayStartedAt,
                gameOver = state.gameOver,
                truncated = replayTruncated,
            )
        }

    /**
     * Total number of replay frames: the initial state plus one per recorded action. Zero until the
     * game is started via [startGame] (injected-state sessions have no setup and aren't replayable).
     */
    fun getReplayFrameCount(): Int = if (replaySetup != null) 1 + recordedActions.size else 0

    // =========================================================================
    // Test Support (for scenario-based testing)
    // =========================================================================

    /**
     * Replace the runaway backstops with tighter ones.
     *
     * **Testing only.** The shipped thresholds are set so that only a game that has already gone
     * wrong can reach them, which also puts them out of reach of a test that would have to play
     * tens of thousands of actions to get there. Must be called before the game starts — the guard
     * carries the counters, so swapping it mid-game resets them.
     */
    internal fun tightenBackstopsForTesting(guard: GameStallGuard, replayCap: Int) {
        stallGuard = guard
        replayActionCap = replayCap
    }


    /**
     * Inject a pre-built game state for testing purposes.
     * This allows tests to set up specific game scenarios without playing through.
     *
     * **WARNING:** This method is for testing only. Do not use in production code.
     *
     * @param state The game state to inject
     * @param testPlayers Map of player IDs to PlayerSession instances
     */
    fun injectStateForTesting(state: GameState, testPlayers: Map<EntityId, PlayerSession>) {
        synchronized(stateLock) {
            gameState = state
            players.clear()
            players.putAll(testPlayers)
            testPlayers.forEach { (_, session) ->
                session.currentGameSessionId = sessionId
            }
        }
    }

    /**
     * Inject a pre-built game state for dev scenario testing.
     * Unlike injectStateForTesting, this doesn't require PlayerSession objects,
     * allowing scenarios to be created before players connect via WebSocket.
     *
     * Players will be associated when they connect using associatePlayer().
     *
     * **WARNING:** This method is for development testing only.
     *
     * @param state The game state to inject
     */
    fun injectStateForDevScenario(state: GameState) {
        synchronized(stateLock) {
            gameState = state
            players.clear()
        }
    }

    /**
     * Enable single-client "hotseat" (play-against-yourself) for this session: route the
     * input authority of *every* seat to [controllerId] by stamping a
     * [HotseatControlComponent] onto each player entity. One connection then receives every
     * decision and may submit actions for both seats. Resource ownership is unaffected (see
     * [HotseatControlComponent] / [GameState.actorFor]).
     *
     * Must be called after the scenario state is injected. Only the seat matching
     * [controllerId] is expected to connect over WebSocket.
     */
    fun enableHotseat(controllerId: EntityId) {
        synchronized(stateLock) {
            val current = gameState ?: return
            var next = current
            for (playerId in current.turnOrder) {
                next = next.updateEntity(playerId) { it.with(HotseatControlComponent(controllerId)) }
            }
            gameState = next
        }
    }

    /**
     * Reset the game state for dev scenario testing while preserving connected player sessions.
     * This allows resetting to a new scenario without requiring players to reconnect.
     *
     * **WARNING:** This method is for development testing only.
     *
     * @param state The new game state to inject
     */
    fun resetStateForDevScenario(state: GameState) {
        synchronized(stateLock) {
            gameState = state
            undoCheckpoint = null
            gameLogs.clear()
            lastProcessedMessageId.clear()
            lastSentState.clear()
            // Players map is preserved so connected sessions remain valid
        }
    }

    /**
     * Get the raw game state for testing assertions.
     * **WARNING:** This method is for testing only.
     */
    fun getStateForTesting(): GameState? = gameState

    /**
     * Whether this session resolves set-scoped token art, for testing assertions.
     *
     * [tokenArtRegistry] is an optional constructor argument, so a code path that builds a session
     * and forgets it degrades silently: every token still gets *an* image, just the engine-wide
     * generic one for its creature type. Nothing fails, the art is merely wrong — which is exactly
     * how the scenario path shipped without it. Each site that creates a session should assert this.
     *
     * **WARNING:** This method is for testing only.
     */
    fun hasTokenArtForTesting(): Boolean = services.tokenArtRegistry != null

    /**
     * Read-only snapshot of the current game state. Used by the engine AI controller
     * to evaluate board positions and simulate actions.
     *
     * Thread-safe: GameState is immutable, so reading the reference is safe.
     */
    fun getStateSnapshot(): GameState? = gameState

    /** Get deck list for a specific player. Used by engine AI to know the opponent's deck. */
    fun getDeckList(playerId: EntityId): List<String>? = deckLists[playerId]

    /**
     * The deck a seat *started the game with*, for anything that has to describe the game after the
     * fact (match-history recording, re-wiring an AI after a restart). Prefers the live submitted
     * deck and falls back to the copy frozen into the replay setup at [startGame], which is captured
     * once and never mutated for the life of the session — so a seat's deck can still be reported
     * even if its live entry was dropped (a pre-game leave, or a reseat bug like the one that used to
     * blank multiplayer decks). Null only for sessions that never recorded a setup (dev scenario /
     * hotseat) and have no live deck either.
     */
    fun getStartingDeckList(playerId: EntityId): List<String>? =
        deckLists[playerId]
            ?: replaySetup?.players?.firstOrNull { it.playerId == playerId.value }?.deck?.cards

    // =========================================================================
    // Persistence Support (for Redis caching)
    // =========================================================================

    /**
     * Get the current game state for persistence.
     */
    internal fun getStateForPersistence(): GameState? = gameState

    /**
     * Get the deck lists for persistence.
     */
    internal fun getDeckListsForPersistence(): Map<EntityId, List<String>> = deckLists.toMap()

    /**
     * Get the per-player sideboards for persistence. Empty for almost every session.
     */
    internal fun getSideboardsForPersistence(): Map<EntityId, List<String>> = sideboards.toMap()

    /**
     * Get the game logs for persistence.
     */
    internal fun getLogsForPersistence(): Map<EntityId, List<ClientEvent>> =
        gameLogs.mapValues { it.value.toList() }

    /**
     * Get the last processed message IDs for persistence.
     */
    internal fun getLastMessageIdsForPersistence(): Map<EntityId, String> =
        lastProcessedMessageId.toMap()

    /**
     * Restore session state from persistence.
     * Called when loading a session from Redis after server restart.
     *
     * Note: Player sessions are NOT restored here. Players reconnect and
     * re-associate with their identity via token.
     */
    internal fun restoreFromPersistence(
        state: GameState?,
        decks: Map<EntityId, List<String>>,
        logs: Map<EntityId, MutableList<ClientEvent>>,
        lastIds: Map<EntityId, String>,
        sideboardLists: Map<EntityId, List<String>> = emptyMap()
    ) {
        synchronized(stateLock) {
            gameState = state
            deckLists.clear()
            deckLists.putAll(decks)
            sideboards.clear()
            sideboards.putAll(sideboardLists)
            gameLogs.clear()
            gameLogs.putAll(logs)
            lastProcessedMessageId.clear()
            lastProcessedMessageId.putAll(lastIds)
            lastSentState.clear()
        }
    }

    /**
     * Resume the compact-replay recording of a game interrupted by a restart, so it is still saved
     * as a replay when it finishes.
     *
     * The recording is flushed to the store periodically rather than on every action, so the stored
     * log can be *behind* the live state we just recovered. Appending the rest of the game onto a
     * short prefix would produce a record of a game nobody played — worse than no record, because it
     * looks fine. [expectedFingerprint] is the position the flush captured; if the recovered state
     * doesn't match it, actions were lost and we stop recording here, keeping the shorter but honest
     * replay that was already stored.
     *
     * A null [expectedFingerprint] means the flush couldn't capture one, so there is nothing to
     * check against and we resume unverified — the one path where a stale prefix could still be
     * extended. The flusher always writes a fingerprint for a recorded session, so this should be
     * unreachable; it is logged rather than assumed so it can't become reachable quietly.
     *
     * Returns whether recording continues.
     */
    internal fun restoreReplayRecording(
        record: com.wingedsheep.gameserver.replay.CompactReplay,
        expectedFingerprint: String?,
    ): Boolean = synchronized(stateLock) {
        val live = gameState
        val actual = live?.let { com.wingedsheep.gameserver.replay.ReplayFingerprint.of(it) }
        if (expectedFingerprint == null) {
            logger.warn(
                "Replay recording for $sessionId has no stored fingerprint " +
                    "(${record.actions.size} actions at $actual) — resuming without verifying that " +
                    "the stored log matches the recovered state"
            )
        }
        if (expectedFingerprint != null && actual != expectedFingerprint) {
            logger.warn(
                "Replay recording for $sessionId is behind the recovered state " +
                    "(stored ${record.actions.size} actions at $expectedFingerprint, live at $actual) — " +
                    "keeping the stored prefix and stopping recording"
            )
            replaySetup = null
            return false
        }

        replaySetup = record.setup
        recordedActions.clear()
        recordedActions.addAll(record.actions)
        recordedYields.clear()
        recordedYields.addAll(record.yields)
        recordedCheckpoints.clear()
        recordedCheckpoints.addAll(record.checkpoints)
        // A record frozen by the size cap before the restart stays frozen: the actions played
        // between the cap and now were never recorded, so appending from here would splice a hole
        // into the log exactly as extending a stale prefix would.
        replayTruncated = record.truncated
        replayStartedAt = runCatching { Instant.parse(record.startedAt) }.getOrNull()
        return true
    }

    /**
     * Seat this player session — either a first association after a restore, or a reconnecting player
     * being put back in the chair they already had.
     *
     * Seats are keyed by [PlayerSession.playerId], so this replaces any existing entry in place and
     * leaves everything that describes the seat (deck, sideboard, commander, per-player log) alone.
     * It is the whole reconnect operation on its own: don't call [removePlayer] first.
     */
    fun associatePlayer(playerSession: PlayerSession) {
        players[playerSession.playerId] = playerSession
        playerSession.currentGameSessionId = sessionId
    }

    /**
     * Store a player's info for persistence.
     * Should be called when a player joins the game.
     */
    fun setPlayerPersistenceInfo(
        playerId: EntityId,
        playerName: String,
        token: String,
        isAi: Boolean = false,
        aiModelOverride: String? = null
    ) {
        playerPersistenceInfo[playerId] = PlayerPersistenceInfo(playerName, token, isAi, aiModelOverride)
    }

    /**
     * Get all stored player info for persistence.
     */
    fun getPlayerPersistenceInfo(): Map<EntityId, PlayerPersistenceInfo> = playerPersistenceInfo.toMap()

    /**
     * Restore player info from persistence.
     */
    internal fun restorePlayerPersistenceInfo(info: Map<EntityId, PlayerPersistenceInfo>) {
        playerPersistenceInfo.clear()
        playerPersistenceInfo.putAll(info)
    }
}
