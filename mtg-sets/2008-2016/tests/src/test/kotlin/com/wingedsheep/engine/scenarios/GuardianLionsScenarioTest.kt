package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Guardian Lions (M13 #17) — {4}{W} 1/6 Creature — Cat. Vigilance.
 */
class GuardianLionsScenarioTest : ScenarioTestBase() {
    init {
        test("attacking doesn't tap Guardian Lions") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Guardian Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val lions = game.findPermanent("Guardian Lions")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Guardian Lions" to 2)).error shouldBe null

            withClue("vigilance keeps the attacker untapped") {
                game.state.getEntity(lions)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
