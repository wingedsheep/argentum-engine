package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Moldervine Reclamation (M20 #214) — "Whenever a creature you control dies, you gain 1 life and
 * draw a card."
 */
class MoldervineReclamationScenarioTest : ScenarioTestBase() {

    init {
        context("Moldervine Reclamation") {

            test("a creature you control dying gains 1 life and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Moldervine Reclamation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                val handAfterCast = game.handSize(1)
                game.resolveStack()

                withClue("Grizzly Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("you gain 1 life") { game.getLifeTotal(1) shouldBe lifeBefore + 1 }
                withClue("you draw a card") { game.handSize(1) shouldBe handAfterCast + 1 }
            }

            test("an opponent's creature dying does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Moldervine Reclamation")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lifeBefore = game.getLifeTotal(1)
                game.castSpell(1, "Murder", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                val handAfterCast = game.handSize(1)
                game.resolveStack()

                withClue("Grizzly Bears died") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                game.getLifeTotal(1) shouldBe lifeBefore
                game.handSize(1) shouldBe handAfterCast
            }
        }
    }
}
