package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hatchery Spider (GRN #132): the undergrowth cast trigger reveals the top X cards, X = creature
 * cards in your graveyard, and offers only a green *permanent* card with mana value X or less.
 */
class HatcherySpiderScenarioTest : ScenarioTestBase() {

    init {
        context("Hatchery Spider cast trigger") {

            test("reveals X cards and puts only an eligible green permanent onto the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hatchery Spider")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    // Four creature cards -> X = 4; the instant doesn't count.
                    .apply { repeat(4) { withCardInGraveyard(1, "Grizzly Bears") } }
                    .withCardInGraveyard(1, "Giant Growth")
                    // Exactly X cards in the library, so all of them are revealed.
                    .withCardInLibrary(1, "Llanowar Elves")    // green permanent, MV 1 — eligible
                    .withCardInLibrary(1, "Force of Nature")   // green permanent, MV 5 > X
                    .withCardInLibrary(1, "Giant Growth")      // green, but not a permanent card
                    .withCardInLibrary(1, "Forest")            // colorless land
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hatchery Spider").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val elves = game.findCardsInLibrary(1, "Llanowar Elves").single()
                withClue("only the green permanent card with mana value <= X is selectable") {
                    decision.options shouldContainExactly listOf(elves)
                }

                game.selectCards(listOf(elves)).error shouldBe null
                game.resolveStack()

                withClue("Elves entered, then the Spider resolved") {
                    (game.findPermanent("Llanowar Elves") != null) shouldBe true
                    (game.findPermanent("Hatchery Spider") != null) shouldBe true
                }
                withClue("the rest went back to the library") {
                    game.librarySize(1) shouldBe 3
                    game.findCardsInLibrary(1, "Force of Nature").size shouldBe 1
                }
            }

            test("with no creature cards in the graveyard nothing is revealed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hatchery Spider")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardInLibrary(1, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hatchery Spider").error shouldBe null
                game.resolveStack()

                withClue("X = 0: no choice, the Elves stay in the library") {
                    game.hasPendingDecision() shouldBe false
                    game.findPermanent("Llanowar Elves") shouldBe null
                    game.findCardsInLibrary(1, "Llanowar Elves").size shouldBe 1
                    (game.findPermanent("Hatchery Spider") != null) shouldBe true
                }
            }
        }
    }
}
