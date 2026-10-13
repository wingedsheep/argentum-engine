package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BenalishSleeperScenarioTest : ScenarioTestBase() {
    init {
        test("kicked: each player chooses in APNAP order, then all creatures are sacrificed together") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Benalish Sleeper")
                .withCardOnBattlefield(1, "Blood Artist")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.execute(CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Benalish Sleeper").first(),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            )).error shouldBe null
            game.resolveStack()

            // The active player chooses first, between Blood Artist and the Sleeper itself.
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe game.player1Id
            game.selectCards(listOf(game.findPermanent("Blood Artist")!!)).error shouldBe null

            // Nothing is sacrificed until the opponent has chosen too.
            game.isOnBattlefield("Blood Artist") shouldBe true
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe game.player2Id
            game.selectCards(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null

            game.isInGraveyard(1, "Blood Artist") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Benalish Sleeper") shouldBe true
            game.isOnBattlefield("Raging Goblin") shouldBe true

            // Blood Artist died alongside Hill Giant, so it sees both deaths (two triggers).
            var drains = 0
            while (true) {
                val decision = game.getPendingDecision() as? ChooseTargetsDecision ?: break
                decision.playerId shouldBe game.player1Id
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                drains++
                game.resolveStack()
            }
            game.resolveStack()
            drains shouldBe 2
            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 18
        }

        test("unkicked: nobody sacrifices") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Benalish Sleeper")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Benalish Sleeper").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeNull()
            game.isOnBattlefield("Benalish Sleeper") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
