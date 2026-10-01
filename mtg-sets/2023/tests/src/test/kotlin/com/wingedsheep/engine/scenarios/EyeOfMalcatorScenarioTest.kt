package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Eye of Malcator (ONE #50).
 *
 * "Whenever another artifact you control enters, this artifact becomes a 4/4 Phyrexian Eye
 * artifact creature until end of turn."
 */
class EyeOfMalcatorScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("another artifact entering animates it into a 4/4 Phyrexian Eye until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val eye = driver.putPermanentOnBattlefield(me, "Eye of Malcator")

        projector.project(driver.state).hasType(eye, "CREATURE") shouldBe false

        val thopter = driver.putCardInHand(me, "Ornithopter")
        driver.castSpell(me, thopter)
        driver.bothPass() // Ornithopter resolves, trigger goes on the stack
        driver.stackSize shouldBe 1
        driver.bothPass() // trigger resolves

        val projected = projector.project(driver.state)
        projected.hasType(eye, "CREATURE") shouldBe true
        projected.hasType(eye, "ARTIFACT") shouldBe true
        projected.hasSubtype(eye, "PHYREXIAN") shouldBe true
        projected.hasSubtype(eye, "EYE") shouldBe true
        projected.getPower(eye) shouldBe 4
        projected.getToughness(eye) shouldBe 4

        driver.passPriorityUntil(Step.UPKEEP)

        val nextTurn = projector.project(driver.state)
        nextTurn.hasType(eye, "CREATURE") shouldBe false
        nextTurn.hasType(eye, "ARTIFACT") shouldBe true
    }

    test("a nonartifact entering does not animate it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val eye = driver.putPermanentOnBattlefield(me, "Eye of Malcator")

        val bears = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bears)
        driver.bothPass()

        driver.stackSize shouldBe 0
        projector.project(driver.state).hasType(eye, "CREATURE") shouldBe false
    }

    test("casting it scries 2 on entry") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val eye = driver.putCardInHand(me, "Eye of Malcator")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, eye).error shouldBe null
        driver.bothPass() // Eye resolves, its enters trigger goes on the stack
        driver.stackSize shouldBe 1
        driver.bothPass() // scry trigger resolves

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("an artifact entering under an opponent's control does not animate it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val eye = driver.putPermanentOnBattlefield(me, "Eye of Malcator")

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe opponent

        val thopter = driver.putCardInHand(opponent, "Ornithopter")
        driver.castSpell(opponent, thopter)
        driver.bothPass()

        driver.stackSize shouldBe 0
        projector.project(driver.state).hasType(eye, "CREATURE") shouldBe false
    }
})
