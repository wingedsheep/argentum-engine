package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * [Costs.RemoveAllCountersFromSelf] + [DynamicAmounts.countersRemovedAsCost] — the Molten Hydra
 * shape, "Remove all +1/+1 counters from this creature: It deals damage to any target equal to the
 * number of +1/+1 counters removed this way." (The granter-scoped form is covered by
 * `HankyuScenarioTest`.)
 */
class RemoveAllCountersCostTest : FunSpec({

    val hydra = card("Counter Hydra") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Hydra"
        power = 1
        toughness = 1
        oracleText = "Remove all +1/+1 counters from this creature: It deals damage to any target " +
            "equal to the number of +1/+1 counters removed this way."

        activatedAbility {
            cost = Costs.RemoveAllCountersFromSelf(CounterType.PLUS_ONE_PLUS_ONE)
            val t = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.countersRemovedAsCost(), t)
        }
    }
    val abilityId = hydra.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(hydra))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.counters(id: EntityId, type: CounterType): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("description") {
        Costs.RemoveAllCountersFromSelf(CounterType.PLUS_ONE_PLUS_ONE).description shouldBe
            "Remove all +1/+1 counters from this permanent"
        Costs.RemoveAllCountersFromGrantingPermanent(CounterType.AIM).description shouldBe
            "Remove all aim counters from the granting permanent"
    }

    test("removes every counter of the named kind only, and the effect reads how many came off") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val source = driver.putCreatureOnBattlefield(me, "Counter Hydra")
        driver.replaceState(
            driver.state.updateEntity(source) { c ->
                c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4, CounterType.CHARGE to 2)))
            }
        )

        val result = driver.submit(
            ActivateAbility(me, source, abilityId, targets = listOf(ChosenTarget.Player(opponent)))
        )
        result.outcome shouldBe Outcome.Done
        result.events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 4
        driver.counters(source, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
        driver.counters(source, CounterType.CHARGE) shouldBe 2

        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 16
    }

    test("with none of those counters it is still offered and activatable, deals 0, and emits no removal") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val source = driver.putCreatureOnBattlefield(me, "Counter Hydra")

        driver.legalActions(me).any { (it.action as? ActivateAbility)?.abilityId == abilityId } shouldBe true

        val result = driver.submit(
            ActivateAbility(me, source, abilityId, targets = listOf(ChosenTarget.Player(opponent)))
        )
        result.outcome shouldBe Outcome.Done
        result.events.filterIsInstance<CountersRemovedEvent>().shouldBeEmpty()

        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
