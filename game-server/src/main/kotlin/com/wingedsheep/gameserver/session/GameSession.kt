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
import com.wingedsheep.gameserver.replay.SpectatorReplayDelta
import com.wingedsheep.gameserver.replay.SpectatorReplayDiffCalculator
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
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
    private val useHandSmoother: Boolean = false
) {
    /** Backward-compatible constructor: wraps a CardRegistry in EngineServices. */
    constructor(
        sessionId: String = UUID.randomUUID().toString(),
        cardRegistry: CardRegistry,
        stateTransformer: ClientStateTransformer = ClientStateTransformer(cardRegistry),
        useHandSmoother: Boolean = false,
        debugMode: Boolean = false
    ) : this(sessionId, EngineServices(cardRegistry), if (debugMode) ClientStateTransformer(cardRegistry, debugMode = true) else stateTransformer, useHandSmoother)

    private val cardRegistry: CardRegistry get() = services.cardRegistry
    // Lock for synchronizing state modifications to prevent lost updates
    private val stateLock = Any()

    @Volatile
    private var gameState: GameState? = null

    /** Checkpoint for undoing the last non-respondable action (e.g., play land, declare attackers) */
    @Volatile
    private var undoCheckpoint: GameState? = null

    /** The player who owns the current undo checkpoint */
    @Volatile
    private var undoCheckpointOwner: EntityId? = null

    /** State saved when the active player passes priority in precombat main, used to undo combat entry */
    @Volatile
    private var preCombatState: GameState? = null
    /** Set code used for quick game deck generation (so joining player uses the same set) */
    @Volatile
    var quickGameSetCode: String? = null

    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, List<String>>()
    private val spectators = mutableSetOf<PlayerSession>()

    /** Player info for persistence (playerId -> (playerName, token)) */
    private val playerPersistenceInfo = mutableMapOf<EntityId, PlayerPersistenceInfo>()

    data class PlayerPersistenceInfo(
        val playerName: String,
        val token: String,
        val isAi: Boolean = false,
        val aiModelOverride: String? = null
    )

    private val actionProcessor = ActionProcessor(services)
    private val gameInitializer = GameInitializer(cardRegistry)
    private val autoPassManager = AutoPassManager()
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

    // Replay recording — stores initial snapshot + diffs for memory efficiency
    private var replayInitialSnapshot: ServerMessage.SpectatorStateUpdate? = null
    private var replayLastSnapshot: ServerMessage.SpectatorStateUpdate? = null
    private val replayDeltas = CopyOnWriteArrayList<SpectatorReplayDelta>()
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

    val player1: PlayerSession? get() = players.values.firstOrNull()
    val player2: PlayerSession? get() = players.values.drop(1).firstOrNull()

    val isFull: Boolean get() = players.size >= 2
    val isReady: Boolean get() = players.size == 2 && deckLists.size == 2
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
    fun addPlayer(playerSession: PlayerSession, deckList: Map<String, Int>): EntityId {
        require(!isFull) { "Game session is full" }

        val playerId = playerSession.playerId
        players[playerId] = playerSession

        // Convert deck list map to flat list of card names
        val cards = deckList.flatMap { (cardName, count) ->
            List(count) { cardName }
        }
        deckLists[playerId] = cards
        playerSession.currentGameSessionId = sessionId

        return playerId
    }

    /**
     * Remove a player from the session.
     */
    fun removePlayer(playerId: EntityId) {
        players[playerId]?.currentGameSessionId = null
        players.remove(playerId)
        deckLists.remove(playerId)
    }

    /**
     * Get the opponent's player ID.
     */
    fun getOpponentId(playerId: EntityId): EntityId? {
        return players.keys.firstOrNull { it != playerId }
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
     * Add a spectator to this game session.
     */
    fun addSpectator(spectator: PlayerSession) {
        spectators.add(spectator)
    }

    /**
     * Remove a spectator from this game session.
     */
    fun removeSpectator(spectator: PlayerSession) {
        spectators.remove(spectator)
    }

    /**
     * Get all spectators.
     */
    fun getSpectators(): Set<PlayerSession> = spectators.toSet()

    /**
     * Get player names for spectator display.
     */
    fun getPlayerNames(): Pair<String, String>? {
        val p1 = player1 ?: return null
        val p2 = player2 ?: return null
        return Pair(p1.playerName, p2.playerName)
    }

    /**
     * Get current life totals for spectator display.
     */
    fun getLifeTotals(): Pair<Int, Int>? {
        val state = gameState ?: return null
        val p1Id = player1?.playerId ?: return null
        val p2Id = player2?.playerId ?: return null
        val p1Life = state.getEntity(p1Id)?.get<LifeTotalComponent>()?.life ?: 20
        val p2Life = state.getEntity(p2Id)?.get<LifeTotalComponent>()?.life ?: 20
        return Pair(p1Life, p2Life)
    }

    fun buildSpectatorState(): ServerMessage.SpectatorStateUpdate? {
        val state = gameState ?: return null
        val p1 = player1 ?: return null
        val p2 = player2 ?: return null
        return spectatorStateBuilder.buildState(state, p1, p2, sessionId)
    }

    /**
     * Start the game. Both players must have joined with deck lists.
     * Initializes the game with the new engine - mulligan phase is handled by the engine.
     */
    fun startGame(): GameState {
        require(isReady) { "Game session not ready - need 2 players with deck lists" }

        val playerConfigs = players.map { (playerId, session) ->
            PlayerConfig(
                name = session.playerName,
                deck = Deck(deckLists[playerId]!!),
                playerId = playerId  // Pass existing player ID to the engine
            )
        }

        val config = GameConfig(
            players = playerConfigs,
            useHandSmoother = useHandSmoother
        )

        val result = gameInitializer.initializeGame(config)
        gameState = result.state
        return result.state
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
        val cards = if (state != null) {
            hand.associateWith { entityId ->
                val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
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
        } else {
            emptyMap()
        }
        val isOnThePlay = gameState?.activePlayerId == playerId
        return ServerMessage.MulliganDecision(
            hand = hand,
            mulliganCount = count,
            cardsToPutOnBottom = count,
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
        val cards = if (state != null) {
            hand.associateWith { entityId ->
                val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
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
        } else {
            emptyMap()
        }
        return ServerMessage.ChooseBottomCards(
            hand = hand,
            cardsToPutOnBottom = count,
            cards = cards
        )
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

        // Idempotency check: if this messageId was already processed, skip
        if (messageId != null) {
            val lastId = lastProcessedMessageId[playerId]
            if (lastId == messageId) {
                return ActionResult.Failure("Duplicate message")
            }
        }

        // If the opponent takes any action, invalidate the undo checkpoint.
        // The opponent has seen the game state after the undoable action, so allowing undo
        // would revert information the opponent has already processed.
        if (undoCheckpoint != null && undoCheckpointOwner != null && playerId != undoCheckpointOwner) {
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
        if (state.priorityPlayerId != playerId) return emptyList()
        if (state.pendingDecision != null) return emptyList()
        val engineActions = legalActionEnumerator.enumerate(state, playerId)
        return legalActionEnricher.enrich(engineActions, state, playerId)
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

        // Include pending decision only for the player who needs to make it
        // Enrich with imageUri from card registry since engine doesn't have access to metadata
        val pendingDecision = state.pendingDecision?.takeIf { it.playerId == playerId }?.let {
            decisionEnricher.enrich(it, state)
        }

        // Calculate next stop point for the Pass button (only if player has priority)
        val playerOverrides = getStopOverrides(playerId)
        val playerMode = getPriorityMode(playerId)
        val nextStopPoint = if (state.priorityPlayerId == playerId && playerMode != PriorityMode.FULL_CONTROL) {
            val hasMeaningfulActions = legalActions.any { action ->
                action.actionType != "PassPriority" &&
                (!action.isManaAbility || action.additionalCostInfo?.costType == "SacrificePermanent")
            }
            autoPassManager.getNextStopPoint(state, playerId, hasMeaningfulActions, myTurnStops = playerOverrides.myTurnStops, opponentTurnStops = playerOverrides.opponentTurnStops, stopsMode = playerMode == PriorityMode.STOPS)
        } else {
            null
        }

        // Include opponent decision status for the OTHER player (so they know opponent is making a choice)
        val opponentDecisionStatus = state.pendingDecision?.takeIf { it.playerId != playerId }?.let {
            decisionEnricher.createOpponentDecisionStatus(it)
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

        // Get the player with priority
        val priorityPlayer = state.priorityPlayerId ?: return null

        // Check if player has full control enabled - never auto-pass
        val playerMode = getPriorityMode(priorityPlayer)
        if (playerMode == PriorityMode.FULL_CONTROL) {
            return null
        }

        // Get legal actions for that player
        val legalActions = getLegalActions(priorityPlayer)

        // Check if they should auto-pass
        val overrides = getStopOverrides(priorityPlayer)

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
            val isMyTurn = state.activePlayerId == priorityPlayer
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
        when (policy) {
            UndoCheckpointAction.SET_CHECKPOINT -> {
                // For DeclareAttackers, use the pre-combat state so undo goes back to main phase
                undoCheckpoint = if (action is DeclareAttackers && preCombatState != null) {
                    preCombatState
                } else {
                    preActionState
                }
                undoCheckpointOwner = playerId
            }
            UndoCheckpointAction.SET_PRECOMBAT_CHECKPOINT -> {
                preCombatState = preActionState
                undoCheckpoint = preActionState
                undoCheckpointOwner = playerId
            }
            UndoCheckpointAction.SET_IF_NO_EXISTING_CHECKPOINT -> {
                if (undoCheckpoint == null) {
                    undoCheckpoint = preActionState
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
        val action: GameAction = when {
            state.step == Step.DECLARE_ATTACKERS && playerId == state.activePlayerId &&
                state.getEntity(playerId)?.get<AttackersDeclaredThisCombatComponent>() == null ->
                DeclareAttackers(playerId, emptyMap())

            state.step == Step.DECLARE_BLOCKERS && playerId != state.activePlayerId &&
                state.getEntity(playerId)?.get<BlockersDeclaredThisCombatComponent>() == null ->
                DeclareBlockers(playerId, emptyMap())

            else -> PassPriority(playerId)
        }
        val (result, undoPolicy) = actionProcessor.process(state, action)

        val error = result.error
        if (error != null) {
            return ActionResult.Failure(error)
        }

        // Opponent auto-passing invalidates checkpoint (opponent has acted)
        if (undoCheckpoint != null && undoCheckpointOwner != null && playerId != undoCheckpointOwner) {
            clearCheckpoint()
        }

        // Apply the engine's undo policy
        applyUndoPolicy(undoPolicy, action, state, playerId)

        gameState = result.state
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
     * Get the winner ID if the game is over.
     */
    fun getWinnerId(): EntityId? = gameState?.winnerId

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

        // Find the losing player (the one who is not the winner)
        val loserId = state.turnOrder.find { it != state.winnerId }
        val lossComponent = loserId?.let { state.getEntity(it)?.get<PlayerLostComponent>() }

        return when (lossComponent?.reason) {
            LossReason.LIFE_ZERO -> GameOverReason.LIFE_ZERO
            LossReason.EMPTY_LIBRARY -> GameOverReason.DECK_OUT
            LossReason.POISON_COUNTERS -> GameOverReason.POISON_COUNTERS
            LossReason.CONCESSION -> GameOverReason.CONCESSION
            LossReason.CARD_EFFECT -> GameOverReason.CARD_EFFECT
            null -> GameOverReason.LIFE_ZERO // Fallback
        }
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
     * Record a spectator state snapshot for replay.
     * Internally stores it as a delta from the previous snapshot to save memory.
     */
    fun recordSnapshot(snapshot: ServerMessage.SpectatorStateUpdate) {
        if (replayStartedAt == null) {
            replayStartedAt = Instant.now()
        }
        val last = replayLastSnapshot
        if (last == null) {
            replayInitialSnapshot = snapshot
        } else {
            replayDeltas.add(SpectatorReplayDiffCalculator.computeDelta(last, snapshot))
        }
        replayLastSnapshot = snapshot
    }

    /**
     * Get the initial replay snapshot (null if no snapshots recorded).
     */
    fun getReplayInitialSnapshot(): ServerMessage.SpectatorStateUpdate? = replayInitialSnapshot

    /**
     * Get all recorded replay deltas.
     */
    fun getReplayDeltas(): List<SpectatorReplayDelta> = replayDeltas.toList()

    /**
     * Total number of replay frames recorded.
     */
    fun getReplayFrameCount(): Int = if (replayInitialSnapshot != null) 1 + replayDeltas.size else 0

    // =========================================================================
    // Test Support (for scenario-based testing)
    // =========================================================================

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
     * Read-only snapshot of the current game state. Used by the engine AI controller
     * to evaluate board positions and simulate actions.
     *
     * Thread-safe: GameState is immutable, so reading the reference is safe.
     */
    fun getStateSnapshot(): GameState? = gameState

    /** Get deck list for a specific player. Used by engine AI to know the opponent's deck. */
    fun getDeckList(playerId: EntityId): List<String>? = deckLists[playerId]

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
        lastIds: Map<EntityId, String>
    ) {
        synchronized(stateLock) {
            gameState = state
            deckLists.clear()
            deckLists.putAll(decks)
            gameLogs.clear()
            gameLogs.putAll(logs)
            lastProcessedMessageId.clear()
            lastProcessedMessageId.putAll(lastIds)
            lastSentState.clear()
        }
    }

    /**
     * Associate a player identity with this session (for reconnection after restore).
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
