package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Essenceknit Scholar (Secrets of Strixhaven #187).
 *
 * Essenceknit Scholar ({B}{B/G}{G}, 3/1, Dryad Warlock):
 *   When this creature enters, create a 1/1 black and green Pest creature token with
 *   "Whenever this token attacks, you gain 1 life."
 *   At the beginning of your end step, if a creature died under your control this turn, draw a card.
 *
 * Exercises the enters-the-battlefield Pest token and the conditional end-step draw.
 */
class EssenceknitScholarScenarioTest : ScenarioTestBase() {

    init {
        context("Essenceknit Scholar — ETB Pest + conditional end-step draw") {

            test("entering creates a Pest token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Essenceknit Scholar")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Essenceknit Scholar")
                withClue("Essenceknit Scholar should cast: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("A Pest token should have been created") {
                    (game.findPermanent("Pest Token") != null) shouldBe true
                }
            }

            test("end step draws a card when a creature died under your control this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Essenceknit Scholar")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Lightning Bolt", targetId = bears)
                withClue("Lightning Bolt should kill our Grizzly Bears: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()
                withClue("Grizzly Bears died") { game.findPermanent("Grizzly Bears") shouldBe null }

                // hand after casting Bolt = handBefore - 1 (the Bolt). Advance to end step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("End-step trigger drew a card (creature died this turn)") {
                    game.handSize(1) shouldBe (handBefore - 1) + 1
                }
            }

            test("end step does NOT draw when no creature died this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Essenceknit Scholar")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("No creature died, so no draw") {
                    game.handSize(1) shouldBe handBefore
                }
            }
        }
    }
}
