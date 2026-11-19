package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Pheres-Band Tromper (BNG #134) — "Inspired — Whenever this creature becomes untapped, put a +1/+1 counter on it." */
class PheresBandTromperScenarioTest : ScenarioTestBase() {
    private fun untap(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Pheres-Band Tromper", tapped = true)
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
        test("untapping puts a +1/+1 counter on it") {
            val game = untap()
            game.resolveStack()
            val tromper = game.findPermanent("Pheres-Band Tromper")!!
            withClue("a 3/3 with one counter is a 4/4") {
                game.state.projectedState.getPower(tromper) shouldBe 4
                game.state.projectedState.getToughness(tromper) shouldBe 4
            }
        }
    }
}
