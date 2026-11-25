package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Craterize (M13 #126) — {3}{R} Sorcery.
 *   Destroy target land.
 */
class CraterizeScenarioTest : ScenarioTestBase() {
    init {
        test("destroys the target land") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Craterize")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val forest = game.findPermanent("Forest")!!

            val result = game.castSpell(1, "Craterize", forest)
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Forest") shouldBe true
        }
    }
}
