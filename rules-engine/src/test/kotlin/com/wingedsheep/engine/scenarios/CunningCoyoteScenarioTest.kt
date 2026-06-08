package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cunning Coyote (OTJ #118) — {1}{R} Coyote, 2/2, Haste, Plot {1}{R}.
 *
 *   "When this creature enters, another target creature you control gets +1/+1 and gains haste
 *    until end of turn."
 *
 * Verifies the ETB trigger targets *another* creature the controller owns (not the Coyote
 * itself) and grants +1/+1 plus haste until end of turn, which wears off next turn.
 */
class CunningCoyoteScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Cunning Coyote") {

            test("ETB pumps another creature you control with +1/+1 and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Cunning Coyote")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                var projected = stateProjector.project(game.state)
                projected.getPower(bears) shouldBe 2
                projected.getToughness(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.HASTE) shouldBe false

                val cast = game.castSpell(1, "Cunning Coyote")
                withClue("Casting Cunning Coyote should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // resolve the creature spell; ETB trigger goes on the stack

                // The ETB trigger pauses for its "another target creature you control" choice.
                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the ETB trigger; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(bears))))
                game.resolveStack()

                projected = stateProjector.project(game.state)
                withClue("Grizzly Bears gets +1/+1 and haste from the Coyote's ETB") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                    projected.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
            }
        }
    }
}
