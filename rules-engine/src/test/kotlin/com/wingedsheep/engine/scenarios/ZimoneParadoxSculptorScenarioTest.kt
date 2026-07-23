package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fdn.cards.ZimoneParadoxSculptor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Zimone, Paradox Sculptor {2}{G}{U} — Legendary Creature — Human Wizard 1/4
 *   At the beginning of combat on your turn, put a +1/+1 counter on each of up to two target
 *   creatures you control.
 *   {G}{U}, {T}: Double the number of each kind of counter on up to two target creatures and/or
 *   artifacts you control.
 *
 * The activated ability exercises the every-kind doubling path of `DoubleCountersEffect`
 * (`counterType = null`): a permanent carrying two different kinds of counter has both doubled in
 * one activation, and each chosen target is processed independently.
 */
class ZimoneParadoxSculptorScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ZimoneParadoxSculptor))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Island" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, entityId: EntityId, type: CounterType): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()?.getCount(type) ?: 0

    val doubleAbilityId = ZimoneParadoxSculptor.script.activatedAbilities[0].id

    test("the activated ability doubles every kind of counter on both chosen permanents") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val zimone = driver.putCreatureOnBattlefield(me, "Zimone, Paradox Sculptor")
        driver.removeSummoningSickness(zimone)

        val bears = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.addComponent(
            bears,
            CountersComponent(
                mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3, CounterType.CHARGE to 1)
            )
        )
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        driver.addComponent(lions, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))

        driver.giveMana(me, Color.GREEN, 1)
        driver.giveMana(me, Color.BLUE, 1)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = zimone,
                abilityId = doubleAbilityId,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(lions))
            )
        )
        while (driver.stackSize > 0) driver.bothPass()

        counters(driver, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 6
        counters(driver, bears, CounterType.CHARGE) shouldBe 2
        counters(driver, lions, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
    }

    test("the activated ability may be activated with no targets and does nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val zimone = driver.putCreatureOnBattlefield(me, "Zimone, Paradox Sculptor")
        driver.removeSummoningSickness(zimone)
        val bears = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.addComponent(bears, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))

        driver.giveMana(me, Color.GREEN, 1)
        driver.giveMana(me, Color.BLUE, 1)

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = zimone, abilityId = doubleAbilityId)
        )
        while (driver.stackSize > 0) driver.bothPass()

        counters(driver, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
    }

    test("the begin-combat trigger puts a +1/+1 counter on each of the two chosen creatures") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Zimone, Paradox Sculptor")
        val bears = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        var safety = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && safety < 40) {
            driver.bothPass()
            safety++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true

        driver.submitTargetSelection(me, listOf(bears, lions))
        while (driver.stackSize > 0) driver.bothPass()

        counters(driver, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        counters(driver, lions, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }
})
