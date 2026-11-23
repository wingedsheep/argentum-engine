package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Duskmantle Seer (Gatecrash).
 *
 * "At the beginning of your upkeep, each player reveals the top card of their library, loses life
 *  equal to that card's mana value, then puts it into their hand."
 */
class DuskmantleSeerScenarioTest : ScenarioTestBase() {

    init {
        test("each player loses life equal to their own top card's mana value and puts it into hand") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UNTAP)
                .withCardOnBattlefield(1, "Duskmantle Seer")
                .withCardInLibrary(1, "Laughing Mad") // mana value 3 ({2}{R})
                .withCardInLibrary(2, "Grizzly Bears") // mana value 2 ({1}{G})
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            withClue("controller's revealed card goes to their hand") {
                game.isInHand(1, "Laughing Mad") shouldBe true
            }
            withClue("opponent's revealed card goes to their own hand") {
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
            withClue("controller loses life equal to their card's mana value (3)") {
                game.getLifeTotal(1) shouldBe 17
            }
            withClue("opponent loses life equal to their card's mana value (2)") {
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
