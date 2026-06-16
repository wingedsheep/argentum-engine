package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ThisTownAintBigEnough
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * This Town Ain't Big Enough (OTJ #74) — {4}{U} Instant.
 *
 *   "This spell costs {3} less to cast if it targets a permanent you control.
 *    Return up to two target nonland permanents to their owners' hands."
 *
 * Verifies: returns two targets to their owners' hands; the {3} reduction applies when a target is
 * a permanent you control (castable with only {U} + {1} on the board).
 */
class ThisTownAintBigEnoughScenarioTest : FunSpec({

    fun GameTestDriver.inHand(playerId: EntityId, name: String): Boolean =
        getHand(playerId).any { state.getEntity(it)?.get<CardComponent>()?.name == name }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThisTownAintBigEnough))
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("returns up to two nonland permanents to their owners' hands") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val bears1 = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val bears2 = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val card = driver.putCardInHand(me, "This Town Ain't Big Enough")
        // Full cost {4}{U}; no own-permanent target here.
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, card, targets = listOf(bears1, bears2))
        driver.bothPass()

        // Both creatures returned.
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(opp, com.wingedsheep.sdk.core.Zone.BATTLEFIELD))
            .contains(bears1) shouldBe false
        driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(opp, com.wingedsheep.sdk.core.Zone.BATTLEFIELD))
            .contains(bears2) shouldBe false
        driver.inHand(opp, "Grizzly Bears") shouldBe true
    }

    test("costs {3} less when it targets a permanent you control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val myBears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val card = driver.putCardInHand(me, "This Town Ain't Big Enough")
        // Reduced cost: {4}{U} - {3} = {1}{U}. Provide exactly that.
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, card, targets = listOf(myBears, oppBears))
        driver.bothPass()

        // Spell resolved (was affordable at the reduced cost) and bounced both.
        driver.inHand(me, "Grizzly Bears") shouldBe true
        driver.inHand(opp, "Grizzly Bears") shouldBe true
    }
})
