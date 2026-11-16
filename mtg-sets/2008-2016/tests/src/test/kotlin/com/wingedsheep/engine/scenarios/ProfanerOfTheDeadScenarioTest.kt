package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.ProfanerOfTheDead
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Profaner of the Dead (DTK #70) — {3}{U} 3/3 Snake Wizard.
 *
 * "Exploit. When this creature exploits a creature, return to their owners' hands all creatures your
 * opponents control with toughness less than the exploited creature's toughness."
 *
 * Pins that the threshold is the exploited creature's toughness *as it last existed on the
 * battlefield* (a +1/+1 counter counts, though it's gone once the creature is in the graveyard),
 * that the comparison is strict, that only opponents' creatures are bounced, that exploiting the
 * Profaner itself uses its own toughness, and that declining bounces nothing.
 */
class ProfanerOfTheDeadScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ProfanerOfTheDead))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castProfaner(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Profaner of the Dead")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("uses the exploited creature's last-known toughness and bounces only smaller opposing creatures") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        // A 2/2 with a +1/+1 counter: 3 toughness on the battlefield, base 2 in the graveyard.
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.addComponent(fodder, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        val myLions = driver.putCreatureOnBattlefield(me, "Savannah Lions")
        val oppLions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        val oppBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val oppCourser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        castProfaner(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass() // resolve the reflexive mass bounce

        driver.getGraveyard(me) shouldContain fodder
        driver.getHand(opp) shouldContain oppLions
        driver.getHand(opp) shouldContain oppBears // toughness 2 < 3 (last-known, counter included)
        driver.getPermanents(opp) shouldContain oppCourser // toughness 3 is not less than 3
        driver.getPermanents(me) shouldContain myLions // only opponents' creatures return
    }

    test("exploiting itself compares against the Profaner's own toughness") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val oppBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val oppCourser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val profaner = castProfaner(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true) // the Profaner is my only creature -> sacrifices itself
        if (driver.pendingDecision != null) driver.submitCardSelection(me, listOf(profaner))
        driver.bothPass()

        driver.getGraveyard(me) shouldContain profaner
        driver.getHand(opp) shouldContain oppBears
        driver.getPermanents(opp) shouldContain oppCourser
    }

    test("declining the exploit sacrifice bounces nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val oppLions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        castProfaner(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.pendingDecision shouldBe null
        driver.getPermanents(opp) shouldContain oppLions
        driver.getHand(opp) shouldNotContain oppLions
    }
})
