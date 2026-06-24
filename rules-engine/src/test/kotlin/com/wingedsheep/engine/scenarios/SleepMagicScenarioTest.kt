package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sleep Magic (FIN #74) — {U} Enchantment — Aura.
 *
 * "Enchant creature
 *  When this Aura enters, tap enchanted creature.
 *  Enchanted creature doesn't untap during its controller's untap step.
 *  When enchanted creature is dealt damage, sacrifice this Aura."
 *
 * Verifies the ETB tap and the "released on damage" trigger: when the enchanted creature is dealt
 * damage (here by a Shock), the Aura is sacrificed.
 */
class SleepMagicScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    // Resolve everything on the stack (the spell, then any triggered abilities it produced).
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard < 20) {
            bothPass()
            guard++
        }
    }

    test("ETB taps the enchanted creature, and damaging it sacrifices the Aura") {
        val driver = newDriver()
        val creature = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        val aura = driver.putCardInHand(driver.player1, "Sleep Magic")
        driver.giveMana(driver.player1, Color.BLUE, 1)
        driver.castSpell(driver.player1, aura, listOf(creature)).isSuccess shouldBe true
        driver.resolveStack()

        // ETB trigger taps the enchanted creature, and the Aura is attached.
        driver.isTapped(creature) shouldBe true
        driver.findPermanent(driver.player1, "Sleep Magic") shouldBe aura

        // Deal damage to the enchanted creature: Shock it.
        val shock = driver.putCardInHand(driver.player1, "Shock")
        driver.giveMana(driver.player1, Color.RED, 1)
        driver.castSpell(driver.player1, shock, listOf(creature)).isSuccess shouldBe true
        driver.resolveStack()

        // The "when enchanted creature is dealt damage" trigger sacrifices the Aura.
        driver.findPermanent(driver.player1, "Sleep Magic") shouldBe null
    }
})
