package com.wingedsheep.gameserver.ai

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.protocol.ServerMessage.SealedCardInfo
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
        pack: List<SealedCardInfo>,
        pickedSoFar: List<SealedCardInfo>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int = 1
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
        pileCards: List<SealedCardInfo>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<SealedCardInfo>
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
        grid: List<SealedCardInfo?>,
        availableSelections: List<String>,
        pickedSoFar: List<SealedCardInfo>
    ): String
}
