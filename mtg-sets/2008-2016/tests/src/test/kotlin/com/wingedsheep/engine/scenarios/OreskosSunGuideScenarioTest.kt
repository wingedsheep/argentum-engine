package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Oreskos Sun Guide (BNG #22) — "Inspired — Whenever this creature becomes untapped, you gain 2 life." */
class OreskosSunGuideScenarioTest : ScenarioTestBase() {
    private fun untap(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Oreskos Sun Guide", tapped = true)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .extra()
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        return game
    }

    init {
        test("untapping in the untap step gains 2 life") {
            val game = untap()
            game.resolveStack()
            withClue("inspired gains 2 life") { game.getLifeTotal(1) shouldBe 22 }
        }
    }
}
