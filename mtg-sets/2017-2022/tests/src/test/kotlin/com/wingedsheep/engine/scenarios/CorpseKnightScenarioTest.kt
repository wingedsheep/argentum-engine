package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Corpse Knight (M20 #206) — {W}{B} Creature — Zombie Knight 2/2.
 *
 *   Whenever another creature you control enters, each opponent loses 1 life.
 */
class CorpseKnightScenarioTest : ScenarioTestBase() {

    init {
        context("Corpse Knight") {

            test("another creature entering under your control drains each opponent for 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Corpse Knight")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 20
            }

            test("Corpse Knight entering itself does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Corpse Knight")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Corpse Knight").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Corpse Knight") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }

            test("an opponent's creature entering does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Corpse Knight")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("Bears entered for Player2") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
