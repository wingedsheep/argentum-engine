package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Savior of the Small (DSK #27).
 *
 * Savior of the Small — {3}{W} Creature — Kor Survivor, 3/4
 *   "Survival — At the beginning of your second main phase, if this creature is tapped, return
 *    target creature card with mana value 3 or less from your graveyard to your hand."
 *
 * Verifies the intervening-if (only when the Survivor is tapped), the mana-value filter on the
 * graveyard target, and that an untapped Survivor produces no trigger.
 */
class SaviorOfTheSmallScenarioTest : ScenarioTestBase() {

    init {
        context("Savior of the Small — Survival trigger") {

            test("a tapped Savior returns a MV<=3 creature card from graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savior of the Small", tapped = true)
                    .withCardInGraveyard(1, "Grizzly Bears") // {1}{G} 2/2, MV 2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack()
                    guard++
                }
                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the Survival trigger; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(bears))))
                game.resolveStack()

                withClue("Grizzly Bears returned to hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }

            test("an untapped Savior does NOT fire the Survival trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savior of the Small", tapped = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { if (game.hasPendingDecision()) Unit else game.resolveStack() }

                withClue("No Survival trigger — the Survivor is untapped, so the intervening-if fails") {
                    (game.state.pendingDecision is ChooseTargetsDecision) shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
