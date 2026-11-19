package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deepwater Hypnotist (BNG #35) — "Inspired — Whenever this creature becomes untapped, target
 * creature an opponent controls gets -3/-0 until end of turn."
 */
class DeepwaterHypnotistScenarioTest : ScenarioTestBase() {

    init {
        test("untapping gives the opponent's creature -3/-0") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Deepwater Hypnotist", tapped = true)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            if (game.hasPendingDecision()) game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Hill Giant is 0/3 until end of turn") {
                projected.getPower(giant) shouldBe 0
                projected.getToughness(giant) shouldBe 3
            }
        }
    }
}
