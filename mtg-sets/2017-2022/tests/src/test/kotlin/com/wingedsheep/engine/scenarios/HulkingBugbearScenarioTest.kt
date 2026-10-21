package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hulking Bugbear (AFR #149) — {1}{R}{R} Creature — Goblin 3/3, haste.
 */
class HulkingBugbearScenarioTest : ScenarioTestBase() {

    init {
        test("haste lets it attack the turn it is cast") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hulking Bugbear")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hulking Bugbear").error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hulking Bugbear" to 2)).error shouldBe null
        }
    }
}
