package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mist Intruder — a flying ingest creature. Ingest fires only on combat damage to a player, so a
 * reach blocker that stops the flier stops the ingest too.
 */
class MistIntruderScenarioTest : ScenarioTestBase() {
    init {
        test("unblocked, it exiles the top card of the defending player's library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mist Intruder")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Mist Intruder" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 19
            game.isInExile(2, "Island") shouldBe true
        }

        test("a reach blocker stops the flier, and with it ingest") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Mist Intruder")
                .withCardOnBattlefield(2, "Giant Spider")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Mist Intruder" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Giant Spider" to listOf("Mist Intruder"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 20
            game.isInExile(2, "Island") shouldBe false
            game.librarySize(2) shouldBe 1
        }
    }
}
