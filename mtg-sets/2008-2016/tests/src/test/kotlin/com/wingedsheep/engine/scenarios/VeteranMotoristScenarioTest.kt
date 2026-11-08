package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class VeteranMotoristScenarioTest : ScenarioTestBase() {

    init {
        context("Veteran Motorist") {
            test("crewing a Vehicle gives that Vehicle +1/+1 once it becomes a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Veteran Motorist", summoningSickness = false)
                    .withCardOnBattlefield(1, "Sky Skiff")
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val motorist = game.findPermanent("Veteran Motorist")!!
                val skiff = game.findPermanent("Sky Skiff")!!

                val crew = game.execute(CrewVehicle(game.player1Id, skiff, listOf(motorist)))
                withClue("crew should succeed: ${crew.error}") { crew.error shouldBe null }
                // The crews trigger goes on the stack above the crew ability and resolves first,
                // pumping a Vehicle that is not yet a creature; the bonus must still apply once
                // the crew ability animates it.
                game.resolveStack()

                val projected = game.state.projectedState
                projected.isCreature(skiff) shouldBe true
                projected.getPower(skiff) shouldBe 3
                projected.getToughness(skiff) shouldBe 4
            }
        }
    }
}
