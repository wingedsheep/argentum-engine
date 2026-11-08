package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.LieutenantCommanderData
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lieutenant Commander Data (TRK) — "{1}, Remove three counters from among creatures you control:
 * Draw a card."
 *
 * The offer never told the client how many counters the cost takes, so the client skipped its
 * counter-distribution picker and the activation was refused with "Counter removal total (0) does
 * not match required count (3)".
 */
class LieutenantCommanderDataScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(LieutenantCommanderData)
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.addCounters(id: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    fun GameTestDriver.counters(id: EntityId, type: CounterType) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun GameTestDriver.drawOffer() = legalActions(player1)
        .first { it.actionType == "ActivateAbility" && it.additionalCostInfo?.costType == "RemoveCounters" }

    test("the offer names the fixed total, so the client opens its counter picker") {
        val d = driver()
        val data = d.putCreatureOnBattlefield(d.player1, "Lieutenant Commander Data")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addCounters(data, CounterType.PLUS_ONE_PLUS_ONE, 2)
        d.addCounters(bears, CounterType.MINUS_ONE_MINUS_ONE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)

        val info = d.drawOffer().additionalCostInfo!!
        info.distributedCounterRemovalTotal shouldBe 3
        info.description shouldBe "Remove three counters from among creatures you control"
        info.counterRemovalCreatures.map { it.entityId }.toSet() shouldBe setOf(data, bears)
    }

    test("counters of any kind, split across creatures, pay it and draw a card") {
        val d = driver()
        val data = d.putCreatureOnBattlefield(d.player1, "Lieutenant Commander Data")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addCounters(data, CounterType.PLUS_ONE_PLUS_ONE, 2)
        d.addCounters(bears, CounterType.MINUS_ONE_MINUS_ONE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)
        val handBefore = d.getHandSize(d.player1)

        val activate = d.drawOffer().action as ActivateAbility
        val result = d.submit(activate.copy(costPayment = AdditionalCostPayment(
            distributedCounterRemovals = listOf(
                DistributedCounterRemoval(data, CounterType.PLUS_ONE_PLUS_ONE.printed, 2),
                DistributedCounterRemoval(bears, CounterType.MINUS_ONE_MINUS_ONE.printed, 1),
            ),
        )))
        withClue("activation failed: ${result.error}") { result.outcome shouldBe Outcome.Done }
        d.bothPass()

        d.counters(data, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        d.counters(bears, CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
        d.getHandSize(d.player1) shouldBe handBefore + 1
    }
})
