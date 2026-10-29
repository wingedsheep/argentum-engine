package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Turret Ogre (WAR #148) — {3}{R} 4/3 Reach.
 *
 *   When this creature enters, if you control another creature with power 4 or greater,
 *   this creature deals 2 damage to each opponent.
 *
 * The Ogre is power 4 itself, so the "another" exclusion is what this proves: alone it does
 * nothing; next to a power-4 creature it deals 2 to the opponent.
 */
class TurretOgreScenarioTest : ScenarioTestBase() {

    init {
        context("Turret Ogre ETB") {

            test("with another power-4 creature you control, deals 2 damage to each opponent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Turret Ogre")
                    .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Turret Ogre").error shouldBe null
                game.resolveStack()

                withClue("opponent takes 2 damage") { game.getLifeTotal(2) shouldBe 18 }
                withClue("controller is untouched") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("the Ogre's own power doesn't satisfy 'another creature'") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Turret Ogre")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Turret Ogre").error shouldBe null
                game.resolveStack()

                withClue("Turret Ogre is on the battlefield") { game.isOnBattlefield("Turret Ogre") shouldBe true }
                withClue("no damage: Hill Giant is power 3 and the opponent's Craw Wurm isn't yours") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
