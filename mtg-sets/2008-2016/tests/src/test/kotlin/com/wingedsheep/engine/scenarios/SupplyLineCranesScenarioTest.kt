package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Supply-Line Cranes (JOU #28) — {3}{W}{W} Creature — Bird 2/4.
 *
 *   Flying
 *   When this creature enters, put a +1/+1 counter on target creature.
 */
class SupplyLineCranesScenarioTest : ScenarioTestBase() {
    init {
        test("its entry puts a +1/+1 counter on the targeted creature") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Supply-Line Cranes")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Supply-Line Cranes").error shouldBe null
            game.resolveStack()

            withClue("the enters trigger asks for a target creature") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("the Bears carry the counter and are a 3/3") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }
        }
    }
}
