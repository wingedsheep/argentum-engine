package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Infernal Phantom (DSK #141) — {3}{R} Creature — Spirit 2/3.
 *
 * "Eerie — Whenever an enchantment you control enters and whenever you fully unlock a Room,
 *  this creature gets +2/+0 until end of turn.
 *  When this creature dies, it deals damage equal to its power to any target."
 *
 * Exercises the Eerie self-pump (enchantment-enters branch) feeding the dies trigger's
 * power-based damage (DynamicAmounts.sourcePower, last-known on death).
 */
class InfernalPhantomScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("when Infernal Phantom dies, it deals damage equal to its power (2) to any target") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2
        driver.setLifeTotal(opponent, 20)

        val phantom = driver.putCreatureOnBattlefield(player, "Infernal Phantom")

        // Bolt the 2/3 Phantom to its death (3 damage > 3 toughness).
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(phantom)).isSuccess shouldBe true
        driver.bothPass() // resolve the bolt — Phantom dies, queuing the dies trigger

        // Dies trigger asks for "any target" — point it at the opponent.
        driver.submitTargetSelection(player, listOf(opponent))
        driver.bothPass()

        driver.isPaused shouldBe false
        driver.findPermanent(player, "Infernal Phantom") shouldBe null
        // Base power 2 → 2 damage to the opponent.
        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("Eerie pump raises the death damage — enchantment enters, then Phantom dies for 4") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.player2
        driver.setLifeTotal(opponent, 20)

        val phantom = driver.putCreatureOnBattlefield(player, "Infernal Phantom")

        // Cast a Test Enchantment — its ETB fires the Eerie trigger (+2/+0 until end of turn).
        val enchantment = driver.putCardInHand(player, "Test Enchantment")
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, enchantment).isSuccess shouldBe true
        driver.bothPass() // resolve the enchantment — ETB queues the Eerie trigger
        driver.bothPass() // resolve the Eerie self-pump (now a 4/3)

        // Kill the (now 4/3) Phantom.
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(phantom)).isSuccess shouldBe true
        driver.bothPass() // resolve the bolt — Phantom dies, queuing the dies trigger

        driver.submitTargetSelection(player, listOf(opponent))
        driver.bothPass()

        driver.isPaused shouldBe false
        driver.findPermanent(player, "Infernal Phantom") shouldBe null
        // Pumped power 4 → 4 damage to the opponent.
        driver.getLifeTotal(opponent) shouldBe 16
    }
})
