package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Valgavoth, Harrower of Souls (DSC 6).
 *
 * "Whenever an opponent loses life for the first time during each of their turns, put a +1/+1
 *  counter on Valgavoth and draw a card."
 */
class ValgavothHarrowerOfSoulsScenarioTest : ScenarioTestBase() {
    init {
        test("an opponent's first life loss on their own turn triggers once; the second doesn't") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Valgavoth, Harrower of Souls")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInHand(2, "Lightning Bolt")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val valgavoth = game.findPermanent("Valgavoth, Harrower of Souls")!!

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 2)
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
            withClue("first loss: +1/+1 counter and a card") {
                game.state.projectedState.getPower(valgavoth) shouldBe 5
                game.handSize(1) shouldBe 1
            }

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 2)
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 14
            withClue("second loss the same turn: no trigger") {
                game.state.projectedState.getPower(valgavoth) shouldBe 5
                game.handSize(1) shouldBe 1
            }
        }

        test("an opponent losing life during your turn doesn't trigger") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Valgavoth, Harrower of Souls")
                .withCardInLibrary(1, "Forest")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val valgavoth = game.findPermanent("Valgavoth, Harrower of Souls")!!

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17
            game.state.projectedState.getPower(valgavoth) shouldBe 4
            game.handSize(1) shouldBe 0
        }

        test("your own life loss on an opponent's turn doesn't trigger") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Valgavoth, Harrower of Souls")
                .withCardInLibrary(1, "Forest")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val valgavoth = game.findPermanent("Valgavoth, Harrower of Souls")!!

            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
            game.state.projectedState.getPower(valgavoth) shouldBe 4
            game.handSize(1) shouldBe 0
        }
    }
}
