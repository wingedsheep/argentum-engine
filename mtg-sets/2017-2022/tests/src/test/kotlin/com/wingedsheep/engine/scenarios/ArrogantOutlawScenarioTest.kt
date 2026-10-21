package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Arrogant Outlaw (MID #84) — {2}{B} 3/2 whose ETB reads "if an opponent lost life this turn,
 * each opponent loses 2 life and you gain 2 life." Proves the intervening "if" gates the drain.
 */
class ArrogantOutlawScenarioTest : ScenarioTestBase() {

    init {
        context("Arrogant Outlaw") {

            test("drains 2 when an opponent already lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Arrogant Outlaw")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 17

                game.castSpell(1, "Arrogant Outlaw").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 15
                game.getLifeTotal(1) shouldBe 22
            }

            test("does nothing when no opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Arrogant Outlaw")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Arrogant Outlaw").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Arrogant Outlaw") shouldBe true
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
