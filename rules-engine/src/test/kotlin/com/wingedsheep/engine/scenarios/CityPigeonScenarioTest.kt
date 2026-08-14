package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * City Pigeon — {W} Creature — Bird (1/1)
 *   Flying
 *   When this creature leaves the battlefield, create a Food token.
 *
 * Pins the leaves-the-battlefield Food trigger: destroying the Pigeon sends it to the
 * graveyard and, as a result of leaving the battlefield, its controller gets a Food token.
 */
class CityPigeonScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "City Pigeon")
        .withCardInHand(1, "Doom Blade")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .build()

    init {
        test("creates a Food token when it leaves the battlefield") {
            val g = game()
            val pigeon = g.findPermanent("City Pigeon")!!

            withClue("no Food before the Pigeon leaves") {
                g.findPermanents("Food").size shouldBe 0
            }

            // Doom Blade destroys the (nonblack) Pigeon.
            g.castSpell(1, "Doom Blade", pigeon).error shouldBe null
            g.resolveStack()

            withClue("Pigeon is gone from the battlefield") {
                g.isOnBattlefield("City Pigeon") shouldBe false
            }
            withClue("Pigeon is in its owner's graveyard") {
                g.isInGraveyard(1, "City Pigeon") shouldBe true
            }
            withClue("leaves-the-battlefield trigger created one Food token") {
                g.findPermanents("Food").size shouldBe 1
            }
        }
    }
}
