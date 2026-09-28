package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.FaerieMastermind
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Faerie Mastermind (MOM #58) —
 *  "Whenever an opponent draws their second card each turn, you draw a card."
 *  "{3}{U}: Each player draws a card."
 */
class FaerieMastermindScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(FaerieMastermind)

        val activateId = FaerieMastermind.script.activatedAbilities.first().id

        context("Faerie Mastermind") {

            test("{3}{U}: each player draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Faerie Mastermind")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(2, 0)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mastermind = game.findPermanent("Faerie Mastermind")!!
                val before = game.handSize(1) to game.handSize(2)

                game.execute(
                    ActivateAbility(game.player1Id, mastermind, activateId)
                ).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe before.first + 1
                game.handSize(2) shouldBe before.second + 1
            }

            test("an opponent's second draw each turn draws the controller one card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Faerie Mastermind")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withCardOnBattlefield(2, "Faerie Mastermind")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Island")
                    .withCardsDrawnThisTurn(2, 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.handSize(1)

                // Player 2 activates their own Mastermind: each player draws. Player 2's
                // draw is their second card this turn, so Player 1's Mastermind triggers.
                val mine = game.findPermanents("Faerie Mastermind").first { id ->
                    game.state.getEntity(id)
                        ?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()
                        ?.playerId == game.player2Id
                }
                game.execute(ActivateAbility(game.player2Id, mine, activateId)).error shouldBe null
                game.resolveStack()

                withClue("one card from the ability, one from the trigger") {
                    game.handSize(1) shouldBe before + 2
                }
            }
        }
    }
}
