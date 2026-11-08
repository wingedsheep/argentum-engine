package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Deranged Whelp (EMN #125) — {1}{R} Creature — Wolf, 2/1.
 *
 *   Menace
 */
class DerangedWhelpScenarioTest : ScenarioTestBase() {

    init {
        test("Deranged Whelp is a 2/1 with menace") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Deranged Whelp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val whelp = game.findPermanent("Deranged Whelp")!!
            game.state.projectedState.getPower(whelp) shouldBe 2
            game.state.projectedState.getToughness(whelp) shouldBe 1
            game.state.projectedState.hasKeyword(whelp, Keyword.MENACE) shouldBe true
        }
    }
}
