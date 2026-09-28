package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Righteous Fury — "Destroy all tapped creatures. You gain 2 life for each creature destroyed this way."
 */
class RighteousFuryScenarioTest : ScenarioTestBase() {

    init {
        context("Righteous Fury") {

            test("destroys only tapped creatures on both sides and gains 2 life for each") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Righteous Fury")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Savannah Lions", tapped = true)
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Righteous Fury").error shouldBe null
                game.resolveStack()

                withClue("tapped creatures die") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
                withClue("untapped creatures survive") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("two tapped creatures destroyed -> 4 life") {
                    game.getLifeTotal(1) shouldBe 24
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
