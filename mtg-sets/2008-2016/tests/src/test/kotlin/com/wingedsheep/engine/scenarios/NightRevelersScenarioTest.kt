package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Night Revelers (ISD #153) — {4}{R} 4/4 Vampire.
 *
 *   This creature has haste as long as an opponent controls a Human.
 */
class NightRevelersScenarioTest : ScenarioTestBase() {

    init {
        test("has haste while an opponent controls a Human") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Night Revelers", summoningSickness = true)
                .withCardOnBattlefield(2, "Elite Vanguard")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val revelers = game.findPermanent("Night Revelers")!!
            game.state.projectedState.hasKeyword(revelers, Keyword.HASTE) shouldBe true
        }

        test("a Human you control doesn't grant haste") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Night Revelers", summoningSickness = true)
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val revelers = game.findPermanent("Night Revelers")!!
            game.state.projectedState.hasKeyword(revelers, Keyword.HASTE) shouldBe false
        }
    }
}
