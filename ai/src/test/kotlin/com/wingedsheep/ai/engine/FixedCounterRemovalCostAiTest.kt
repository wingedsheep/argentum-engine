package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
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
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * "Remove three counters from among creatures you control" (Lieutenant Commander Data) is a
 * selection cost the AI must fill itself — the engine refuses an activation that names no
 * counters. -1/-1 counters go first (shedding them is a gain); the rest come off the least
 * valuable creature.
 */
class FixedCounterRemovalCostAiTest : FunSpec({

    fun GameTestDriver.addCounters(id: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(type, count))
        })
    }

    test("the AI pays Data's counter cost with the -1/-1 counter first, and the activation resolves") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + LieutenantCommanderData)
            initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val data = d.putCreatureOnBattlefield(d.player1, "Lieutenant Commander Data")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.addCounters(data, CounterType.PLUS_ONE_PLUS_ONE, 4)
        d.addCounters(bears, CounterType.MINUS_ONE_MINUS_ONE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)

        val offer = d.legalActions(d.player1)
            .first { it.actionType == "ActivateAbility" && it.additionalCostInfo?.costType == "RemoveCounters" }
        val paid = SelectionCostPayment.withPlainCosts(
            d.state, offer, offer.action, IntentCatalog.of(d.cardRegistry),
        ) as ActivateAbility

        paid.costPayment!!.distributedCounterRemovals shouldContainExactlyInAnyOrder listOf(
            DistributedCounterRemoval(bears, CounterType.MINUS_ONE_MINUS_ONE.printed, 1),
            DistributedCounterRemoval(data, CounterType.PLUS_ONE_PLUS_ONE.printed, 2),
        )
        val result = d.submit(paid)
        withClue("activation failed: ${result.error}") { result.outcome shouldBe Outcome.Done }
    }
})
