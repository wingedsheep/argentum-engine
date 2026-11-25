package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Glorious Charge (M10 #11) — {1}{W} Instant.
 *   Creatures you control get +1/+1 until end of turn.
 */
class GloriousChargeScenarioTest : ScenarioTestBase() {
    init {
        test("pumps only the caster's creatures") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Glorious Charge")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val (myBears, theirBears) = game.findPermanents("Grizzly Bears")
                .partition { game.state.getBattlefield(game.player1Id).contains(it) }
                .let { it.first.single() to it.second.single() }
            val lions = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Glorious Charge").error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(myBears) shouldBe 3
            projected.getToughness(myBears) shouldBe 3
            projected.getPower(lions) shouldBe 2
            projected.getToughness(lions) shouldBe 2
            withClue("the opponent's creature is untouched") {
                projected.getPower(theirBears) shouldBe 2
                projected.getToughness(theirBears) shouldBe 2
            }
        }
    }
}
