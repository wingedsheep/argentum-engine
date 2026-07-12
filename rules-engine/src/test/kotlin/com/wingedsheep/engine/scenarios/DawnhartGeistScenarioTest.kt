package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dawnhart Geist (VOW #8) — {1}{W} Creature — Spirit Warlock, 1/3.
 *
 *   Whenever you cast an enchantment spell, you gain 2 life.
 *
 * Exercises the "you cast an enchantment spell" trigger: casting an enchantment gains 2 life.
 */
class DawnhartGeistScenarioTest : ScenarioTestBase() {

    init {
        context("Dawnhart Geist — casting an enchantment gains 2 life") {

            test("casting an enchantment spell gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dawnhart Geist", summoningSickness = false)
                    .withCardInHand(1, "Test Enchantment")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Enchantment").error shouldBe null
                game.resolveStack() // the cast trigger (and then the enchantment spell) resolve

                withClue("Player 1 gains 2 life (20 -> 22) from the cast trigger") {
                    game.getLifeTotal(1) shouldBe 22
                }

                withClue("Test Enchantment resolves onto the battlefield") {
                    game.isOnBattlefield("Test Enchantment") shouldBe true
                }
            }
        }
    }
}
