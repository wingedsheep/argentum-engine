package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Amnesia — "Target player reveals their hand and discards all nonland cards."
 *
 * The pipeline gathers with a nonland predicate and moves the whole collection, so the thing worth
 * proving is that the filter actually discriminates: lands stay in hand while everything else goes.
 * A gather written without the predicate would empty the hand entirely and still look like the card
 * doing its job.
 */
class AmnesiaScenarioTest : ScenarioTestBase() {

    init {
        context("Amnesia — discards all nonland cards") {

            test("nonlands are discarded and lands are left in hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Amnesia")
                    .withLandsOnBattlefield(1, "Island", 6)
                    // Victim's hand: three nonlands and two lands.
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Craw Wurm")
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.handSize(2) shouldBe 5
                game.castSpellTargetingPlayer(1, "Amnesia", 2).error shouldBe null
                game.resolveStack()

                withClue("only the two lands survive") {
                    game.handSize(2) shouldBe 2
                }
                withClue("and they are the lands, not two arbitrary survivors") {
                    game.isInHand(2, "Forest") shouldBe true
                    game.isInHand(2, "Mountain") shouldBe true
                    game.isInHand(2, "Grizzly Bears") shouldBe false
                    game.isInHand(2, "Craw Wurm") shouldBe false
                    game.isInHand(2, "Lightning Bolt") shouldBe false
                }
                withClue("the nonlands went to the graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Lightning Bolt") shouldBe true
                }
            }

            test("an all-land hand is untouched") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Amnesia")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Amnesia", 2).error shouldBe null
                game.resolveStack()

                withClue("nothing to gather, and an empty move must not throw") {
                    game.handSize(2) shouldBe 2
                }
            }
        }
    }
}
