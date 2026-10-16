package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [PreventDamageByRemovingCounter.onRemoved] — the rest of the replacement after the counter goes,
 * run as a replacement rider with the number of counters removed. Bloatfly Swarm's scenario test
 * covers the "that many" + `requiresCounter` shape with rad counters, in and out of combat; this pins
 * the Unbreathing Horde shape: one counter per event, and no rider when there was nothing to remove
 * (the damage is still prevented).
 */
class PreventDamageByRemovingCounterRiderTest : FunSpec({

    // "If this creature would be dealt damage, prevent that damage, remove a +1/+1 counter from it,
    // then you gain 1 life for each counter removed this way."
    val hoarder = card("Test Counter Hoarder") {
        manaCost = "{2}"
        typeLine = "Creature — Zombie"
        power = 0
        toughness = 1
        replacementEffect(
            PreventDamageByRemovingCounter(
                onRemoved = Effects.GainLife(DynamicAmounts.triggerCountersRemoved())
            )
        )
    }

    val pinger = card("Test Rider Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Wizard"
        power = 1
        toughness = 1
        activatedAbility {
            val any = target(Targets.Any)
            cost = Costs.Mana("{0}")
            effect = Effects.DealDamage(3, any)
        }
    }

    data class Setup(val driver: GameTestDriver, val you: EntityId, val opponent: EntityId, val hoarderId: EntityId, val pingerId: EntityId)

    fun setUp(counters: Int): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(hoarder, pinger))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val hoarderId = driver.putCreatureOnBattlefield(you, "Test Counter Hoarder")
        if (counters > 0) driver.replaceState(driver.state.updateEntity(hoarderId) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, counters))
        })
        val pingerId = driver.putCreatureOnBattlefield(you, "Test Rider Pinger")
        driver.removeSummoningSickness(pingerId)
        return Setup(driver, you, opponent, hoarderId, pingerId)
    }

    fun Setup.ping() {
        val ping = driver.cardRegistry.requireCard("Test Rider Pinger").activatedAbilities.single()
        driver.submit(ActivateAbility(playerId = you, sourceId = pingerId, abilityId = ping.id,
            targets = listOf(ChosenTarget.Permanent(hoarderId)))).error shouldBe null
        driver.bothPass()
    }

    fun Setup.plusOnes(): Int =
        driver.state.getEntity(hoarderId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("one counter per event, and the rider reads the one counter removed") {
        val setup = setUp(counters = 3)
        setup.ping()
        setup.plusOnes() shouldBe 2
        (setup.driver.state.getEntity(setup.hoarderId)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        setup.driver.assertLifeTotal(setup.you, 21)
        setup.driver.state.stack.isEmpty() shouldBe true
    }

    test("no counter to remove: the damage is still prevented and the rider does not run") {
        val setup = setUp(counters = 0)
        setup.ping()
        (setup.driver.state.getEntity(setup.hoarderId)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        setup.driver.assertLifeTotal(setup.you, 20)
    }
})
