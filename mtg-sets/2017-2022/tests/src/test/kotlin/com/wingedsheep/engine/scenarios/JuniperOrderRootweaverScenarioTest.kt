package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class JuniperOrderRootweaverScenarioTest : ScenarioTestBase() {
    init {
        for (targetSelf in listOf(false, true)) {
            test("kicked Rootweaver puts its counter on ${if (targetSelf) "itself" else "another creature you control"}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Juniper Order Rootweaver")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Juniper Order Rootweaver").first()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.execute(CastSpell(
                    playerId = game.player1Id,
                    cardId = card,
                    declaredCostSlot = ChoiceSlot.KICKED,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )).error shouldBe null
                game.resolveStack()

                val rootweaver = game.findPermanent("Juniper Order Rootweaver")!!
                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, rootweaver)
                val recipient = if (targetSelf) rootweaver else bears
                game.selectTargets(listOf(recipient)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(recipient)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                val other = if (targetSelf) bears else rootweaver
                (game.state.getEntity(other)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }
        }

        test("unkicked Rootweaver requests no targets and adds no counters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Juniper Order Rootweaver")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Juniper Order Rootweaver").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeNull()
            game.isOnBattlefield("Juniper Order Rootweaver") shouldBe true
            for (name in listOf("Juniper Order Rootweaver", "Grizzly Bears")) {
                val permanent = game.findPermanent(name)!!
                (game.state.getEntity(permanent)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }
        }
    }
}
