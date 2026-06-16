package com.wingedsheep.ai.engine

import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.ai.llm.MulliganInfo
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Momir Basic decks are 60 basic lands and the avatar's only cost is generic {X}, so every opening
 * hand is interchangeable — a mulligan can only shrink the hand without improving it. The engine
 * AI's generic "6-7 lands is a flood → mulligan" heuristic would otherwise mulligan every all-lands
 * Momir hand down to the forced keep at mulliganCount >= 2, leaving the AI on a 5-card hand every
 * game. These tests pin that the format short-circuit keeps, while the flood heuristic is untouched
 * outside Momir.
 */
class EngineAiMomirMulliganTest : ScenarioTestBase() {

    // The avatar + pool creatures live outside MtgSetCatalog, so build the registry the way the
    // other engine-AI tests do (TestCards bundles the avatar + basics).
    private val aiRegistry: CardRegistry = CardRegistry().apply {
        register(TestCards.all)
        register(PredefinedTokens.allTokens)
    }

    /** A fresh opening hand of seven basic lands — the only kind of hand a Momir deck can produce. */
    private fun sevenBasicLandHand(): MulliganInfo {
        val ids = (1..7).map { EntityId.of("land-$it") }
        val cards = ids.associateWith { CardSummary(name = "Forest", typeLine = "Basic Land — Forest") }
        return MulliganInfo(hand = ids, mulliganCount = 0, cardsToPutOnBottom = 0, cards = cards)
    }

    init {
        test("engine AI keeps an all-lands opening hand in Momir Basic") {
            val game = scenario().withPlayers().withFormat(Format.MomirBasic()).build()
            val controller = EngineAiPlayerController(aiRegistry, game.player1Id) { game.state }

            controller.decideMulligan(sevenBasicLandHand()) shouldBe true
        }

        test("engine AI still mulligans a 7-land flood outside Momir Basic") {
            // Standard format: a seven-land opener is a genuine flood, so the heuristic mulligans.
            val game = scenario().withPlayers().build()
            val controller = EngineAiPlayerController(aiRegistry, game.player1Id) { game.state }

            controller.decideMulligan(sevenBasicLandHand()) shouldBe false
        }
    }
}
