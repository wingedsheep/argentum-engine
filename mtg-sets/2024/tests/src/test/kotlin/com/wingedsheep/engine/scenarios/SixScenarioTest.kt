package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Six (MH3 #169) — {2}{G} Legendary Creature — Treefolk 2/4. Reach. Attacking mills three and may
 * put a milled land card into hand; during your turn, nonland permanent cards in your graveyard have
 * retrace (cast from the graveyard by discarding a land card in addition to their other costs).
 */
class SixScenarioTest : ScenarioTestBase() {
    init {
        fun graveyardCasts(game: TestGame, cardId: com.wingedsheep.sdk.model.EntityId, player: Int = 1) =
            game.getLegalActions(player).filter { (it.action as? CastSpell)?.cardId == cardId }

        test("attacking mills three and puts a milled land card into hand") {
            val game = scenario().withPlayers("P1", "P2")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Six")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Island")
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Six" to 2)).error shouldBe null
            var guard = 0
            while (!game.hasPendingDecision() && guard++ < 5) game.passPriority()
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val forest = game.findCardsInGraveyard(1, "Forest").single()
            decision.options.contains(forest) shouldBe true
            game.selectCards(listOf(forest)).error shouldBe null
            game.isInHand(1, "Forest") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.librarySize(1) shouldBe 1
        }

        test("during your turn a nonland permanent card is cast from the graveyard by discarding a land card") {
            val game = scenario().withPlayers("P1", "P2")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Six")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInHand(1, "Island")
                .build()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            val island = game.findCardsInHand(1, "Island").single()

            graveyardCasts(game, bolt).size shouldBe 0
            val cast = graveyardCasts(game, bears).single()
            cast.additionalCostInfo shouldNotBe null

            game.execute(
                CastSpell(
                    game.player1Id, bears,
                    additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(island)),
                    graveyardCastRider = (cast.action as CastSpell).graveyardCastRider
                )
            ).error shouldBe null
            game.isInGraveyard(1, "Island") shouldBe true
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("without a land card to discard there is no retrace cast") {
            val game = scenario().withPlayers("P1", "P2")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Six")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .build()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            graveyardCasts(game, bears).size shouldBe 0
            game.execute(CastSpell(game.player1Id, bears)).error shouldNotBe null
        }

        test("retrace is not granted during an opponent's turn") {
            val game = scenario().withPlayers("P1", "P2")
                .withActivePlayer(2).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Six")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInGraveyard(1, "Ambush Viper")
                .withCardInHand(1, "Island")
                .build()
            game.passPriority()
            val viper = game.findCardsInGraveyard(1, "Ambush Viper").single()
            val island = game.findCardsInHand(1, "Island").single()
            graveyardCasts(game, viper).size shouldBe 0
            game.execute(
                CastSpell(
                    game.player1Id, viper,
                    additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(island))
                )
            ).error shouldNotBe null
        }
    }
}
