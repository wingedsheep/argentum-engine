package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tome Raider (ELD #68) — {2}{U} Creature — Faerie 1/1.
 *
 * "Flying. When this creature enters, draw a card."
 */
class TomeRaiderScenarioTest : ScenarioTestBase() {

    init {
        context("Tome Raider") {

            test("entering draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Tome Raider")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Tome Raider").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Tome Raider") shouldBe true
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
