package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId

/**
 * Common interface for AI controllers.
 *
 * Two implementations:
 * - [AiPlayerController] — LLM-based (sends game state to an LLM API)
 * - [EngineAiController] — Rules-engine based (uses the built-in [com.wingedsheep.engine.ai.AIPlayer])
 */
interface AiController {
    /**
     * Choose an action from the legal actions or respond to a pending decision.
     */
    fun chooseAction(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?,
        recentGameLog: List<String> = emptyList()
    ): ActionResponse

    /**
     * Decide whether to keep or mulligan.
     * @return true to keep, false to mulligan.
     */
    fun decideMulligan(mulliganMessage: ServerMessage.MulliganDecision): Boolean

    /**
     * Choose which cards to put on the bottom after a mulligan.
     */
    fun chooseBottomCards(message: ServerMessage.ChooseBottomCards): List<EntityId>

    /**
     * Provide deck composition context. Called once when a game starts.
     */
    fun setDeckList(deckList: Map<String, Int>, archetype: String? = null)
}
