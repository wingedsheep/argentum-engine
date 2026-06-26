package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Commune with Beavers (FIN #182).
 *
 * Commune with Beavers — {G} Sorcery
 *   "Look at the top three cards of your library. You may reveal an artifact, creature, or land
 *    card from among them and put it into your hand. Put the rest on the bottom of your library
 *    in any order."
 *
 * Verifies the `Patterns.Library.lookAtTopRevealMatchingToHand` pipeline with an
 * artifact-or-creature-or-land filter: matching cards are selectable, a non-matching card is
 * shown but not selectable, the reveal is optional (0..1), and the rest go to the bottom.
 */
class CommuneWithBeaversScenarioTest : ScenarioTestBase() {

    init {
        context("Commune with Beavers") {

            test("reveal a creature; only artifact/creature/land selectable; rest go to bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Commune with Beavers")
                    .withLandsOnBattlefield(1, "Forest", 1) // {G}
                    // Top three: a creature, a land, and an instant (non-matching).
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Commune with Beavers")
                withClue("Casting Commune with Beavers should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val decision = game.state.pendingDecision
                withClue("A selection decision should be presented") { (decision != null) shouldBe true }
                val select = decision as SelectCardsDecision

                // "May" reveal → 0..1 selections.
                select.minSelections shouldBe 0
                select.maxSelections shouldBe 1

                val bear = game.findCardsInLibrary(1, "Grizzly Bears").first()
                val forest = game.findCardsInLibrary(1, "Forest").first()
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").first()

                // Creature and land are selectable; the instant is shown but not selectable.
                select.options shouldContainExactlyInAnyOrder listOf(bear, forest)
                select.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bolt)

                game.selectCards(listOf(bear))
                game.resolveStack()

                withClue("The revealed creature ends up in hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("The other two looked-at cards remain in the library (put on bottom)") {
                    game.findCardsInLibrary(1, "Forest").isNotEmpty() shouldBe true
                    game.findCardsInLibrary(1, "Lightning Bolt").size shouldBe 1
                }
                withClue("Nothing was milled to the graveyard") {
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 0
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 0
                }
                game.findCardsInGraveyard(1, "Commune with Beavers").size shouldBe 1
            }

            test("the reveal is optional — declining keeps all three in the library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Commune with Beavers")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Commune with Beavers")
                game.resolveStack()

                val select = game.state.pendingDecision as SelectCardsDecision
                select.minSelections shouldBe 0

                // Decline the optional reveal.
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("Nothing went to hand from the look") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
                }
                withClue("All looked-at cards remain in the library (none milled)") {
                    game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
                    game.findCardsInLibrary(1, "Lightning Bolt").size shouldBe 1
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                }
            }
        }
    }
}
