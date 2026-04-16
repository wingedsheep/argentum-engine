package com.wingedsheep.ai

import com.wingedsheep.ai.llm.BottomCardsInfo
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.ai.llm.MulliganInfo
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.sdk.model.EntityId

/**
 * Common interface for AI controllers.
 *
 * Two implementations:
 * - [com.wingedsheep.ai.llm.LlmAiPlayerController] — LLM-based (sends game state to an LLM API)
 * - [com.wingedsheep.ai.engine.EngineAiPlayerController] — Rules-engine based (uses the built-in [com.wingedsheep.ai.engine.AIPlayer])
 */
interface AiPlayerController {
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
    fun decideMulligan(mulliganMessage: MulliganInfo): Boolean

    /**
     * Choose which cards to put on the bottom after a mulligan.
     */
    fun chooseBottomCards(message: BottomCardsInfo): List<EntityId>

    /**
     * Provide deck composition context. Called once when a game starts.
     */
    fun setDeckList(deckList: Map<String, Int>, archetype: String? = null)

    // =========================================================================
    // Draft Picking
    // =========================================================================

    /**
     * Choose cards to pick from a booster draft pack.
     *
     * @param pack The current pack of cards to choose from.
     * @param pickedSoFar Cards already picked in this draft.
     * @param packNumber Which pack (1-based, e.g., 1, 2, or 3).
     * @param pickNumber Which pick within the pack (1-based).
     * @param picksRequired How many cards to pick (usually 1).
     * @return List of card names to pick.
     */
    fun chooseDraftPick(
        pack: List<CardSummary>,
        pickedSoFar: List<CardSummary>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int = 1,
        passDirection: String = "LEFT"
    ): List<String>

    /**
     * Choose whether to take the current pile or skip it in Winston Draft.
     *
     * @param pileCards Cards in the current pile being examined.
     * @param pileIndex Which pile (0-based).
     * @param pileSizes Sizes of all piles.
     * @param pickedSoFar Cards already picked.
     * @return true to take the pile, false to skip.
     */
    fun chooseWinstonAction(
        pileCards: List<CardSummary>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<CardSummary>
    ): Boolean

    /**
     * Choose a row or column in Grid Draft.
     *
     * @param grid 9 elements (row-major), null = empty slot.
     * @param availableSelections Available selections (e.g., "ROW_0", "COL_1").
     * @param pickedSoFar Cards already picked.
     * @return Selection string (e.g., "ROW_0", "COL_2").
     */
    fun chooseGridDraftPick(
        grid: List<CardSummary?>,
        availableSelections: List<String>,
        pickedSoFar: List<CardSummary>
    ): String
}

/**
 * Represents the AI's response to a game prompt.
 */
sealed interface ActionResponse {
    /** Submit a game action (cast spell, pass priority, declare attackers, etc.) */
    data class SubmitAction(val action: GameAction) : ActionResponse

    /** Submit a decision response */
    data class SubmitDecision(val playerId: EntityId, val response: DecisionResponse) : ActionResponse
}
