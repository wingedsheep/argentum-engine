package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Tempest Hawk (TDM #31) — {2}{W} Bird, 2/2, Flying.
 *
 * "Whenever this creature deals combat damage to a player, you may search your library for a card
 *  named Tempest Hawk, reveal it, put it into your hand, then shuffle."
 *
 * Verifies the combat-damage trigger's optional library search filtered to the card's own name:
 *  - finding a second Tempest Hawk in the library and putting it into hand, and
 *  - declining (the "may") leaves the library copy untouched.
 */
class TempestHawkScenarioTest : ScenarioTestBase() {

    init {
        context("Tempest Hawk combat damage trigger") {

            test("dealing combat damage lets the controller fetch another Tempest Hawk to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tempest Hawk", summoningSickness = false)
                    .withCardInLibrary(1, "Tempest Hawk")
                    .withCardInLibrary(1, "Mountain")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Tempest Hawk" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (!game.hasPendingDecision() && game.state.step != Step.POSTCOMBAT_MAIN && iterations++ < 20) {
                    game.passPriority()
                }

                withClue("Tempest Hawk (2/2) deals 2 combat damage to the defender") {
                    game.getLifeTotal(2) shouldBe 18
                }

                val decision = game.getPendingDecision()
                withClue("The combat damage trigger should pause for the optional name search") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                withClue("Only the second Tempest Hawk should be offered — the Mountain is not 'named Tempest Hawk'") {
                    decision.options.size shouldBe 1
                }

                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()

                withClue("The fetched Tempest Hawk should be in hand") {
                    game.findCardsInHand(1, "Tempest Hawk").size shouldBe 1
                }
                withClue("The fetched Tempest Hawk should have left the library") {
                    game.findCardsInLibrary(1, "Tempest Hawk").size shouldBe 0
                }
            }

            test("declining the optional search leaves the library copy in place") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tempest Hawk", summoningSickness = false)
                    .withCardInLibrary(1, "Tempest Hawk")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Tempest Hawk" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (!game.hasPendingDecision() && game.state.step != Step.POSTCOMBAT_MAIN && iterations++ < 20) {
                    game.passPriority()
                }

                // Decline the optional search (select nothing).
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Declining leaves the Tempest Hawk in the library") {
                    game.findCardsInLibrary(1, "Tempest Hawk").size shouldBe 1
                }
                withClue("Nothing was added to hand") {
                    game.findCardsInHand(1, "Tempest Hawk").size shouldBe 0
                }
            }
        }
    }
}
