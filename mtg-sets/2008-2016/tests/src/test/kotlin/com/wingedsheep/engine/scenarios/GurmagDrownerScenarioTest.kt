package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.GurmagDrowner
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gurmag Drowner (DTK #57) — {3}{U} 2/4 Snake Wizard.
 *
 * "Exploit. When this creature exploits a creature, look at the top four cards of your library.
 * Put one of them into your hand and the rest into your graveyard."
 *
 * Pins that exploiting offers exactly the top four cards, the chosen one goes to hand and the other
 * three to the graveyard, and that declining the sacrifice looks at nothing.
 */
class GurmagDrownerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GurmagDrowner))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castDrowner(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Gurmag Drowner")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting a creature puts one of the top four into hand and the other three into the graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castDrowner(driver, me)

        val topFour = driver.state.getLibrary(me).take(4)
        val librarySizeBefore = driver.state.getLibrary(me).size
        val handBefore = driver.getHand(me).size

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass() // resolve the reflexive "when this creature exploits" payoff

        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options.toSet() shouldBe topFour.toSet()
        pick.minSelections shouldBe 1
        pick.maxSelections shouldBe 1
        val kept = topFour.first()
        driver.submitCardSelection(me, listOf(kept))

        driver.getHand(me) shouldContain kept
        driver.getHand(me).size shouldBe handBefore + 1
        driver.getGraveyard(me) shouldContainAll (topFour - kept)
        driver.getGraveyard(me) shouldContain fodder
        driver.state.getLibrary(me).size shouldBe librarySizeBefore - 4
    }

    test("declining the exploit sacrifice looks at nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castDrowner(driver, me)

        val librarySizeBefore = driver.state.getLibrary(me).size
        val handBefore = driver.getHand(me).size

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.pendingDecision shouldBe null
        driver.getHand(me).size shouldBe handBefore
        driver.state.getLibrary(me).size shouldBe librarySizeBefore
    }
})
