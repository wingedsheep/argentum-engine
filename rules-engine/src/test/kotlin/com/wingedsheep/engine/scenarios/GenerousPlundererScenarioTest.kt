package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Generous Plunderer (BIG #11, {1}{R}, Creature — Human Rogue, 2/2).
 *
 *   Menace
 *   At the beginning of your upkeep, you may create a Treasure token. When you do, target
 *   opponent creates a tapped Treasure token.
 *   Whenever this creature attacks, it deals damage to defending player equal to the number
 *   of artifacts they control.
 */
class GenerousPlundererScenarioTest : ScenarioTestBase() {

    init {
        context("Generous Plunderer") {

            test("upkeep: creating a Treasure gives a target opponent a tapped Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Generous Plunderer", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                // The "you may create a Treasure" upkeep trigger should be on the stack.
                game.resolveStack()
                // Accept the optional Treasure, then choose the opponent for the reflexive trigger.
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Player1 makes one Treasure, the opponent makes one") {
                    treasures(game, 1) shouldBe 1
                    treasures(game, 2) shouldBe 1
                }
            }

            test("attack: deals damage equal to defending player's artifact count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Generous Plunderer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardOnBattlefield(2, "Bottle Gnomes")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Generous Plunderer" to 2))
                attack.error shouldBe null
                game.resolveStack()

                withClue("Defending player controls 2 artifacts → 2 damage from the attack trigger") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }

    private fun treasures(game: TestGame, playerNumber: Int): Int {
        val owner = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield().count { id ->
            val e = game.state.getEntity(id) ?: return@count false
            e.get<CardComponent>()?.name == "Treasure" &&
                e.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == owner
        }
    }
}
