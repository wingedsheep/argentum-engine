package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Felothar, Dawn of the Abzan ({W}{B}{G}, 3/3, Trample):
 * "Whenever Felothar enters or attacks, you may sacrifice a nonland permanent. When you do,
 *  put a +1/+1 counter on each creature you control."
 *
 * Composed from a [com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect] (optional
 * sacrifice) whose reflexive effect is a `ForEachInGroupEffect(AllCreaturesYouControl, +1/+1)`.
 */
class FelotharDawnOfTheAbzanTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusCounters(driver: GameTestDriver, id: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("enters trigger: sacrificing a nonland permanent puts a +1/+1 counter on each creature you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // A second creature so we can verify "each creature you control" gets a counter,
        // and an extra nonland permanent to feed the sacrifice (so it's a genuine choice).
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val fodder = driver.putCreatureOnBattlefield(me, "Hill Giant")

        // Cast Felothar so its "enters" trigger fires.
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.GREEN, 1)
        val felotharCard = driver.putCardInHand(me, "Felothar, Dawn of the Abzan")
        driver.castSpell(me, felotharCard).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the enters trigger off the stack

        // "You may sacrifice a nonland permanent."
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        // Choose the Hill Giant as the sacrifice.
        driver.submitCardSelection(me, listOf(fodder))

        // Each creature you control still in play gets a +1/+1 counter (Felothar + the Bears).
        plusCounters(driver, felotharCard) shouldBe 1
        plusCounters(driver, bears) shouldBe 1
        driver.state.getGraveyard(me).contains(fodder) shouldBe true
    }

    test("declining the optional sacrifice places no counters") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.putCreatureOnBattlefield(me, "Hill Giant")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.GREEN, 1)
        val felotharCard = driver.putCardInHand(me, "Felothar, Dawn of the Abzan")
        driver.castSpell(me, felotharCard).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        plusCounters(driver, felotharCard) shouldBe 0
        plusCounters(driver, bears) shouldBe 0
    }

    test("attacks trigger: sacrificing a nonland permanent puts a +1/+1 counter on each creature you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Felothar already in play (so its "attacks" trigger — the second triggeredAbility block —
        // is what fires), plus a second creature to verify "each creature you control" and a
        // sacrifice victim.
        val felothar = driver.putCreatureOnBattlefield(me, "Felothar, Dawn of the Abzan")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val fodder = driver.putCreatureOnBattlefield(me, "Hill Giant")
        driver.removeSummoningSickness(felothar)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(felothar), opp)
        driver.bothPass() // resolve the attacks trigger off the stack

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))

        plusCounters(driver, felothar) shouldBe 1
        plusCounters(driver, bears) shouldBe 1
        driver.state.getGraveyard(me).contains(fodder) shouldBe true
    }
})
