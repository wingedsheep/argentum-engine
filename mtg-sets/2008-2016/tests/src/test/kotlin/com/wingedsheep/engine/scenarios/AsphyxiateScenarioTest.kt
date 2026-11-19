package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Asphyxiate (BNG #60) — "Destroy target untapped creature."
 */
class AsphyxiateScenarioTest : ScenarioTestBase() {

    init {
        test("destroys an untapped creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Asphyxiate")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Asphyxiate", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
        }

        test("a tapped creature is not a legal target") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Asphyxiate")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Asphyxiate", game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }
    }
}
