package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Elim Garak, Simple Tailor (TRK #106) — {2}{B} Legendary Creature — Cardassian Rogue 3/3.
 *
 *   Menace
 *   Whenever you face a dilemma, each opponent loses 2 life and you gain 2 life.
 *
 * Choosing a mode for a modal spell (Make It So, "Choose one —") is facing a dilemma; a non-modal
 * spell isn't, and an opponent's dilemma isn't yours.
 */
class ElimGarakSimpleTailorScenarioTest : ScenarioTestBase() {

    init {
        context("Elim Garak, Simple Tailor") {

            test("casting a modal spell drains each opponent for 2 and gains you 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elim Garak, Simple Tailor")
                    .withCardInHand(1, "Make It So")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Make It So", modeIndex = 1).error shouldBe null
                game.resolveStack()

                withClue("Garak's trigger drained the opponent and gained its controller 2") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }
                withClue("Make It So itself still resolved") {
                    game.handSize(1) shouldBe 2
                }
            }

            test("casting a non-modal spell is not a dilemma") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elim Garak, Simple Tailor")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("an opponent casting a modal spell does not trigger Garak") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elim Garak, Simple Tailor")
                    .withCardInHand(2, "Make It So")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(2, "Make It So", modeIndex = 1).error shouldBe null
                game.resolveStack()

                withClue("the opponent's spell resolved") {
                    game.handSize(2) shouldBe 2
                }
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
