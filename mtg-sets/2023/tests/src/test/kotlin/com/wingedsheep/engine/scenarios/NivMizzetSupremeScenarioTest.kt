package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Niv-Mizzet, Supreme (MAT #40): an instant or sorcery card in its controller's graveyard that is
 * exactly two colors has jump-start — castable from the graveyard by discarding a card in addition
 * to its other costs, then exiled. A monocolored one doesn't.
 */
class NivMizzetSupremeScenarioTest : ScenarioTestBase() {

    init {
        test("a two-color instant is jump-started from the graveyard with a discard and then exiled") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Niv-Mizzet, Supreme")
                .withCardInGraveyard(1, "Lightning Helix")
                .withCardInHand(1, "Island")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val helix = game.findCardsInGraveyard(1, "Lightning Helix").single()
            val island = game.findCardsInHand(1, "Island").single()

            val offered = game.getLegalActions(1).single { (it.action as? CastSpell)?.cardId == helix }
            val cast = offered.action as CastSpell
            game.execute(
                cast.copy(
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(island))
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 23
            game.isInExile(1, "Lightning Helix") shouldBe true
            game.isInGraveyard(1, "Lightning Helix") shouldBe false
            game.isInGraveyard(1, "Island") shouldBe true
        }

        test("a monocolored instant in the graveyard gains no jump-start") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Niv-Mizzet, Supreme")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInHand(1, "Island")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()

            game.getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == bolt }.shouldBeEmpty()
        }
    }
}
