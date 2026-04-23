package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Reproduces: "a planeswalker doesn't appear the turn you put it on the battlefield".
 * Cast Ajani from hand, resolve, and verify he's in the owner's battlefield zone
 * with starting loyalty.
 */
class AjaniCastBugTest : FunSpec({

    test("Ajani, Outland Chaperone cast from hand ends up on the battlefield with 3 loyalty") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)

        val player = driver.activePlayer!!

        // Give P1 three Plains on the battlefield and Ajani in hand.
        driver.putPermanentOnBattlefield(player, "Plains")
        driver.putPermanentOnBattlefield(player, "Plains")
        driver.putPermanentOnBattlefield(player, "Plains")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ajani = driver.putCardInHand(player, "Ajani, Outland Chaperone")

        val castResult = driver.castSpell(player, ajani)
        castResult.isSuccess shouldBe true

        // Resolve the spell.
        driver.bothPass()

        // Ajani should now be on player's battlefield zone.
        val battlefield = driver.state.getZone(player, Zone.BATTLEFIELD)
        println("Battlefield after resolving Ajani: $battlefield")
        println("Ajani entity: ${driver.state.getEntity(ajani)}")
        (ajani in battlefield) shouldBe true

        // And with 3 loyalty counters.
        val counters = driver.state.getEntity(ajani)?.get<CountersComponent>()
        (counters?.getCount(CounterType.LOYALTY) ?: 0) shouldBe 3
    }
})
