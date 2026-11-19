package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Excoriate (BNG #10) — "Exile target tapped creature."
 */
class ExcoriateScenarioTest : ScenarioTestBase() {

    private fun setup(tapped: Boolean): TestGame = scenario()
        .withPlayers("P1", "P2")
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = tapped)
        .withCardInHand(1, "Excoriate")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exiles a tapped creature") {
            val game = setup(tapped = true)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Excoriate", bears).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Grizzly Bears") shouldBe true
        }

        test("can't target an untapped creature") {
            val game = setup(tapped = false)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Excoriate", bears).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
