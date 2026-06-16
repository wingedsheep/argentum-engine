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
 * Sterling Supplier (OTJ #33) — {4}{W} Bird Soldier, 3/4, Flying.
 *
 *   "When this creature enters, put a +1/+1 counter on another target creature you control."
 *
 * Verifies the ETB trigger targets *another* creature the controller owns and adds a +1/+1
 * counter (a permanent stat boost via the counter, distinct from the Supplier itself).
 */
class SterlingSupplierScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Sterling Supplier") {

            test("ETB puts a +1/+1 counter on another creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sterling Supplier")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                var projected = stateProjector.project(game.state)
                projected.getPower(bears) shouldBe 2
                projected.getToughness(bears) shouldBe 2

                val cast = game.castSpell(1, "Sterling Supplier")
                withClue("Casting Sterling Supplier should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // resolve the creature spell; ETB trigger goes on the stack

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the ETB trigger; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(bears))))
                game.resolveStack()

                projected = stateProjector.project(game.state)
                withClue("Grizzly Bears gains a +1/+1 counter from Sterling Supplier's ETB") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }

                val supplier = game.findPermanent("Sterling Supplier")!!
                projected.hasKeyword(supplier, Keyword.FLYING) shouldBe true
            }
        }
    }
}
