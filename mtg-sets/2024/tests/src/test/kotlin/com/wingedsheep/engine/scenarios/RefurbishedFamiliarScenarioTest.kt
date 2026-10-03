package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Refurbished Familiar (MH3 #105).
 *
 *   Affinity for artifacts. Flying.
 *   When this creature enters, each opponent discards a card. For each opponent who can't,
 *   you draw a card.
 */
class RefurbishedFamiliarScenarioTest : ScenarioTestBase() {

    init {
        context("Refurbished Familiar") {

            test("opponent with a card in hand discards it and you draw nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Refurbished Familiar")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Refurbished Familiar")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    val bears = game.findCardsInHand(2, "Grizzly Bears")
                    if (bears.isNotEmpty()) game.selectCards(bears)
                }
                game.resolveStack()

                withClue("Opponent discarded their only card") { game.handSize(2) shouldBe 0 }
                withClue("Opponent could discard, so you draw nothing") { game.handSize(1) shouldBe 0 }
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("opponent with an empty hand can't discard, so you draw a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Refurbished Familiar")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Refurbished Familiar")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.resolveStack()

                withClue("You draw one card for the opponent who couldn't discard") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("affinity for artifacts reduces the cost") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Refurbished Familiar")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Refurbished Familiar")
                withClue("Three artifacts cut {3}{B} to {B}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    val bears = game.findCardsInHand(2, "Grizzly Bears")
                    if (bears.isNotEmpty()) game.selectCards(bears)
                }
                game.resolveStack()

                game.isOnBattlefield("Refurbished Familiar") shouldBe true
            }
        }
    }
}
