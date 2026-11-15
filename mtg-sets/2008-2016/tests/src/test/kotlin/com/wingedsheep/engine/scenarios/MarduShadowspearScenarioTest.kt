package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mardu Shadowspear (Fate Reforged #74) — {B} Creature — Human Warrior 1/1.
 *
 * "Whenever this creature attacks, each opponent loses 1 life. Dash {1}{B}"
 */
class MarduShadowspearScenarioTest : ScenarioTestBase() {
    init {
        test("attacking makes each opponent lose 1 life before combat damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mardu Shadowspear")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Mardu Shadowspear" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
