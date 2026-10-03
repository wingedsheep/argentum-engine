package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shrieking Drake (VIS) — "Flying. When this creature enters, return a creature you control to
 * its owner's hand." The return is chosen on resolution (no target), and the Drake itself is a
 * legal choice.
 */
class ShriekingDrakeScenarioTest : ScenarioTestBase() {

    init {
        context("Shrieking Drake enters trigger") {
            test("alone: the Drake must return itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shrieking Drake")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shrieking Drake").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Shrieking Drake") shouldBe false
                game.isInHand(1, "Shrieking Drake") shouldBe true
            }

            test("with another creature: the controller chooses on resolution and may keep the Drake") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shrieking Drake")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shrieking Drake").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                val bears = game.findPermanent("Grizzly Bears")
                bears shouldNotBe null
                decision.options.contains(game.findPermanent("Hill Giant")!!) shouldBe false
                game.selectCards(listOf(bears!!)).error shouldBe null

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Shrieking Drake") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("with another creature: the Drake may return itself instead") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shrieking Drake")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shrieking Drake").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val drake = game.findPermanent("Shrieking Drake")!!
                game.selectCards(listOf(drake)).error shouldBe null

                game.isInHand(1, "Shrieking Drake") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
