package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Zhang He, Wei General (PTK): attacking gives each OTHER creature you control +1/+0. */
class ZhangHeWeiGeneralScenarioTest : ScenarioTestBase() {
    init {
        test("attacking pumps other creatures but not Zhang He") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Zhang He, Wei General", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val zhang = game.findPermanent("Zhang He, Wei General")!!
            val mine = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Zhang He, Wei General" to 2)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(zhang) shouldBe 4
            game.state.projectedState.getPower(mine) shouldBe 3
        }
    }
}
