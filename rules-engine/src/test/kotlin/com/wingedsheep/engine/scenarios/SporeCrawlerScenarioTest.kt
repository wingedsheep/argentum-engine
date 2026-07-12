package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Spore Crawler (VOW #222) — {2}{G} Creature — Fungus, 3/2.
 *
 *   When this creature dies, draw a card.
 *
 * Exercises the Triggers.Dies + DrawCardsEffect(1) composition: killing the creature with
 * lethal damage fires the trigger and draws a card.
 */
class SporeCrawlerScenarioTest : ScenarioTestBase() {

    init {
        context("Spore Crawler dies trigger") {

            test("draws a card when it dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spore Crawler", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crawler = game.findPermanent("Spore Crawler")!!
                val handBefore = game.handSize(1)

                // Lightning Bolt deals 3 damage, lethal to the 3/2 Spore Crawler.
                game.castSpell(1, "Lightning Bolt", crawler).error shouldBe null
                game.resolveStack() // resolve the bolt (and the resulting SBA death)
                game.resolveStack() // resolve the dies trigger

                withClue("Spore Crawler died") {
                    game.isOnBattlefield("Spore Crawler") shouldBe false
                    game.isInGraveyard(1, "Spore Crawler") shouldBe true
                }
                // Hand: -1 (Lightning Bolt leaves hand) + 1 (drawn card) = net 0.
                withClue("Dying draws a card") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
