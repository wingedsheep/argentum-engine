package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Yamabushi's Storm (CHK #199) — "Yamabushi's Storm deals 1 damage to each creature. If a creature
 * dealt damage this way would die this turn, exile it instead."
 */
class YamabushisStormScenarioTest : ScenarioTestBase() {

    init {
        context("Yamabushi's Storm") {

            test("each creature takes 1 damage; those it kills are exiled, on both sides") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Yamabushi's Storm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Yamabushi's Storm").error shouldBe null
                game.resolveStack()

                game.isInExile(1, "Llanowar Elves") shouldBe true
                game.isInExile(2, "Llanowar Elves") shouldBe true
                game.isInGraveyard(1, "Llanowar Elves") shouldBe false
                game.isInGraveyard(2, "Llanowar Elves") shouldBe false
                withClue("a 2/2 survives 1 damage") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
            }

            test("a creature that survives the Storm but dies later this turn is still exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Yamabushi's Storm")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Yamabushi's Storm").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe true

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Shock", bears).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }
        }
    }
}
