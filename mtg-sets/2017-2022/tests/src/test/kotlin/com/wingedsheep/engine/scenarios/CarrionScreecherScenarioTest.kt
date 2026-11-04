package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CarrionScreecherScenarioTest : ScenarioTestBase() {
    init {
        test("a ground creature cannot block its flying attack") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Carrion Screecher", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            game.declareAttackers(mapOf("Carrion Screecher" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Carrion Screecher"))).error shouldNotBe null
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 17
        }
    }
}
