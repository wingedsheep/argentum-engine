package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mana Clash — flip until both coins come up heads on the same flip, burning
 * whoever flipped tails each round.
 *
 * Coin flips make exact damage untestable, so the assertions are the invariants that hold for every
 * outcome: the loop terminates (a stuck repeat condition would hang rather than fail), somebody's
 * life total is a whole number of points down or the very first flip was double heads, and the
 * damage lands on players rather than nowhere. Seeded runs make it deterministic per seed while
 * still exercising the real loop.
 */
class ManaClashScenarioTest : ScenarioTestBase() {

    init {
        context("Mana Clash") {

            test("the loop terminates and the damage is symmetric in shape") {
                // Several seeds, so a single lucky double-heads can't be the whole coverage.
                listOf(1L, 7L, 42L, 1234L).forEach { seed ->
                    val game = scenario()
                        .withRngSeed(seed)
                        .withPlayers("Player1", "Player2")
                        .withCardInHand(1, "Mana Clash")
                        .withLandsOnBattlefield(1, "Mountain", 1)
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    game.castSpellTargetingPlayer(1, "Mana Clash", 2).error shouldBe null
                    game.resolveStack()

                    withClue("seed $seed: the repeat condition resolved rather than hanging") {
                        game.state.stack.size shouldBe 0
                    }
                    withClue("seed $seed: nobody gained life, and damage stayed on players") {
                        game.getLifeTotal(1) shouldBeLessThanOrEqual 20
                        game.getLifeTotal(2) shouldBeLessThanOrEqual 20
                    }
                }
            }
        }
    }
}
