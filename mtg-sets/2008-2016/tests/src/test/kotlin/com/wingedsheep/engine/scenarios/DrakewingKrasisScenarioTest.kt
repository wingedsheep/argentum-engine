package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Drakewing Krasis (GTC) — 3/1 flying, trample. */
class DrakewingKrasisScenarioTest : ScenarioTestBase() {
    init {
        test("is a 3/1 with flying and trample") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drakewing Krasis")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val krasis = game.findPermanent("Drakewing Krasis")!!
            game.state.projectedState.hasKeyword(krasis, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(krasis, Keyword.TRAMPLE) shouldBe true
            game.state.projectedState.getPower(krasis) shouldBe 3
            game.state.projectedState.getToughness(krasis) shouldBe 1
        }
    }
}
