package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * End-Raze Forerunners (RNA #124) — "When this creature enters, other creatures you control get
 * +2/+2 and gain vigilance and trample until end of turn." The Forerunners itself and opponents'
 * creatures are untouched.
 */
class EndRazeForerunnersScenarioTest : ScenarioTestBase() {

    init {
        context("End-Raze Forerunners") {

            test("entering pumps other creatures you control and grants vigilance and trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "End-Raze Forerunners")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myBears = game.findPermanents("Grizzly Bears")
                    .first { game.state.projectedState.getController(it) == game.player1Id }
                val theirBears = game.findPermanents("Grizzly Bears")
                    .first { game.state.projectedState.getController(it) == game.player2Id }

                game.castSpell(1, "End-Raze Forerunners").error shouldBe null
                game.resolveStack()

                val forerunners = game.findPermanent("End-Raze Forerunners")!!
                val projected = game.state.projectedState
                withClue("your other creature gets +2/+2, vigilance and trample") {
                    projected.getPower(myBears) shouldBe 4
                    projected.getToughness(myBears) shouldBe 4
                    projected.hasKeyword(myBears, Keyword.VIGILANCE) shouldBe true
                    projected.hasKeyword(myBears, Keyword.TRAMPLE) shouldBe true
                }
                withClue("the Forerunners itself is not pumped") {
                    projected.getPower(forerunners) shouldBe 7
                    projected.getToughness(forerunners) shouldBe 7
                }
                withClue("the opponent's creature is untouched") {
                    projected.getPower(theirBears) shouldBe 2
                    projected.hasKeyword(theirBears, Keyword.TRAMPLE) shouldBe false
                }
            }
        }
    }
}
