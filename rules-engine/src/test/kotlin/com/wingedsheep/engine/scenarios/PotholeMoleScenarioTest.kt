package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pothole Mole (DFT #176) — {2}{G} Creature — Mole, 2/3.
 *
 *   "When this creature enters, mill three cards, then you may return a land card from your
 *    graveyard to your hand."
 *
 * The mill happens first, so a land milled by this very ability is a legal pick. "You may return"
 * is a resolution-time "up to one" choice — declining leaves everything in the graveyard.
 */
class PotholeMoleScenarioTest : ScenarioTestBase() {

    init {
        context("Pothole Mole enters trigger") {

            test("mills three, then returns a land milled by this very ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pothole Mole")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pothole Mole")
                game.resolveStack()

                withClue("The mole resolved onto the battlefield") {
                    game.findPermanent("Pothole Mole") shouldNotBe null
                }
                withClue("All three library cards were milled") {
                    game.librarySize(1) shouldBe 0
                    game.graveyardSize(1) shouldBe 3
                }

                val decision = game.state.pendingDecision as? SelectCardsDecision
                withClue("The optional return pauses for a selection") {
                    decision shouldNotBe null
                }
                val plains = game.findCardsInGraveyard(1, "Plains")
                withClue("Only the milled land is offered — creatures and artifacts are not") {
                    decision!!.options shouldContainExactly plains
                    decision.minSelections shouldBe 0
                    decision.maxSelections shouldBe 1
                }

                game.selectCards(plains)

                withClue("The chosen land moves from the graveyard to your hand") {
                    game.isInHand(1, "Plains") shouldBe true
                    game.isInGraveyard(1, "Plains") shouldBe false
                    game.graveyardSize(1) shouldBe 2
                }
            }

            test("declining the optional return leaves the land in the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pothole Mole")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pothole Mole")
                game.resolveStack()

                (game.state.pendingDecision as? SelectCardsDecision) shouldNotBe null
                game.skipSelection()

                withClue("Declining returns nothing — the milled land stays put") {
                    game.isInHand(1, "Plains") shouldBe false
                    game.isInGraveyard(1, "Plains") shouldBe true
                    game.graveyardSize(1) shouldBe 3
                }
            }

            test("an empty graveyard of lands skips the prompt entirely") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pothole Mole")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Sol Ring")
                    .withCardInLibrary(1, "Pacifism")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pothole Mole")
                game.resolveStack()

                withClue("Nothing to return, so the ability finishes without a decision") {
                    game.graveyardSize(1) shouldBe 3
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
