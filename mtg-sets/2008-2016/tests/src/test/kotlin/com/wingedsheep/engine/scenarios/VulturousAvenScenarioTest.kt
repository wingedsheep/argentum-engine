package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.VulturousAven
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vulturous Aven (DTK #126) — {3}{B} 2/3 Flying Bird Shaman.
 *
 * "Exploit. When this creature exploits a creature, you draw two cards and you lose 2 life."
 *
 * Pins that exploiting draws exactly two and costs exactly 2 life (the controller's, not the
 * opponent's), that it still pays off when the Aven exploits itself, and that declining does nothing.
 */
class VulturousAvenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VulturousAven))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castAven(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Vulturous Aven")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting another creature draws two cards and loses 2 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castAven(driver, me)
        val handBefore = driver.getHandSize(me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass() // resolve the reflexive draw-and-drain

        driver.getGraveyard(me) shouldContain fodder
        driver.getHandSize(me) shouldBe handBefore + 2
        driver.getLifeTotal(me) shouldBe 18
        driver.getLifeTotal(opp) shouldBe 20
    }

    test("exploiting itself still draws two and loses 2 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val aven = castAven(driver, me)
        val handBefore = driver.getHandSize(me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true) // the Aven is my only creature -> sacrifices itself
        driver.bothPass()

        driver.getGraveyard(me) shouldContain aven
        driver.getHandSize(me) shouldBe handBefore + 2
        driver.getLifeTotal(me) shouldBe 18
    }

    test("declining the exploit sacrifice draws nothing and loses no life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castAven(driver, me)
        val handBefore = driver.getHandSize(me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.pendingDecision shouldBe null
        driver.getHandSize(me) shouldBe handBefore
        driver.getLifeTotal(me) shouldBe 20
    }
})
