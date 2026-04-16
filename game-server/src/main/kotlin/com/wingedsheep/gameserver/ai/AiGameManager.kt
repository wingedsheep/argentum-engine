package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.ai.engine.EngineAiPlayerController
import com.wingedsheep.ai.llm.LlmAiPlayerController
import com.wingedsheep.ai.llm.LlmClient
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger(AiGameManager::class.java)

/**
 * Manages the lifecycle of AI opponents in games.
 *
 * Supports two AI modes:
 * - **engine** (default): Built-in rules-engine AI. No API key needed. Fast, deterministic.
 * - **llm**: LLM-based AI via OpenAI-compatible API. Requires API key.
 */
@Service
class AiGameManager(
    private val gameProperties: GameProperties,
    private val sessionRegistry: SessionRegistry,
    private val deckGenerator: SealedDeckGenerator,
    private val cardRegistry: CardRegistry
) {
    private val activeSessions = ConcurrentHashMap<String, AiWebSocketSession>()

    @PostConstruct
    fun logConfig() {
        val ai = gameProperties.ai
        if (!ai.enabled) {
            logger.info("AI opponent: disabled")
            return
        }
        if (ai.isEngineMode) {
            logger.info("AI opponent: enabled | mode=engine (built-in)")
        } else {
            val provider = if (ai.baseUrl.contains("openrouter")) "OpenRouter" else "Local (${ai.baseUrl})"
            logger.info("AI opponent: enabled | mode=llm | provider={} | model={} | deckbuilding-model={}",
                provider, ai.model, ai.effectiveDeckbuildingModel)
        }
    }

    companion object {
        private val AI_NAMES = listOf(
            "Cruel Optimus",
            "Thought Harvester",
            "The Stack Tyrant",
            "Mindripper Prime",
            "Soulless Topdeckr",
            "The Unblinkable",
            "Dread Calculus",
            "Synapse Ravager",
            "Neural Butcher",
            "The Iron Oracle",
            "Phyrexian Brainframe",
            "Darksteel Nemesis",
            "Voltaic Mastermind",
            "Myr Overlord",
            "Blightsteel Brain",
        )

        fun randomAiName(): String = "[AI] ${AI_NAMES.random()}"
    }

    val isEnabled: Boolean get() {
        if (!gameProperties.ai.enabled) return false
        // Engine mode doesn't need an API key
        if (gameProperties.ai.isEngineMode) return true
        // LLM mode requires an API key
        return gameProperties.ai.effectiveApiKey.isNotBlank()
    }

    /** Per-player model overrides set at identity-creation time, looked up when wiring games. */
    private val aiModelOverrides = ConcurrentHashMap<EntityId, String>()

    /**
     * Create the appropriate AI controller based on configuration.
     * @param modelOverride If non-null, overrides the server's configured model for LLM mode.
     */
    private fun createController(
        aiPlayerId: EntityId,
        gameSession: GameSession? = null,
        modelOverride: String? = null
    ): AiPlayerController {
        val ai = gameProperties.ai
        // A model override implicitly requests LLM mode for this player,
        // regardless of the server's global mode setting.
        val aiConfig = ai.toAiConfig().let { cfg ->
            if (modelOverride != null) cfg.copy(model = modelOverride, mode = "llm") else cfg
        }
        return if (aiConfig.isEngineMode) {
            EngineAiPlayerController(
                cardRegistry = cardRegistry,
                playerId = aiPlayerId,
                gameStateProvider = { gameSession?.getStateSnapshot() }
            )
        } else {
            val engineFallback = EngineAiPlayerController(
                cardRegistry = cardRegistry,
                playerId = aiPlayerId,
                gameStateProvider = { gameSession?.getStateSnapshot() }
            )
            val llmClient = LlmClient(aiConfig)
            LlmAiPlayerController(aiConfig, llmClient, aiPlayerId, fallback = engineFallback)
        }
    }

    private fun com.wingedsheep.gameserver.config.AiProperties.toAiConfig() = com.wingedsheep.ai.llm.AiConfig(
        enabled = enabled, mode = mode, baseUrl = baseUrl,
        apiKey = apiKey, openRouterApiKey = openRouterApiKey,
        model = model, deckbuildingModel = deckbuildingModel,
        reasoningEffort = reasoningEffort, maxRetries = maxRetries,
        timeoutMs = timeoutMs, thinkingDelayMs = thinkingDelayMs
    )

    /**
     * Create an AI opponent and add it to the game session.
     *
     * @param gameSession The game session to add the AI to.
     * @param onActionReady Callback invoked (async) when the AI wants to submit an action.
     *        This MUST NOT be called while holding stateLock.
     * @param onMulliganKeep Callback for AI keeping hand.
     * @param onMulliganTake Callback for AI taking mulligan.
     * @param onBottomCards Callback for AI choosing bottom cards.
     * @return The AI player's EntityId and PlayerSession.
     */
    fun createAiOpponent(
        gameSession: GameSession,
        setCode: String? = null,
        onActionReady: (EntityId, GameAction) -> Unit,
        onMulliganKeep: (EntityId) -> Unit,
        onMulliganTake: (EntityId) -> Unit,
        onBottomCards: (EntityId, List<EntityId>) -> Unit
    ): PlayerSession {
        require(isEnabled) { "AI is not enabled. Set game.ai.enabled=true." }

        val aiPlayerId = EntityId("ai-${UUID.randomUUID().toString().take(8)}")
        val aiProperties = gameProperties.ai
        val aiName = randomAiName()

        val controller = createController(aiPlayerId, gameSession)

        val aiSession = AiWebSocketSession(
            aiPlayerId = aiPlayerId,
            controller = controller,
            thinkingDelayMs = aiProperties.thinkingDelayMs,
            onActionReady = onActionReady,
            onMulliganKeep = onMulliganKeep,
            onMulliganTake = onMulliganTake,
            onBottomCards = onBottomCards
        )

        val playerSession = PlayerSession(
            webSocketSession = aiSession,
            playerId = aiPlayerId,
            playerName = aiName
        )

        // Register a fake identity and session so MessageSender can find the lock
        val identity = com.wingedsheep.gameserver.session.PlayerIdentity(
            token = "ai-token-${UUID.randomUUID().toString().take(8)}",
            playerId = aiPlayerId,
            playerName = aiName,
            isAi = true
        )
        identity.webSocketSession = aiSession
        sessionRegistry.register(identity, aiSession, playerSession)

        // Quick games use a sealed deck — use same set as human player if provided
        val aiDeck = if (setCode != null) deckGenerator.generate(setCode) else deckGenerator.generate()
        gameSession.addPlayer(playerSession, aiDeck)

        // Give the AI knowledge of its deck composition
        controller.setDeckList(aiDeck)

        // Store persistence info
        gameSession.setPlayerPersistenceInfo(aiPlayerId, aiName, identity.token)
        identity.currentGameSessionId = gameSession.sessionId

        activeSessions[gameSession.sessionId] = aiSession

        logger.info("Created AI opponent ({}) for game {} [mode={}]",
            aiPlayerId.value, gameSession.sessionId, aiProperties.mode)
        return playerSession
    }

    /**
     * Create an AI PlayerIdentity for use in a tournament lobby.
     * The AI session is created with no-op callbacks initially — they'll be wired
     * when a tournament match starts via [wireAiForGame].
     *
     * @param modelOverride Optional LLM model override for this specific AI player.
     * @return The AI PlayerIdentity, registered in SessionRegistry.
     */
    fun createAiIdentity(modelOverride: String? = null): PlayerIdentity {
        require(isEnabled) { "AI is not enabled. Set game.ai.enabled=true." }

        val aiPlayerId = EntityId("ai-${UUID.randomUUID().toString().take(8)}")
        val aiProperties = gameProperties.ai

        if (modelOverride != null) {
            aiModelOverrides[aiPlayerId] = modelOverride
        }

        // Use a placeholder controller — will be replaced when match starts
        val controller = createController(aiPlayerId, modelOverride = modelOverride)

        val aiSession = AiWebSocketSession(
            aiPlayerId = aiPlayerId,
            controller = controller,
            thinkingDelayMs = aiProperties.thinkingDelayMs,
            // No-op callbacks — will be replaced when match starts
            onActionReady = { _, _ -> },
            onMulliganKeep = { _ -> },
            onMulliganTake = { _ -> },
            onBottomCards = { _, _ -> }
        )

        val effectiveModel = modelOverride ?: if (gameProperties.ai.isLlmMode) gameProperties.ai.model else null
        val modelSuffix = effectiveModel?.substringAfterLast('/')?.let { " ($it)" } ?: ""
        val aiName = randomAiName() + modelSuffix
        val identity = PlayerIdentity(
            token = "ai-token-${UUID.randomUUID().toString().take(8)}",
            playerId = aiPlayerId,
            playerName = aiName,
            isAi = true
        )
        identity.webSocketSession = aiSession

        val playerSession = PlayerSession(
            webSocketSession = aiSession,
            playerId = aiPlayerId,
            playerName = aiName
        )
        sessionRegistry.register(identity, aiSession, playerSession)

        // Track this AI identity so we know which players are AI
        aiPlayerIds.add(aiPlayerId)

        val modelInfo = if (modelOverride != null) "model=$modelOverride" else "model=${aiProperties.model}"
        logger.info("Created AI identity: {} ({}) [mode={}, {}]", identity.playerName, aiPlayerId.value, aiProperties.mode, modelInfo)
        return identity
    }

    /**
     * Wire an AI player's session for a specific tournament match.
     * Replaces the no-op callbacks with ones that feed actions into the given GameSession.
     */
    fun wireAiForGame(
        gameSession: GameSession,
        aiPlayerId: EntityId,
        deckList: Map<String, Int>?,
        onActionReady: (EntityId, GameAction) -> Unit,
        onMulliganKeep: (EntityId) -> Unit,
        onMulliganTake: (EntityId) -> Unit,
        onBottomCards: (EntityId, List<EntityId>) -> Unit
    ) {
        val identity = sessionRegistry.getAllIdentities().find { it.playerId == aiPlayerId }
        val oldSession = identity?.webSocketSession as? AiWebSocketSession
        if (oldSession != null) {
            oldSession.shutdown()
        }

        val aiProperties = gameProperties.ai
        val modelOverride = aiModelOverrides[aiPlayerId]
        val controller = createController(aiPlayerId, gameSession, modelOverride)

        // Give the AI knowledge of its deck composition
        if (deckList != null) {
            controller.setDeckList(deckList)
        }

        val newSession = AiWebSocketSession(
            aiPlayerId = aiPlayerId,
            controller = controller,
            thinkingDelayMs = aiProperties.thinkingDelayMs,
            onActionReady = onActionReady,
            onMulliganKeep = onMulliganKeep,
            onMulliganTake = onMulliganTake,
            onBottomCards = onBottomCards
        )

        // Update identity and registry to use the new session
        identity?.webSocketSession = newSession
        if (identity != null) {
            val playerSession = PlayerSession(
                webSocketSession = newSession,
                playerId = aiPlayerId,
                playerName = identity.playerName
            )
            sessionRegistry.setPlayerSession(newSession.id, playerSession)
        }

        activeSessions[gameSession.sessionId] = newSession
        logger.info("Wired AI {} for game {} [mode={}]", aiPlayerId.value, gameSession.sessionId, aiProperties.mode)
    }

    /** Set of all AI player IDs (persists across matches within a tournament). */
    private val aiPlayerIds = ConcurrentHashMap.newKeySet<EntityId>()

    /**
     * Check if a player is an AI.
     */
    fun isAiPlayer(playerId: EntityId): Boolean = playerId in aiPlayerIds

    /**
     * Clean up AI resources when a game ends.
     */
    fun cleanupGame(gameSessionId: String) {
        val session = activeSessions.remove(gameSessionId)
        if (session != null) {
            session.shutdown()
            logger.info("Cleaned up AI session for game $gameSessionId")
        }
    }

    /**
     * Check if a game has an AI player.
     */
    fun hasAiPlayer(gameSessionId: String): Boolean = activeSessions.containsKey(gameSessionId)

}
