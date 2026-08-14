package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Frontline Rush. */
class FrontlineRushScenarioTest : ScenarioTestBase() {

    init {
        context("Frontline Rush") {

            test("token mode creates two 1/1 red Goblins") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Frontline Rush")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Frontline Rush", modeIndex = 0)
                game.resolveStack()

                withClue("Two Goblin tokens should exist") {
                    game.findPermanents("Goblin Token").size shouldBe 2
                }
            }

            test("pump mode gives +X/+X where X is the number of creatures you control") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Frontline Rush")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 — X = 2
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanents("Glory Seeker").first()
                game.castSpellWithMode(1, "Frontline Rush", modeIndex = 1, targetId = target)
                game.resolveStack()

                val clientState = game.getClientState(1)
                val pumped = clientState.cards[target]
                withClue("Targeted Glory Seeker should be 4/4 (2/2 + 2 creatures you control)") {
                    pumped shouldNotBe null
                    pumped!!.power shouldBe 4
                    pumped.toughness shouldBe 4
                }
            }
        }
    }
}
