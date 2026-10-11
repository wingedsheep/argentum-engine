package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Skyfisher Spider (BRO #221) — the dies trigger:
 *
 *   When this creature dies, you may gain 1 life for each creature card in your graveyard.
 *   If you do, exile this card from your graveyard.
 *
 * The count is taken at resolution with the Spider itself in the graveyard, and the exile finds
 * the card in the graveyard by source identity. Declining gains nothing and exiles nothing.
 */
class SkyfisherSpiderScenarioTest : ScenarioTestBase() {

    init {
        context("Skyfisher Spider dies trigger") {

            fun killSpider(): TestGame {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skyfisher Spider", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Plains")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spider = game.findPermanent("Skyfisher Spider")!!
                game.castSpell(1, "Lightning Bolt", targetId = spider).error shouldBe null
                game.resolveStack()
                return game
            }

            test("accepting gains life per creature card, Spider included, then exiles it") {
                val game = killSpider()
                val lifeBefore = game.getLifeTotal(1)

                withClue("the dies trigger asks whether to gain life") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("two Grizzly Bears plus the Spider itself are three creature cards") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 3
                }
                withClue("the Spider was exiled from the graveyard") {
                    game.isInGraveyard(1, "Skyfisher Spider") shouldBe false
                    game.isInExile(1, "Skyfisher Spider") shouldBe true
                }
            }

            test("declining gains no life and leaves the Spider in the graveyard") {
                val game = killSpider()
                val lifeBefore = game.getLifeTotal(1)

                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe lifeBefore
                game.isInGraveyard(1, "Skyfisher Spider") shouldBe true
            }
        }
    }
}
