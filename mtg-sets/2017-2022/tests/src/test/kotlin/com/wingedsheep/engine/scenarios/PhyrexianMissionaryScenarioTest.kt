package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PhyrexianMissionaryScenarioTest : ScenarioTestBase() {
    init {
        for (removeSource in listOf(false, true)) {
            test("kicked Missionary returns your creature card${if (removeSource) " even after the Missionary dies" else ""}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Phyrexian Missionary")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Plains")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Phyrexian Missionary").first()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()
                game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = card,
                    declaredCostSlot = ChoiceSlot.KICKED,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears)
                game.selectTargets(listOf(bears)).error shouldBe null
                if (removeSource) {
                    val missionary = game.findPermanent("Phyrexian Missionary")!!
                    game.castSpell(1, "Lightning Bolt", missionary).error shouldBe null
                }
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Plains") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Phyrexian Missionary") shouldBe removeSource
            }
        }

        test("unkicked Missionary requests no target and leaves the graveyard alone") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Phyrexian Missionary")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Phyrexian Missionary").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeNull()
            game.isOnBattlefield("Phyrexian Missionary") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }
    }
}
