package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Malicious Malfunction (NEO #110): "All creatures get -2/-2 until end of turn. If a creature would
 * die this turn, exile it instead."
 *
 * The replacement covers every creature — yours too — and lasts the turn, so a creature that
 * survives the -2/-2 and dies later that turn is exiled as well.
 */
class MaliciousMalfunctionScenarioTest : ScenarioTestBase() {

    init {
        context("Malicious Malfunction") {
            test("creatures it kills are exiled, on both sides, and a later death this turn is exiled too") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Malicious Malfunction")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Malicious Malfunction").error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true

                game.castSpell(1, "Shock", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe false
            }
        }
    }
}
