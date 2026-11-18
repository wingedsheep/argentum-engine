package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m10.cards.PlanarCleansing
import com.wingedsheep.mtg.sets.definitions.m10.cards.TomeScour
import com.wingedsheep.mtg.sets.definitions.m15.cards.ProfaneMemento
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Profane Memento — "Whenever a creature card is put into an opponent's graveyard from anywhere,
 * you gain 1 life."
 *
 * Pins the "from anywhere" reach (a dying creature and milled creature cards both count, your own
 * don't) and the CR 603.6c ruling: a "from anywhere" ability is never a leaves-the-battlefield
 * ability, so Profane Memento destroyed at the same time as an opponent's creature doesn't look
 * back in time and doesn't trigger.
 */
class ProfaneMementoScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ProfaneMemento, TomeScour, PlanarCleansing))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an opponent's creature dying gains 1 life; your own creature dying doesn't") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Profane Memento")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")
        val mine = driver.putCreatureOnBattlefield(me, "Savannah Lions")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(theirs)).outcome shouldBe Outcome.Done
        driver.bothPass() // Bolt resolves; the Lions die
        driver.bothPass() // Profane Memento's trigger resolves
        withClue("an opponent's creature card went to their graveyard") {
            driver.getLifeTotal(me) shouldBe 21
        }

        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(mine)).outcome shouldBe Outcome.Done
        driver.bothPass()
        withClue("my own creature card is not in an opponent's graveyard") {
            driver.state.stack.isEmpty() shouldBe true
            driver.getLifeTotal(me) shouldBe 21
        }
    }

    test("milled creature cards count — each one put into an opponent's graveyard from their library") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Profane Memento")
        driver.putCardOnTopOfLibrary(opponent, "Savannah Lions")
        driver.putCardOnTopOfLibrary(opponent, "Centaur Courser")

        val scour = driver.putCardInHand(me, "Tome Scour")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, scour, targets = listOf(opponent)).outcome shouldBe Outcome.Done
        driver.bothPass() // Tome Scour mills five: two creature cards and three Mountains
        driver.bothPass() // first Memento trigger
        driver.bothPass() // second Memento trigger
        driver.getLifeTotal(me) shouldBe 22
    }

    test("destroyed at the same time as an opponent's creature, it doesn't look back and doesn't trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val memento = driver.putPermanentOnBattlefield(me, "Profane Memento")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Savannah Lions")

        val cleansing = driver.putCardInHand(me, "Planar Cleansing")
        driver.giveMana(me, Color.WHITE, 6)
        driver.castSpell(me, cleansing).outcome shouldBe Outcome.Done
        driver.bothPass() // Planar Cleansing destroys every nonland permanent at once

        withClue("both left the battlefield together") {
            driver.state.getBattlefield().contains(memento) shouldBe false
            driver.state.getBattlefield().contains(theirs) shouldBe false
        }
        withClue("CR 603.6c: a 'from anywhere' trigger is never a leaves-the-battlefield ability") {
            driver.state.stack.isEmpty() shouldBe true
            driver.getLifeTotal(me) shouldBe 20
        }
    }
})
