package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.ElderscaleWurm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Elderscale Wurm (M13 #167) — "When this creature enters, if your life total is less than 7,
 * your life total becomes 7. As long as you have 7 or more life, damage that would reduce your
 * life total to less than 7 reduces it to 7 instead."
 *
 * Pins the intervening-if on the enter trigger and the floor's 7-life gate: at 6 or less, damage
 * reduces your life total as normal.
 */
class ElderscaleWurmScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ElderscaleWurm))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.boltYourself() {
        val you = activePlayer!!
        giveMana(you, Color.RED, 1)
        val bolt = putCardInHand(you, "Lightning Bolt")
        castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you))).error shouldBe null
        bothPass()
    }

    test("entering with less than 7 life sets your life total to 7") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.setLifeTotal(you, 3)

        driver.giveMana(you, Color.GREEN, 7)
        val wurm = driver.putCardInHand(you, "Elderscale Wurm")
        driver.castSpell(you, wurm).error shouldBe null
        driver.bothPass() // creature spell resolves, trigger goes on the stack
        driver.bothPass() // trigger resolves

        driver.assertLifeTotal(you, 7)
    }

    test("entering with 7 or more life leaves your life total alone") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.setLifeTotal(you, 12)

        driver.giveMana(you, Color.GREEN, 7)
        val wurm = driver.putCardInHand(you, "Elderscale Wurm")
        driver.castSpell(you, wurm).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.assertLifeTotal(you, 12)
    }

    test("damage that would take you below 7 leaves you at 7") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Elderscale Wurm")
        driver.setLifeTotal(you, 8)

        driver.boltYourself()

        driver.assertLifeTotal(you, 7)
    }

    test("with less than 7 life, damage reduces your life total as normal") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCreatureOnBattlefield(you, "Elderscale Wurm")
        driver.setLifeTotal(you, 6)

        driver.boltYourself()

        driver.assertLifeTotal(you, 3)
    }
})
