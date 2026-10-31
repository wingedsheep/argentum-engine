package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Thunderous Snapper (ELD #215) — {G/U}{G/U}{G/U}{G/U} Creature — Turtle Hydra 4/4.
 *
 * "Whenever you cast a spell with mana value 5 or greater, draw a card."
 */
class ThunderousSnapperScenarioTest : ScenarioTestBase() {

    init {
        context("Thunderous Snapper") {

            test("casting a mana value 6 spell draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thunderous Snapper")
                    .withCardInHand(1, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Craw Wurm").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 1
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }

            test("casting a spell with mana value below 5 draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thunderous Snapper")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 0
            }
        }
    }
}
