package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.NeutralizeTheGuards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Neutralize the Guards — {2}{B} Instant
 *
 * "Creatures target opponent controls get -1/-1 until end of turn. Surveil 2."
 *
 * Verifies the -1/-1 debuff applies only to the targeted opponent's creatures (and lethally
 * shrinks 1-toughness creatures), and that surveil 2 follows.
 */
class NeutralizeTheGuardsScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NeutralizeTheGuards)
        return driver
    }

    test("opponent's creatures get -1/-1, mine are unaffected, then surveil 2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Opponent has a 3/3 and a 1/1; the 1/1 should die to -1/-1.
        val oppCourser = driver.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3
        val oppLion = driver.putCreatureOnBattlefield(opp, "Savannah Lions")     // 1/1
        // My own creature should be untouched.
        val myCourser = driver.putCreatureOnBattlefield(me, "Centaur Courser")   // 3/3

        // Seed two known cards on top so surveil presents them.
        val top2 = driver.putCardOnTopOfLibrary(me, "Swamp")
        val top1 = driver.putCardOnTopOfLibrary(me, "Swamp")

        val spell = driver.putCardInHand(me, "Neutralize the Guards")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, spell, targets = listOf(opp)).isSuccess shouldBe true
        driver.bothPass() // resolve the spell -> applies debuff, then pauses for surveil

        // The opponent's 3/3 is now a 2/2.
        val projected = projector.project(driver.state)
        projected.getPower(oppCourser) shouldBe 2
        projected.getToughness(oppCourser) shouldBe 2

        // My 3/3 is unaffected.
        projected.getPower(myCourser) shouldBe 3
        projected.getToughness(myCourser) shouldBe 3

        // Surveil 2 pauses for the keep/graveyard choice.
        driver.isPaused shouldBe true
        val select = driver.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()
        (select as SelectCardsDecision).options.size shouldBe 2

        // Mill the top card; reorder the rest.
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(top1)))
        driver.isPaused shouldBe true
        val reorder = driver.pendingDecision as ReorderLibraryDecision
        driver.submitOrderedResponse(me, reorder.cards)
        driver.isPaused shouldBe false

        // After resolution completes, SBAs reduce the opponent's 0/0 Lions and it dies.
        driver.findPermanent(opp, "Savannah Lions") shouldBe null

        driver.getGraveyard(me).contains(top1) shouldBe true
        driver.state.getLibrary(me).first() shouldBe top2
    }
})
