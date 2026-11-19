package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Sphinx's Disciple (BNG #51) — "Flying. Inspired — Whenever this creature becomes untapped, draw a card." */
class SphinxsDiscipleScenarioTest : ScenarioTestBase() {
    private fun untap(extra: ScenarioBuilder.() -> ScenarioBuilder = { this }): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Sphinx's Disciple", tapped = true)
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
        test("untapping draws a card") {
            val game = untap { withCardInLibrary(1, "Mountain") }
            val before = game.handSize(1)
            game.resolveStack()
            withClue("inspired draws one card") { game.handSize(1) shouldBe before + 1 }
        }
    }
}
