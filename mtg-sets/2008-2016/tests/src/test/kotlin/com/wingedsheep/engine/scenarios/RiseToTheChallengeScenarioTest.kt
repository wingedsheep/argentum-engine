package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Rise to the Challenge (BNG #107) — "Target creature gets +2/+0 and gains first strike until end of turn." */
class RiseToTheChallengeScenarioTest : ScenarioTestBase() {
    init {
        test("target creature gets +2/+0 and first strike") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Rise to the Challenge")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Rise to the Challenge", bears).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Grizzly Bears is a 4/2 first striker") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 2
                projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            }
        }
    }
}
