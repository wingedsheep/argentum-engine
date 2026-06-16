package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AnotherRound
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Another Round — {X}{X}{2}{W} Sorcery.
 *
 * "Exile any number of creatures you control, then return them to the battlefield under their
 *  owner's control. Then repeat this process X more times."
 *
 * The blink is composed atomically (gather creatures you control → choose any number → exile
 * linked → return from linked exile under owners' control). The process runs X + 1 times total.
 */
class OtjAnotherRoundScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AnotherRound)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Cast Another Round paying its {X}{X}{2}{W} cost from an explicit mana pool. With X=N the
    // generic + colored remainder is {2}{W} plus 2N for the doubled X.
    fun GameTestDriver.castAnotherRound(player: EntityId, spell: EntityId, xValue: Int) {
        giveMana(player, Color.WHITE, 1)            // {W}
        giveColorlessMana(player, 2 + 2 * xValue)   // {2} + {X}{X}
        submit(CastSpell(playerId = player, cardId = spell, xValue = xValue))
    }

    test("X=0 blinks a chosen creature once and returns it") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Another Round")
        driver.castAnotherRound(me, spell, xValue = 0)
        driver.bothPass() // begin resolving -> the (single) choose-any-number decision

        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        driver.submitCardSelection(me, listOf(bear))

        // The creature returned to the battlefield (new object), still controlled by me.
        val returned = driver.findPermanent(me, "Grizzly Bears")
        returned shouldNotBe null
        driver.getController(returned!!) shouldBe me
    }

    test("X=0 may exile no creatures (choose none is legal)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Another Round")
        driver.castAnotherRound(me, spell, xValue = 0)
        driver.bothPass()

        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        decision.minSelections shouldBe 0
        driver.submitCardSelection(me, emptyList())

        // Nothing was exiled; the bear is still on the battlefield.
        driver.findPermanent(me, "Grizzly Bears") shouldNotBe null
    }

    test("X=1 performs the process twice (a second choose-any-number decision)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Another Round")
        driver.castAnotherRound(me, spell, xValue = 1)
        driver.bothPass() // first round's choose decision

        val first = driver.pendingDecision
        (first is SelectCardsDecision) shouldBe true
        driver.submitCardSelection(me, listOf(bear))

        // After returning, the second round prompts again.
        val second = driver.pendingDecision
        (second is SelectCardsDecision) shouldBe true
        second as SelectCardsDecision
        val bear2 = driver.findPermanent(me, "Grizzly Bears")!!
        driver.submitCardSelection(me, listOf(bear2))

        driver.findPermanent(me, "Grizzly Bears") shouldNotBe null
    }
})
