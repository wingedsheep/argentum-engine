package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.AjaniCallerOfThePride
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ajani, Caller of the Pride {1}{W}{W} — Legendary Planeswalker — Ajani (loyalty 4)
 *   +1: Put a +1/+1 counter on up to one target creature.
 *   −3: Target creature gains flying and double strike until end of turn.
 *   −8: Create X 2/2 white Cat creature tokens, where X is your life total.
 *
 * Covers the "up to one" optional target in both directions (chosen / declined), the −3 keyword
 * grant, and the −8 life-total-sized token count.
 */
class AjaniCallerOfThePrideScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AjaniCallerOfThePride))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun putAjani(driver: GameTestDriver, playerId: EntityId, loyalty: Int): EntityId {
        val ajani = driver.putPermanentOnBattlefield(playerId, "Ajani, Caller of the Pride")
        driver.addComponent(ajani, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        return ajani
    }

    fun loyalty(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    fun plusCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    val abilities = AjaniCallerOfThePride.script.activatedAbilities

    test("+1 puts a +1/+1 counter on the chosen creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val ajani = putAjani(driver, me, 4)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = ajani,
                abilityId = abilities[0].id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        driver.bothPass()

        plusCounters(driver, bears) shouldBe 1
        loyalty(driver, ajani) shouldBe 5
    }

    test("+1 may be activated with no target — Ajani still gains loyalty, nothing else happens") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val ajani = putAjani(driver, me, 4)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = ajani, abilityId = abilities[0].id)
        )
        driver.bothPass()

        plusCounters(driver, bears) shouldBe 0
        loyalty(driver, ajani) shouldBe 5
    }

    test("−3 grants flying and double strike until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val ajani = putAjani(driver, me, 4)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = ajani,
                abilityId = abilities[1].id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        )
        driver.bothPass()

        driver.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        driver.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
        loyalty(driver, ajani) shouldBe 1
    }

    test("−8 creates one 2/2 white Cat per point of the controller's life total") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val ajani = putAjani(driver, me, 8)
        driver.setLifeTotal(me, 3)

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = ajani, abilityId = abilities[2].id)
        )
        driver.bothPass()

        val cats = driver.getCreatures(me).filter { driver.getCardName(it)?.contains("Cat") == true }
        cats.size shouldBe 3
        cats.forEach { cat ->
            driver.state.projectedState.getPower(cat) shouldBe 2
            driver.state.projectedState.getToughness(cat) shouldBe 2
        }
        loyalty(driver, ajani) shouldBe 0
    }
})
