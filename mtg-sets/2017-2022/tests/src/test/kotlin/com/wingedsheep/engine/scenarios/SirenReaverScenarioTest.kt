package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Siren Reaver (RIX #54) — "Raid — This spell costs {1} less to cast if you attacked this turn.
 * Flying"
 */
class SirenReaverScenarioTest : ScenarioTestBase() {

    private fun build(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Siren Reaver")
        .withLandsOnBattlefield(1, "Island", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("without attacking, three lands can't pay for it") {
            val game = build()
            game.castSpell(1, "Siren Reaver").error shouldNotBe null
        }

        test("after attacking it costs {2}{U}") {
            val game = build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.castSpell(1, "Siren Reaver").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Siren Reaver") shouldBe true
        }
    }
}
