package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m14.cards.StaffOfTheDeathMagus
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Staff of the Death Magus is one triggered ability over two events — `Triggers.or` of a spell-cast
 * atom and a land-enters atom — so both halves are exercised here, plus the misses on each axis.
 */
class StaffOfTheDeathMagusScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(StaffOfTheDeathMagus)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(you, "Staff of the Death Magus")
        return driver to you
    }

    test("a Swamp you control entering gains 1 life") {
        val (driver, you) = setup()

        driver.playLand(you, driver.putCardInHand(you, "Swamp"))
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 21
    }

    test("a land without the Swamp type does not trigger") {
        val (driver, you) = setup()

        driver.playLand(you, driver.putCardInHand(you, "Caves of Koilos"))

        driver.stackSize shouldBe 0
        driver.getLifeTotal(you) shouldBe 20
    }

    test("casting a black spell gains 1 life; a nonblack spell does not") {
        val (driver, you) = setup()

        val black = driver.putCardInHand(you, "Black Creature")
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, black)
        driver.stackSize shouldBe 2 // the creature spell and the Staff trigger above it
        driver.bothPass() // resolve the Staff trigger
        driver.getLifeTotal(you) shouldBe 21
        driver.bothPass() // resolve the creature

        val green = driver.putCardInHand(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveColorlessMana(you, 2)
        driver.castSpell(you, green)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(you) shouldBe 21
    }
})
