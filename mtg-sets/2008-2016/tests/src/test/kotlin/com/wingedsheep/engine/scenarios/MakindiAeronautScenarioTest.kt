package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import io.kotest.matchers.shouldBe

/**
 * Makindi Aeronaut — {1}{W} 1/3 Kor Scout Ally. "Flying"
 */
class MakindiAeronautScenarioTest : ScenarioTestBase() {
    init {
        test("a white 1/3 Ally with flying") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Makindi Aeronaut")
                .build()
            val aeronaut = game.findPermanent("Makindi Aeronaut")!!
            game.state.projectedState.hasKeyword(aeronaut, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasSubtype(aeronaut, "Ally") shouldBe true
            game.state.projectedState.getColors(aeronaut) shouldBe setOf(Color.WHITE.name)
            game.state.projectedState.getPower(aeronaut) shouldBe 1
            game.state.projectedState.getToughness(aeronaut) shouldBe 3
        }
    }
}
