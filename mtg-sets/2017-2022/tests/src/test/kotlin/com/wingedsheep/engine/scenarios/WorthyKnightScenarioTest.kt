package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Worthy Knight (ELD #36) — {1}{W} Creature — Human Knight 2/2.
 *
 * "Whenever you cast a Knight spell, create a 1/1 white Human creature token."
 */
class WorthyKnightScenarioTest : ScenarioTestBase() {

    init {
        context("Worthy Knight") {

            test("casting a Knight spell creates a 1/1 white Human token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Worthy Knight")
                    .withCardInHand(1, "Knight of the Keep")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Knight of the Keep").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Knight of the Keep") shouldBe true
                val tokens = game.findAllPermanents("Human Token")
                tokens.size shouldBe 1
                game.state.projectedState.getPower(tokens.single()) shouldBe 1
                game.state.projectedState.getToughness(tokens.single()) shouldBe 1
            }

            test("casting a non-Knight spell creates no token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Worthy Knight")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.findAllPermanents("Human Token").size shouldBe 0
            }
        }
    }
}
