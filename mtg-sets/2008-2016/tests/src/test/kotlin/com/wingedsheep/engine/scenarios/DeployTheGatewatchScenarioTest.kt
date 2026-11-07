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
 * Deploy the Gatewatch (EMN #20) — {4}{W}{W} Sorcery.
 *
 *   Look at the top seven cards of your library. Put up to two planeswalker cards from among them
 *   onto the battlefield. Put the rest on the bottom of your library in a random order.
 */
class DeployTheGatewatchScenarioTest : ScenarioTestBase() {

    init {
        test("puts up to two planeswalkers from the top seven onto the battlefield") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Deploy the Gatewatch")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Jace Beleren")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Liliana Vess")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Elite Vanguard")
                .withCardInLibrary(1, "Craw Wurm")
                .withCardInLibrary(1, "Garruk Wildspeaker") // eighth card — out of reach
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Deploy the Gatewatch").error shouldBe null
            game.resolveStack()

            val jace = game.findCardsInLibrary(1, "Jace Beleren").single()
            val liliana = game.findCardsInLibrary(1, "Liliana Vess").single()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the two planeswalkers in the top seven are selectable") {
                decision.options shouldContainExactlyInAnyOrder listOf(jace, liliana)
            }
            game.selectCards(listOf(jace, liliana)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Jace Beleren") shouldBe true
            game.isOnBattlefield("Liliana Vess") shouldBe true
            game.isOnBattlefield("Garruk Wildspeaker") shouldBe false
            game.librarySize(1) shouldBe 6
            withClue("the five non-planeswalkers went under the untouched eighth card") {
                game.state.getLibrary(game.player1Id).first() shouldBe
                    game.findCardsInLibrary(1, "Garruk Wildspeaker").single()
            }
        }
    }
}
