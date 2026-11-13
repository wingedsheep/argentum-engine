package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dominator Drone (DDP #51 / BFZ #92): "When this creature enters, if you control another
 * colorless creature, each opponent loses 2 life."
 *
 * The intervening "if" reads projected colour — a devoid creature counts as colorless even
 * though its mana cost is black — never counts the Drone itself, and is rechecked on resolution.
 */
class DominatorDroneScenarioTest : ScenarioTestBase() {

    init {
        context("Dominator Drone") {

            test("with another devoid creature, each opponent loses 2 life") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Dominator Drone")
                    .withCardOnBattlefield(1, "Culling Drone") // {1}{B} devoid: colorless
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                game.castSpell(1, "Dominator Drone").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Dominator Drone") shouldBe true
                withClue("the opponent lost 2; the controller is untouched") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("with only a coloured creature, the ability does nothing — the Drone itself doesn't count") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Dominator Drone")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                game.castSpell(1, "Dominator Drone").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Dominator Drone") shouldBe true
                withClue("no colorless creature besides the Drone: no life loss") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("an opponent's colorless creature doesn't count") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Dominator Drone")
                    .withCardOnBattlefield(2, "Culling Drone")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .build()

                game.castSpell(1, "Dominator Drone").error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("losing the other colorless creature before resolution removes the ability") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Dominator Drone")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(1, "Culling Drone")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .build()

                game.castSpell(1, "Dominator Drone").error shouldBe null
                // Both pass: the creature spell resolves and its enters trigger goes on the stack.
                game.passPriority()
                game.passPriority()
                game.isOnBattlefield("Dominator Drone") shouldBe true
                withClue("the intervening if held as the Drone entered, so the trigger is on the stack") {
                    game.state.stack.size shouldBe 1
                }

                val cullingDrone = game.findPermanent("Culling Drone")!!
                game.castSpell(1, "Lightning Bolt", cullingDrone).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Culling Drone") shouldBe false
                withClue("no other colorless creature on resolution: the ability does nothing") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
