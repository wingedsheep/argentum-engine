package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class RecklessVelocitaurScenarioTest : ScenarioTestBase() {

    init {
        context("Reckless Velocitaur") {
            test("saddling a Mount during your main phase buffs that Mount and grants trample") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Reckless Velocitaur", summoningSickness = false)
                    .withCardOnBattlefield(1, "Alacrian Jaguar", summoningSickness = false)
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val velocitaur = game.findPermanent("Reckless Velocitaur")!!
                val jaguar = game.findPermanent("Alacrian Jaguar")!!

                val saddle = game.execute(SaddleMount(game.player1Id, jaguar, listOf(velocitaur)))
                withClue("saddle should succeed: ${saddle.error}") { saddle.error shouldBe null }
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(jaguar) shouldBe 6
                projected.getToughness(jaguar) shouldBe 4
                projected.hasKeyword(jaguar, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
