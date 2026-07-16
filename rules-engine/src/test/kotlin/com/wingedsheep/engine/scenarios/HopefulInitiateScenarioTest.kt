package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.vow.cards.HopefulInitiate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hopeful Initiate (VOW #20, canonical printing) — {W} 1/2 Creature — Human Warlock, Training + a
 * counter-fueled removal ability.
 *
 * "{2}{W}, Remove two +1/+1 counters from among creatures you control: Destroy target artifact or
 * enchantment." The cost is a [Costs.Composite] of {2}{W} and a distributed removal of two +1/+1
 * counters scoped to creatures you control (they may come from any of your creatures, not just the
 * Initiate). Proven:
 *  - remove two counters (from the Initiate) + pay {2}{W} → the target enchantment is destroyed;
 *  - the removal may draw from *multiple* creatures (one counter from each of two) — the "from among
 *    creatures you control" pooling;
 *  - an insufficient-counter payment is rejected (the gate: you must actually have two to remove).
 *
 * Counters are placed directly on the board here; Training's counter-generation is proven in
 * [TrainingTest]. The +1/+1 counter is fed to this ability's cost.
 */
class HopefulInitiateScenarioTest : FunSpec({

    val abilityId = HopefulInitiate.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.giveCounters(id: EntityId, count: Int) {
        replaceState(
            state.updateEntity(id) { c ->
                c.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to count)))
            }
        )
    }

    test("remove two +1/+1 counters + pay {2}{W} destroys the target enchantment") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val initiate = driver.putCreatureOnBattlefield(you, "Hopeful Initiate")
        driver.removeSummoningSickness(initiate)
        driver.giveCounters(initiate, 2)
        val enchantment = driver.putPermanentOnBattlefield(you, "Test Enchantment")

        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 2)

        val result = driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = initiate,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(initiate, "+1/+1", 2)
                    )
                )
            )
        )
        result.isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // The two +1/+1 counters were removed and the enchantment destroyed.
        driver.state.getEntity(initiate)
            ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
        driver.getGraveyard(you).any { driver.getCardName(it) == "Test Enchantment" } shouldBe true
    }

    test("the two counters may be pooled from among multiple creatures you control") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val initiate = driver.putCreatureOnBattlefield(you, "Hopeful Initiate")
        val ally = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        driver.removeSummoningSickness(initiate)
        driver.giveCounters(initiate, 1)
        driver.giveCounters(ally, 1)
        val enchantment = driver.putPermanentOnBattlefield(you, "Test Enchantment")

        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 2)

        val result = driver.submit(
            ActivateAbility(
                playerId = you,
                sourceId = initiate,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(initiate, "+1/+1", 1),
                        DistributedCounterRemoval(ally, "+1/+1", 1)
                    )
                )
            )
        )
        result.isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.getEntity(initiate)
            ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
        driver.state.getEntity(ally)
            ?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 shouldBe 0
        driver.getGraveyard(you).any { driver.getCardName(it) == "Test Enchantment" } shouldBe true
    }

    test("activation is rejected when there aren't two +1/+1 counters to remove") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val initiate = driver.putCreatureOnBattlefield(you, "Hopeful Initiate")
        driver.removeSummoningSickness(initiate)
        driver.giveCounters(initiate, 1) // only one counter — not enough
        val enchantment = driver.putPermanentOnBattlefield(you, "Test Enchantment")

        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 2)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = you,
                sourceId = initiate,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(enchantment)),
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(initiate, "+1/+1", 2)
                    )
                )
            )
        )

        // The enchantment survives — the ability never resolved.
        driver.getGraveyard(you).any { driver.getCardName(it) == "Test Enchantment" } shouldBe false
    }
})
