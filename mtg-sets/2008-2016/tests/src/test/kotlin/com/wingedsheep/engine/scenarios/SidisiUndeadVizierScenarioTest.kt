package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SidisiUndeadVizier
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sidisi, Undead Vizier (DTK #120) — {3}{B}{B} 4/6 legendary Zombie Snake, deathtouch.
 *
 * "Exploit. When Sidisi exploits a creature, you may search your library for a card, put it into
 * your hand, then shuffle."
 *
 * Pins that exploiting offers the whole library and the chosen card lands in hand, and that the
 * tutor is optional ("you may") even after the sacrifice.
 */
class SidisiUndeadVizierScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SidisiUndeadVizier))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castSidisi(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Sidisi, Undead Vizier")
        driver.giveMana(me, Color.BLACK, 2)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting a creature tutors any card from the library into hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castSidisi(driver, me)

        val library = driver.state.getLibrary(me)
        val handBefore = driver.getHand(me).size

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass() // resolve the reflexive payoff -> "you may search"

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        val search = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.options.toSet() shouldBe library.toSet()
        val wanted = library.last() // the bottom card — proves it is a search, not a look at the top
        driver.submitCardSelection(me, listOf(wanted))

        driver.getGraveyard(me) shouldContain fodder
        driver.getHand(me) shouldContain wanted
        driver.getHand(me).size shouldBe handBefore + 1
        driver.state.getLibrary(me).size shouldBe library.size - 1
    }

    test("the search is optional even after exploiting") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        castSidisi(driver, me)

        val librarySizeBefore = driver.state.getLibrary(me).size
        val handBefore = driver.getHand(me).size

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.pendingDecision shouldBe null
        driver.getGraveyard(me) shouldContain fodder
        driver.getHand(me).size shouldBe handBefore
        driver.state.getLibrary(me).size shouldBe librarySizeBefore
    }
})
