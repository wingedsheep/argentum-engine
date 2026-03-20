package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.StateDelta
import com.wingedsheep.gameserver.protocol.LegalActionInfo
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
                val keep = controller.decideMulligan(message)
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
                val bottomCards = controller.chooseBottomCards(message)
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
                else -> {
                    logger.warn("AI fallback: can't resolve {} without full state, passing if possible",
                        pendingDecision::class.simpleName)
                    val passAction = legalActions.find { it.actionType == "PassPriority" }
                    if (passAction != null) ActionResponse.SubmitAction(passAction.action) else return
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

        val zones = if (delta.updatedZones != null) {
            val updatedMap = delta.updatedZones.associateBy { it.zoneId }
            previous.zones.map { updatedMap[it.zoneId] ?: it }
        } else {
            previous.zones
        }

        val gameLog = if (delta.newLogEntries != null) {
            previous.gameLog + delta.newLogEntries
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
