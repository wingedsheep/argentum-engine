package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Sanctify (VOW #33) — {1}{W} Sorcery.
 *
 *   Destroy target artifact or enchantment. You gain 3 life.
 *
 * Exercises the destroy + gain-life composite against an enchantment target.
 */
class SanctifyScenarioTest : ScenarioTestBase() {

    init {
        context("Sanctify destroy + gain life") {

            test("destroys a target enchantment and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sanctify")
                    .withCardOnBattlefield(2, "Test Enchantment") // opponent's enchantment
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enchantment = game.findPermanent("Test Enchantment")!!

                game.castSpell(1, "Sanctify", targetId = enchantment).error shouldBe null
                game.resolveStack()

                withClue("The enchantment is destroyed (no longer on the battlefield)") {
                    game.findPermanent("Test Enchantment") shouldBe null
                }
                withClue("The enchantment is in its owner's graveyard") {
                    game.findCardsInGraveyard(2, "Test Enchantment").size shouldBe 1
                }
                withClue("Player 1 gains 3 life (20 -> 23)") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }
        }
    }
}
