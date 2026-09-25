package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Graceful Adept (CHK #63) — "You have no maximum hand size."
 *
 * With nine cards in hand at cleanup, its controller discards nothing; the control case without
 * the Adept is the ordinary discard-to-seven.
 */
class GracefulAdeptScenarioTest : ScenarioTestBase() {

    init {
        context("Graceful Adept") {

            test("its controller keeps nine cards through cleanup") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Graceful Adept")
                    .withCardsInHand(1, "Island", 9)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("it's Bob's turn and no discard decision was raised") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Alice still holds all nine cards") { game.handSize(1) shouldBe 9 }
            }

            test("without it, the same hand is cut to seven") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardsInHand(1, "Island", 9)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passPriority()
                game.passPriority()

                withClue("cleanup asks Alice to discard down to seven") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
