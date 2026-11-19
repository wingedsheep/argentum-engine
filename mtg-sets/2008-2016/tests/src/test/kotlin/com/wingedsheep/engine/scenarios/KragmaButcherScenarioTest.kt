package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kragma Butcher (BNG #100) — "Inspired — Whenever this creature becomes untapped, it gets +2/+0
 * until end of turn."
 */
class KragmaButcherScenarioTest : ScenarioTestBase() {
    init {
        test("untapping in the untap step makes it a 4/3 for the turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Kragma Butcher", tapped = true)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()
            val butcher = game.findPermanent("Kragma Butcher")!!

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            withClue("inspired gives +2/+0") {
                game.state.projectedState.getPower(butcher) shouldBe 4
                game.state.projectedState.getToughness(butcher) shouldBe 3
            }
        }
    }
}
