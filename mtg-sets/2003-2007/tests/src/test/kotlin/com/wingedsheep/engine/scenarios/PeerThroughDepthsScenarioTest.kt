package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Peer Through Depths (CHK #78) — "Look at the top five cards of your library. You may reveal an
 * instant or sorcery card from among them and put it into your hand. Put the rest on the bottom of
 * your library in any order."
 */
class PeerThroughDepthsScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Peer Through Depths")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Divination")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Shock") // sixth card — out of reach
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Peer Through Depths") {

            test("only instants and sorceries among the top five can be taken") {
                val game = setup()
                game.castSpell(1, "Peer Through Depths").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("Lightning Bolt and Divination qualify; Shock is sixth and unseen") {
                    decision.options.size shouldBe 2
                }
                val divination = game.findCardsInLibrary(1, "Divination").single()
                game.selectCards(listOf(divination)).error shouldBe null
                while (game.hasPendingDecision()) game.keepLibraryOrder()
                game.resolveStack()

                game.isInHand(1, "Divination") shouldBe true
                game.isInHand(1, "Lightning Bolt") shouldBe false
                withClue("the other four went to the bottom, so Shock is now on top") {
                    game.state.getLibrary(game.player1Id).first().let {
                        game.findCardsInLibrary(1, "Shock").single() shouldBe it
                    }
                }
            }

            test("declining keeps every card in the library") {
                val game = setup()
                val before = game.librarySize(1)
                game.castSpell(1, "Peer Through Depths").error shouldBe null
                game.resolveStack()
                game.skipSelection()
                while (game.hasPendingDecision()) game.keepLibraryOrder()
                game.resolveStack()

                game.librarySize(1) shouldBe before
                game.isInHand(1, "Lightning Bolt") shouldBe false
            }
        }
    }
}
