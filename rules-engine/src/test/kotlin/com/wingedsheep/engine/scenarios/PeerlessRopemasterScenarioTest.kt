package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.PeerlessRopemaster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Peerless Ropemaster (OTJ #60) — {4}{U} Creature — Human Rogue 4/4.
 *
 * "When this creature enters, return up to one target tapped creature to its owner's hand."
 *
 * Verifies the ETB bounces a chosen tapped creature back to its owner's hand, and that "up to one"
 * is optional — declining the target leaves the board untouched.
 */
class PeerlessRopemasterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PeerlessRopemaster)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("ETB returns a chosen tapped creature to its owner's hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // An opponent's tapped creature to bounce.
        val bear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.tapPermanent(bear)

        val rope = driver.putCardInHand(me, "Peerless Ropemaster")
        driver.giveMana(me, Color.BLUE, 5)
        driver.castSpell(me, rope).isSuccess shouldBe true
        driver.bothPass() // resolve the creature spell -> enters -> ETB trigger on stack

        // The ETB targets up to one tapped creature.
        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass() // resolve the ETB return

        // Bear is back in the opponent's hand, off the battlefield.
        driver.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)).contains(bear) shouldBe false
        driver.getHand(opp).contains(bear) shouldBe true
    }

    test("up to one is optional — declining returns nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.tapPermanent(bear)

        val rope = driver.putCardInHand(me, "Peerless Ropemaster")
        driver.giveMana(me, Color.BLUE, 5)
        driver.castSpell(me, rope).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        // Decline: choose no targets.
        driver.submitTargetSelection(me, emptyList())
        driver.bothPass()

        // Nothing was returned.
        driver.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)).contains(bear) shouldBe true
        driver.getHand(opp).contains(bear) shouldBe false
    }
})
