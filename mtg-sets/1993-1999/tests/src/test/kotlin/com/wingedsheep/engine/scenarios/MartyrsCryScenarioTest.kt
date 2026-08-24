package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Martyr's Cry — "Exile all white creatures. For each creature exiled this way,
 * its controller draws a card."
 *
 * The draws have to be attributed *per controller*, and controllers are stripped the moment a
 * permanent leaves the battlefield — so the board is deliberately lopsided (two white creatures on
 * one side, one on the other) and the two hands must grow by different amounts. A version that
 * captured controllers after the move, or drew for the caster only, would fail exactly there.
 *
 * Non-white creatures are the control: the sweep is a colour test, not a board wipe.
 */
class MartyrsCryScenarioTest : ScenarioTestBase() {

    init {
        context("Martyr's Cry") {

            test("every white creature is exiled and each controller draws for their own") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Martyr's Cry")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    // Mine: two white, one green decoy.
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(1, "Squire")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // Theirs: one white.
                    .withCardOnBattlefield(2, "Savannah Lions")
                    // The scenario builder starts both libraries empty; the payoff is a draw, so
                    // stock enough that neither player is drawing from nothing.
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myHandBefore = game.handSize(1)
                val theirHandBefore = game.handSize(2)

                game.castSpell(1, "Martyr's Cry").error shouldBe null
                game.resolveStack()

                withClue("all three white creatures left the battlefield") {
                    game.findPermanents("Savannah Lions").size shouldBe 0
                    game.isOnBattlefield("Squire") shouldBe false
                }
                withClue("the green creature is untouched — this is a colour sweep") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("I lost two white creatures, so I draw two (the Cry itself left my hand)") {
                    game.handSize(1) shouldBe myHandBefore - 1 + 2
                }
                withClue("they lost one, so they draw one") {
                    game.handSize(2) shouldBe theirHandBefore + 1
                }
            }
        }
    }
}
