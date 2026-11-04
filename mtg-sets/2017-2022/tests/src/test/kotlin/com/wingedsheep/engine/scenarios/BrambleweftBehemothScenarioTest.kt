package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class BrambleweftBehemothScenarioTest : ScenarioTestBase() {
    init {
        test("tramples over a 2 toughness blocker for 4 damage") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Brambleweft Behemoth", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            game.declareAttackers(mapOf("Brambleweft Behemoth" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Brambleweft Behemoth"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 16
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Brambleweft Behemoth") shouldBe true
        }
    }
}
