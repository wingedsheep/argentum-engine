package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jori En, Ruin Diver — {1}{U}{R} 2/3 Legendary Merfolk Wizard.
 * "Whenever you cast your second spell each turn, draw a card."
 */
class JoriEnRuinDiverScenarioTest : ScenarioTestBase() {
    init {
        test("the second spell of the turn draws a card; the first does not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Jori En, Ruin Diver")
                .withCardsInHand(1, "Grizzly Bears", 2)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("the first spell draws nothing") {
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 2
            }

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            withClue("the second spell draws one card") {
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
