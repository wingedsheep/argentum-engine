package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Seven of Nine ({2}{U}{B} Legendary Artifact Creature — Human Scientist, 2/4):
 * "Whenever you face a dilemma, draw a card."
 */
class SevenOfNineScenarioTest : ScenarioTestBase() {

    init {
        test("casting a modal spell faces a dilemma and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Seven of Nine")
                .withCardInHand(1, "Make It So")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellWithMode(1, "Make It So", modeIndex = 1).error shouldBe null
            game.resolveStack()

            // Two from Make It So's mode, one from Seven of Nine's dilemma trigger.
            game.handSize(1) shouldBe 3
        }

        test("a non-modal spell is no dilemma") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Seven of Nine")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 0
        }
    }
}
