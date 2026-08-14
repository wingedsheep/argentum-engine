package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Disciple of the Vault (MRD #62) — {B} Creature — Human Cleric 1/1.
 *
 *   Whenever an artifact is put into a graveyard from the battlefield, you may have target
 *   opponent lose 1 life.
 *
 * The trigger is scoped to *any* artifact regardless of controller (`TriggerBinding.ANY` over a
 * bare `GameObjectFilter.Artifact`), and the "you may" is a resolution-time yes/no on top of a
 * target chosen when the ability goes on the stack.
 */
class DiscipleOfTheVaultScenarioTest : ScenarioTestBase() {

    init {
        context("Disciple of the Vault") {

            test("an opponent's artifact dying drains the targeted opponent for 1") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Disciple of the Vault")
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Bottle Gnomes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)
                val gnomes = game.findPermanent("Bottle Gnomes")!!

                game.castSpell(1, "Shatter", gnomes).isSuccess shouldBe true
                game.resolveStack()

                withClue("The artifact should be in the graveyard") {
                    game.isInGraveyard(2, "Bottle Gnomes") shouldBe true
                }

                // The trigger is optional — accept it, then let it resolve.
                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()

                withClue("The targeted opponent should have lost 1 life") {
                    game.getLifeTotal(2) shouldBe (startingLife - 1)
                }
            }

            test("declining the 'you may' costs the opponent nothing") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Disciple of the Vault")
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Bottle Gnomes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)
                val gnomes = game.findPermanent("Bottle Gnomes")!!

                game.castSpell(1, "Shatter", gnomes).isSuccess shouldBe true
                game.resolveStack()

                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.resolveStack()

                withClue("Declining should leave the opponent's life untouched") {
                    game.getLifeTotal(2) shouldBe startingLife
                }
            }

            test("your own artifact dying also triggers it (any controller)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Disciple of the Vault")
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Bottle Gnomes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)
                val gnomes = game.findPermanent("Bottle Gnomes")!!

                game.castSpell(1, "Shatter", gnomes).isSuccess shouldBe true
                game.resolveStack()

                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()

                withClue("An artifact you control dying triggers the Disciple too") {
                    game.getLifeTotal(2) shouldBe (startingLife - 1)
                }
            }

            test("a nonartifact creature dying does not trigger it") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Disciple of the Vault")
                    .withCardInHand(1, "Terror")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Terror", bears).isSuccess shouldBe true
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(true)
                game.resolveStack()

                withClue("The creature should be dead") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("A nonartifact creature dying must not drain") {
                    game.getLifeTotal(2) shouldBe startingLife
                }
            }
        }
    }
}
