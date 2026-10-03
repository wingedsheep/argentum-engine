package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Shatter
import com.wingedsheep.mtg.sets.definitions.mh3.cards.MarionetteApprentice
import com.wingedsheep.mtg.sets.definitions.wth.cards.MindStone
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Marionette Apprentice (MH3 #100) — {1}{B} 1/2 Creature — Human Artificer.
 *
 * "Fabricate 1. Whenever another creature or artifact you control is put into a graveyard from the
 *  battlefield, each opponent loses 1 life."
 */
class MarionetteApprenticeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarionetteApprentice, MindStone, Shatter))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveAll(driver: GameTestDriver) {
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        while (driver.state.stack.isNotEmpty()) {
            driver.bothPass()
            while (driver.pendingDecision != null) driver.autoResolveDecision()
        }
    }

    fun bolt(driver: GameTestDriver, target: EntityId) {
        val bolt = driver.putCardInHand(driver.player1, "Lightning Bolt")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpellWithTargets(driver.player1, bolt, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        resolveAll(driver)
    }

    fun shatter(driver: GameTestDriver, target: EntityId) {
        val spell = driver.putCardInHand(driver.player1, "Shatter")
        driver.giveMana(driver.player1, Color.RED, 2)
        driver.castSpellWithTargets(driver.player1, spell, listOf(ChosenTarget.Permanent(target))).error shouldBe null
        resolveAll(driver)
    }

    test("fabricate 1 on entering yields either a +1/+1 counter or a Servo") {
        val driver = newDriver()
        val p1 = driver.player1
        val card = driver.putCardInHand(p1, "Marionette Apprentice")
        driver.giveMana(p1, Color.BLACK, 1)
        driver.giveColorlessMana(p1, 1)
        driver.castSpell(p1, card).error shouldBe null
        resolveAll(driver)

        val apprentice = driver.findPermanent(p1, "Marionette Apprentice")!!
        val grew = driver.state.projectedState.getPower(apprentice) == 2
        val servo = driver.state.getBattlefield().any { driver.state.projectedState.hasSubtype(it, "Servo") }
        (grew xor servo) shouldBe true
    }

    test("another creature you control dying drains each opponent; an opponent's creature does not") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Marionette Apprentice")
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Savannah Lions")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Savannah Lions")

        bolt(driver, mine)
        driver.assertInGraveyard(driver.player1, "Savannah Lions")
        driver.getLifeTotal(driver.player2) shouldBe 19
        driver.getLifeTotal(driver.player1) shouldBe 20

        bolt(driver, theirs)
        driver.assertInGraveyard(driver.player2, "Savannah Lions")
        driver.getLifeTotal(driver.player2) shouldBe 19
    }

    test("a noncreature artifact you control going to the graveyard also drains") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Marionette Apprentice")
        val stone = driver.putPermanentOnBattlefield(driver.player1, "Mind Stone")

        shatter(driver, stone)
        driver.assertInGraveyard(driver.player1, "Mind Stone")
        driver.getLifeTotal(driver.player2) shouldBe 19
    }

    test("the Apprentice itself dying does not trigger") {
        val driver = newDriver()
        val apprentice = driver.putCreatureOnBattlefield(driver.player1, "Marionette Apprentice")

        bolt(driver, apprentice)
        driver.assertInGraveyard(driver.player1, "Marionette Apprentice")
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
