package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Siren of the Silent Song (BNG #155) — "Inspired — Whenever this creature becomes untapped, each
 * opponent discards a card, then each opponent mills a card."
 */
class SirenOfTheSilentSongScenarioTest : ScenarioTestBase() {
    init {
        test("untapping makes the opponent discard a card, then mill a card") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Siren of the Silent Song", tapped = true)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()
            val discard = game.getPendingDecision()
            if (discard is SelectCardsDecision) {
                game.selectCards(game.findCardsInHand(2, "Hill Giant")).error shouldBe null
                game.resolveStack()
            }

            withClue("the opponent discarded one of two cards") { game.handSize(2) shouldBe 1 }
            withClue("the opponent milled one card off the top") { game.librarySize(2) shouldBe 1 }
            withClue("discarded card plus milled card are in the opponent's graveyard") {
                game.graveyardSize(2) shouldBe 2
            }
            withClue("the controller loses nothing") { game.graveyardSize(1) shouldBe 0 }
        }
    }
}
