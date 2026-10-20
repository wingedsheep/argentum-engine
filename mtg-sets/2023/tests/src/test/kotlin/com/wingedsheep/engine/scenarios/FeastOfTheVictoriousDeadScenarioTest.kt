package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Feast of the Victorious Dead — "At the beginning of your end step, if one or more creatures died
 * this turn, you gain that much life and distribute that many +1/+1 counters among any number of
 * creatures you control."
 */
class FeastOfTheVictoriousDeadScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("creatures that died under either player's control set both the life and the counter pool") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Feast of the Victorious Dead")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = g.findPermanent("Grizzly Bears")!!
            val giant = g.findPermanent("Hill Giant")!!

            g.castSpell(1, "Shock", g.findPermanent("Llanowar Elves")!!).error shouldBe null
            g.resolveStack()
            g.castSpell(1, "Lightning Bolt", g.findPermanent("Savannah Lions")!!).error shouldBe null
            g.resolveStack()

            g.passUntilPhase(Phase.ENDING, Step.END)
            g.resolveStack()

            val decision = g.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
            decision.totalAmount shouldBe 2
            decision.targets shouldContainExactlyInAnyOrder listOf(bears, giant)
            g.submitDecision(DistributionResponse(decision.id, mapOf(bears to 2)))

            g.getLifeTotal(1) shouldBe 22
            g.plusOnes(bears) shouldBe 2
            g.plusOnes(giant) shouldBe 0
        }

        test("the counters can be split among several creatures") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Feast of the Victorious Dead")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = g.findPermanent("Grizzly Bears")!!
            val giant = g.findPermanent("Hill Giant")!!

            g.castSpell(1, "Shock", g.findPermanent("Llanowar Elves")!!).error shouldBe null
            g.resolveStack()
            g.castSpell(1, "Lightning Bolt", g.findPermanent("Savannah Lions")!!).error shouldBe null
            g.resolveStack()

            g.passUntilPhase(Phase.ENDING, Step.END)
            g.resolveStack()

            val decision = g.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
            g.submitDecision(DistributionResponse(decision.id, mapOf(bears to 1, giant to 1)))

            g.getLifeTotal(1) shouldBe 22
            g.plusOnes(bears) shouldBe 1
            g.plusOnes(giant) shouldBe 1
        }

        test("no creature died — the trigger does nothing") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Feast of the Victorious Dead")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            g.passUntilPhase(Phase.ENDING, Step.END)
            g.resolveStack()

            g.getPendingDecision() shouldBe null
            g.getLifeTotal(1) shouldBe 20
            g.plusOnes(g.findPermanent("Grizzly Bears")!!) shouldBe 0
        }
    }
}
