package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mana Vortex.
 *
 * The clause worth pinning is the upkeep one: "that player sacrifices a land" means the player whose
 * upkeep it is, not the Vortex's controller. So the Vortex goes down on my side and the *opponent's*
 * upkeep is the one measured — an implementation aimed at the controller would leave their lands
 * untouched and eat mine instead.
 *
 * The state trigger's "no lands on the battlefield" is global, so the board is emptied of lands
 * entirely rather than just the controller's.
 */
class ManaVortexScenarioTest : ScenarioTestBase() {

    init {
        context("Mana Vortex") {

            test("the upkeep tax falls on whoever's upkeep it is") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mana Vortex")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myLandsBefore = game.findPermanents("Island").size
                val theirLandsBefore = game.findPermanents("Forest").size

                // Round to the opponent's upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 20) {
                    val decision = game.getPendingDecision()
                    if (decision is SelectCardsDecision) {
                        // "a land of their choice" — take the first offered.
                        game.selectCards(decision.options.take(maxOf(1, decision.minSelections)))
                    } else {
                        game.skipSelection()
                    }
                    game.resolveStack()
                }

                withClue("their upkeep took one of their lands") {
                    game.findPermanents("Forest").size shouldBeLessThan theirLandsBefore
                }
                withClue("and left mine alone — the tax follows the active player") {
                    game.findPermanents("Island").size shouldBe myLandsBefore
                }
            }

            test("with no lands anywhere, the Vortex sacrifices itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mana Vortex")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("the state trigger fires on an empty-of-lands board") {
                    game.isOnBattlefield("Mana Vortex") shouldBe false
                }
            }
        }
    }
}
