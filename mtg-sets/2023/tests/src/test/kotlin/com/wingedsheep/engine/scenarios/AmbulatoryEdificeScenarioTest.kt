package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.AmbulatoryEdifice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ambulatory Edifice (ONE #79) — "When this creature enters, you may pay 2 life. When you do,
 * target creature gets -1/-1 until end of turn."
 */
class AmbulatoryEdificeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AmbulatoryEdifice))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castEdifice(driver: GameTestDriver, me: EntityId) {
        val card = driver.putCardInHand(me, "Ambulatory Edifice")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell
        driver.bothPass() // resolve the ETB trigger
    }

    test("paying 2 life gives target creature -1/-1 until end of turn via a reflexive trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        castEdifice(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true).error shouldBe null

        // Life is paid before the reflexive trigger's target is chosen.
        driver.getLifeTotal(me) shouldBe 18
        val decision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitDecision(me, TargetsResponse(decision.id, mapOf(0 to listOf(bears)))).error shouldBe null
        driver.bothPass() // resolve the reflexive trigger

        driver.state.projectedState.getPower(bears) shouldBe 1
        driver.state.projectedState.getToughness(bears) shouldBe 1

        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("declining to pay leaves life and creatures untouched") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        castEdifice(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false).error shouldBe null

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 20
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("with less than 2 life the payment is not offered") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.setLifeTotal(me, 1)

        castEdifice(driver, me)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 1
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }
})
