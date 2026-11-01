package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Emmara, Soul of the Accord (GRN #168) — {G}{W} 2/2 Legendary Elf Cleric.
 *
 *   Whenever Emmara becomes tapped, create a 1/1 white Soldier creature token with lifelink.
 */
class EmmaraSoulOfTheAccordScenarioTest : ScenarioTestBase() {

    init {
        test("attacking taps Emmara and creates a 1/1 lifelink Soldier") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Emmara, Soul of the Accord", tapped = false, summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Emmara, Soul of the Accord" to 2)).error shouldBe null
            game.resolveStack()

            val soldier = game.findPermanents("Soldier Token").single()
            game.state.projectedState.getPower(soldier) shouldBe 1
            game.state.projectedState.getToughness(soldier) shouldBe 1
            game.state.projectedState.hasKeyword(soldier, Keyword.LIFELINK) shouldBe true
        }
    }
}
