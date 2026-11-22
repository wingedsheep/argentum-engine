package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.YesNoDecision
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
 * Vastwood Hydra — "This creature enters with X +1/+1 counters on it. When this creature dies, you
 * may distribute a number of +1/+1 counters equal to the number of +1/+1 counters on this creature
 * among any number of creatures you control."
 *
 * The dies ability reads the hydra's last-known +1/+1 counters, and doesn't target: the division is
 * made at resolution among the creatures its controller controls.
 */
class VastwoodHydraScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun hydraBoard() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Vastwood Hydra")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withLandsOnBattlefield(1, "Mountain", 4) // spare red for the Bolt whichever lands pay {X}
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enters with X counters and, on dying, distributes that many among your creatures") {
            val g = hydraBoard()
            val bears = g.findPermanent("Grizzly Bears")!!
            val giant = g.findPermanent("Hill Giant")!!

            g.castXSpell(1, "Vastwood Hydra", 3).error shouldBe null
            g.resolveStack()
            val hydra = g.findPermanent("Vastwood Hydra")!!
            g.plusOnes(hydra) shouldBe 3

            g.castSpell(1, "Lightning Bolt", hydra).error shouldBe null
            g.resolveStack()
            g.findPermanent("Vastwood Hydra") shouldBe null

            g.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            g.answerYesNo(true)

            val decision = g.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
            decision.totalAmount shouldBe 3
            decision.targets shouldContainExactlyInAnyOrder listOf(bears, giant)
            g.submitDecision(DistributionResponse(decision.id, mapOf(bears to 1, giant to 2)))

            g.plusOnes(bears) shouldBe 1
            g.plusOnes(giant) shouldBe 2
            g.plusOnes(g.findPermanent("Savannah Lions")!!) shouldBe 0
        }

        test("declining the may puts no counters anywhere") {
            val g = hydraBoard()
            val bears = g.findPermanent("Grizzly Bears")!!
            val giant = g.findPermanent("Hill Giant")!!

            g.castXSpell(1, "Vastwood Hydra", 2).error shouldBe null
            g.resolveStack()
            g.castSpell(1, "Lightning Bolt", g.findPermanent("Vastwood Hydra")!!).error shouldBe null
            g.resolveStack()

            g.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            g.answerYesNo(false)

            g.getPendingDecision() shouldBe null
            g.plusOnes(bears) shouldBe 0
            g.plusOnes(giant) shouldBe 0
        }
    }
}
