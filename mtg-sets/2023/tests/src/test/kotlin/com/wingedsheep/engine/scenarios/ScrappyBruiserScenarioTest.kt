package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Scrappy Bruiser — attack trigger pumps an attacker +2/+0 and trample, then bounces it at end of combat. */
class ScrappyBruiserScenarioTest : ScenarioTestBase() {
    init {
        test("pumps the chosen attacker and returns it to hand at end of combat") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Scrappy Bruiser")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Scrappy Bruiser" to 2, "Grizzly Bears" to 2)).error shouldBe null
            val bears = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 20 - 3 - 4
        }
    }
}
