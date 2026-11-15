package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Goblin Boom Keg (FRF #159) — {4} Artifact.
 *
 *   At the beginning of your upkeep, sacrifice this artifact.
 *   When this artifact is put into a graveyard from the battlefield, it deals 3 damage to any target.
 *
 * The upkeep sacrifice is what feeds the graveyard trigger, so the two abilities chain off each other.
 */
class GoblinBoomKegScenarioTest : ScenarioTestBase() {

    private fun upkeepOf(activePlayer: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Goblin Boom Keg")
            .withActivePlayer(activePlayer)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        return game
    }

    init {
        context("Goblin Boom Keg") {

            test("your upkeep sacrifices it, and going to the graveyard deals 3 damage to any target") {
                val game = upkeepOf(activePlayer = 1)
                game.resolveStack()

                withClue("the upkeep trigger sacrificed the Keg") {
                    game.isInGraveyard(1, "Goblin Boom Keg") shouldBe true
                }
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.playerId shouldBe game.player1Id
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
            }

            test("an opponent's upkeep doesn't sacrifice it") {
                val game = upkeepOf(activePlayer = 2)
                game.resolveStack()

                game.isOnBattlefield("Goblin Boom Keg") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
