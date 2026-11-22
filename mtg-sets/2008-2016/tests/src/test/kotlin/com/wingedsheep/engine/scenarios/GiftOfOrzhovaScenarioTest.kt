package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Gift of Orzhova (GTC) — enchanted creature gets +1/+1 and has flying and lifelink. */
class GiftOfOrzhovaScenarioTest : ScenarioTestBase() {
    init {
        test("enchanted creature is 3/3 with flying and lifelink") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Gift of Orzhova", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val projected = game.state.projectedState

            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
            projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
        }
    }
}
