package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Giant Shark.
 *
 * Two of its three clauses look at Islands from opposite sides of the table, and swapping them is
 * the obvious way to get this card wrong: the attack restriction reads the *defending player's*
 * Islands, while the sacrifice trigger reads the *Shark controller's*. A board with Islands on
 * exactly one side tells those apart, so each clause is tested with the Islands on the wrong side
 * as well as the right one.
 */
class GiantSharkScenarioTest : ScenarioTestBase() {

    init {
        context("Giant Shark — the attack restriction") {

            test("can't attack a defender who controls no Island") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant Shark", summoningSickness = false)
                    // My own Island keeps the sacrifice clause quiet; the defender has none.
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Giant Shark" to 2)).error shouldNotBe null
            }

            test("can attack a defender who does") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant Shark", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Giant Shark" to 2)).error shouldBe null
            }
        }

        context("Giant Shark — \"when you control no Islands, sacrifice\"") {

            test("sacrificed when its own controller has no Island") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant Shark", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    // The opponent's Islands are irrelevant — this clause reads my board.
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("the opponent's Islands don't count") {
                    game.isOnBattlefield("Giant Shark") shouldBe false
                }
            }

            test("survives while its controller keeps one") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Giant Shark", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                game.isOnBattlefield("Giant Shark") shouldBe true
            }
        }
    }
}
