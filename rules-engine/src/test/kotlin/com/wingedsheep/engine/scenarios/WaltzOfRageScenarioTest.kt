package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/** Scenario tests for Waltz of Rage. */
class WaltzOfRageScenarioTest : ScenarioTestBase() {

    init {
        context("Waltz of Rage — chosen creature pings each other creature; deaths impulse-draw") {

            test("chosen creature deals power damage to each other creature and survives itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Waltz of Rage")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // 2/2
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanents("Hill Giant").first()
                game.castSpell(1, "Waltz of Rage", giant).error shouldBe null
                game.resolveStack()

                // Hill Giant (power 3) deals 3 to each OTHER creature, so both 2/2 Grizzly Bears die,
                // while Hill Giant itself survives (it's excluded from "each other creature").
                withClue("Hill Giant survives (excluded from 'each other creature')") {
                    game.findPermanents("Hill Giant").size shouldBe 1
                }
                withClue("Both 2/2 Grizzly Bears took 3 damage and died") {
                    game.findAllPermanents("Grizzly Bears").size shouldBe 0
                }
                withClue("Player 1's creature death triggered the delayed impulse-draw to exile") {
                    game.state.getExile(game.player1Id).size shouldBeGreaterThanOrEqual 1
                }
            }
        }
    }
}
