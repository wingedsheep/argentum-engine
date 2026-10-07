package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Timetwister
import com.wingedsheep.mtg.sets.definitions.mbs.cards.PsychosisCrawler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PsychosisCrawlerScenarioTest : FunSpec({
    test("survives an empty hand during Timetwister and drains once per controller draw") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(PsychosisCrawler, Timetwister))
            initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val crawler = d.putPermanentOnBattlefield(d.player1, "Psychosis Crawler")
        d.state.projectedState.getPower(crawler) shouldBe d.getHandSize(d.player1)
        d.state.projectedState.getToughness(crawler) shouldBe d.getHandSize(d.player1)

        val wheel = d.putCardInHand(d.player1, "Timetwister")
        d.giveMana(d.player1, Color.BLUE, 3)
        d.castSpell(d.player1, wheel).error shouldBe null
        d.bothPass().error shouldBe null

        // The hand is empty during resolution, but state-based actions wait for the full spell.
        d.getHandSize(d.player1) shouldBe 7
        d.getHandSize(d.player2) shouldBe 7
        d.state.projectedState.getPower(crawler) shouldBe 7
        d.state.projectedState.getToughness(crawler) shouldBe 7
        d.state.stack.size shouldBe 7
        repeat(7) { d.bothPass().error shouldBe null }
        d.state.stack.size shouldBe 0
        d.getLifeTotal(d.player2) shouldBe 13
        d.getLifeTotal(d.player1) shouldBe 20
    }
})
