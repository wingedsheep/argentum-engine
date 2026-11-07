package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Courageous Outrider (EMN #18) — {3}{W} Creature — Human Scout 3/4.
 *
 *   When this creature enters, look at the top four cards of your library. You may reveal a Human
 *   card from among them and put it into your hand. Put the rest on the bottom of your library in
 *   any order.
 */
class CourageousOutriderScenarioTest : ScenarioTestBase() {

    init {
        test("entering digs four deep for a Human and bottoms the rest") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Courageous Outrider")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Elite Vanguard") // Human Soldier
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Craw Wurm") // fifth card — out of reach
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Courageous Outrider").error shouldBe null
            game.resolveStack()

            val vanguard = game.findCardsInLibrary(1, "Elite Vanguard").single()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the Human card is selectable") {
                decision.options shouldContainExactly listOf(vanguard)
            }
            game.selectCards(listOf(vanguard)).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
            game.resolveStack()

            game.isInHand(1, "Elite Vanguard") shouldBe true
            game.librarySize(1) shouldBe 4
            withClue("the three non-Humans went under the untouched fifth card") {
                game.state.getLibrary(game.player1Id).first() shouldBe
                    game.findCardsInLibrary(1, "Craw Wurm").single()
            }
        }
    }
}
