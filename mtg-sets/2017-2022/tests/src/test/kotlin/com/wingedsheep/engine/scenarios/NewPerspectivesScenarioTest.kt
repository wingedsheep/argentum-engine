package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * New Perspectives (AKH #63) — "When this enchantment enters, draw three cards. As long as you have
 * seven or more cards in hand, you may pay {0} rather than pay cycling costs."
 *
 * Pins the seven-card gate (the card being cycled counts — it's still in hand as the ability is
 * activated) and the enters-draw.
 */
class NewPerspectivesScenarioTest : ScenarioTestBase() {

    private fun cycleAction(game: TestGame, cardName: String) =
        game.getLegalActions(1).firstOrNull { (it.action as? CycleCard)?.cardId == game.findCardsInHand(1, cardName).first() }

    init {
        test("with seven cards in hand, a card cycles for {0} with no mana available") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "New Perspectives")
                .withCardInHand(1, "Hieroglyphic Illumination")
                .withCardsInHand(1, "Island", 6)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val offered = cycleAction(game, "Hieroglyphic Illumination")
            offered.shouldNotBeNull()
            withClue("the cycle is offered as affordable, priced {0}") {
                offered.isAffordable shouldBe true
                offered.manaCostString shouldBe "{0}"
            }

            val illumination = game.findCardsInHand(1, "Hieroglyphic Illumination").first()
            game.execute(CycleCard(game.player1Id, illumination)).error shouldBe null
            game.resolveStack()

            withClue("discarded and replaced: still seven cards in hand") {
                game.isInGraveyard(1, "Hieroglyphic Illumination") shouldBe true
                game.handSize(1) shouldBe 7
            }
        }

        test("with six cards in hand, cycling costs its printed {U}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "New Perspectives")
                .withCardInHand(1, "Hieroglyphic Illumination")
                .withCardsInHand(1, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val offered = cycleAction(game, "Hieroglyphic Illumination")
            offered.shouldNotBeNull()
            offered.isAffordable shouldBe false
            offered.manaCostString shouldBe "{U}"

            val illumination = game.findCardsInHand(1, "Hieroglyphic Illumination").first()
            game.execute(CycleCard(game.player1Id, illumination)).error shouldNotBe null
        }

        test("entering draws three cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "New Perspectives")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "New Perspectives").error shouldBe null
            game.resolveStack()

            game.findPermanent("New Perspectives").shouldNotBeNull()
            game.handSize(1) shouldBe 3
        }
    }
}
