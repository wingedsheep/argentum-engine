package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Vile Consumption (Invasion).
 *
 * Vile Consumption ({1}{U}{B}, Enchantment):
 *   "All creatures have 'At the beginning of your upkeep, sacrifice this creature unless
 *    you pay 1 life.'"
 *
 * Implemented as a GrantTriggeredAbility over GroupFilter.AllCreatures: every creature gains
 * its own beginning-of-upkeep trigger that fires on its controller's upkeep (Triggers.YourUpkeep)
 * and lets that controller pay 1 life (EffectTarget.Controller) or sacrifice the creature itself
 * (EffectTarget.Self) via PayOrSufferEffect.
 */
class VileConsumptionScenarioTest : ScenarioTestBase() {

    init {
        context("Vile Consumption — granted upkeep pay-or-sacrifice") {

            test("controller pays 1 life to keep their creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Vile Consumption")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("upkeep trigger should pause for the pay-or-sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Pay 1 life to keep the creature.
                game.answerYesNo(true)

                withClue("Elvish Warrior survives when its controller pays 1 life") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
                withClue("P1 paid 1 life (20 -> 19)") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("declining the payment sacrifices that very creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Vile Consumption")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("upkeep trigger should pause for the pay-or-sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Decline — the source creature is sacrificed.
                game.answerYesNo(false)

                withClue("Elvish Warrior is sacrificed when its controller declines to pay") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
                withClue("P1 kept their life total (no payment)") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("only the active player's creatures trigger on their upkeep") {
                // On P1's upkeep, only P1-controlled creatures' triggers fire ("your upkeep").
                // P2's creature triggers on P2's own upkeep, so it is untouched here and no
                // decision is presented.
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Vile Consumption")
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("no decision pends — P2's creature triggers on P2's upkeep, not P1's") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("P2's creature is unaffected on P1's upkeep") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
            }
        }
    }
}
