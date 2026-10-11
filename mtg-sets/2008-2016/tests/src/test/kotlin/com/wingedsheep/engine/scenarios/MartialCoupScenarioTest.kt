package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Martial Coup (CON #11).
 *
 * "Create X 1/1 white Soldier creature tokens. If X is 5 or more, destroy all other creatures."
 *
 * The wipe spares exactly the Soldiers this spell just made — not the caster's older creatures,
 * and not Soldier tokens that were already on the battlefield.
 */
class MartialCoupScenarioTest : ScenarioTestBase() {

    private fun battle(x: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Martial Coup")
        .withLandsOnBattlefield(1, "Plains", x + 2)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Craw Wurm")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Martial Coup") {

            test("X = 5: five Soldiers, every other creature is destroyed") {
                val game = battle(5)

                game.castXSpell(1, "Martial Coup", 5).error shouldBe null
                game.resolveStack()

                withClue("the five new Soldiers survive the wipe") {
                    game.findPermanents("Soldier Token").size shouldBe 5
                }
                withClue("both the opponent's and the caster's own creatures are destroyed") {
                    game.isOnBattlefield("Craw Wurm") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("X = 4: four Soldiers, nothing is destroyed") {
                val game = battle(4)

                game.castXSpell(1, "Martial Coup", 4).error shouldBe null
                game.resolveStack()

                game.findPermanents("Soldier Token").size shouldBe 4
                withClue("below the threshold the other creatures stay") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("a second X = 5 Coup destroys the first Coup's Soldiers") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Martial Coup")
                    .withCardInHand(1, "Martial Coup")
                    .withLandsOnBattlefield(1, "Plains", 14)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Martial Coup", 5).error shouldBe null
                game.resolveStack()
                game.findPermanents("Soldier Token").size shouldBe 5

                game.castXSpell(1, "Martial Coup", 5).error shouldBe null
                game.resolveStack()

                withClue("only the second spell's five Soldiers remain") {
                    game.findPermanents("Soldier Token").size shouldBe 5
                }
            }
        }
    }
}
