package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tome Anima — "This creature can't be blocked as long as you've drawn two or more cards this turn."
 *
 * The threshold is two, and it counts the controller's draws only.
 */
class TomeAnimaScenarioTest : ScenarioTestBase() {

    private fun attack(controllerDraws: Int, opponentDraws: Int = 0): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Tome Anima", summoningSickness = false)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
            .withCardsDrawnThisTurn(1, controllerDraws)
            .withCardsDrawnThisTurn(2, opponentDraws)
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Tome Anima" to 2)).error shouldBe null
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        context("Tome Anima — unblockable after two draws") {

            test("can't be blocked once you've drawn two cards this turn") {
                val game = attack(controllerDraws = 2)
                withClue("two draws make Tome Anima unblockable") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Tome Anima"))).error shouldNotBe null
                }
            }

            test("can be blocked after only one draw") {
                val game = attack(controllerDraws = 1)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Tome Anima"))).error shouldBe null
            }

            test("the opponent's draws don't count") {
                val game = attack(controllerDraws = 0, opponentDraws = 3)
                withClue("'you've drawn' is controller-scoped") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Tome Anima"))).error shouldBe null
                }
            }
        }
    }
}
