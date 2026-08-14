package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Delver of Secrets // Insectile Aberration (ISD #51).
 *
 * "At the beginning of your upkeep, look at the top card of your library. You may reveal that card.
 *  If an instant or sorcery card is revealed this way, transform this creature."
 *
 * Three branches of the same gate:
 *  - reveal an instant → transforms (and the card stays on top of the library),
 *  - decline to reveal an instant → no transform (the ruling: the transform keys off the *reveal*),
 *  - reveal a non-instant/sorcery → no transform (revealing a creature is legal, just useless).
 */
class DelverOfSecretsScenarioTest : ScenarioTestBase() {

    init {
        context("Delver of Secrets") {

            /**
             * Player 1 controls Delver; [topCard] is the top card of their library. Starts on player
             * 2's main phase so the very next upkeep is player 1's (and no draw step intervenes
             * before it, leaving [topCard] on top).
             */
            fun gameWithTopCard(topCard: String) = run {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Delver of Secrets", summoningSickness = false)
                    .withCardInLibrary(1, topCard) // first added = index 0 = top
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
                builder.build()
            }

            test("revealing an instant from the top transforms it into Insectile Aberration") {
                val game = gameWithTopCard("Lightning Bolt")
                val delver = game.findPermanent("Delver of Secrets")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // The "you may reveal" choice: select the looked-at card to reveal it.
                withClue("the upkeep trigger should pause for the reveal choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val topCard = game.findCardsInLibrary(1, "Lightning Bolt").first()
                game.selectCards(listOf(topCard))
                game.resolveStack()

                withClue("an instant was revealed → transformed") {
                    game.state.getEntity(delver)!!.get<CardComponent>()!!.name shouldBe "Insectile Aberration"
                }
                withClue("the card stays on top of the library — nothing moved zones") {
                    game.state.getLibrary(game.player1Id).first() shouldBe topCard
                }
            }

            test("declining to reveal leaves it as Delver of Secrets") {
                val game = gameWithTopCard("Lightning Bolt")
                val delver = game.findPermanent("Delver of Secrets")!!
                val libraryBefore = game.librarySize(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.skipSelection()
                game.resolveStack()

                withClue("nothing was revealed → no transform even though the top card is an instant") {
                    game.state.getEntity(delver)!!.get<CardComponent>()!!.name shouldBe "Delver of Secrets"
                }
                withClue("the library is untouched") {
                    game.librarySize(1) shouldBe libraryBefore
                }
            }

            test("revealing a creature card does not transform it") {
                val game = gameWithTopCard("Grizzly Bears")
                val delver = game.findPermanent("Delver of Secrets")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val topCard = game.state.getLibrary(game.player1Id).first()
                game.selectCards(listOf(topCard))
                game.resolveStack()

                withClue("a creature was revealed → no transform") {
                    game.state.getEntity(delver)!!.get<CardComponent>()!!.name shouldBe "Delver of Secrets"
                }
            }
        }
    }
}
