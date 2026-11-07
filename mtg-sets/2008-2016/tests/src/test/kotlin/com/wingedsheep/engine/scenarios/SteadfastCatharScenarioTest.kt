package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Steadfast Cathar (EMN #44) — {1}{W} 2/1 Creature — Human Soldier.
 *
 *   Whenever this creature attacks, it gets +0/+2 until end of turn.
 */
class SteadfastCatharScenarioTest : ScenarioTestBase() {

    init {
        test("attacking gives it +0/+2 until end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Steadfast Cathar")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cathar = game.findPermanent("Steadfast Cathar")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Steadfast Cathar" to 2)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(cathar) shouldBe 2
            game.state.projectedState.getToughness(cathar) shouldBe 3
        }
    }
}
