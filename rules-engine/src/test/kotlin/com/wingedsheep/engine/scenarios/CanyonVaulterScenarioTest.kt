package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.ScenarioTestBase.TestGame
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class CanyonVaulterScenarioTest : ScenarioTestBase() {

    init {
        context("Canyon Vaulter") {
            test("crewing a Vehicle during your main phase grants that Vehicle flying") {
                val game = canyonGame(Step.PRECOMBAT_MAIN)
                val vaulter = game.findPermanent("Canyon Vaulter")!!
                val ferry = game.findPermanent("Spotcycle Scouter")!!

                val crew = game.execute(CrewVehicle(game.player1Id, ferry, listOf(vaulter)))
                withClue("crew should succeed: ${crew.error}") { crew.error shouldBe null }
                game.resolveStack()

                game.state.projectedState.hasKeyword(ferry, Keyword.FLYING) shouldBe true
            }

            test("crewing outside your main phase does not grant flying") {
                val game = canyonGame(Step.DECLARE_ATTACKERS)
                val vaulter = game.findPermanent("Canyon Vaulter")!!
                val ferry = game.findPermanent("Spotcycle Scouter")!!

                val crew = game.execute(CrewVehicle(game.player1Id, ferry, listOf(vaulter)))
                withClue("crew should be legal at instant speed: ${crew.error}") {
                    crew.error shouldBe null
                }
                game.resolveStack()

                game.state.projectedState.hasKeyword(ferry, Keyword.FLYING) shouldBe false
            }
        }
    }

    private fun canyonGame(step: Step): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Canyon Vaulter", summoningSickness = false)
        .withCardOnBattlefield(1, "Spotcycle Scouter")
        .withActivePlayer(1)
        .withTurnNumber(3)
        .inPhase(if (step == Step.PRECOMBAT_MAIN) Phase.PRECOMBAT_MAIN else Phase.COMBAT, step)
        .build()
}
