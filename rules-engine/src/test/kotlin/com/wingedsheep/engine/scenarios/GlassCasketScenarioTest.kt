package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Glass Casket. */
class GlassCasketScenarioTest : ScenarioTestBase() {

    init {
        context("Glass Casket — linked exile for cheap creatures") {
            test("exiles a cheap creature and returns it when the Casket dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Glass Casket")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val card = game.findCardsInHand(1, "Glass Casket").first()
                game.execute(CastSpell(game.player1Id, card, emptyList())).error shouldBe null
                game.resolveStack() // Casket enters -> ETB trigger asks for its target

                if (game.hasPendingDecision()) game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears (mana value 2) is exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }

                val casket = game.findPermanent("Glass Casket")!!
                game.castSpell(1, "Shatter", casket)
                game.resolveStack()

                withClue("the Casket leaving returns its linked exile to the battlefield") {
                    game.isInExile(2, "Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
