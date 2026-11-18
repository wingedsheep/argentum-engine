package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Might Makes Right (M15 #156) — {5}{R} Enchantment.
 *
 *   At the beginning of combat on your turn, if you control each creature on the battlefield with
 *   the greatest power, gain control of target creature an opponent controls until end of turn.
 *   Untap that creature. It gains haste until end of turn.
 *
 * Covers the intervening-if: a strictly greatest creature steals, a tie with an opposing creature
 * doesn't trigger, controlling no creature doesn't trigger, and the condition is checked again on
 * resolution (rulings).
 */
class MightMakesRightScenarioTest : ScenarioTestBase() {

    init {
        context("Might Makes Right") {

            test("controlling the strictly greatest-power creature steals a creature, untapped and hasty, until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Might Makes Right")
                    .withCardOnBattlefield(1, "Craw Wurm")                     // 6/4
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true) // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                withClue("the ability triggers and asks for a target") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Player1 controls the Bears, untapped and with haste") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("control returns to Player2 when the turn ends") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }
            }

            test("a tie for greatest power with an opposing creature means it doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Might Makes Right")
                    .withCardOnBattlefield(1, "Runeclaw Bear")  // 2/2
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 — tied
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("no trigger: no target prompt and nothing on the stack") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }

            test("controlling no creatures means it doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Might Makes Right")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                withClue("no trigger: no target prompt and nothing on the stack") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }

            test("the condition is checked again on resolution") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Might Makes Right")
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(bears))

                // In response, pump the Bears to 5/5 — now greater than Hill Giant's 3.
                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()

                withClue("the ability does nothing: Player2 keeps the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
