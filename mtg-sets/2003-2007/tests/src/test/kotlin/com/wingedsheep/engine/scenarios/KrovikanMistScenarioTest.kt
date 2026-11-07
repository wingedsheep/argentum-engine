package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Krovikan Mist (CSP #38) — {1}{U} Creature — Illusion, flying.
 * "Krovikan Mist's power and toughness are each equal to the number of Illusions on the battlefield."
 */
class KrovikanMistScenarioTest : ScenarioTestBase() {
    init {
        test("alone it counts itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Krovikan Mist")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mist = game.findPermanent("Krovikan Mist")!!

            game.state.projectedState.getPower(mist) shouldBe 1
            game.state.projectedState.getToughness(mist) shouldBe 1
            game.state.projectedState.hasKeyword(mist, Keyword.FLYING) shouldBe true
        }

        test("it counts Illusions under every player's control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Krovikan Mist")
                .withCardOnBattlefield(1, "Phantom Monster")
                .withCardOnBattlefield(2, "Palinchron")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mist = game.findPermanent("Krovikan Mist")!!

            withClue("itself, your Phantom Monster, and the opponent's Palinchron") {
                game.state.projectedState.getPower(mist) shouldBe 3
                game.state.projectedState.getToughness(mist) shouldBe 3
            }
        }
    }
}
