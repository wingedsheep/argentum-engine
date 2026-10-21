package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Drider (AFR #98): Reach; whenever it deals combat damage to a player, create a 2/1 black
 * Spider token with reach and menace.
 */
class DriderScenarioTest : ScenarioTestBase() {

    init {
        test("combat damage to a player creates a 2/1 Spider with reach and menace") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drider", summoningSickness = false)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Drider" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 16
            val spider = game.findPermanent("Spider Token")!!
            val projected = game.state.projectedState
            projected.getPower(spider) shouldBe 2
            projected.getToughness(spider) shouldBe 1
            projected.hasKeyword(spider, Keyword.REACH) shouldBe true
            projected.hasKeyword(spider, Keyword.MENACE) shouldBe true
        }
    }
}
