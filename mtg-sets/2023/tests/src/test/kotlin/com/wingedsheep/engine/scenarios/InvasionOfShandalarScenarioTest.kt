package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Invasion of Shandalar // Leyline Surge. */
class InvasionOfShandalarScenarioTest : ScenarioTestBase() {
    init {
        test("front: returns up to three target permanent cards from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Shandalar")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Shandalar").error shouldBe null
            game.resolveStack()

            val targets = game.findCardsInGraveyard(1, "Grizzly Bears") +
                game.findCardsInGraveyard(1, "Hill Giant") +
                game.findCardsInGraveyard(1, "Forest")
            game.selectTargets(targets).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
            // A non-permanent card is not a legal target and stays put.
            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
        }

        test("back: at the beginning of your upkeep, you may put a permanent card from your hand onto the battlefield") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Shandalar")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            // Player 1 defeats their own Siege at instant speed during the opponent's turn.
            game.passPriority()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Shandalar")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Leyline Surge") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.minSelections shouldBe 0
            game.selectCards(game.findCardsInHand(1, "Hill Giant")).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
