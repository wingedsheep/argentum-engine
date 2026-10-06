package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gorging Vulture (M20 #102) — {2}{B} Creature — Bird, 2/2, Flying.
 *
 * "When this creature enters, mill four cards. You gain 1 life for each creature card milled
 *  this way."
 *
 * Covers that only the creature cards among the milled cards count, and that a short library
 * mills (and counts) only what is there.
 */
class GorgingVultureScenarioTest : ScenarioTestBase() {

    init {
        context("Gorging Vulture") {

            test("gains 1 life per creature card among the four milled") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Gorging Vulture")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gorging Vulture").error shouldBe null
                game.resolveStack()

                withClue("all four cards were milled") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 2
                    game.findCardsInGraveyard(1, "Swamp").size shouldBe 2
                }
                withClue("two creature cards milled -> gain 2") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("a short library mills what is there and counts only those creatures") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Gorging Vulture")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gorging Vulture").error shouldBe null
                game.resolveStack()

                game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                game.getLifeTotal(1) shouldBe 21
            }
        }
    }
}
