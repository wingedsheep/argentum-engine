package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glistening Deluge: all creatures get -1/-1; green and/or white creatures get an additional -2/-2.
 */
class GlisteningDelugeScenarioTest : ScenarioTestBase() {

    init {
        test("-1/-1 to everything, additional -2/-2 to green and white creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Glistening Deluge")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Hill Giant") // red 3/3 -> 2/2
                .withCardOnBattlefield(2, "Grizzly Bears") // green 2/2 -> dies
                .withCardOnBattlefield(2, "Serra Angel") // white 4/4 -> 1/1
                .withCardOnBattlefield(2, "Wind Drake") // blue 2/2 -> 1/1
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Glistening Deluge").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            val projected = game.state.projectedState
            val giant = game.findPermanent("Hill Giant")!!
            projected.getPower(giant) shouldBe 2
            projected.getToughness(giant) shouldBe 2
            val angel = game.findPermanent("Serra Angel")!!
            projected.getPower(angel) shouldBe 1
            projected.getToughness(angel) shouldBe 1
            val drake = game.findPermanent("Wind Drake")!!
            projected.getPower(drake) shouldBe 1
            projected.getToughness(drake) shouldBe 1
            game.findPermanent("Serra Angel") shouldNotBe null
        }
    }
}
