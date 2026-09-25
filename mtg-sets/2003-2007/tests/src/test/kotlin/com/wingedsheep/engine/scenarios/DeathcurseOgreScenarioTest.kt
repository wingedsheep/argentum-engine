package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deathcurse Ogre (CHK #109) — "When this creature dies, each player loses 3 life."
 *
 * "Each player" includes the Ogre's own controller, so both life totals must drop.
 */
class DeathcurseOgreScenarioTest : ScenarioTestBase() {

    init {
        context("Deathcurse Ogre") {

            test("dying makes each player — its controller included — lose 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Deathcurse Ogre")
                    .withCardInHand(2, "Murder")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ogre = game.findPermanent("Deathcurse Ogre")!!

                game.castSpell(2, "Murder", ogre).error shouldBe null
                game.resolveStack()

                withClue("the Ogre died") { game.isInGraveyard(1, "Deathcurse Ogre") shouldBe true }
                withClue("its controller lost 3") { game.getLifeTotal(1) shouldBe 17 }
                withClue("the opponent lost 3") { game.getLifeTotal(2) shouldBe 17 }
            }
        }
    }
}
