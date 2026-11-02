package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Chemister's Insight (GRN #32): draw two cards, with jump-start — castable from the graveyard by
 * discarding a card in addition to its mana cost, then exiled rather than returning to the graveyard.
 */
class ChemistersInsightScenarioTest : ScenarioTestBase() {

    init {
        test("jump-started from the graveyard with a discard, it draws two and is exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Chemister's Insight")
                .withCardInHand(1, "Island")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val insight = game.findCardsInGraveyard(1, "Chemister's Insight").single()
            val discard = game.findCardsInHand(1, "Island").single()

            val cast = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == insight }.action as CastSpell
            game.execute(
                cast.copy(additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(discard)))
            ).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe 2
            game.isInGraveyard(1, "Island") shouldBe true
            game.isInExile(1, "Chemister's Insight") shouldBe true
            game.isInGraveyard(1, "Chemister's Insight") shouldBe false
        }

        test("with no card to discard, it can't be jump-started") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Chemister's Insight")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val insight = game.findCardsInGraveyard(1, "Chemister's Insight").single()

            game.getLegalActions(1)
                .filter { (it.action as? CastSpell)?.cardId == insight && it.isAffordable }
                .shouldBeEmpty()
        }
    }
}
