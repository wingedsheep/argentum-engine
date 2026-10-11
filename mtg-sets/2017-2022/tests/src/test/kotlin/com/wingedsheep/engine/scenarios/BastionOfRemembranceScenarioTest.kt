package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bastion of Remembrance (IKO #73) — {2}{B} Enchantment.
 *
 *   When this enchantment enters, create a 1/1 white Human Soldier creature token.
 *   Whenever a creature you control dies, each opponent loses 1 life and you gain 1 life.
 */
class BastionOfRemembranceScenarioTest : ScenarioTestBase() {

    init {
        context("Bastion of Remembrance") {

            test("entering creates a 1/1 Human Soldier token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bastion of Remembrance")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Bastion of Remembrance")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Bastion is on the battlefield") {
                    game.isOnBattlefield("Bastion of Remembrance") shouldBe true
                }
                withClue("its enters trigger created exactly one Human Soldier token") {
                    game.findPermanents("Human Soldier Token").size shouldBe 1
                }
            }

            test("a creature you control dying drains each opponent for 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bastion of Remembrance")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("the Bears died") { game.isInGraveyard(1, "Grizzly Bears") shouldBe true }
                withClue("each opponent loses 1 life") { game.getLifeTotal(2) shouldBe 19 }
                withClue("you gain 1 life") { game.getLifeTotal(1) shouldBe 21 }
            }

            test("an opponent's creature dying does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bastion of Remembrance")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()

                withClue("the opponent's Bears died") { game.isInGraveyard(2, "Grizzly Bears") shouldBe true }
                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
