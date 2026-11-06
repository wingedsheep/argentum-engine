package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Exquisite Archangel — "If you would lose the game, instead exile this creature and your life
 * total becomes equal to your starting life total." The loss routes and edge cases are pinned by
 * the engine's GameLossReplacementTest; this proves the printed card.
 */
class ExquisiteArchangelScenarioTest : ScenarioTestBase() {

    init {
        context("Exquisite Archangel") {
            test("a lethal Lightning Bolt exiles it and resets its controller to the starting life total") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Exquisite Archangel")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                game.state.gameOver shouldBe false
                game.getLifeTotal(1) shouldBe 20
                game.isInExile(1, "Exquisite Archangel") shouldBe true
                game.findPermanent("Exquisite Archangel") shouldBe null
            }

            test("it only covers its controller: the opponent still loses") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Exquisite Archangel")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(2, 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }
        }
    }
}
