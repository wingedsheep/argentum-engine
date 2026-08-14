package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Ratcatcher Trainee. */
class RatcatcherTraineeScenarioTest : ScenarioTestBase() {

    init {
        context("Ratcatcher Trainee — first strike only during your turn") {
            test("has first strike on your turn and loses it on the opponent's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ratcatcher Trainee", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val trainee = game.findPermanent("Ratcatcher Trainee")!!
                withClue("during your turn") {
                    game.state.projectedState.hasKeyword(trainee, Keyword.FIRST_STRIKE) shouldBe true
                }

                val opponentsTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ratcatcher Trainee", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirTurn = opponentsTurn.findPermanent("Ratcatcher Trainee")!!
                withClue("during the opponent's turn the grant is off") {
                    opponentsTurn.state.projectedState.hasKeyword(theirTurn, Keyword.FIRST_STRIKE) shouldBe false
                }
            }

            test("Pest Problem creates two Rat tokens and exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ratcatcher Trainee")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // faceIndex = 0 is the Adventure face; the creature face casts with faceIndex = null.
                val cardId = game.findCardsInHand(1, "Ratcatcher Trainee").first()
                game.execute(CastSpell(game.player1Id, cardId, emptyList(), faceIndex = 0))
                    .isSuccess shouldBe true
                game.resolveStack()

                withClue("two Rats, not one") {
                    game.findAllPermanents("Rat Token").size shouldBe 2
                }
                withClue("resolving the Adventure exiles the card so it can be cast as a creature later") {
                    game.isInExile(1, "Ratcatcher Trainee") shouldBe true
                }
            }
        }
    }
}
