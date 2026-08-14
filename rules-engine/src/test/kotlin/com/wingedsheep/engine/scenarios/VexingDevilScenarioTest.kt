package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.VexingDevil
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vexing Devil — {R} 4/3 Creature — Devil
 * "When this creature enters, any opponent may have it deal 4 damage to them.
 *  If a player does, sacrifice this creature."
 *
 * The choice belongs to the *opponent*, not the Devil's controller, and accepting must both deal
 * the damage and sacrifice the Devil in one uninterruptible step (2018-12-07 ruling).
 */
class VexingDevilScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(VexingDevil)
        return driver
    }

    fun castDevil(driver: GameTestDriver) {
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val devil = driver.putCardInHand(caster, "Vexing Devil")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, devil).isSuccess shouldBe true
        // Resolve the creature spell, then the ETB trigger it puts on the stack.
        driver.bothPass()
        driver.stackSize shouldBe 1
        driver.bothPass()
    }

    test("the opponent — not the controller — is asked, and accepting deals 4 and sacrifices the Devil") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        castDevil(driver)

        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent

        driver.submitYesNo(opponent, true).isSuccess shouldBe true

        driver.getLifeTotal(opponent) shouldBe 16
        driver.getLifeTotal(caster) shouldBe 20
        driver.findPermanent(caster, "Vexing Devil") shouldBe null
        driver.getGraveyardCardNames(caster) shouldBe listOf("Vexing Devil")
    }

    test("declining leaves the Devil on the battlefield and the opponent at full life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), startingLife = 20)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)

        castDevil(driver)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opponent
        driver.submitYesNo(opponent, false).isSuccess shouldBe true

        driver.getLifeTotal(opponent) shouldBe 20
        driver.findPermanent(caster, "Vexing Devil") shouldNotBe null
        driver.getGraveyardCardNames(caster) shouldBe emptyList()
    }
})
