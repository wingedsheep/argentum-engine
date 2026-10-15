package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Buy Your Silence (SNC #6) — {4}{W} Sorcery
 *
 *   Exile target nonland permanent. Its controller creates a Treasure token.
 *
 * The Treasure goes to the exiled permanent's controller (read after it has left the
 * battlefield), not to the caster.
 */
class BuyYourSilenceScenarioTest : ScenarioTestBase() {

    private fun treasures(game: TestGame, playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield().count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Treasure" &&
                game.state.getEntity(id)?.get<ControllerComponent>()?.playerId == playerId
        }
    }

    init {
        context("Buy Your Silence") {

            test("exiles the opponent's permanent and the opponent gets the Treasure") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Buy Your Silence")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val result = game.castSpell(1, "Buy Your Silence", bears)
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                withClue("the exiled permanent's controller creates the Treasure") {
                    treasures(game, 2) shouldBe 1
                    treasures(game, 1) shouldBe 0
                }
            }
        }
    }
}
