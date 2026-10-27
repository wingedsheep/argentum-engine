package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Smashing Success (KHM #151) — {3}{R} Instant.
 *
 *   Destroy target artifact or land. If an artifact is destroyed this way, create a Treasure token.
 *
 * The Treasure depends on two things the script composes by hand: the target was an artifact as it
 * existed on the battlefield (checked before the destroy), and it was actually destroyed.
 */
class SmashingSuccessScenarioTest : ScenarioTestBase() {
    init {
        context("Smashing Success") {

            fun castAt(targetName: String): TestGame {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Smashing Success")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, targetName)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findPermanent(targetName)!!
                game.castSpell(1, "Smashing Success", target).error shouldBe null
                game.resolveStack()
                return game
            }

            test("destroying an artifact creates a Treasure") {
                val game = castAt("Bonesplitter")
                game.isInGraveyard(2, "Bonesplitter") shouldBe true
                game.findPermanents("Treasure").size shouldBe 1
            }

            test("destroying an artifact land creates a Treasure") {
                val game = castAt("Seat of the Synod")
                game.isInGraveyard(2, "Seat of the Synod") shouldBe true
                game.findPermanents("Treasure").size shouldBe 1
            }

            test("destroying a nonartifact land creates no Treasure") {
                val game = castAt("Forest")
                game.isInGraveyard(2, "Forest") shouldBe true
                game.findPermanents("Treasure").size shouldBe 0
            }

            test("an indestructible artifact isn't destroyed, so no Treasure") {
                val game = castAt("Darksteel Ingot")
                withClue("Darksteel Ingot survives") {
                    game.isOnBattlefield("Darksteel Ingot") shouldBe true
                }
                game.findPermanents("Treasure").size shouldBe 0
            }
        }
    }
}
