package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Contingency Plan (EMN #52) — {1}{U} Sorcery.
 *
 *   Surveil 5.
 */
class ContingencyPlanScenarioTest : ScenarioTestBase() {

    init {
        test("surveils the top five: chosen cards go to the graveyard, the rest stay on top") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Contingency Plan")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Shock")
                .withCardInLibrary(1, "Craw Wurm") // sixth card — out of reach
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Contingency Plan").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("exactly the top five are looked at") {
                decision.options.size shouldBe 5
            }
            val toGraveyard = listOf("Lightning Bolt", "Hill Giant", "Shock")
                .map { game.findCardsInLibrary(1, it).single() }
            game.selectCards(toGraveyard).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
            game.resolveStack()

            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Shock") shouldBe true
            game.librarySize(1) shouldBe 3
            withClue("the two kept cards stay above the untouched sixth card") {
                game.state.getLibrary(game.player1Id).last() shouldBe
                    game.findCardsInLibrary(1, "Craw Wurm").single()
            }
        }
    }
}
