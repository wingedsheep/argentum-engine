package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Karsus Depthguard — Defender, but attacks freely while its power is 5 or greater. */
class KarsusDepthguardScenarioTest : ScenarioTestBase() {
    private fun game(withGrowth: Boolean) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Karsus Depthguard")
        .withCardOnBattlefield(1, "Grizzly Bears") // decoy so the declare-attackers step isn't skipped
        .also { if (withGrowth) it.withCardInHand(1, "Giant Growth").withLandsOnBattlefield(1, "Forest", 1) }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("at power 4 it cannot attack") {
            val game = game(false)
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            (game.declareAttackers(mapOf("Karsus Depthguard" to 2)).error != null) shouldBe true
        }

        test("at power 5 it can attack despite defender") {
            val game = game(true)
            game.castSpell(1, "Giant Growth", game.findPermanent("Karsus Depthguard")!!).error shouldBe null
            game.resolveStack()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Karsus Depthguard" to 2)).error shouldBe null
        }
    }
}
