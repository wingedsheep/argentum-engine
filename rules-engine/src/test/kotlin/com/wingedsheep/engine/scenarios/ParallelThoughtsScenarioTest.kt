package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ParallelThoughtsScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true,
            startingPlayer = 0
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drainStack(maxIterations: Int = 30) {
        var guard = 0
        while (guard++ < maxIterations && state.stack.isNotEmpty() && !isPaused) {
            bothPass()
        }
    }

    fun GameTestDriver.findInHand(name: String): EntityId? =
        getHand(player1).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    fun GameTestDriver.isInHand(id: EntityId): Boolean =
        getHand(player1).contains(id)

    fun GameTestDriver.isInExile(id: EntityId): Boolean =
        state.getExile(player1).contains(id)

    test("accepting the replacement puts the top card of the exiled pile into hand instead of drawing") {
        val driver = newDriver()
        val me = driver.player1

        val pt = driver.putPermanentOnBattlefield(me, "Parallel Thoughts")
        val exiled1 = driver.putCardInExile(me, "Mountain")
        val exiled2 = driver.putCardInExile(me, "Plains")

        driver.replaceState(driver.state.updateEntity(pt) { c ->
            c.with(LinkedExileComponent(listOf(exiled1, exiled2)))
        })

        val libSize = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 3)
        val spell = driver.putCardInHand(me, "Counsel of the Soratami")
        driver.castSpell(me, spell)
        driver.drainStack()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.drainStack()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.drainStack()

        // Both exiled cards in hand, library untouched
        driver.isInHand(exiled1) shouldBe true
        driver.isInHand(exiled2) shouldBe true
        driver.state.getLibrary(me).size shouldBe libSize

        // Linked exile on PT is empty
        val linked = driver.state.getEntity(pt)?.get<LinkedExileComponent>()
        linked shouldNotBe null
        linked!!.exiledIds shouldBe emptyList()
    }

    test("declining the replacement draws from library normally") {
        val driver = newDriver()
        val me = driver.player1

        val pt = driver.putPermanentOnBattlefield(me, "Parallel Thoughts")
        val exiled = driver.putCardInExile(me, "Mountain")

        driver.replaceState(driver.state.updateEntity(pt) { c ->
            c.with(LinkedExileComponent(listOf(exiled)))
        })

        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 3)
        val spell = driver.putCardInHand(me, "Counsel of the Soratami")
        driver.castSpell(me, spell)
        driver.drainStack()

        // 1st draw: decline
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)
        driver.drainStack()

        // 2nd draw: decline
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)
        driver.drainStack()

        // Two library cards drawn
        driver.state.getLibrary(me).size shouldBe libBefore - 2

        // Exiled card still in exile
        driver.isInExile(exiled) shouldBe true
    }

    test("accepting with an empty exiled pile draws no card") {
        val driver = newDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Parallel Thoughts")

        val handBefore = driver.getHandSize(me)
        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 3)
        val spell = driver.putCardInHand(me, "Counsel of the Soratami")
        driver.castSpell(me, spell)
        driver.drainStack()

        // 1st draw: accept (empty pile → no card)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.drainStack()

        // 2nd draw: accept (empty pile → no card)
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.drainStack()

        // Neither draw added a card
        driver.getHandSize(me) shouldBe handBefore
        driver.state.getLibrary(me).size shouldBe libBefore
    }

    test("drawing multiple cards prompts for each draw, decisions can differ") {
        val driver = newDriver()
        val me = driver.player1

        val pt = driver.putPermanentOnBattlefield(me, "Parallel Thoughts")
        val exiled = driver.putCardInExile(me, "Mountain")

        driver.replaceState(driver.state.updateEntity(pt) { c ->
            c.with(LinkedExileComponent(listOf(exiled)))
        })

        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 3)
        val spell = driver.putCardInHand(me, "Counsel of the Soratami")
        driver.castSpell(me, spell)
        driver.drainStack()

        // 1st draw: accept → exiled card to hand
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.drainStack()

        driver.isInHand(exiled) shouldBe true

        // 2nd draw: decline → draw from library
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)
        driver.drainStack()

        driver.state.getLibrary(me).size shouldBe libBefore - 1
    }
})
