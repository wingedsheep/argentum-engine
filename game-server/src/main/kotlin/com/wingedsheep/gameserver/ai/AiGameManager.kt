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
    private val cardRegistry: CardRegistry,
    private val llmCostTracker: com.wingedsheep.gameserver.tournament.llm.LlmCostTracker
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

    /**
     * Master AI toggle, ignoring the LLM-key gate that [isEnabled] also applies. Use this
     * for code paths that force engine mode (e.g. dev scenarios) and so don't need a key
     * even when the server's global config is LLM.
     */
    val aiEnabledToggle: Boolean get() = gameProperties.ai.enabled

    /**
     * Look up the LLM model override for an AI player by querying its identity in the SessionRegistry.
     * The override is stored on `PlayerIdentity.aiModelOverride` so it survives server restart.
     */
    private fun lookupModelOverride(aiPlayerId: EntityId): String? {
        return sessionRegistry.getAllIdentities()
            .firstOrNull { it.playerId == aiPlayerId }
            ?.aiModelOverride
    }

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
            // Attribute in-game LLM token usage + cost to this game session, so the LLM-tournament
            // can report cost per game. No-op when there's no game (e.g. placeholder identities).
            val gameId = gameSession?.sessionId
            val usageSink: ((com.wingedsheep.ai.llm.LlmUsage) -> Unit)? =
                if (gameId != null) { usage -> llmCostTracker.record(gameId, usage) } else null
            val llmClient = LlmClient(aiConfig, usageSink)
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
     * Wire the AI plumbing common to every bring-up path: synthetic [AiWebSocketSession],
     * its [PlayerSession]/[PlayerIdentity], registration with [SessionRegistry] +
     * [activeSessions] + [aiPlayerIds].
     *
     * Each caller is responsible for the game-state side (deck list, [GameSession.addPlayer]
     * vs [GameSession.associatePlayer], persistence info) since those vary per flow.
     */
    private fun registerAiSession(
        gameSession: GameSession,
        aiPlayerId: EntityId,
        playerName: String,
        controller: AiPlayerController,
        modelOverride: String? = null,
        onActionReady: (EntityId, GameAction) -> Unit,
        onMulliganKeep: (EntityId) -> Unit,
        onMulliganTake: (EntityId) -> Unit,
        onBottomCards: (EntityId, List<EntityId>) -> Unit,
    ): Pair<PlayerSession, PlayerIdentity> {
        val aiSession = AiWebSocketSession(
            aiPlayerId = aiPlayerId,
            controller = controller,
            thinkingDelayMs = gameProperties.ai.thinkingDelayMs,
            onActionReady = onActionReady,
            onMulliganKeep = onMulliganKeep,
            onMulliganTake = onMulliganTake,
            onBottomCards = onBottomCards
        )

        val playerSession = PlayerSession(
            webSocketSession = aiSession,
            playerId = aiPlayerId,
            playerName = playerName
        )

        val identity = PlayerIdentity(
            token = "ai-token-${UUID.randomUUID().toString().take(8)}",
            playerId = aiPlayerId,
            playerName = playerName,
            isAi = true,
            aiModelOverride = modelOverride
        )
        identity.webSocketSession = aiSession
        identity.currentGameSessionId = gameSession.sessionId
        sessionRegistry.register(identity, aiSession, playerSession)

        activeSessions[gameSession.sessionId] = aiSession
        aiPlayerIds.add(aiPlayerId)
        return playerSession to identity
    }

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
        onBottomCards: (EntityId, List<EntityId>) -> Unit,
        /**
         * Fixed deck (card name → count) the AI must play instead of a generated sealed pool. Used
         * by deckless formats such as Momir Basic, where every seat plays the same 60 basics.
         * Null = the existing behaviour (generate a random sealed deck for [setCode]).
         */
        deckOverride: Map<String, Int>? = null,
    ): PlayerSession {
        require(isEnabled) { "AI is not enabled. Set game.ai.enabled=true." }

        val aiPlayerId = EntityId("ai-${UUID.randomUUID().toString().take(8)}")
        val aiName = randomAiName()

        val controller = createController(aiPlayerId, gameSession)

        val (playerSession, identity) = registerAiSession(
            gameSession = gameSession,
            aiPlayerId = aiPlayerId,
            playerName = aiName,
            controller = controller,
            onActionReady = onActionReady,
            onMulliganKeep = onMulliganKeep,
            onMulliganTake = onMulliganTake,
            onBottomCards = onBottomCards,
        )

        // Deckless formats (Momir Basic) supply a fixed deck; otherwise quick games generate a
        // sealed deck, using the same set as the human player when one was provided.
        val aiDeck = deckOverride
            ?: if (setCode != null) deckGenerator.generate(setCode) else deckGenerator.generate()
        gameSession.addPlayer(playerSession, aiDeck)

        // Give the AI knowledge of its deck composition
        controller.setDeckList(aiDeck)

        // Store persistence info
        gameSession.setPlayerPersistenceInfo(aiPlayerId, aiName, identity.token, isAi = true)

        logger.info("Created AI opponent ({}) for game {} [mode={}]",
            aiPlayerId.value, gameSession.sessionId, gameProperties.ai.mode)
        return playerSession
    }

    /**
     * Wire an AI opponent into an existing dev-scenario seat.
     *
     * Unlike [createAiOpponent], this does NOT generate a fresh `ai-<uuid>` entity or call
     * `gameSession.addPlayer(...)` — the player entity (with all its zones and life total) was
     * already built by [com.wingedsheep.gameserver.controller.DevScenarioController]'s
     * `ScenarioBuilder` and lives in the injected GameState under the supplied [aiPlayerId].
     *
     * Dev scenarios always use the in-process engine AI, never the LLM controller: scenarios
     * have no deck list to ground prompts against, the engine AI reads zones straight from
     * GameState, and we don't want test runs to need API keys or burn LLM tokens.
     *
     * @return the AI's [PlayerSession] (also added to [GameSession.players]).
     */
    fun wireAiForDevScenario(
        gameSession: GameSession,
        aiPlayerId: EntityId,
        playerName: String,
        onActionReady: (EntityId, GameAction) -> Unit,
        onMulliganKeep: (EntityId) -> Unit,
        onMulliganTake: (EntityId) -> Unit,
        onBottomCards: (EntityId, List<EntityId>) -> Unit
    ): PlayerSession {
        // Only check the master toggle — engine AI doesn't need the LLM API key that
        // [isEnabled] also gates on, so dev scenarios run even when the server is
        // configured for LLM mode without a key.
        require(gameProperties.ai.enabled) { "AI is not enabled. Set game.ai.enabled=true." }

        val controller = EngineAiPlayerController(
            cardRegistry = cardRegistry,
            playerId = aiPlayerId,
            gameStateProvider = { gameSession.getStateSnapshot() }
        )

        val (playerSession, identity) = registerAiSession(
            gameSession = gameSession,
            aiPlayerId = aiPlayerId,
            playerName = playerName,
            controller = controller,
            onActionReady = onActionReady,
            onMulliganKeep = onMulliganKeep,
            onMulliganTake = onMulliganTake,
            onBottomCards = onBottomCards,
        )

        // Add the AI to gameSession.players so player1/player2 accessors and broadcastStateUpdate see it.
        // (No addPlayer — that requires a deck list, but the dev scenario already injected the full state.)
        gameSession.associatePlayer(playerSession)
        gameSession.setPlayerPersistenceInfo(aiPlayerId, playerName, identity.token, isAi = true)

        logger.info("Wired AI ({}) into dev scenario {} [engine mode forced]",
            aiPlayerId.value, gameSession.sessionId)
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
            isAi = true,
            aiModelOverride = modelOverride
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
     * Re-establish in-memory AI tracking for a [PlayerIdentity] that was loaded from
     * Redis on server startup. The persisted identity has `isAi = true` and (optionally)
     * an `aiModelOverride`, but no live [AiWebSocketSession] (those aren't persisted).
     *
     * This adds the player back to [aiPlayerIds], creates a fresh placeholder
     * [AiWebSocketSession] with no-op callbacks (real ones get wired by `wireAiForGame`
     * when a match starts), and re-registers it in [SessionRegistry] so message routing
     * and `identity.isConnected` work again.
     */
    fun rehydrateAiIdentity(identity: PlayerIdentity) {
        require(identity.isAi) { "rehydrateAiIdentity called on non-AI identity ${identity.playerName}" }
        if (!isEnabled) {
            logger.warn("AI is disabled but recovered AI identity {}; AI players will not act.",
                identity.playerName)
            return
        }

        val aiPlayerId = identity.playerId
        val aiProperties = gameProperties.ai

        val controller = createController(aiPlayerId, modelOverride = identity.aiModelOverride)
        val aiSession = AiWebSocketSession(
            aiPlayerId = aiPlayerId,
            controller = controller,
            thinkingDelayMs = aiProperties.thinkingDelayMs,
            // No-op callbacks — replaced when a match (or draft) wires this AI.
            onActionReady = { _, _ -> },
            onMulliganKeep = { _ -> },
            onMulliganTake = { _ -> },
            onBottomCards = { _, _ -> }
        )

        identity.webSocketSession = aiSession
        val playerSession = PlayerSession(
            webSocketSession = aiSession,
            playerId = aiPlayerId,
            playerName = identity.playerName
        )
        sessionRegistry.register(identity, aiSession, playerSession)

        aiPlayerIds.add(aiPlayerId)

        logger.info("Rehydrated AI identity: {} ({}) [model={}]",
            identity.playerName, aiPlayerId.value, identity.aiModelOverride ?: aiProperties.model)
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
        val modelOverride = lookupModelOverride(aiPlayerId)
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

    /**
     * Adjust the per-decision thinking delay of an AI player's live session. Used by the
     * LLM-tournament pacing control to speed up / slow down an in-progress AI-vs-AI game.
     * No-op if the player isn't an AI or has no live session.
     */
    fun setThinkingDelay(aiPlayerId: EntityId, thinkingDelayMs: Long) {
        val ws = sessionRegistry.getAllIdentities()
            .firstOrNull { it.playerId == aiPlayerId }
            ?.webSocketSession as? AiWebSocketSession
        ws?.thinkingDelayMs = thinkingDelayMs
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
