package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nimble Birdsticker (M20 #333) — {2}{R} Creature — Goblin, 2/3. Reach.
 */
class NimbleBirdstickerScenarioTest : ScenarioTestBase() {

    init {
        context("Nimble Birdsticker") {

            test("reach lets it block and kill an attacking flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nimble Birdsticker")
                    .withCardOnBattlefield(2, "Wind Drake") // 2/2 flier
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Wind Drake" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Nimble Birdsticker" to listOf("Wind Drake"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.isOnBattlefield("Wind Drake") shouldBe false
                game.isOnBattlefield("Nimble Birdsticker") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
