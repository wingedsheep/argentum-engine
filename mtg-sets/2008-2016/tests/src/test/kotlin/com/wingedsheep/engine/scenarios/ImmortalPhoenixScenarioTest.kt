package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Immortal Phoenix (PZ2 #70773) — {4}{R}{R} 5/3 Creature — Phoenix, flying.
 *
 *   When this creature dies, return it to its owner's hand.
 */
class ImmortalPhoenixScenarioTest : ScenarioTestBase() {

    init {
        context("Immortal Phoenix") {
            test("has flying and returns to its owner's hand when it dies") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Immortal Phoenix")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val phoenix = game.findPermanent("Immortal Phoenix")!!
                game.state.projectedState.hasKeyword(phoenix, Keyword.FLYING) shouldBe true

                game.castSpell(1, "Lightning Bolt", phoenix).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Immortal Phoenix") shouldBe false
                game.isInHand(2, "Immortal Phoenix") shouldBe true
                game.isInGraveyard(2, "Immortal Phoenix") shouldBe false
            }
        }
    }
}
