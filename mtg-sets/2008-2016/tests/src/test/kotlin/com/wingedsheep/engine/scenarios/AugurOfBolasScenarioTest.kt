package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Augur of Bolas (M13 #43) — {1}{U} Creature — Merfolk Wizard 1/3.
 *
 *   When this creature enters, look at the top three cards of your library. You may reveal an
 *   instant or sorcery card from among them and put it into your hand. Put the rest on the bottom
 *   of your library in any order.
 */
class AugurOfBolasScenarioTest : ScenarioTestBase() {

    private fun board(vararg library: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Augur of Bolas")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        library.forEach { builder.withCardInLibrary(1, it) }
        return builder.build()
    }

    /** Answer any leftover bottom-ordering prompt by keeping the current order. */
    private fun TestGame.finishOrdering() {
        if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
        resolveStack()
    }

    init {
        test("puts an instant from the top three into your hand and bottoms the rest") {
            val game = board("Lightning Bolt", "Forest", "Grizzly Bears")
            val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()

            game.castSpell(1, "Augur of Bolas").error shouldBe null
            game.resolveStack()
            (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            game.selectCards(listOf(bolt)).error shouldBe null
            game.finishOrdering()

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 2
        }

        test("with no instant or sorcery among them nothing goes to hand") {
            val game = board("Forest", "Island", "Grizzly Bears")

            game.castSpell(1, "Augur of Bolas").error shouldBe null
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) game.skipSelection()
            game.finishOrdering()

            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 3
            game.isOnBattlefield("Augur of Bolas") shouldBe true
        }
    }
}
