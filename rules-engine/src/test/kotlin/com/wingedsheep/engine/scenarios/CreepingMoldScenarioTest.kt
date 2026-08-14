package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Creeping Mold (VIS #103, reprinted in MRD) — {2}{G}{G} Sorcery.
 *
 *   Destroy target artifact, enchantment, or land.
 *
 * Exercises the new `Targets.ArtifactEnchantmentOrLand` union: one target slot that accepts
 * any of the three types, and rejects everything else (a plain creature).
 */
class CreepingMoldScenarioTest : ScenarioTestBase() {

    init {
        context("Creeping Mold") {

            test("destroys a target artifact") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Creeping Mold")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Bottle Gnomes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gnomes = game.findPermanent("Bottle Gnomes")!!
                game.castSpell(1, "Creeping Mold", gnomes).isSuccess shouldBe true
                game.resolveStack()

                withClue("The targeted artifact should be destroyed") {
                    game.isOnBattlefield("Bottle Gnomes") shouldBe false
                    game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
                }
            }

            test("destroys a target enchantment") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Creeping Mold")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Rule of Law")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ruleOfLaw = game.findPermanent("Rule of Law")!!
                game.castSpell(1, "Creeping Mold", ruleOfLaw).isSuccess shouldBe true
                game.resolveStack()

                withClue("The targeted enchantment should be destroyed") {
                    game.isOnBattlefield("Rule of Law") shouldBe false
                    game.isInGraveyard(2, "Rule of Law") shouldBe true
                }
            }

            test("destroys a target land") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Creeping Mold")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val island = game.findPermanent("Island")!!
                game.castSpell(1, "Creeping Mold", island).isSuccess shouldBe true
                game.resolveStack()

                withClue("The targeted land should be destroyed") {
                    game.isInGraveyard(2, "Island") shouldBe true
                }
            }

            test("a plain creature is not a legal target") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Creeping Mold")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("A nonartifact creature must not be targetable") {
                    game.castSpell(1, "Creeping Mold", bears).isSuccess shouldBe false
                }
                withClue("The creature should still be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
