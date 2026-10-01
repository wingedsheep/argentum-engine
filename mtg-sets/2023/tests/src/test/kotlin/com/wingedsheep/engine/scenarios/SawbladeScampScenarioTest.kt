package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.SawbladeScamp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sawblade Scamp (ONE #147) — {R} 1/1 Creature — Phyrexian Beast.
 *
 * "Haste. Whenever you cast a noncreature spell, put an oil counter on this creature.
 *  {T}, Remove an oil counter from this creature: It deals 1 damage to each opponent."
 */
class SawbladeScampScenarioTest : FunSpec({

    val pingId = SawbladeScamp.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SawbladeScamp))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    test("noncreature spell adds oil; creature spell does not") {
        val driver = newDriver()
        val p1 = driver.player1
        val opp = driver.getOpponent(p1)
        val scamp = driver.putCreatureOnBattlefield(p1, "Sawblade Scamp")

        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, bolt, listOf(opp)).outcome shouldBe Outcome.Done
        driver.bothPass()
        oil(driver, scamp) shouldBe 1
        driver.bothPass()

        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, bears).outcome shouldBe Outcome.Done
        driver.bothPass()
        oil(driver, scamp) shouldBe 1
    }

    test("haste: freshly cast scamp can tap and spend oil to deal 1 damage to each opponent") {
        val driver = newDriver()
        val p1 = driver.player1
        val opp = driver.getOpponent(p1)
        val scamp = driver.putCardInHand(p1, "Sawblade Scamp")
        driver.giveMana(p1, Color.RED, 1)
        driver.castSpell(p1, scamp).error shouldBe null
        driver.bothPass()
        driver.replaceState(driver.state.updateEntity(scamp) { it.with(CountersComponent(mapOf(CounterType.OIL to 2))) })

        val before = driver.getLifeTotal(opp)
        val selfBefore = driver.getLifeTotal(p1)
        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = scamp, abilityId = pingId))
        oil(driver, scamp) shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(opp) shouldBe before - 1
        driver.getLifeTotal(p1) shouldBe selfBefore
    }

    test("can't activate without an oil counter") {
        val driver = newDriver()
        val p1 = driver.player1
        val scamp = driver.putCreatureOnBattlefield(p1, "Sawblade Scamp")
        driver.removeSummoningSickness(scamp)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = scamp, abilityId = pingId))
    }
})
