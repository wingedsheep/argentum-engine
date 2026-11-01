package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kaya's Wrath — "Destroy all creatures. You gain life equal to the number of creatures you
 * controlled that were destroyed this way."
 *
 * Proves the life gain counts only *your* creatures, and only those actually destroyed (an
 * indestructible creature of yours survives and doesn't count).
 */
class KayasWrathScenarioTest : ScenarioTestBase() {

    init {
        context("Kaya's Wrath") {

            test("destroys every creature and gains life only for your creatures destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Kaya's Wrath")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Darksteel Myr")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kaya's Wrath").error shouldBe null
                game.resolveStack()

                withClue("all destructible creatures die") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("indestructible creature survives") {
                    game.isOnBattlefield("Darksteel Myr") shouldBe true
                }
                withClue("two of your creatures destroyed -> 2 life; opponent's don't count") {
                    game.getLifeTotal(1) shouldBe 22
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
