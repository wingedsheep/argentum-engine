package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cut a Deal — "Each opponent draws a card, then you draw a card for each opponent who drew a
 * card this way." The count is frozen from the opponents' libraries before they draw.
 */
class CutADealScenarioTest : ScenarioTestBase() {
    init {
        test("the opponent draws one, then you draw one for them") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cut a Deal")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Cut a Deal")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.handSize(2) shouldBe 1
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 1
        }

        test("an opponent with an empty library drew nothing, so you draw nothing") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cut a Deal")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cut a Deal").error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 1
        }
    }
}
