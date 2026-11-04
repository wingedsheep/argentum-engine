package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unconventional Tactics (HOU #27).
 *
 * Target creature gets +3/+3 and gains flying until end of turn.
 * Whenever a Zombie you control enters, you may pay {W}. If you do, return this card from your
 * graveyard to your hand.
 */
class UnconventionalTacticsScenarioTest : ScenarioTestBase() {

    init {
        context("Unconventional Tactics") {

            test("gives target creature +3/+3 and flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Unconventional Tactics")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Unconventional Tactics", bears).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(bears) shouldBe 5
                projected.getToughness(bears) shouldBe 5
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            }

            test("a Zombie entering under your control lets you pay {W} to return it from the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Unconventional Tactics")
                    .withCardInHand(1, "Black Creature") // a {1}{B} 2/2 Zombie
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Plains", 2) // auto-pay may spend one on the Zombie's generic
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Black Creature").error shouldBe null
                game.resolveStack()

                withClue("the graveyard trigger offers the {W} payment") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true)
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInHand(1, "Unconventional Tactics") shouldBe true
                game.isInGraveyard(1, "Unconventional Tactics") shouldBe false
            }
        }
    }
}
