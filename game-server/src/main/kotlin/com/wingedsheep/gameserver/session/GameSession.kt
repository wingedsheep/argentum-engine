package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.dto.ClientEvent
import com.wingedsheep.gameserver.dto.ClientEventTransformer
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientStateTransformer
import com.wingedsheep.gameserver.legalactions.LegalActionsCalculator
import com.wingedsheep.gameserver.protocol.GameOverReason
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.priority.AutoPassManager
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatManager
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
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
    private val cardRegistry: CardRegistry,
    private val stateTransformer: ClientStateTransformer = ClientStateTransformer(cardRegistry),
    private val useHandSmoother: Boolean = false
) {
    // Lock for synchronizing state modifications to prevent lost updates
    private val stateLock = Any()

    @Volatile
    private var gameState: GameState? = null

    /** Checkpoint for undoing the last non-respondable action (e.g., play land, declare attackers) */
    @Volatile
    private var undoCheckpoint: GameState? = null
    private val players = mutableMapOf<EntityId, PlayerSession>()
    private val deckLists = mutableMapOf<EntityId, List<String>>()
    private val spectators = mutableSetOf<PlayerSession>()

    /** Player info for persistence (playerId -> (playerName, token)) */
    private val playerPersistenceInfo = mutableMapOf<EntityId, PlayerPersistenceInfo>()

    data class PlayerPersistenceInfo(val playerName: String, val token: String)

    private val actionProcessor = ActionProcessor(cardRegistry)
    private val gameInitializer = GameInitializer(cardRegistry)
    private val manaSolver = ManaSolver(cardRegistry)
    private val costCalculator = CostCalculator(cardRegistry)
    private val conditionEvaluator = ConditionEvaluator()
    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()
    private val turnManager = TurnManager(combatManager = CombatManager(cardRegistry))
    private val autoPassManager = AutoPassManager()
    private val spectatorStateBuilder = SpectatorStateBuilder(cardRegistry, stateTransformer)
    private val decisionEnricher = DecisionEnricher(cardRegistry)
    private val legalActionsCalculator = LegalActionsCalculator(
        cardRegistry, stateProjector, manaSolver, costCalculator,
        predicateEvaluator, conditionEvaluator, turnManager
    )

    /** Tracks the last processed messageId per player for idempotency */
    private val lastProcessedMessageId = java.util.concurrent.ConcurrentHashMap<EntityId, String>()

    /** Accumulated game log per player (player-specific due to masking) */
    private val gameLogs = java.util.concurrent.ConcurrentHashMap<EntityId, MutableList<ClientEvent>>()

    /** Per-player priority mode setting (AUTO = smart auto-pass, STOPS = stop on opponent stack + combat damage, FULL_CONTROL = never auto-pass) */
    private val priorityModes = java.util.concurrent.ConcurrentHashMap<EntityId, PriorityMode>()
    private val stopOverrides = java.util.concurrent.ConcurrentHashMap<EntityId, StopOverrideSettings>()

    // Replay recording
    private val replaySnapshots = CopyOnWriteArrayList<ServerMessage.SpectatorStateUpdate>()
    var replayStartedAt: Instant? = null
        private set

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
        val result = actionProcessor.process(state, action)

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
        val result = actionProcessor.process(state, action)

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
        val result = actionProcessor.process(state, action)

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
                    imageUri = imageUri
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
        return ServerMessage.ChooseBottomCards(
            hand = hand,
            cardsToPutOnBottom = count
        )
    }

    sealed interface MulliganActionResult {
        data object Success : MulliganActionResult
        data class NeedsBottomCards(val count: Int) : MulliganActionResult
        data class Failure(val reason: String) : MulliganActionResult
    }

    /**
     * Check if an action is eligible for undo (non-respondable actions where
     * the opponent can't respond before the active player passes priority).
     */
    private fun isUndoEligibleAction(action: GameAction): Boolean = when (action) {
        is PlayLand, is DeclareAttackers, is DeclareBlockers, is TurnFaceUp, is PassPriority -> true
        else -> false
    }

    /**
     * Execute a game action.
     *
     * Routes the action through the engine's ActionProcessor.
     * Synchronized to prevent lost updates when multiple players act simultaneously.
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

        // Manage undo checkpoint: save before undo-eligible actions, clear otherwise
        if (isUndoEligibleAction(action)) {
            undoCheckpoint = state
        } else {
            undoCheckpoint = null
        }

        val result = actionProcessor.process(state, action)

        val error = result.error
        val pendingDecision = result.pendingDecision
        when {
            error != null -> {
                // Revert checkpoint on failure
                if (isUndoEligibleAction(action)) undoCheckpoint = null
                ActionResult.Failure(error)
            }
            pendingDecision != null -> {
                gameState = result.state
                if (messageId != null) lastProcessedMessageId[playerId] = messageId
                ActionResult.PausedForDecision(result.state, pendingDecision, result.events)
            }
            else -> {
                gameState = result.state
                if (messageId != null) lastProcessedMessageId[playerId] = messageId
                ActionResult.Success(result.state, result.events)
            }
        }
    }

    /**
     * Handle player concession.
     * Synchronized to prevent lost updates.
     */
    fun playerConcedes(playerId: EntityId): GameState? = synchronized(stateLock) {
        val state = gameState ?: return null
        val action = Concede(playerId)
        val result = actionProcessor.process(state, action)

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
        return legalActionsCalculator.calculate(state, playerId)
    }


    /**
     * Create a state update message for a player.
     */
    fun createStateUpdate(playerId: EntityId, events: List<GameEvent>): ServerMessage.StateUpdate? {
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
            autoPassManager.getNextStopPoint(state, playerId, hasMeaningfulActions, stateProjector, playerOverrides.myTurnStops, playerOverrides.opponentTurnStops, stopsMode = playerMode == PriorityMode.STOPS)
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
        return ServerMessage.StateUpdate(stateWithLog, clientEvents, legalActions, pendingDecision, nextStopPoint, opponentDecisionStatus, stopOverrideInfo, isUndoAvailable(playerId), priorityModeStr)
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
        undoCheckpoint = null
        logger.info("Player $playerId undid their last action")
        ActionResult.Success(checkpoint, emptyList())
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
        val result = actionProcessor.process(state, action)

        val error = result.error
        val pendingDecision = result.pendingDecision
        when {
            error != null -> ActionResult.Failure(error)
            pendingDecision != null -> {
                gameState = result.state
                ActionResult.PausedForDecision(result.state, pendingDecision, result.events)
            }
            else -> {
                gameState = result.state
                ActionResult.Success(result.state, result.events)
            }
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
     */
    fun recordSnapshot(snapshot: ServerMessage.SpectatorStateUpdate) {
        if (replayStartedAt == null) {
            replayStartedAt = Instant.now()
        }
        replaySnapshots.add(snapshot)
    }

    /**
     * Get all recorded replay snapshots.
     */
    fun getReplaySnapshots(): List<ServerMessage.SpectatorStateUpdate> = replaySnapshots.toList()

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
            // Players map is preserved so connected sessions remain valid
        }
    }

    /**
     * Get the raw game state for testing assertions.
     * **WARNING:** This method is for testing only.
     */
    fun getStateForTesting(): GameState? = gameState

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
    fun setPlayerPersistenceInfo(playerId: EntityId, playerName: String, token: String) {
        playerPersistenceInfo[playerId] = PlayerPersistenceInfo(playerName, token)
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
