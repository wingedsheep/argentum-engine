package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Warclamp Mastiff (M13 #40) — {W} 1/1 Creature — Dog. First strike.
 */
class WarclampMastiffScenarioTest : ScenarioTestBase() {
    init {
        test("first strike kills a 2/1 blocker before it can strike back") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Warclamp Mastiff")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Warclamp Mastiff" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Warclamp Mastiff"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the blocker died to first-strike damage") { game.isInGraveyard(2, "Savannah Lions") shouldBe true }
            withClue("the Mastiff survived") { game.isOnBattlefield("Warclamp Mastiff") shouldBe true }
        }
    }
}
