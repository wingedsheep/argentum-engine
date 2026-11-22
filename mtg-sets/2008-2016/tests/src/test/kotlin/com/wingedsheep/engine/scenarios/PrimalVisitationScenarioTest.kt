package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Primal Visitation (GTC) — enchanted creature gets +3/+3 and has haste. */
class PrimalVisitationScenarioTest : ScenarioTestBase() {
    init {
        test("gives +3/+3 and haste, letting a fresh creature attack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                .withCardAttachedTo(1, "Primal Visitation", "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 5
            projected.hasKeyword(bears, Keyword.HASTE) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        }
    }
}
