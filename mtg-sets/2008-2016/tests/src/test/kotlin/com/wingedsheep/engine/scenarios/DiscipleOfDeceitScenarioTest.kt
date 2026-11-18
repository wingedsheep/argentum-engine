package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Disciple of Deceit (JOU #148): "Inspired — Whenever this creature becomes untapped, you may
 * discard a nonland card. If you do, search your library for a card with the same mana value as
 * that card, reveal it, put it into your hand, then shuffle."
 *
 * Pins the collection-linked search filter: only nonland cards may be discarded, the library search
 * offers exactly the cards whose mana value equals the discarded card's (read after it hit the
 * graveyard), and declining the discard skips the search entirely.
 */
class DiscipleOfDeceitScenarioTest : ScenarioTestBase() {

    private fun untapDisciple(): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Disciple of Deceit", tapped = true)
            .withCardInHand(1, "Gray Ogre")          // MV 3
            .withCardInHand(1, "Forest")             // land — can't be discarded to it
            .withCardInLibrary(1, "Llanowar Elves")  // MV 1
            .withCardInLibrary(1, "Centaur Courser") // MV 3 — the only match
            .withCardInLibrary(1, "Hill Giant")      // MV 4
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("discarding a nonland card searches for a card of the same mana value") {
            val game = untapDisciple()

            val discardPrompt = game.getPendingDecision()
            discardPrompt.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the nonland card in hand is offered for the discard") {
                discardPrompt.options shouldContainExactlyInAnyOrder game.findCardsInHand(1, "Gray Ogre")
            }
            game.selectCards(game.findCardsInHand(1, "Gray Ogre")).error shouldBe null

            val searchPrompt = game.getPendingDecision()
            searchPrompt.shouldBeInstanceOf<SelectCardsDecision>()
            val courser = game.findCardsInLibrary(1, "Centaur Courser")
            withClue("only the mana value 3 card in the library is choosable") {
                searchPrompt.options shouldContainExactlyInAnyOrder courser
            }
            game.selectCards(courser).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Gray Ogre") shouldBe true
            game.findCardsInHand(1, "Centaur Courser").size shouldBe 1
            game.findCardsInHand(1, "Forest").size shouldBe 1
        }

        test("declining the discard skips the search") {
            val game = untapDisciple()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.handSize(1) shouldBe 2
            game.findCardsInHand(1, "Centaur Courser").size shouldBe 0
        }
    }
}
