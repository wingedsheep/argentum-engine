package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Overgrown Pest (MOM #197) — {2}{G} Creature — Pest 2/2.
 *
 *   When this creature enters, look at the top five cards of your library. You may reveal a land
 *   or double-faced card from among them and put that card into your hand. Put the rest on the
 *   bottom of your library in a random order.
 */
class OvergrownPestScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Overgrown Pest")
        .withLandsOnBattlefield(1, "Forest", 3)
        // Exactly five cards: a land, a double-faced card, and three that qualify as neither.
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Invasion of Pyrulea")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Overgrown Pest ETB") {
            test("only the land and the double-faced card are offered; the DFC can be taken") {
                val game = buildGame()

                game.castSpell(1, "Overgrown Pest").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("selection offers exactly the land and the double-faced card") {
                    (decision is SelectCardsDecision) shouldBe true
                    decision as SelectCardsDecision
                    decision.options.size shouldBe 2
                    decision.minSelections shouldBe 0
                    decision.maxSelections shouldBe 1
                }
                val options = (decision as SelectCardsDecision).options
                val invasion = options.first { game.state.getEntity(it)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Invasion of Pyrulea" }

                game.selectCards(listOf(invasion)).error shouldBe null
                game.resolveStack()

                withClue("the double-faced card is in hand, the rest back in the library") {
                    game.isInHand(1, "Invasion of Pyrulea") shouldBe true
                    game.isInHand(1, "Forest") shouldBe false
                    game.librarySize(1) shouldBe 4
                }
                (game.findPermanent("Overgrown Pest") != null) shouldBe true
            }

            test("declining keeps all five cards in the library") {
                val game = buildGame()

                game.castSpell(1, "Overgrown Pest").error shouldBe null
                game.resolveStack()
                game.skipSelection()
                game.resolveStack()

                game.isInHand(1, "Forest") shouldBe false
                game.isInHand(1, "Invasion of Pyrulea") shouldBe false
                game.librarySize(1) shouldBe 5
            }
        }
    }
}
