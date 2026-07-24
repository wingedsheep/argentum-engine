package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class FinaleOfRevelationScenarioTest : ScenarioTestBase() {

    init {
        test("X of ten performs the replacement sequence and exiles Finale") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Finale of Revelation")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 12)
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val finale = game.findCardsInHand(1, "Finale of Revelation").single()
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = finale,
                    xValue = 10,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )
            ).error shouldBe null
            game.resolveStack()

            val untapDecision = game.state.pendingDecision as SelectCardsDecision
            game.selectCards(untapDecision.options.take(5))
            game.resolveStack()

            game.handSize(1) shouldBe 10
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
            game.isInExile(1, "Finale of Revelation") shouldBe true
            game.state.getEntity(game.player1Id)?.has<PlayerNoMaximumHandSizeComponent>() shouldBe true
        }
    }
}
