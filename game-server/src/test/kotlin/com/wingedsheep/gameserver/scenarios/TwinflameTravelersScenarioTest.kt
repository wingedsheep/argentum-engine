package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Twinflame Travelers.
 *
 * Card reference:
 * - Twinflame Travelers ({2}{U}{R}): 3/3 Creature — Elemental Sorcerer
 *   Flying
 *   If a triggered ability of another Elemental you control triggers,
 *   it triggers an additional time.
 */
class TwinflameTravelersScenarioTest : ScenarioTestBase() {

    init {
        context("Twinflame Travelers") {

            test("ETB trigger of another Elemental fires an additional time") {
                // Twinflame Travelers on the battlefield, cast Flamekin Gildweaver (Elemental
                // with "When this creature enters, create a Treasure token"). The trigger
                // should fire twice and produce two Treasure tokens.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Twinflame Travelers")
                    .withCardInHand(1, "Flamekin Gildweaver")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Flamekin Gildweaver")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell — the ETB trigger goes on the stack twice (once
                // naturally, once from Twinflame Travelers).
                game.resolveStack()

                // No pending decision — Treasure creation needs no targets.
                game.hasPendingDecision() shouldBe false

                val treasures = game.findAllPermanents("Treasure")
                withClue("Should have 2 Treasure tokens (trigger fired twice)") {
                    treasures.size shouldBe 2
                }
            }

            test("without Twinflame Travelers, ETB trigger fires only once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Flamekin Gildweaver")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Flamekin Gildweaver")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val treasures = game.findAllPermanents("Treasure")
                withClue("Should have exactly 1 Treasure token (trigger fires once)") {
                    treasures.size shouldBe 1
                }
            }
        }
    }
}
