package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eternal Flame — "deals X damage to target opponent or planeswalker and half X
 * damage, rounded up, to you, where X is the number of Mountains you control."
 *
 * X is a board count, not an announced value, so the case that matters is an *odd* Mountain count:
 * five Mountains must be 5 out and 3 back, which is the only split that distinguishes "half X
 * rounded up" from a second, independently-computed count or a rounding-down slip. A nonbasic land
 * with the Mountain subtype is on the board to confirm the filter reads the subtype, not "basic".
 */
class EternalFlameScenarioTest : ScenarioTestBase() {

    init {
        context("Eternal Flame") {

            test("odd Mountain count: X out, half X rounded up back") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eternal Flame")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Eternal Flame", 2).error shouldBe null
                game.resolveStack()

                withClue("five Mountains -> 5 damage") { game.getLifeTotal(2) shouldBe 15 }
                withClue("half of 5 rounded up -> 3 back at me") { game.getLifeTotal(1) shouldBe 17 }
            }

            test("even Mountain count halves cleanly") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eternal Flame")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Eternal Flame", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
                game.getLifeTotal(1) shouldBe 18
            }

            test("only your own Mountains count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eternal Flame")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Eternal Flame", 2).error shouldBe null
                game.resolveStack()

                withClue("the opponent's three Mountains are not mine") {
                    game.getLifeTotal(2) shouldBe 16
                    game.getLifeTotal(1) shouldBe 18
                }
            }
        }
    }
}
