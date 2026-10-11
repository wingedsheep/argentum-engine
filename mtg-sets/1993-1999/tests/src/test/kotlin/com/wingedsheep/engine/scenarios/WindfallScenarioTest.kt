package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Windfall — "Each player discards their hand, then draws cards equal to the
 * greatest number of cards a player discarded this way."
 *
 * The draw count has to be captured *before* the discards (every hand is empty afterwards) and has
 * to be the maximum across players, not the caster's own count. So the board is asymmetric: the
 * caster holds one card besides Windfall, the opponent holds four — both players must end on four.
 */
class WindfallScenarioTest : ScenarioTestBase() {

    init {
        context("Windfall") {

            test("each player draws the greatest number discarded, not their own count") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Windfall")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Craw Wurm")
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Island")
                }
                val game = builder.build()

                game.castSpell(1, "Windfall").error shouldBe null
                game.resolveStack()

                withClue("the opponent discarded four, so both players draw four") {
                    game.handSize(1) shouldBe 4
                    game.handSize(2) shouldBe 4
                }
                withClue("the old hands went to the graveyard (plus Windfall itself for the caster)") {
                    game.graveyardSize(1) shouldBe 2
                    game.graveyardSize(2) shouldBe 4
                }
                withClue("four cards left each library") {
                    game.librarySize(1) shouldBe 6
                    game.librarySize(2) shouldBe 6
                }
            }

            test("nobody has cards to discard: nobody draws") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Windfall")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Island")
                }
                val game = builder.build()

                game.castSpell(1, "Windfall").error shouldBe null
                game.resolveStack()

                withClue("zero discarded this way means zero drawn") {
                    game.handSize(1) shouldBe 0
                    game.handSize(2) shouldBe 0
                    game.librarySize(1) shouldBe 5
                    game.librarySize(2) shouldBe 5
                }
            }
        }
    }
}
