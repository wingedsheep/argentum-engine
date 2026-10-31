package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rotting Regisaur (M20 #111) — {2}{B} 7/6 Zombie Dinosaur.
 *
 * "At the beginning of your upkeep, discard a card."
 *
 * Covers that the controller chooses which card to discard, and that an empty hand discards
 * nothing.
 */
class RottingRegisaurScenarioTest : ScenarioTestBase() {

    init {
        context("Rotting Regisaur") {

            test("its controller discards a card of their choice at the beginning of their upkeep") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Rotting Regisaur")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("two cards in hand -> the controller picks one") {
                    game.hasPendingDecision() shouldBe true
                }
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Swamp") shouldBe true
                game.handSize(1) shouldBe 1
            }

            test("an empty hand discards nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Rotting Regisaur")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.graveyardSize(1) shouldBe 0
                game.isOnBattlefield("Rotting Regisaur") shouldBe true
            }
        }
    }
}
