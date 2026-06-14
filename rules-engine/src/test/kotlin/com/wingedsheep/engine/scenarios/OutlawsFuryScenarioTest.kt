package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Outlaws' Fury — {2}{R} Instant
 *
 * "Creatures you control get +2/+0 until end of turn. If you control an outlaw, exile the
 *  top card of your library. Until the end of your next turn, you may play that card."
 *
 * Verifies the unconditional team pump, and the outlaw-gated impulse: an outlaw on the
 * battlefield exiles the top card; with no outlaw, the library is left untouched.
 */
class OutlawsFuryScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("creatures you control get +2/+0; with an outlaw, exiles the top card") {
        val driver = createDriver()
        val me = driver.player1

        // Ragavan is a Monkey Pirate — Pirate is an outlaw subtype.
        val ragavan = driver.putCreatureOnBattlefield(me, "Ragavan, Nimble Pilferer") // 2/1
        val exileBefore = driver.getExile(me).size

        val fury = driver.putCardInHand(me, "Outlaws' Fury")
        driver.giveMana(me, Color.RED, 3)
        driver.castSpell(me, fury)
        driver.bothPass() // resolve

        // +2/+0 — Ragavan is now 4/1.
        val projected = projector.project(driver.state)
        projected.getPower(ragavan) shouldBe 4
        projected.getToughness(ragavan) shouldBe 1

        // Controlling an outlaw → top card exiled (impulse).
        driver.getExile(me).size shouldBe exileBefore + 1
    }

    test("with no outlaw, no card is exiled") {
        val driver = createDriver()
        val me = driver.player1

        // A non-outlaw creature (Grizzly Bears — Bear).
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        val exileBefore = driver.getExile(me).size

        val fury = driver.putCardInHand(me, "Outlaws' Fury")
        driver.giveMana(me, Color.RED, 3)
        driver.castSpell(me, fury)
        driver.bothPass() // resolve

        // Pump still happens.
        projector.project(driver.state).getPower(bear) shouldBe 4

        // No outlaw → nothing exiled.
        driver.getExile(me).size shouldBe exileBefore
    }
})
