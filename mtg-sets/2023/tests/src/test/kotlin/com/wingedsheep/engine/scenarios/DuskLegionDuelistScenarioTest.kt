package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dusk Legion Duelist (March of the Machine #11).
 *
 * Whenever one or more +1/+1 counters are put on this creature, draw a card. This ability
 * triggers only once each turn.
 */
class DuskLegionDuelistScenarioTest : ScenarioTestBase() {

    init {
        context("Dusk Legion Duelist — counters draw, once each turn") {

            test("first +1/+1 counter draws a card; a second counter the same turn does not") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dusk Legion Duelist")
                    .withCardInHand(1, "Battlegrowth")
                    .withCardInHand(1, "Battlegrowth")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Plains") }
                val game = builder.build()

                val duelist = game.findPermanent("Dusk Legion Duelist")!!
                val handStart = game.state.getHand(game.player1Id).size

                game.castSpell(1, "Battlegrowth", duelist)
                game.resolveStack()
                withClue("first counter: -1 Battlegrowth +1 draw") {
                    game.state.getHand(game.player1Id).size shouldBe handStart
                }

                game.castSpell(1, "Battlegrowth", duelist)
                game.resolveStack()
                withClue("second counter same turn: no draw") {
                    game.state.getHand(game.player1Id).size shouldBe handStart - 1
                }
                game.state.projectedState.getPower(duelist) shouldBe 4
            }
        }
    }
}
