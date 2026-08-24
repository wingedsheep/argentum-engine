package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Festival — "Cast this spell only during an opponent's upkeep. Creatures can't
 * attack this turn."
 *
 * Both halves of the timing restriction are load-bearing and fail differently, so both get a
 * negative case: the right step on the wrong turn, and the right turn at the wrong step. A card
 * carrying only `castOnlyDuring(UPKEEP)` would pass the fog test and still be castable on your own
 * upkeep.
 */
class FestivalScenarioTest : ScenarioTestBase() {

    init {
        context("Festival — timing") {

            test("castable during an opponent's upkeep") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Festival")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                game.castSpell(1, "Festival").error shouldBe null
            }

            test("not castable during your own upkeep") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Festival")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                withClue("the upkeep is right but the turn is yours") {
                    game.castSpell(1, "Festival").error shouldNotBe null
                }
            }

            test("not castable during an opponent's main phase") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Festival")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the turn is right but the step is not the upkeep") {
                    game.castSpell(1, "Festival").error shouldNotBe null
                }
            }
        }

        context("Festival — the fog") {

            test("no creature can attack for the rest of the turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Festival")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                game.castSpell(1, "Festival").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("the active player's creature is held back by Festival") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldNotBe null
                }
            }
        }
    }
}
