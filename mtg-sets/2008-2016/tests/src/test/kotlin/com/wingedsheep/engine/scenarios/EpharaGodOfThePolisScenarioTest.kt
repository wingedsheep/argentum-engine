package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.EpharaGodOfThePolis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ephara, God of the Polis (Born of the Gods #145) — "At the beginning of each upkeep, if you had
 * another creature enter the battlefield under your control last turn, draw a card."
 */
class EpharaGodOfThePolisScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + EpharaGodOfThePolis)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castLions(me: EntityId) {
        giveMana(me, Color.WHITE, 1)
        castSpell(me, putCardInHand(me, "Savannah Lions")).outcome shouldBe Outcome.Done
        bothPass()
        findPermanent(me, "Savannah Lions").shouldNotBeNull()
    }

    test("a creature entering on your turn draws a card on the next upkeep, the opponent's") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Ephara, God of the Polis")
        driver.castLions(me)

        driver.passPriorityUntil(Step.END)
        val handBefore = driver.getHandSize(me)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe driver.getOpponent(me)
        driver.bothPass() // resolve Ephara's trigger
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("nothing entering last turn means no draw, even if a creature entered two turns ago") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Ephara, God of the Polis")
        driver.castLions(me)

        driver.passPriorityUntil(Step.UPKEEP) // opponent's upkeep: trigger from my turn
        driver.bothPass()
        driver.passPriorityUntil(Step.END) // opponent's turn passes with no entry for me
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        driver.state.stack.size shouldBe 0
    }

    test("an opponent's creature entering doesn't count") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(opponent, "Ephara, God of the Polis")
        driver.castLions(me)

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.stack.size shouldBe 0
    }

    test("Ephara entering as a creature herself is not another creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        // Devotion to white and blue of 5 from the board, + Ephara's {W}{U} = 7: she enters a creature.
        driver.putPermanentOnBattlefield(me, "Phantom Warrior")
        driver.putPermanentOnBattlefield(me, "Phantom Warrior")
        driver.putPermanentOnBattlefield(me, "Savannah Lions")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 2)
        val ephara = driver.putCardInHand(me, "Ephara, God of the Polis")
        driver.castSpell(me, ephara).outcome shouldBe Outcome.Done
        driver.bothPass()
        val onBattlefield = driver.findPermanent(me, "Ephara, God of the Polis")!!
        driver.state.projectedState.isCreature(onBattlefield) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe driver.getOpponent(me)
        driver.state.stack.size shouldBe 0
    }
})
