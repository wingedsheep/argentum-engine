package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Unexpected Results (GTC #203).
 *
 * The library is filled with copies of one card so the shuffle can't change what's revealed. Covers
 * the self-bounce straight from the stack when the revealed land is put onto the battlefield, the
 * graveyard when it's declined, and the free cast of a revealed nonland card.
 */
class UnexpectedResultsScenarioTest : ScenarioTestBase() {

    init {
        fun board(libraryCard: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Unexpected Results")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInLibrary(1, libraryCard)
            .withCardInLibrary(1, libraryCard)
            .withCardInLibrary(1, libraryCard)
            .withCardInLibrary(1, libraryCard)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("putting the revealed land onto the battlefield returns Unexpected Results to hand") {
            val game = board("Mountain")
            game.castSpell(1, "Unexpected Results").error shouldBe null
            game.resolveStack()

            withClue("offered the revealed Mountain") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.state.getLibrary(game.player1Id).first()))
            game.resolveStack()

            game.findAllPermanents("Mountain").size shouldBe 1
            game.isInHand(1, "Unexpected Results") shouldBe true
            game.isInGraveyard(1, "Unexpected Results") shouldBe false
        }

        test("declining the revealed land leaves it on top and sends the spell to the graveyard") {
            val game = board("Mountain")
            game.castSpell(1, "Unexpected Results").error shouldBe null
            game.resolveStack()
            game.skipSelection()
            game.resolveStack()

            game.findAllPermanents("Mountain").size shouldBe 0
            game.librarySize(1) shouldBe 4
            game.isInGraveyard(1, "Unexpected Results") shouldBe true
        }

        test("a revealed nonland card may be cast without paying its mana cost") {
            val game = board("Divination")
            val handBefore = game.handSize(1) - 1
            game.castSpell(1, "Unexpected Results").error shouldBe null
            game.resolveStack()

            withClue("offered the revealed Divination") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.state.getLibrary(game.player1Id).first()))
            game.resolveStack()

            game.isInGraveyard(1, "Divination") shouldBe true
            game.handSize(1) shouldBe handBefore + 2
            game.isInGraveyard(1, "Unexpected Results") shouldBe true
        }
    }
}
