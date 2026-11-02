package com.wingedsheep.gameserver.ai

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.ai.llm.BottomCardsInfo
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.ai.llm.MulliganInfo
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.gameserver.config.AiProperties
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.gameserver.tournament.llm.LlmCostTracker
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * The engine AI may know every opponent's registered decklist (a deliberate design decision). The
 * server hands each AI seat a source for them when it wires the seat to a game, read lazily since
 * opponents may register after the AI is seated. It is AI-internal: no client message carries it.
 */
class AiOpponentDeckListTest : FunSpec({
    test("an AI wired to a game reads each opponent's registered decklist, and not its own") {
        val aiPlayerId = EntityId.of("ai-seat")
        val opponentId = EntityId.of("human-seat")
        val sessions = SessionRegistry()
        sessions.preRegisterIdentity(
            PlayerIdentity(token = "ai-seat", playerId = aiPlayerId, playerName = "AI", isAi = true),
        )
        val opponentDeck = mapOf("Hexhaven Invigorator" to 1, "Forest" to 16)
        val game = mockk<GameSession>(relaxed = true) {
            every { sessionId } returns "game-decklists"
            every { getStartingDeckCardCounts(opponentId) } returns opponentDeck
            every { getStartingDeckCardCounts(aiPlayerId) } returns mapOf("Island" to 40)
        }
        val controller = SourceRecordingController()
        val provider = object : AiControllerProvider {
            override val mode: String = "decklist-test"
            override fun create(context: AiControllerContext): AiPlayerController = controller
        }
        val manager = AiGameManager(
            gameProperties = GameProperties(ai = AiProperties(enabled = true, mode = provider.mode)),
            sessionRegistry = sessions,
            deckGenerator = mockk<SealedDeckGenerator>(relaxed = true),
            cardRegistry = mockk<CardRegistry>(relaxed = true),
            llmCostTracker = LlmCostTracker(),
            aiInsightService = AiInsightService(GameProperties()),
            controllerProviders = listOf(provider),
        )

        manager.wireAiForGame(
            gameSession = game,
            aiPlayerId = aiPlayerId,
            deckList = mapOf("Island" to 40),
            onActionReady = { _, _, _ -> },
            onMulliganKeep = { _ -> },
            onMulliganTake = { _ -> },
            onBottomCards = { _, _ -> },
        )

        val source = controller.source.shouldNotBeNull()
        source(opponentId) shouldBe opponentDeck
        source(aiPlayerId) shouldBe null
        sessions.destroy()
    }
})

private class SourceRecordingController : AiPlayerController {
    var source: ((EntityId) -> Map<String, Int>?)? = null

    override fun setOpponentDeckListSource(source: (EntityId) -> Map<String, Int>?) {
        this.source = source
    }

    override fun chooseAction(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?,
        recentGameLog: List<String>,
    ): ActionResponse = error("not used")

    override fun decideMulligan(mulliganMessage: MulliganInfo): Boolean = error("not used")
    override fun chooseBottomCards(message: BottomCardsInfo): List<EntityId> = error("not used")
    override fun setDeckList(deckList: Map<String, Int>, archetype: String?) = Unit
    override fun chooseDraftPick(
        pack: List<CardSummary>,
        pickedSoFar: List<CardSummary>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int,
        passDirection: String,
        podSize: Int?,
    ): List<String> = error("not used")

    override fun chooseWinstonAction(
        pileCards: List<CardSummary>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<CardSummary>,
    ): Boolean = error("not used")

    override fun chooseGridDraftPick(
        grid: List<CardSummary?>,
        availableSelections: List<String>,
        pickedSoFar: List<CardSummary>,
    ): String = error("not used")
}
