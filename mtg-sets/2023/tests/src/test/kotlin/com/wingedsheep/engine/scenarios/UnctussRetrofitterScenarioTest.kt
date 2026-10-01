package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.UnctussRetrofitter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unctus's Retrofitter (ONE #76) — {2}{U} 2/3 Phyrexian Artificer, toxic 1.
 *
 * "When this creature enters, up to one target artifact you control becomes an artifact creature
 *  with base power and toughness 4/4 for as long as this creature remains on the battlefield."
 */
class UnctussRetrofitterScenarioTest : FunSpec({

    val Trinket = card("Retrofit Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UnctussRetrofitter, Trinket))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val trinket = driver.putPermanentOnBattlefield(me, "Retrofit Trinket")
        val retrofitter = driver.putCardInHand(me, "Unctus's Retrofitter")
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, retrofitter)
        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        return Triple(driver, me, trinket)
    }

    test("the targeted artifact becomes a 4/4 artifact creature") {
        val (driver, me, trinket) = setup()
        driver.submitTargetSelection(me, listOf(trinket))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.isCreature(trinket) shouldBe true
        projected.hasType(trinket, "ARTIFACT") shouldBe true
        projected.getPower(trinket) shouldBe 4
        projected.getToughness(trinket) shouldBe 4
    }

    test("the animation ends when the Retrofitter leaves the battlefield") {
        val (driver, me, trinket) = setup()
        driver.submitTargetSelection(me, listOf(trinket))
        driver.bothPass()
        driver.state.projectedState.isCreature(trinket) shouldBe true

        driver.moveToGraveyard(driver.findPermanent(me, "Unctus's Retrofitter")!!)

        val projected = driver.state.projectedState
        projected.isCreature(trinket) shouldBe false
        projected.hasType(trinket, "ARTIFACT") shouldBe true
    }

    test("choosing no target animates nothing") {
        val (driver, me, trinket) = setup()
        driver.submitTargetSelection(me, emptyList())
        driver.bothPass()

        driver.state.projectedState.isCreature(trinket) shouldBe false
    }
})
