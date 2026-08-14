package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Kin-Tree Severance. */
class KinTreeSeveranceScenarioTest : ScenarioTestBase() {

    init {
        context("Kin-Tree Severance") {

            test("exiles target permanent with mana value 3 or greater") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Kin-Tree Severance")
                    .withLandsOnBattlefield(1, "Plains", 6) // pay {2/W}{2/B}{2/G} as all-generic
                    .withCardOnBattlefield(2, "Marshal of the Lost") // MV 4 creature
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Marshal of the Lost")!!
                game.castSpell(1, "Kin-Tree Severance", targetId = target)
                game.resolveStack()

                withClue("Target should be exiled") {
                    game.findPermanent("Marshal of the Lost") shouldBe null
                    game.state.getExile(game.player2Id).contains(target) shouldBe true
                }
            }
        }
    }
}
