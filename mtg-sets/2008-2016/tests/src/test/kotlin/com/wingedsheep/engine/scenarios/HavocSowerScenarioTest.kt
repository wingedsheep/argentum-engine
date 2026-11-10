package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Havoc Sower — {3}{B} 3/3 Eldrazi Drone.
 * "Devoid. {1}{C}: This creature gets +2/+1 until end of turn."
 */
class HavocSowerScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Havoc Sower")!!.script.activatedAbilities.single().id

        test("devoid, and {1}{C} pumps it +2/+1") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Havoc Sower")
                .withLandsOnBattlefield(1, "Wastes", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val sower = game.findPermanent("Havoc Sower")!!
            game.state.projectedState.getColors(sower) shouldBe emptySet()

            val result = game.execute(ActivateAbility(game.player1Id, sower, pumpId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(sower) shouldBe 5
            game.state.projectedState.getToughness(sower) shouldBe 4
        }
    }
}
