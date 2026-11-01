package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Collar the Culprit (GRN #5) — {3}{W} Instant.
 *
 *   Destroy target creature with toughness 4 or greater.
 */
class CollarTheCulpritScenarioTest : ScenarioTestBase() {

    init {
        test("destroys a creature with toughness 4 or greater") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Collar the Culprit")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Collar the Culprit", targetId = wurm).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Craw Wurm") shouldBe true
        }

        test("can't target a creature with toughness less than 4") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Collar the Culprit")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Collar the Culprit", targetId = giant).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
