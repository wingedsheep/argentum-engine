package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.llm.AiController
import com.wingedsheep.ai.llm.ActionResponse
import com.wingedsheep.ai.llm.BottomCardsInfo
import com.wingedsheep.ai.llm.CardRuling
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.ai.llm.MulliganInfo
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.StateDelta
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketExtension
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.security.Principal
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private val logger = LoggerFactory.getLogger(AiWebSocketSession::class.java)

/**
 * Virtual WebSocket session that intercepts outgoing messages and feeds them
 * to the AI controller. The AI's responses are submitted back via the provided
 * callback, asynchronously on a coroutine to avoid deadlocking on stateLock.
 */
class AiWebSocketSession(
    private val aiPlayerId: EntityId,
    private val controller: AiController,
    private val thinkingDelayMs: Long = 500,
    private val onActionReady: (EntityId, GameAction) -> Unit,
    private val onMulliganKeep: (EntityId) -> Unit,
    private val onMulliganTake: (EntityId) -> Unit,
    private val onBottomCards: (EntityId, List<EntityId>) -> Unit
) : WebSocketSession {

    // =========================================================================
    // Draft callbacks — set by LobbyHandler when a draft starts
    // =========================================================================

    /** Called when AI makes a booster draft pick. Args: (playerId, cardNames) */
    @Volatile var onDraftPick: ((EntityId, List<String>) -> Unit)? = null

    /** Called when AI takes a Winston pile. Args: (playerId) */
    @Volatile var onWinstonTakePile: ((EntityId) -> Unit)? = null

    /** Called when AI skips a Winston pile. Args: (playerId) */
    @Volatile var onWinstonSkipPile: ((EntityId) -> Unit)? = null

    /** Called when AI makes a grid draft pick. Args: (playerId, selection) */
    @Volatile var onGridDraftPick: ((EntityId, String) -> Unit)? = null

    private val sessionId = "ai-${UUID.randomUUID()}"
    private val open = AtomicBoolean(true)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Cache the last full game state so we can use it when delta updates arrive. */
    @Volatile
    private var lastFullState: ClientGameState? = null

    /** Rolling game log of recent event descriptions for AI context. */
    private val gameLog = mutableListOf<String>()
    private val maxGameLogSize = 30

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    /**
     * Called by MessageSender when the server sends a message to this "player".
     * We parse the JSON, extract the game state, and feed it to the AI controller
     * asynchronously.
     */
    override fun sendMessage(message: WebSocketMessage<*>) {
        if (!open.get()) return

        val text = message.payload.toString()

        scope.launch {
            try {
                val serverMessage = json.decodeFromString<ServerMessage>(text)
                handleServerMessage(serverMessage)
            } catch (e: Exception) {
                logger.error("AI failed to process server message: ${e.message}", e)
            }
        }
    }

    private suspend fun handleServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.StateUpdate -> {
                logger.info("AI received StateUpdate: phase={}, step={}, priority={}, legalActions={}, pendingDecision={}",
                    message.state.currentPhase, message.state.currentStep,
                    if (message.state.priorityPlayerId == aiPlayerId) "AI" else "opponent",
                    message.legalActions.size,
                    message.pendingDecision?.let { it::class.simpleName })
                lastFullState = message.state
                accumulateEvents(message.events.map { it.description })
                handleStateUpdate(message.state, message.legalActions, message.pendingDecision)
            }

            is ServerMessage.StateDeltaUpdate -> {
                logger.info("AI received StateDeltaUpdate: legalActions={}, pendingDecision={}, hasCachedState={}",
                    message.legalActions.size,
                    message.pendingDecision?.let { it::class.simpleName },
                    lastFullState != null)
                accumulateEvents(message.events.map { it.description })
                // Apply delta to cached state to keep it current
                val cachedState = lastFullState
                val updatedState = if (cachedState != null) {
                    applyDelta(cachedState, message.delta).also { lastFullState = it }
                } else null
                if (updatedState != null && (message.legalActions.isNotEmpty() || message.pendingDecision != null)) {
                    handleStateUpdate(updatedState, message.legalActions, message.pendingDecision)
                } else if (message.legalActions.isNotEmpty() || message.pendingDecision != null) {
                    logger.warn("AI received delta update but has no cached state — falling back to heuristics")
                    handleActionsOnlyFallback(message.legalActions, message.pendingDecision)
                }
            }

            is ServerMessage.MulliganDecision -> {
                logger.info("AI received MulliganDecision: mulliganCount={}, hand={} cards, onThePlay={}",
                    message.mulliganCount, message.hand.size, message.isOnThePlay)
                delay(thinkingDelayMs)
                val keep = controller.decideMulligan(message.toMulliganInfo())
                logger.info("AI mulligan result: {}", if (keep) "KEEP" else "MULLIGAN")
                if (keep) {
                    onMulliganKeep(aiPlayerId)
                } else {
                    onMulliganTake(aiPlayerId)
                }
            }

            is ServerMessage.ChooseBottomCards -> {
                logger.info("AI received ChooseBottomCards: hand={}, bottomCount={}", message.hand.size, message.cardsToPutOnBottom)
                delay(thinkingDelayMs)
                val bottomCards = controller.chooseBottomCards(message.toBottomCardsInfo())
                logger.info("AI chose bottom cards: {}", bottomCards)
                onBottomCards(aiPlayerId, bottomCards)
            }

            is ServerMessage.GameStarted -> {
                logger.info("AI received GameStarted: opponent={}", message.opponentName)
            }

            is ServerMessage.GameOver -> {
                logger.info("AI game over. Winner: {}", message.winnerId)
                open.set(false)
                scope.cancel()
            }

            is ServerMessage.WaitingForOpponentMulligan -> {
                logger.info("AI waiting for opponent mulligan")
            }

            is ServerMessage.MulliganComplete -> {
                logger.info("AI mulligan complete, final hand size: {}", message.finalHandSize)
            }

            is ServerMessage.GameCreated -> {
                logger.info("AI received GameCreated")
            }

            is ServerMessage.GameCancelled -> {
                logger.info("AI game cancelled")
                open.set(false)
                scope.cancel()
            }

            // =================================================================
            // Draft messages
            // =================================================================

            is ServerMessage.DraftPackReceived -> {
                logger.info("AI received DraftPackReceived: pack={}, pick={}, cards={}, picksPerRound={}",
                    message.packNumber, message.pickNumber, message.cards.size, message.picksPerRound)
                handleDraftPack(message)
            }

            is ServerMessage.DraftPickConfirmed -> {
                logger.info("AI draft pick confirmed: {} (total: {})",
                    message.cardNames.joinToString(", "), message.totalPicked)
            }

            is ServerMessage.DraftComplete -> {
                logger.info("AI draft complete: {} cards picked", message.pickedCards.size)
            }

            is ServerMessage.WinstonDraftState -> {
                if (message.isYourTurn) {
                    logger.info("AI received WinstonDraftState: pile={}, pileCards={}, pileSizes={}",
                        message.currentPileIndex, message.currentPileCards?.size, message.pileSizes)
                    handleWinstonTurn(message)
                } else {
                    logger.info("AI waiting for opponent's Winston turn")
                }
            }

            is ServerMessage.GridDraftState -> {
                if (message.isYourTurn) {
                    logger.info("AI received GridDraftState: grid #{}, selections={}",
                        message.gridNumber, message.availableSelections)
                    handleGridDraftTurn(message)
                } else {
                    logger.info("AI waiting for opponent's grid draft turn")
                }
            }

            is ServerMessage.SealedPoolGenerated -> {
                logger.info("AI received SealedPoolGenerated: {} cards", message.cardPool.size)
            }

            else -> {
                logger.info("AI received message type: {}", message::class.simpleName)
            }
        }
    }

    private fun accumulateEvents(descriptions: List<String>) {
        val newEntries = descriptions.filter { it.isNotBlank() }
        if (newEntries.isEmpty()) return
        synchronized(gameLog) {
            gameLog.addAll(newEntries)
            // Trim to keep only the most recent entries
            while (gameLog.size > maxGameLogSize) {
                gameLog.removeFirst()
            }
        }
    }

    private fun getRecentGameLog(): List<String> {
        synchronized(gameLog) {
            return gameLog.toList()
        }
    }

    private suspend fun handleStateUpdate(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?
    ) {
        // If we have legal actions or a pending decision addressed to us, it's our turn.
        // Don't rely on state.priorityPlayerId — it may be stale when using a cached state
        // from a previous StateUpdate combined with a newer StateDeltaUpdate's legal actions.
        val isOurDecision = pendingDecision?.playerId == aiPlayerId
        val hasLegalActions = legalActions.isNotEmpty()

        if (!hasLegalActions && !isOurDecision) {
            logger.info("AI skipping — no legal actions and no pending decision for us")
            return
        }

        if (legalActions.isNotEmpty()) {
            logger.info("AI has {} legal actions: {}", legalActions.size,
                legalActions.joinToString(", ") { "${it.actionType}${if (it.description.isNotBlank()) "(${it.description})" else ""}" })
        }
        if (pendingDecision != null) {
            logger.info("AI has pending decision: {} — {}", pendingDecision::class.simpleName, pendingDecision.prompt)
        }

        delay(thinkingDelayMs)

        val response = controller.chooseAction(state, legalActions, pendingDecision, getRecentGameLog())
        logger.info("AI chose response: {}", when (response) {
            is ActionResponse.SubmitAction -> "Action(${response.action::class.simpleName})"
            is ActionResponse.SubmitDecision -> "Decision(${response.response::class.simpleName})"
        })

        // Extra delay after declaring blockers so the human player can see assignments
        if (response is ActionResponse.SubmitAction && response.action is DeclareBlockers) {
            delay(thinkingDelayMs * 4)
        }

        submitResponse(response)
    }

    /**
     * Fallback when we have legal actions / pending decision but no cached state.
     * Can only auto-resolve simple decisions or pass priority.
     */
    private suspend fun handleActionsOnlyFallback(
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?
    ) {
        if (legalActions.isEmpty() && pendingDecision == null) return

        val isOurDecision = pendingDecision?.playerId == aiPlayerId
        if (!isOurDecision && legalActions.isEmpty()) return

        delay(thinkingDelayMs)

        if (pendingDecision != null && isOurDecision) {
            val autoResponse = when (pendingDecision) {
                is com.wingedsheep.engine.core.SelectManaSourcesDecision -> {
                    logger.info("AI fallback: auto-paying mana sources")
                    ActionResponse.SubmitDecision(
                        aiPlayerId,
                        com.wingedsheep.engine.core.ManaSourcesSelectedResponse(
                            decisionId = pendingDecision.id, autoPay = true
                        )
                    )
                }
                is com.wingedsheep.engine.core.AssignDamageDecision -> {
                    logger.info("AI fallback: using default damage assignment")
                    ActionResponse.SubmitDecision(
                        aiPlayerId,
                        com.wingedsheep.engine.core.DamageAssignmentResponse(
                            decisionId = pendingDecision.id,
                            assignments = pendingDecision.defaultAssignments
                        )
                    )
                }
                is com.wingedsheep.engine.core.SelectCardsDecision -> {
                    // Pick the first `minSelections` options — matches SelectCardsHandler.heuristic.
                    // Not an ideal choice, but submitting *something* valid is strictly better
                    // than silently hanging the game.
                    logger.warn("AI fallback: no cached state for SelectCardsDecision '{}', picking first {} option(s)",
                        pendingDecision.prompt, pendingDecision.minSelections)
                    ActionResponse.SubmitDecision(
                        aiPlayerId,
                        com.wingedsheep.engine.core.CardsSelectedResponse(
                            decisionId = pendingDecision.id,
                            selectedCards = pendingDecision.options.take(pendingDecision.minSelections)
                        )
                    )
                }
                else -> {
                    val passAction = legalActions.find { it.actionType == "PassPriority" }
                    if (passAction != null) {
                        logger.warn("AI fallback: can't resolve {} without full state, passing priority",
                            pendingDecision::class.simpleName)
                        ActionResponse.SubmitAction(passAction.action)
                    } else {
                        // No PassPriority available (we hold the decision, no state to answer it).
                        // Silently returning here hangs the game — log loudly so this surfaces.
                        logger.error(
                            "AI fallback: cannot auto-resolve {} (id={}, prompt=\"{}\") without cached state and no PassPriority available — AI cannot progress. Add handling for this decision type in handleActionsOnlyFallback.",
                            pendingDecision::class.simpleName,
                            pendingDecision.id,
                            pendingDecision.prompt
                        )
                        return
                    }
                }
            }
            submitResponse(autoResponse)
        } else if (legalActions.isNotEmpty()) {
            logger.info("AI fallback: passing priority (no state available)")
            val passAction = legalActions.find { it.actionType == "PassPriority" }
            if (passAction != null) {
                submitResponse(ActionResponse.SubmitAction(passAction.action))
            }
        }
    }

    // =========================================================================
    // Draft pick handlers
    // =========================================================================

    private suspend fun handleDraftPack(message: ServerMessage.DraftPackReceived) {
        val callback = onDraftPick
        if (callback == null) {
            logger.warn("AI received draft pack but no onDraftPick callback is set")
            return
        }

        delay(thinkingDelayMs)

        val picks = controller.chooseDraftPick(
            pack = message.cards.map { it.toCardSummary() },
            pickedSoFar = message.pickedCards.map { it.toCardSummary() },
            packNumber = message.packNumber,
            pickNumber = message.pickNumber,
            picksRequired = message.picksPerRound,
            passDirection = message.passDirection
        )
        logger.info("AI draft pick: {}", picks.joinToString(", "))
        callback(aiPlayerId, picks)
    }

    private suspend fun handleWinstonTurn(message: ServerMessage.WinstonDraftState) {
        val pileCards = message.currentPileCards
        if (pileCards == null) {
            logger.warn("AI Winston turn but no pile cards visible")
            return
        }

        delay(thinkingDelayMs)

        val take = controller.chooseWinstonAction(
            pileCards = pileCards.map { it.toCardSummary() },
            pileIndex = message.currentPileIndex,
            pileSizes = message.pileSizes,
            pickedSoFar = message.pickedCards.map { it.toCardSummary() }
        )

        if (take) {
            val callback = onWinstonTakePile
            if (callback != null) {
                logger.info("AI Winston: TAKE pile {}", message.currentPileIndex)
                callback(aiPlayerId)
            } else {
                logger.warn("AI wants to take Winston pile but no callback set")
            }
        } else {
            val callback = onWinstonSkipPile
            if (callback != null) {
                logger.info("AI Winston: SKIP pile {}", message.currentPileIndex)
                callback(aiPlayerId)
            } else {
                logger.warn("AI wants to skip Winston pile but no callback set")
            }
        }
    }

    private suspend fun handleGridDraftTurn(message: ServerMessage.GridDraftState) {
        val callback = onGridDraftPick
        if (callback == null) {
            logger.warn("AI received grid draft turn but no onGridDraftPick callback is set")
            return
        }

        delay(thinkingDelayMs)

        val selection = controller.chooseGridDraftPick(
            grid = message.grid.map { it?.toCardSummary() },
            availableSelections = message.availableSelections,
            pickedSoFar = message.pickedCards.map { it.toCardSummary() }
        )
        logger.info("AI grid draft pick: {}", selection)
        callback(aiPlayerId, selection)
    }

    private fun submitResponse(response: ActionResponse) {
        when (response) {
            is ActionResponse.SubmitAction -> {
                onActionReady(aiPlayerId, response.action)
            }
            is ActionResponse.SubmitDecision -> {
                val action = SubmitDecision(
                    playerId = response.playerId,
                    response = response.response
                )
                onActionReady(aiPlayerId, action)
            }
        }
    }

    /**
     * Apply a StateDelta to a previous ClientGameState to produce an updated state.
     * Mirrors the logic in ProtocolTestBase/GameServerTestBase.
     */
    private fun applyDelta(previous: ClientGameState, delta: StateDelta): ClientGameState {
        val cards = previous.cards.toMutableMap()
        delta.removedCardIds?.forEach { cards.remove(it) }
        delta.addedCards?.forEach { (id, card) -> cards[id] = card }
        delta.updatedCards?.forEach { (id, card) -> cards[id] = card }

        val updatedZones = delta.updatedZones
        val zones = if (updatedZones != null) {
            val updatedMap = updatedZones.associateBy { it.zoneId }
            previous.zones.map { updatedMap[it.zoneId] ?: it }
        } else {
            previous.zones
        }

        val newLogEntries = delta.newLogEntries
        val gameLog = if (newLogEntries != null) {
            previous.gameLog + newLogEntries
        } else {
            previous.gameLog
        }

        val combat = when {
            delta.combatCleared == true -> null
            delta.combat != null -> delta.combat
            else -> previous.combat
        }

        return previous.copy(
            cards = cards,
            zones = zones,
            players = delta.players,
            currentPhase = delta.currentPhase ?: previous.currentPhase,
            currentStep = delta.currentStep ?: previous.currentStep,
            activePlayerId = delta.activePlayerId ?: previous.activePlayerId,
            priorityPlayerId = delta.priorityPlayerId ?: previous.priorityPlayerId,
            turnNumber = delta.turnNumber ?: previous.turnNumber,
            isGameOver = delta.isGameOver ?: previous.isGameOver,
            winnerId = if (delta.winnerId != null) delta.winnerId else previous.winnerId,
            combat = combat,
            gameLog = gameLog,
        )
    }

    fun shutdown() {
        open.set(false)
        scope.cancel()
    }

    // =========================================================================
    // WebSocketSession interface stubs
    // =========================================================================

    override fun getId(): String = sessionId
    override fun getUri(): URI? = URI.create("ai://localhost/game")
    override fun getHandshakeHeaders(): HttpHeaders = HttpHeaders()
    override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()
    override fun getPrincipal(): Principal? = null
    override fun getLocalAddress(): InetSocketAddress? = null
    override fun getRemoteAddress(): InetSocketAddress? = null
    override fun getAcceptedProtocol(): String? = null
    override fun setTextMessageSizeLimit(messageSizeLimit: Int) {}
    override fun getTextMessageSizeLimit(): Int = Int.MAX_VALUE
    override fun setBinaryMessageSizeLimit(messageSizeLimit: Int) {}
    override fun getBinaryMessageSizeLimit(): Int = Int.MAX_VALUE
    override fun getExtensions(): MutableList<WebSocketExtension> = mutableListOf()
    override fun isOpen(): Boolean = open.get()

    @Throws(IOException::class)
    override fun close() {
        shutdown()
    }

    @Throws(IOException::class)
    override fun close(status: CloseStatus) {
        shutdown()
    }
}

private fun ServerMessage.MulliganDecision.toMulliganInfo() = MulliganInfo(
    hand = hand,
    mulliganCount = mulliganCount,
    cardsToPutOnBottom = cardsToPutOnBottom,
    cards = cards.mapValues { (_, v) -> v.toCardSummary() },
    isOnThePlay = isOnThePlay
)

private fun ServerMessage.ChooseBottomCards.toBottomCardsInfo() = BottomCardsInfo(
    hand = hand,
    cardsToPutOnBottom = cardsToPutOnBottom,
    cards = cards.mapValues { (_, v) -> v.toCardSummary() }
)

private fun ServerMessage.MulliganCardInfo.toCardSummary() = CardSummary(
    name = name,
    manaCost = manaCost,
    typeLine = typeLine,
    power = power,
    toughness = toughness,
    oracleText = oracleText
)

private fun ServerMessage.SealedCardInfo.toCardSummary() = CardSummary(
    name = name,
    manaCost = manaCost,
    typeLine = typeLine,
    rarity = rarity,
    imageUri = imageUri,
    power = power,
    toughness = toughness,
    oracleText = oracleText,
    rulings = rulings.map { CardRuling(it.date, it.text) }
)
