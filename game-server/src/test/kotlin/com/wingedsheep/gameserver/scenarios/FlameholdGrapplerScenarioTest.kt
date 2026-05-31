package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Flamehold Grappler (TDM #185).
 *
 * "First strike
 *  When this creature enters, copy the next spell you cast this turn when you cast it.
 *  You may choose new targets for the copy."
 *
 * Verifies the ETB arms a pending spell copy and that the next spell cast is copied.
 * Divination (draw two, no targets) is used so the copy needs no retargeting decision:
 * drawing two twice nets four cards.
 */
class FlameholdGrapplerScenarioTest : ScenarioTestBase() {

    init {
        context("Flamehold Grappler") {

            test("casting Grappler from hand then Divination draws four") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Flamehold Grappler")
                    .withCardInHand(1, "Divination")
                    // {U}{R}{W} for Grappler + {2}{U} for Divination.
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .also { b -> repeat(10) { b.withCardInLibrary(1, "Island") } }
                    .build()

                val castGrappler = game.castSpell(1, "Flamehold Grappler")
                withClue("Casting Flamehold Grappler should succeed: ${castGrappler.error}") {
                    castGrappler.error shouldBe null
                }
                game.resolveStack()

                withClue("Grappler's ETB should arm exactly one pending spell copy") {
                    game.state.pendingSpellCopies.size shouldBe 1
                }

                val handBefore = game.handSize(1)
                val castDivination = game.castSpell(1, "Divination")
                withClue("Casting Divination should succeed: ${castDivination.error}") {
                    castDivination.error shouldBe null
                }

                withClue("The pending copy should be consumed by Divination") {
                    game.state.pendingSpellCopies.size shouldBe 0
                }

                game.resolveStack()

                // hand before casting Divination minus Divination (cast) plus 4 drawn (2 + copy 2).
                withClue("Divination + its copy should draw four cards total") {
                    game.handSize(1) shouldBe handBefore - 1 + 4
                }
            }
        }
    }
}
