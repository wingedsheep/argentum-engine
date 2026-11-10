package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Linvala, the Preserver — two independent intervening-if ETB triggers:
 * gain 5 life if an opponent has more life than you, and create a 3/3 flying Angel if an
 * opponent controls more creatures than you.
 */
class LinvalaThePreserverScenarioTest : ScenarioTestBase() {

    private fun baseScenario() = scenario()
        .withPlayers()
        .withCardInHand(1, "Linvala, the Preserver")
        .withLandsOnBattlefield(1, "Plains", 6)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Linvala, the Preserver") {

            test("behind on life and creatures: gains 5 life and makes an Angel") {
                val game = baseScenario()
                    .withLifeTotal(1, 10)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()

                val cast = game.castSpell(1, "Linvala, the Preserver")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Linvala, the Preserver") shouldBe true
                game.getLifeTotal(1) shouldBe 15
                withClue("Linvala counts toward your creatures, but 1 < 2 still holds") {
                    game.findPermanents("Angel Token").size shouldBe 1
                }
            }

            test("ahead or tied on both counts: neither trigger does anything") {
                val game = baseScenario()
                    // Equal life is not "more life than you".
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    // After Linvala enters you control 1 creature, matching the opponent's 1.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()

                game.castSpell(1, "Linvala, the Preserver").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
                game.findPermanents("Angel Token").size shouldBe 0
            }

            test("the two checks are independent: behind on life only gains life") {
                val game = baseScenario()
                    .withLifeTotal(1, 5)
                    .build()

                game.castSpell(1, "Linvala, the Preserver").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 10
                game.findPermanents("Angel Token").size shouldBe 0
            }
        }
    }
}
