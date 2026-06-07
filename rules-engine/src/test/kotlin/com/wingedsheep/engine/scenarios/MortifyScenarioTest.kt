package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Mortify (GPT #122) — {1}{W}{B} Instant.
 *
 * "Destroy target creature or enchantment."
 *
 * Verifies the spell can destroy a creature and (separately) an enchantment.
 */
class MortifyScenarioTest : ScenarioTestBase() {

    init {
        context("Mortify destroys target creature or enchantment") {

            test("destroys a target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mortify")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Centaur Courser", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val result = game.castSpell(1, "Mortify", targetId = courser)
                withClue("Casting Mortify should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Centaur Courser should be destroyed") {
                    game.findPermanent("Centaur Courser") shouldBe null
                }
            }

            test("destroys a target enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mortify")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Test Enchantment", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enchantment = game.findPermanent("Test Enchantment")
                withClue("Test Enchantment should be on the battlefield") { enchantment shouldNotBe null }

                val result = game.castSpell(1, "Mortify", targetId = enchantment!!)
                withClue("Casting Mortify on enchantment should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Test Enchantment should be destroyed") {
                    game.findPermanent("Test Enchantment") shouldBe null
                }
            }
        }
    }
}
